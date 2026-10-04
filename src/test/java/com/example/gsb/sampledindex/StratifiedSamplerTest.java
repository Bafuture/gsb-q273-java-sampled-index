package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StratifiedSamplerTest {

    private static List<Document> skewedPopulation() {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            docs.add(Document.of("a-" + i, "shared rareword", Map.of("category", "A")));
        }
        for (int i = 0; i < 2; i++) {
            docs.add(Document.of("b-" + i, "shared rareword", Map.of("category", "B")));
        }
        docs.add(Document.of("c-0", "shared rareword", Map.of("other", "x")));
        return docs;
    }

    @Test
    void rareCategoryStillRepresentedAtLowRatio() {
        List<Document> sample = new StratifiedSampler(0.05, "category", 2, 1)
                .sample(skewedPopulation());

        long countA = sample.stream().filter(d -> "A".equals(d.attribute("category").orElse(null))).count();
        long countB = sample.stream().filter(d -> "B".equals(d.attribute("category").orElse(null))).count();
        assertThat(countA).isEqualTo(5);
        assertThat(countB).isEqualTo(2);
        // 稀有类别 B 命中的 rareword 查询在采样索引中不会完全消失
        SampledIndex index = SampledIndex.build(skewedPopulation(),
                new StratifiedSampler(0.05, "category", 2, 1));
        assertThat(index.query(List.of("rareword")).matchedIds())
                .anySatisfy(id -> assertThat(id).startsWith("b-"));
    }

    @Test
    void floorGuaranteesAtLeastOneEvenAtRatioZero() {
        List<Document> sample = new StratifiedSampler(0.0, "category", 1, 1)
                .sample(skewedPopulation());
        assertThat(sample).extracting(doc -> doc.attribute("category").orElse("<missing>"))
                .contains("A", "B", "<missing>");
    }

    @Test
    void missingStrataKeyGoesToDedicatedStratum() {
        StratifiedSampler sampler = new StratifiedSampler(1.0, "category", 0, 1);
        assertThat(sampler.sample(skewedPopulation())).hasSize(103);
    }

    @Test
    void rejectsBadArguments() {
        assertThatThrownBy(() -> new StratifiedSampler(0.5, " ", 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StratifiedSampler(0.5, "category", -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
