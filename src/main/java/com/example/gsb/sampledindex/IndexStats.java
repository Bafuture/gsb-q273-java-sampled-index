package com.example.gsb.sampledindex;

/**
 * 采样索引运行统计的只读快照。
 *
 * @param sampleSize       当前索引的样本量（覆盖文档数）
 * @param populationSize   全量数据量
 * @param indexSize        索引大小（posting 条目数）
 * @param termCount        索引中的词项数
 * @param averageMissRate  最近一次误差评估的平均漏检率；未评估过为 {@code -1}
 * @param rebuildCount     索引重建次数（含首次构建）
 */
public record IndexStats(int sampleSize, int populationSize, long indexSize, int termCount,
                         double averageMissRate, int rebuildCount) {
}
