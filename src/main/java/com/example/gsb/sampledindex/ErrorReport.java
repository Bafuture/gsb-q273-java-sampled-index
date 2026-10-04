package com.example.gsb.sampledindex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一批查询的误差评估报告：整体命中率、漏检率与逐查询误差分布。
 */
public record ErrorReport(List<QueryError> queries,
                          long totalExpected,
                          long totalReturned,
                          long totalHits,
                          long totalMissed) {

    /** 微平均命中率：总命中 / 总期望。 */
    public double hitRate() {
        return totalExpected == 0 ? 1.0 : (double) totalHits / totalExpected;
    }

    /** 微平均漏检率：总漏检 / 总期望。 */
    public double missRate() {
        return totalExpected == 0 ? 0.0 : (double) totalMissed / totalExpected;
    }

    /** 宏平均漏检率：逐查询漏检率的算术平均。 */
    public double meanPerQueryMissRate() {
        return queries.stream().mapToDouble(QueryError::missRate).average().orElse(0.0);
    }

    public double maxMissRate() {
        return queries.stream().mapToDouble(QueryError::missRate).max().orElse(0.0);
    }

    /** 逐查询漏检率的分布直方图（按区间分桶）。 */
    public Map<String, Long> missRateDistribution() {
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("0", 0L);
        buckets.put("(0, 0.25]", 0L);
        buckets.put("(0.25, 0.5]", 0L);
        buckets.put("(0.5, 0.75]", 0L);
        buckets.put("(0.75, 1]", 0L);
        for (QueryError query : queries) {
            double rate = query.missRate();
            String bucket;
            if (rate <= 0.0) {
                bucket = "0";
            } else if (rate <= 0.25) {
                bucket = "(0, 0.25]";
            } else if (rate <= 0.5) {
                bucket = "(0.25, 0.5]";
            } else if (rate <= 0.75) {
                bucket = "(0.5, 0.75]";
            } else {
                bucket = "(0.75, 1]";
            }
            buckets.merge(bucket, 1L, Long::sum);
        }
        return buckets;
    }
}
