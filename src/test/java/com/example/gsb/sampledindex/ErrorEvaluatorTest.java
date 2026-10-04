package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ErrorEvaluatorTest {

    private static List<Document> data() {
        return List.of(
                Document.of("s1", "term"),
                Document.of("s2", "term"),
                Document.of("s3", "term"),
                Document.of("s4", "term"),
                Document.of("u1", "uniq other"),
                Document.of("u2", "uniq other"));
    }

    @Test
    void computesHitRateMissRateAndDistribution() {
        List<Document> population = data();
        // 固定样本：term 命中 3/4（漏 1），uniq 命中 0/2（漏 2），other 同样漏 2
        List<Document> kept = population.subList(0, 3);
        SampledIndex sampled = new SampledIndex(InvertedIndex.build(kept), population.size(), "fixed");
        InvertedIndex full = InvertedIndex.build(population);

        ErrorReport report = new ErrorEvaluator().evaluate(sampled, full,
                List.of(List.of("term"), List.of("uniq"), List.of("other")));

        assertThat(report.queries()).isEqualTo(3);
        assertThat(report.queriesWithResults()).isEqualTo(3);
        assertThat(report.hits()).isEqualTo(3);
        assertThat(report.misses()).isEqualTo(5);
        assertThat(report.hitRate()).isCloseTo(3.0 / 8, org.assertj.core.data.Offset.offset(1e-12));
        assertThat(report.missRate()).isCloseTo(5.0 / 8, org.assertj.core.data.Offset.offset(1e-12));
        assertThat(report.maxQueryMissRate()).isEqualTo(1.0);

        // 误差分布：漏检 1 条的查询 1 个（term），漏检 2 条的查询 2 个（uniq、other）
        assertThat(report.missCountHistogram())
                .containsEntry(1, 1L)
                .containsEntry(2, 2L)
                .hasSize(2);

        assertThat(report.details()).hasSize(3);
        assertThat(report.details().get(0).missed()).isEqualTo(1);
        assertThat(report.details().get(1).missRate()).isEqualTo(1.0);
    }

    @Test
    void queriesWithoutFullResultsAreExcludedFromMissStats() {
        List<Document> population = data();
        SampledIndex sampled = SampledIndex.build(population, new RandomSampler(0.5, 1));
        InvertedIndex full = InvertedIndex.build(population);

        ErrorReport report = new ErrorEvaluator().evaluate(sampled, full,
                List.of(List.of("ghost")));

        assertThat(report.queriesWithResults()).isZero();
        assertThat(report.misses()).isZero();
        assertThat(report.missRate()).isZero();
        assertThat(report.hitRate()).isEqualTo(1.0);
        assertThat(report.missCountHistogram()).isEmpty();
    }
}
