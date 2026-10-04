package com.example.gsb.sampledindex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 误差评估器：对同一批查询分别用采样索引与全量索引执行，
 * 以全量索引结果为基准真值，统计命中率、漏检率与误差分布。
 */
public final class ErrorEvaluator {

    public ErrorReport evaluate(InvertedIndex sampledIndex,
                                InvertedIndex fullIndex,
                                Collection<String> queries) {
        List<QueryError> details = new ArrayList<>();
        long totalExpected = 0;
        long totalReturned = 0;
        long totalHits = 0;
        long totalMissed = 0;
        for (String query : queries) {
            Set<Long> expectedIds = idsOf(fullIndex.lookup(query));
            Set<Long> actualIds = idsOf(sampledIndex.lookup(query));
            long hits = actualIds.stream().filter(expectedIds::contains).count();
            long missed = expectedIds.stream().filter(id -> !actualIds.contains(id)).count();
            int expected = expectedIds.size();
            double hitRate = expected == 0 ? 1.0 : (double) hits / expected;
            double missRate = expected == 0 ? 0.0 : (double) missed / expected;
            details.add(new QueryError(query, expected, actualIds.size(),
                    hits, missed, hitRate, missRate));
            totalExpected += expected;
            totalReturned += actualIds.size();
            totalHits += hits;
            totalMissed += missed;
        }
        return new ErrorReport(List.copyOf(details),
                totalExpected, totalReturned, totalHits, totalMissed);
    }

    private static Set<Long> idsOf(Set<DataRecord> records) {
        return records.stream().map(DataRecord::id).collect(Collectors.toSet());
    }
}
