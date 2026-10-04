package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 误差评估：同一批查询在采样索引与全量索引上执行，统计命中率/漏检率/分布。 */
class ErrorEvaluatorTest {

    @Test
    void computesHitRateMissRateAndDistribution() {
        List<DataRecord> all = List.of(
                new DataRecord(1, "a", "s"),
                new DataRecord(2, "a", "s"),
                new DataRecord(3, "a", "s"),
                new DataRecord(4, "a", "s"),
                new DataRecord(5, "b", "s"),
                new DataRecord(6, "b", "s"));
        List<DataRecord> sample = List.of(
                new DataRecord(1, "a", "s"),
                new DataRecord(2, "a", "s"),
                new DataRecord(5, "b", "s"));

        InvertedIndex fullIndex = InvertedIndex.build(all, 1.0, "full");
        InvertedIndex sampledIndex = InvertedIndex.build(sample, 0.5, "random");

        ErrorReport report = new ErrorEvaluator()
                .evaluate(sampledIndex, fullIndex, List.of("a", "b", "c"));

        QueryError queryA = report.queries().get(0);
        assertThat(queryA.expectedCount()).isEqualTo(4);
        assertThat(queryA.returnedCount()).isEqualTo(2);
        assertThat(queryA.hitCount()).isEqualTo(2);
        assertThat(queryA.missedCount()).isEqualTo(2);
        assertThat(queryA.hitRate()).isEqualTo(0.5);
        assertThat(queryA.missRate()).isEqualTo(0.5);

        QueryError queryB = report.queries().get(1);
        assertThat(queryB.expectedCount()).isEqualTo(2);
        assertThat(queryB.returnedCount()).isEqualTo(1);
        assertThat(queryB.missedCount()).isEqualTo(1);

        QueryError queryC = report.queries().get(2);
        assertThat(queryC.expectedCount()).isZero();
        assertThat(queryC.missRate()).isZero();
        assertThat(queryC.hitRate()).isEqualTo(1.0);

        assertThat(report.totalExpected()).isEqualTo(6);
        assertThat(report.totalHits()).isEqualTo(3);
        assertThat(report.totalMissed()).isEqualTo(3);
        assertThat(report.hitRate()).isEqualTo(0.5);
        assertThat(report.missRate()).isEqualTo(0.5);
        assertThat(report.maxMissRate()).isEqualTo(0.5);

        assertThat(report.missRateDistribution())
                .containsEntry("0", 1L)
                .containsEntry("(0.25, 0.5]", 2L);
    }

    @Test
    void fullSampledIndexHasZeroMissRate() {
        List<DataRecord> all = List.of(
                new DataRecord(1, "a", "s"),
                new DataRecord(2, "a", "s"),
                new DataRecord(3, "b", "s"));
        InvertedIndex full = InvertedIndex.build(all, 1.0, "full");

        ErrorReport report = new ErrorEvaluator().evaluate(full, full, List.of("a", "b"));

        assertThat(report.hitRate()).isEqualTo(1.0);
        assertThat(report.missRate()).isZero();
        assertThat(report.missRateDistribution().get("0")).isEqualTo(2L);
    }
}
