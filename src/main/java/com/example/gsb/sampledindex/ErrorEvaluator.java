package com.example.gsb.sampledindex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 误差评估器：对同一批查询分别用采样索引与全量索引执行，
 * 统计命中率、漏检率与误差分布。
 *
 * <p>采样索引的结果一定是全量结果的子集，因此没有假阳性，
 * 误差全部体现为漏检（假阴性）。</p>
 */
public final class ErrorEvaluator {

    public ErrorReport evaluate(SampledIndex sampledIndex, InvertedIndex fullIndex,
                                List<List<String>> queries) {
        List<ErrorReport.QueryError> details = new ArrayList<>();
        TreeMap<Integer, Long> histogram = new TreeMap<>();
        long hits = 0;
        long misses = 0;
        int queriesWithResults = 0;
        double maxQueryMissRate = 0.0;

        for (List<String> terms : queries) {
            Set<String> expected = fullIndex.query(terms);
            Set<String> actual = sampledIndex.delegate().query(terms);
            Set<String> missed = new TreeSet<>(expected);
            missed.removeAll(actual);

            int missedCount = missed.size();
            double queryMissRate = expected.isEmpty() ? 0.0 : (double) missedCount / expected.size();
            details.add(new ErrorReport.QueryError(List.copyOf(terms),
                    expected.size(), actual.size(), missedCount, queryMissRate));

            if (!expected.isEmpty()) {
                queriesWithResults++;
                hits += actual.size();
                misses += missedCount;
                histogram.merge(missedCount, 1L, Long::sum);
                maxQueryMissRate = Math.max(maxQueryMissRate, queryMissRate);
            }
        }

        long total = hits + misses;
        double hitRate = total == 0 ? 1.0 : (double) hits / total;
        double missRate = total == 0 ? 0.0 : (double) misses / total;
        return new ErrorReport(queries.size(), queriesWithResults, hits, misses, hitRate, missRate,
                histogram, maxQueryMissRate, List.copyOf(details));
    }
}
