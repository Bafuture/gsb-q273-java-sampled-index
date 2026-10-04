package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RatioErrorAnalyzerTest {

    private static List<Document> corpus() {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            docs.add(Document.of("d" + i, "alpha beta gamma " + (i % 100)));
        }
        return docs;
    }

    private static final List<List<String>> QUERIES = List.of(
            List.of("alpha", "beta"),
            List.of("gamma"),
            List.of("42"),
            List.of("7"),
            List.of("99"));

    @Test
    void missRateDecreasesAsRatioGrows() {
        List<RatioErrorPoint> points = new RatioErrorAnalyzer()
                .analyze(corpus(), QUERIES, List.of(0.05, 0.1, 0.25, 0.5, 1.0), 2024L);

        assertThat(points).hasSize(5);
        for (int i = 1; i < points.size(); i++) {
            assertThat(points.get(i).missRate())
                    .as("miss rate must be non-increasing: %s", points)
                    .isLessThanOrEqualTo(points.get(i - 1).missRate());
            assertThat(points.get(i).sampleSize())
                    .isGreaterThanOrEqualTo(points.get(i - 1).sampleSize());
        }
        RatioErrorPoint full = points.get(4);
        assertThat(full.ratio()).isEqualTo(1.0);
        assertThat(full.missRate()).isZero();
        assertThat(full.hitRate()).isEqualTo(1.0);
        assertThat(points.get(0).sampleSize()).isBetween(60, 140);
    }

    @Test
    void supportsStratifiedSamplerFactoryAcrossRatios() {
        List<Document> docs = IntStream.range(0, 300)
                .mapToObj(i -> Document.of("d" + i, "alpha " + i,
                        java.util.Map.of("cat", i < 290 ? "common" : "rare")))
                .toList();
        List<RatioErrorPoint> points = new RatioErrorAnalyzer().analyze(
                docs, List.of(List.of("alpha")), List.of(0.01, 0.1, 1.0),
                ratio -> new StratifiedSampler(ratio, "cat", 2, 99));

        // 每层保底 2 条 ⇒ ratio=0.01 时样本量至少 4
        assertThat(points.get(0).sampleSize()).isGreaterThanOrEqualTo(4);
        assertThat(points.get(2).missRate()).isZero();
    }
}
