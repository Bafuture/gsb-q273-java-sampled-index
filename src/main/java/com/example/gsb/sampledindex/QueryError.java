package com.example.gsb.sampledindex;

/**
 * 单个查询在采样索引上的误差明细。
 *
 * @param query          查询的 key
 * @param expectedCount  全量索引命中数（基准真值）
 * @param returnedCount  采样索引返回数
 * @param hitCount       采样索引返回且在全量结果中的条数
 * @param missedCount    全量结果中未被采样索引覆盖的条数（漏检）
 * @param hitRate        命中率 = hitCount / expectedCount（expectedCount 为 0 时记 1.0）
 * @param missRate       漏检率 = missedCount / expectedCount（expectedCount 为 0 时记 0.0）
 */
public record QueryError(String query,
                         int expectedCount,
                         int returnedCount,
                         long hitCount,
                         long missedCount,
                         double hitRate,
                         double missRate) {
}
