package com.example.gsb.sampledindex;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 不可变倒排索引：key -&gt; 记录集合。可对全量数据或样本数据构建。
 */
public final class InvertedIndex {

    private final Map<String, Set<Long>> postings;
    private final Map<Long, DataRecord> recordsById;
    private final double sampleRatio;
    private final String strategy;

    private InvertedIndex(Map<String, Set<Long>> postings,
                          Map<Long, DataRecord> recordsById,
                          double sampleRatio,
                          String strategy) {
        this.postings = postings;
        this.recordsById = recordsById;
        this.sampleRatio = sampleRatio;
        this.strategy = strategy;
    }

    /**
     * @param records     被索引的记录（全量或样本）
     * @param sampleRatio 构建该索引时使用的采样比例（全量索引传 1.0）
     * @param strategy    抽样策略名称（全量索引可传 "full"）
     */
    public static InvertedIndex build(Collection<DataRecord> records, double sampleRatio, String strategy) {
        Map<String, Set<Long>> postings = new HashMap<>();
        Map<Long, DataRecord> recordsById = new HashMap<>();
        for (DataRecord record : records) {
            recordsById.put(record.id(), record);
            postings.computeIfAbsent(record.key(), k -> new HashSet<>()).add(record.id());
        }
        return new InvertedIndex(postings, recordsById, sampleRatio, strategy);
    }

    /** 精确匹配查询，返回 key 相等的全部记录（索引范围内）。 */
    public Set<DataRecord> lookup(String key) {
        Set<Long> ids = postings.get(key);
        if (ids == null) {
            return Set.of();
        }
        return ids.stream().map(recordsById::get).collect(Collectors.toUnmodifiableSet());
    }

    /** 被索引的记录条数（采样索引即样本量）。 */
    public int indexedRecordCount() {
        return recordsById.size();
    }

    public int distinctKeyCount() {
        return postings.size();
    }

    public double sampleRatio() {
        return sampleRatio;
    }

    public String strategy() {
        return strategy;
    }

    /** 索引内存占用的确定性估算（字节），用于对比不同采样比例下的索引大小。 */
    public long estimatedSizeBytes() {
        long bytes = 64;
        for (Map.Entry<String, Set<Long>> entry : postings.entrySet()) {
            bytes += 40L + entry.getKey().length() * 2L;
            bytes += 32L + entry.getValue().size() * 8L;
        }
        bytes += recordsById.size() * 48L;
        return bytes;
    }
}
