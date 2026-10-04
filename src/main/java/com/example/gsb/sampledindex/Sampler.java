package com.example.gsb.sampledindex;

import java.util.List;

/**
 * 抽样策略：从全量数据中选出用于建索引的样本。
 */
@FunctionalInterface
public interface Sampler {

    List<Document> sample(List<Document> population);

    /** 策略描述，用于统计与日志。 */
    default String describe() {
        return getClass().getSimpleName();
    }
}
