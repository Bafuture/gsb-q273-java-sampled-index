package com.example.gsb.sampledindex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 查询标记：采样索引结果必须标记"可能不完整"，全量索引结果不标记。 */
class QueryResultFlagTest {

    private static List<DataRecord> data(int size) {
        List<DataRecord> records = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            records.add(new DataRecord(i, "key-" + (i % 10), "s"));
        }
        return records;
    }

    @Test
    void sampledIndexResultIsMarkedPossiblyIncomplete() {
        SampledIndexService service =
                new SampledIndexService(new RandomSampler(), data(100), 0.3, 0L);

        QueryResult result = service.query("key-1");

        assertThat(result.possiblyIncomplete()).isTrue();
        assertThat(result.explanation()).contains("可能不完整");
        assertThat(result.explanation()).contains("30.00%");
    }

    @Test
    void fullIndexResultIsNotMarked() {
        SampledIndexService service =
                new SampledIndexService(new RandomSampler(), data(100), 1.0, 0L);

        QueryResult result = service.query("key-1");

        assertThat(result.possiblyIncomplete()).isFalse();
        assertThat(result.results()).hasSize(10);
        assertThat(result.explanation()).contains("完整");
    }

    @Test
    void emptyHitOnSampledIndexIsStillMarked() {
        SampledIndexService service =
                new SampledIndexService(new RandomSampler(), data(100), 0.3, 0L);

        QueryResult result = service.query("no-such-key");

        assertThat(result.results()).isEmpty();
        assertThat(result.possiblyIncomplete()).isTrue();
    }
}
