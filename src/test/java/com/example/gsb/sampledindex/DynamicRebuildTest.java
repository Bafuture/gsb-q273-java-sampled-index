package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** 动态调整采样比例：重建期间旧索引仍可用，重建完成后原子切换。 */
class DynamicRebuildTest {

    private static List<DataRecord> data(String keyPrefix, int size, int idOffset) {
        List<DataRecord> records = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            records.add(new DataRecord(idOffset + i, keyPrefix, "s"));
        }
        return records;
    }

    private static final class BlockingSampler implements Sampler {
        private final RandomSampler delegate = new RandomSampler();
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private int invocations;

        @Override
        public List<DataRecord> sample(List<DataRecord> data, double ratio, Random random) {
            boolean secondInvocation;
            synchronized (this) {
                invocations++;
                secondInvocation = invocations == 2;
            }
            if (secondInvocation) {
                entered.countDown();
                try {
                    assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            return delegate.sample(data, ratio, random);
        }

        @Override
        public String name() {
            return "random";
        }

        void awaitEntered() throws InterruptedException {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        }

        void release() {
            release.countDown();
        }
    }

    @Test
    void oldIndexStaysServingDuringRebuildThenSwapsAtomically() throws Exception {
        BlockingSampler sampler = new BlockingSampler();
        SampledIndexService service =
                new SampledIndexService(sampler, data("old-key", 100, 0), 0.5, 0L);

        Thread rebuildThread = new Thread(() ->
                service.rebuild(data("new-key", 100, 10_000), 0.8));
        rebuildThread.start();

        sampler.awaitEntered();

        // 重建进行中：旧索引仍正常服务
        assertThat(service.query("old-key").results()).hasSize(50);
        assertThat(service.query("new-key").results()).isEmpty();
        assertThat(service.stats().rebuildCount()).isZero();

        sampler.release();
        rebuildThread.join(5000);
        assertThat(rebuildThread.isAlive()).isFalse();

        // 切换完成：新索引生效
        assertThat(service.query("new-key").results()).hasSize(80);
        assertThat(service.query("old-key").results()).isEmpty();
        assertThat(service.stats().rebuildCount()).isEqualTo(1);
        assertThat(service.stats().currentRatio()).isEqualTo(0.8);

        // 仅调整比例：重新抽样
        service.resample(1.0);
        assertThat(service.query("new-key").results()).hasSize(100);
        assertThat(service.query("new-key").possiblyIncomplete()).isFalse();
        assertThat(service.stats().rebuildCount()).isEqualTo(2);
        assertThat(service.stats().currentRatio()).isEqualTo(1.0);
    }
}
