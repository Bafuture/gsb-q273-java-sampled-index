package com.example.gsb.sampledindex;

import java.util.List;
import java.util.Set;

/**
 * 基于样本构建的倒排索引。
 *
 * <p>采样索引只能命中样本内的文档，因此：</p>
 * <ul>
 *   <li>无假阳性——返回的结果一定属于全量结果；</li>
 *   <li>有漏检——全量结果中未被抽中的文档不会出现，所有结果都标记为“可能不完整”；</li>
 *   <li>“无命中”不代表全量数据中真的不存在匹配，可能只是匹配文档没被抽中。</li>
 * </ul>
 */
public final class SampledIndex {

    private final InvertedIndex index;
    private final int populationSize;
    private final String samplerDescription;

    public SampledIndex(InvertedIndex index, int populationSize, String samplerDescription) {
        this.index = index;
        this.populationSize = populationSize;
        this.samplerDescription = samplerDescription;
    }

    /** 先用 sampler 抽样、再建索引。 */
    public static SampledIndex build(List<Document> population, Sampler sampler) {
        List<Document> sample = sampler.sample(population);
        return new SampledIndex(InvertedIndex.build(sample), population.size(), sampler.describe());
    }

    public QueryResult query(List<String> terms) {
        Set<String> matched = index.query(terms);
        return QueryResult.of(terms, matched, true, index.documentCount(), populationSize, samplerDescription);
    }

    public int sampleSize() {
        return index.documentCount();
    }

    public int populationSize() {
        return populationSize;
    }

    public double effectiveSampleRatio() {
        return populationSize == 0 ? 0.0 : (double) sampleSize() / populationSize;
    }

    public long postingCount() {
        return index.postingCount();
    }

    public int termCount() {
        return index.termCount();
    }

    public String samplerDescription() {
        return samplerDescription;
    }

    InvertedIndex delegate() {
        return index;
    }
}
