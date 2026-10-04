package com.example.gsb.sampledindex;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 采样索引对一次查询的回答。
 *
 * @param terms               查询词
 * @param matchedIds          命中的文档 id（升序）
 * @param possiblyIncomplete  是否“可能不完整”：采样索引恒为 true；结果一定是全量结果的子集
 * @param indexedDocuments    当前索引覆盖的文档数（采样索引即样本量）
 * @param totalPopulation     全量数据量
 * @param samplerDescription  采样策略描述
 */
public record QueryResult(List<String> terms, SortedSet<String> matchedIds, boolean possiblyIncomplete,
                          int indexedDocuments, int totalPopulation, String samplerDescription) {

    static QueryResult of(List<String> terms, java.util.Set<String> matched, boolean possiblyIncomplete,
                          int indexedDocuments, int totalPopulation, String samplerDescription) {
        return new QueryResult(List.copyOf(terms), new TreeSet<>(matched), possiblyIncomplete,
                indexedDocuments, totalPopulation, samplerDescription);
    }
}
