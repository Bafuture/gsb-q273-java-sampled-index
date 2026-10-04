package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SampledIndexQueryTest {

    private static List<Document> docs(int n) {
        return IntStream.range(0, n)
                .mapToObj(i -> Document.of("d" + i, "alpha beta " + i))
                .toList();
    }

    @Test
    void resultIsAlwaysMarkedPossiblyIncomplete() {
        List<Document> population = docs(50);
        SampledIndex sampled = SampledIndex.build(population, new RandomSampler(0.5, 3));

        QueryResult result = sampled.query(List.of("alpha", "beta"));
        assertThat(result.possiblyIncomplete()).isTrue();
        assertThat(result.indexedDocuments()).isEqualTo(sampled.sampleSize());
        assertThat(result.totalPopulation()).isEqualTo(50);
    }

    @Test
    void sampledResultsAreSubsetOfFullResultsAndCanOnlyMiss() {
        List<Document> population = docs(200);
        InvertedIndex full = InvertedIndex.build(population);
        SampledIndex sampled = SampledIndex.build(population, new RandomSampler(0.5, 3));

        QueryResult result = sampled.query(List.of("alpha"));
        assertThat(full.query(List.of("alpha"))).containsAll(result.matchedIds());
    }

    @Test
    void noHitDoesNotProveAbsence() {
        List<Document> population = docs(200);
        InvertedIndex full = InvertedIndex.build(population);
        SampledIndex sampled = SampledIndex.build(population, new RandomSampler(0.01, 1));

        // 采样索引里没有结果，但全量索引有结果：空结果同样是“可能不完整”的
        QueryResult result = sampled.query(List.of("1"));
        assertThat(result.possiblyIncomplete()).isTrue();
        assertThat(full.query(List.of("1"))).isNotEmpty();
    }

    @Test
    void andSemanticsAndTermNormalization() {
        List<Document> docs = List.of(
                Document.of("x1", "Foo Bar"),
                Document.of("x2", "foo only"));
        InvertedIndex index = InvertedIndex.build(docs);
        assertThat(index.query(List.of("FOO", "bar"))).containsExactly("x1");
        assertThat(index.query(List.of("nonexistent"))).isEmpty();
    }
}
