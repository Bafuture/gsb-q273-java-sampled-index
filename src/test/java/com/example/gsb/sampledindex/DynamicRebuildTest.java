package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DynamicRebuildTest {

    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void oldIndexStillServesQueriesWhileRebuilding() throws Exception {
        executor = Executors.newSingleThreadExecutor();
        SampledIndexService service = new SampledIndexService(executor);

        List<Document> v1 = List.of(
                Document.of("old-1", "version oldword", Map.of("ver", "v1")),
                Document.of("old-2", "version oldword", Map.of("ver", "v1")));
        List<Document> v2 = List.of(
                Document.of("new-1", "version newword", Map.of("ver", "v2")),
                Document.of("new-2", "version newword", Map.of("ver", "v2")));

        service.rebuild(v1, new RandomSampler(1.0, 1));
        assertThat(service.query(List.of("oldword")).matchedIds()).containsExactlyInAnyOrder("old-1", "old-2");

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Sampler blockingSampler = population -> {
            started.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new RandomSampler(1.0, 1).sample(population);
        };

        CompletableFuture<SampledIndex> rebuild = service.rebuildAsync(v2, blockingSampler);
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        // 重建进行中：旧索引仍然可用，且结果不包含新数据
        QueryResult during = service.query(List.of("version"));
        assertThat(during.matchedIds()).containsExactlyInAnyOrder("old-1", "old-2");
        assertThat(during.possiblyIncomplete()).isTrue();
        assertThat(service.stats().rebuildCount()).isEqualTo(1);

        release.countDown();
        rebuild.get(5, TimeUnit.SECONDS);

        QueryResult after = service.query(List.of("newword"));
        assertThat(after.matchedIds()).containsExactlyInAnyOrder("new-1", "new-2");
        assertThat(service.query(List.of("oldword")).matchedIds()).isEmpty();
        assertThat(service.stats().rebuildCount()).isEqualTo(2);
    }

    @Test
    void queryBeforeBuildFails() {
        SampledIndexService service = new SampledIndexService(Runnable::run);
        assertThatThrownBy(() -> service.query(List.of("x")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void statsExposeSampleSizeIndexSizeMissRateAndRebuildCount() {
        SampledIndexService service = new SampledIndexService(Runnable::run);
        List<Document> docs = intStreamDocs(300, "common");
        service.rebuild(docs, new RandomSampler(0.5, 5));

        IndexStats beforeEvaluation = service.stats();
        assertThat(beforeEvaluation.sampleSize()).isBetween(110, 190);
        assertThat(beforeEvaluation.populationSize()).isEqualTo(300);
        assertThat(beforeEvaluation.indexSize()).isGreaterThan(0);
        assertThat(beforeEvaluation.termCount()).isGreaterThan(0);
        assertThat(beforeEvaluation.rebuildCount()).isEqualTo(1);
        assertThat(beforeEvaluation.averageMissRate()).isEqualTo(-1.0);

        ErrorReport report = service.evaluateAgainst(
                List.of(List.of("word")), InvertedIndex.build(docs));
        IndexStats afterEvaluation = service.stats();
        assertThat(afterEvaluation.averageMissRate()).isEqualTo(report.missRate());
        assertThat(afterEvaluation.averageMissRate()).isBetween(0.0, 1.0);

        service.rebuild(docs, new RandomSampler(0.8, 5));
        assertThat(service.stats().rebuildCount()).isEqualTo(2);
    }

    private static List<Document> intStreamDocs(int n, String category) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> Document.of("d" + i, "word " + i, Map.of("cat", category)))
                .toList();
    }
}
