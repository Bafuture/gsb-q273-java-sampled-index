package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** 分层抽样：按维度分层、每层独立抽样、稀有类别保证有样本。 */
class StratifiedSamplerTest {

    @Test
    void samplesProportionallyWithinEachStratum() {
        List<DataRecord> data = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            data.add(new DataRecord(i, "k", "stratum-" + (i % 3)));
        }

        List<DataRecord> sample = new StratifiedSampler().sample(data, 0.1, new Random(0));

        assertThat(sample).hasSize(30);
        Map<String, Long> perStratum = sample.stream()
                .collect(Collectors.groupingBy(DataRecord::stratum, Collectors.counting()));
        assertThat(perStratum).containsOnlyKeys("stratum-0", "stratum-1", "stratum-2");
        assertThat(perStratum.values()).containsExactly(10L, 10L, 10L);
    }

    @Test
    void guaranteesRareStratumIsRepresented() {
        List<DataRecord> data = new ArrayList<>();
        for (int i = 0; i < 999; i++) {
            data.add(new DataRecord(i, "hot", "common"));
        }
        DataRecord rare = new DataRecord(999, "rare", "rare-stratum");
        data.add(rare);

        List<DataRecord> stratified =
                new StratifiedSampler().sample(data, 0.01, new Random(42));

        assertThat(stratified).hasSize(11);
        assertThat(stratified).contains(rare);
        assertThat(stratified.stream().filter(r -> r.stratum().equals("common")).count())
                .isEqualTo(10);
    }

    @Test
    void randomSamplingCanMissTheRareStratum() {
        List<DataRecord> data = new ArrayList<>();
        for (int i = 0; i < 999; i++) {
            data.add(new DataRecord(i, "hot", "common"));
        }
        DataRecord rare = new DataRecord(999, "rare", "rare-stratum");
        data.add(rare);

        long randomHitsAcrossSeeds = 0;
        for (long seed = 0; seed < 50; seed++) {
            List<DataRecord> randomSample =
                    new RandomSampler().sample(data, 0.01, new Random(seed));
            assertThat(randomSample).hasSize(10);
            if (randomSample.contains(rare)) {
                randomHitsAcrossSeeds++;
            }
        }
        // 稀有类别在随机抽样中经常整体缺失，而分层抽样必中
        assertThat(randomHitsAcrossSeeds).isLessThan(10L);
    }
}
