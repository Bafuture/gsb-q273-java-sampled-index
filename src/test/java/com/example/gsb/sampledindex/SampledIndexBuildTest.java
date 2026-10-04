package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** 抽样建索引：随机抽样、比例可配置、索引只覆盖样本。 */
class SampledIndexBuildTest {

    private static List<DataRecord> data(int size) {
        List<DataRecord> records = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            records.add(new DataRecord(i, "key-" + (i % 50), "stratum-" + (i % 5)));
        }
        return records;
    }

    @Test
    void randomSamplingBuildsIndexOverSampleOnly() {
        List<DataRecord> all = data(1000);
        List<DataRecord> sample = new RandomSampler().sample(all, 0.2, new Random(42));

        assertThat(sample).hasSize(200);

        InvertedIndex index = InvertedIndex.build(sample, 0.2, "random");
        assertThat(index.indexedRecordCount()).isEqualTo(200);
        assertThat(index.estimatedSizeBytes()).isPositive();

        Set<Long> sampledIds = sample.stream().map(DataRecord::id).collect(Collectors.toSet());
        for (int key = 0; key < 50; key++) {
            Set<DataRecord> hits = index.lookup("key-" + key);
            assertThat(hits).allMatch(record -> sampledIds.contains(record.id()));
        }
    }

    @Test
    void sampleRatioIsConfigurable() {
        List<DataRecord> all = data(1000);
        RandomSampler sampler = new RandomSampler();
        assertThat(sampler.sample(all, 0.05, new Random(1))).hasSize(50);
        assertThat(sampler.sample(all, 0.5, new Random(1))).hasSize(500);
        assertThat(sampler.sample(all, 1.0, new Random(1))).hasSize(1000);
    }

    @Test
    void fullRatioIndexCoversEveryRecord() {
        List<DataRecord> all = data(500);
        InvertedIndex index = InvertedIndex.build(
                new RandomSampler().sample(all, 1.0, new Random(7)), 1.0, "random");
        assertThat(index.indexedRecordCount()).isEqualTo(500);
        for (int key = 0; key < 50; key++) {
            assertThat(index.lookup("key-" + key)).hasSize(10);
        }
    }

    @Test
    void invalidRatioIsRejected() {
        List<DataRecord> all = data(10);
        RandomSampler sampler = new RandomSampler();
        assertThatThrownBy(() -> sampler.sample(all, 0.0, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sampler.sample(all, 1.5, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SampledIndexService(sampler, all, -0.1, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyDataYieldsEmptyIndex() {
        SampledIndexService service =
                new SampledIndexService(new RandomSampler(), List.of(), 0.5, 0L);
        assertThat(service.query("anything").results()).isEmpty();
        assertThat(service.stats().sampleSize()).isZero();
    }
}
