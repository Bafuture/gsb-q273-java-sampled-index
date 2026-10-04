package com.example.gsb.sampledindex;

import java.util.Locale;
import java.util.Set;

/**
 * 查询结果。来自采样索引的结果会标记 {@link #possiblyIncomplete()} 并附带说明。
 *
 * @param results            命中的记录
 * @param possiblyIncomplete 是否可能不完整（true 表示可能有匹配记录未进入样本而缺失）
 * @param explanation        人类可读的完整性说明
 */
public record QueryResult(Set<DataRecord> results,
                          boolean possiblyIncomplete,
                          String explanation) {

    public static QueryResult complete(Set<DataRecord> results) {
        return new QueryResult(Set.copyOf(results), false,
                "结果来自全量索引，完整。");
    }

    public static QueryResult sampled(Set<DataRecord> results, double sampleRatio, String strategy) {
        String explanation = String.format(Locale.ROOT,
                "结果来自采样索引（策略=%s，采样比例=%.2f%%），可能不完整："
                        + "未进入样本的匹配记录会缺失。适用于趋势分析、聚合估计与探索性查询；"
                        + "不适用于精确计数、合规审计、账单等要求完整结果集的场景。",
                strategy, sampleRatio * 100.0);
        return new QueryResult(Set.copyOf(results), true, explanation);
    }
}
