package com.example.gsb.sampledindex;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * 一批查询在采样索引 vs 全量索引上的误差评估结果。
 *
 * @param queries             参与评估的查询数
 * @param queriesWithResults  全量索引下有结果的查询数（无结果查询不计入漏检统计）
 * @param hits                采样索引命中且属于全量结果的文档数（真阳性）
 * @param misses              全量结果中被采样索引漏掉的文档数（假阴性）
 * @param hitRate             命中率 = hits / (hits + misses)，即召回率
 * @param missRate            漏检率 = misses / (hits + misses)
 * @param missCountHistogram  误差分布：单查询漏检条数 -&gt; 出现该漏检条数的查询数
 * @param maxQueryMissRate    单查询最大漏检率
 * @param details             每个查询的明细
 */
public record ErrorReport(int queries, int queriesWithResults, long hits, long misses,
                          double hitRate, double missRate,
                          SortedMap<Integer, Long> missCountHistogram,
                          double maxQueryMissRate,
                          List<QueryError> details) {

    /** 单个查询的误差明细。 */
    public record QueryError(List<String> terms, int expected, int actual, int missed, double missRate) {
    }
}
