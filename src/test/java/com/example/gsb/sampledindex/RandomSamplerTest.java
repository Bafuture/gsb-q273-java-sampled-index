package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RandomSamplerTest {

    private static List<Document> docs(int n) {
        return IntStream.range(0, n)
                .mapToObj(i -> Document.of("doc-" + i, "word " + i))
                .toList();
    }

    @Test
    void sampleSizeIsCloseToConfiguredRatio() {
        List<Document> population = docs(2000);
        RandomSampler sampler = new RandomSampler(0.5, 42);
        List<Document> sample = sampler.sample(population);

        assertThat(sample.size()).isBetween(900, 1100);
        assertThat(sample).allSatisfy(doc -> assertThat(population).contains(doc));
    }

    @Test
    void sameSeedIsReproducible() {
        List<Document> population = docs(500);
        assertThat(new RandomSampler(0.3, 7).sample(population))
                .isEqualTo(new RandomSampler(0.3, 7).sample(population));
        assertThat(new RandomSampler(0.3, 7).sample(population))
                .isNotEqualTo(new RandomSampler(0.3, 8).sample(population));
    }

    @Test
    void ratioZeroAndOneAreExact() {
        List<Document> population = docs(100);
        assertThat(new RandomSampler(0.0, 1).sample(population)).isEmpty();
        assertThat(new RandomSampler(1.0, 1).sample(population)).hasSize(100);
    }

    @Test
    void samplesAreNestedWhenRatioGrows() {
        List<Document> population = docs(1000);
        Set<String> small = new RandomSampler(0.25, 11).sample(population).stream()
                .map(Document::id).collect(Collectors.toSet());
        Set<String> large = new RandomSampler(0.75, 11).sample(population).stream()
                .map(Document::id).collect(Collectors.toSet());
        assertThat(large).containsAll(small);
    }

    @Test
    void rejectsInvalidRatio() {
        assertThatThrownBy(() -> new RandomSampler(-0.1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RandomSampler(1.1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
