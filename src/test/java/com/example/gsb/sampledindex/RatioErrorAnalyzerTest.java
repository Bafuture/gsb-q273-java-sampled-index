package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 采样比例与误差关系：不同比例下的漏检率数据。 */
class RatioErrorAnalyzerTest {

    private static List<DataRecord> balancedData(int size, int keyCardinality) {
        List<DataRecord> records = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            records.add(new DataRecord(i, "key-" + (i % keyCardinality), "s-" + (i % 5)));
        }
        return records;
    }

    @Test
    void missRateDecreasesAsRatioGrowsAndHitsZeroAtFullRatio() {
        List<DataRecord> data = balancedData(2000, 50);
        List<String> queries = new ArrayList<>();
        for (int k = 0; k < 50; k++) {
            queries.add("key-" + k);
        }

        List<RatioErrorPoint> points = new RatioErrorAnalyzer().analyze(
                data, queries, List.of(0.05, 0.25, 0.5, 1.0),
                new RandomSampler(), 1234L);

        assertThat(points).hasSize(4);
        assertThat(points).extracting(RatioErrorPoint::ratio)
                .containsExactly(0.05, 0.25, 0.5, 1.0);
        assertThat(points).extracting(RatioErrorPoint::sampleSize)
                .containsExactly(100, 500, 1000, 2000);

        double missAt05 = points.get(0).missRate();
        double missAt25 = points.get(1).missRate();
        double missAt50 = points.get(2).missRate();
        double missAtFull = points.get(3).missRate();

        assertThat(missAtFull).isZero();
        assertThat(missAt05).isGreaterThan(missAt25);
        assertThat(missAt25).isGreaterThan(missAt50);
        assertThat(missAt50).isGreaterThan(0.0);

        assertThat(points).allSatisfy(point ->
                assertThat(point.hitRate() + point.missRate()).isEqualTo(1.0,
                        org.assertj.core.data.Offset.offset(1e-12)));
        for (int i = 1; i < points.size(); i++) {
            assertThat(points.get(i).indexSizeBytes())
                    .isGreaterThan(points.get(i - 1).indexSizeBytes());
        }
    }

    @Test
    void stratifiedSamplingKeepsLowerMissRateForRareStratumQueries() {
        List<DataRecord> data = new ArrayList<>();
        int id = 0;
        for (int i = 0; i < 990; i++) {
            data.add(new DataRecord(id++, "hot", "common"));
        }
        for (int i = 0; i < 10; i++) {
            data.add(new DataRecord(id++, "rare", "rare"));
        }
        List<String> queries = List.of("rare");

        List<RatioErrorPoint> randomPoints = new RatioErrorAnalyzer().analyze(
                data, queries, List.of(0.05), new RandomSampler(), 7L);
        List<RatioErrorPoint> stratifiedPoints = new RatioErrorAnalyzer().analyze(
                data, queries, List.of(0.05), new StratifiedSampler(), 7L);

        assertThat(stratifiedPoints.get(0).missRate()).isLessThan(1.0);
        assertThat(stratifiedPoints.get(0).missRate())
                .isLessThan(randomPoints.get(0).missRate());
    }
}
