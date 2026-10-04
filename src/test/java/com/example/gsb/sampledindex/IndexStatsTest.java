package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 统计：样本量、索引大小、平均漏检率、重建次数。 */
class IndexStatsTest {

    @Test
    void reportsSampleSizeIndexSizeAverageMissRateAndRebuildCount() {
        List<DataRecord> data = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            data.add(new DataRecord(i, "key-" + (i % 25), "s"));
        }
        SampledIndexService service =
                new SampledIndexService(new RandomSampler(), data, 0.4, 0L);

        IndexStats initial = service.stats();
        assertThat(initial.sampleSize()).isEqualTo(200);
        assertThat(initial.indexSizeBytes()).isPositive();
        assertThat(initial.averageMissRate()).isZero();
        assertThat(initial.rebuildCount()).isZero();
        assertThat(initial.currentRatio()).isEqualTo(0.4);
        assertThat(initial.strategy()).isEqualTo("random");

        InvertedIndex fullIndex = InvertedIndex.build(data, 1.0, "full");
        List<String> queries = new ArrayList<>();
        for (int k = 0; k < 25; k++) {
            queries.add("key-" + k);
        }
        ErrorReport report = service.evaluate(fullIndex, queries);
        assertThat(report.missRate()).isGreaterThan(0.0);
        assertThat(service.stats().averageMissRate()).isEqualTo(report.missRate());

        service.resample(0.8);
        service.recordMissRate(0.1);
        service.recordMissRate(0.3);
        IndexStats later = service.stats();
        assertThat(later.rebuildCount()).isEqualTo(1);
        assertThat(later.sampleSize()).isEqualTo(400);
        assertThat(later.averageMissRate())
                .isGreaterThan(0.0)
                .isLessThan(report.missRate() == 0.0 ? 1.0 : 1.0);
    }
}
