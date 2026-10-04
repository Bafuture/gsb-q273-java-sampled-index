package com.example.gsb.sampledindex;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 采样索引服务：对全量数据按可配置比例抽样建索引，并对外提供查询。
 *
 * <p>查询语义：采样索引只覆盖样本，未进入样本的匹配记录会缺失，
 * 因此采样索引上的查询结果一律标记为"可能不完整"
 * （{@link QueryResult#possiblyIncomplete()} == true）。
 *
 * <p>何时能用：
 * <ul>
 *   <li>趋势分析、比例估计、聚合统计等对个别漏检不敏感的场景；</li>
 *   <li>探索性查询、存在性判断（可容忍假阴性）；</li>
 *   <li>配合 {@link ErrorEvaluator} 评估确认漏检率可接受后的在线服务。</li>
 * </ul>
 * 何时不能用：
 * <ul>
 *   <li>精确计数、合规审计、账单结算等要求完整结果集的场景；</li>
 *   <li>结果会触发不可逆动作且不允许漏检的场景；</li>
 *   <li>查询集中于稀有类别却使用了简单随机抽样（应改用分层抽样）。</li>
 * </ul>
 *
 * <p>动态重建：{@link #rebuild(List, double)} 先在后台构建新索引，
 * 构建完成后原子切换；重建期间旧索引持续对外服务，查询不中断。
 */
public final class SampledIndexService {

    private final Sampler sampler;
    private final long seed;
    private final AtomicReference<List<DataRecord>> dataset;
    private final AtomicReference<InvertedIndex> activeIndex;
    private final AtomicReference<Double> currentRatio;
    private final AtomicInteger rebuildCount = new AtomicInteger();
    private final AtomicInteger buildGeneration = new AtomicInteger();
    private final AtomicLong missRateEvaluations = new AtomicLong();
    private final java.util.concurrent.atomic.DoubleAdder missRateSum =
            new java.util.concurrent.atomic.DoubleAdder();

    public SampledIndexService(Sampler sampler,
                               List<DataRecord> initialData,
                               double initialRatio,
                               long seed) {
        this.sampler = Objects.requireNonNull(sampler, "sampler");
        Sampler.checkRatio(initialRatio);
        this.seed = seed;
        this.dataset = new AtomicReference<>(List.copyOf(initialData));
        this.currentRatio = new AtomicReference<>(initialRatio);
        this.activeIndex = new AtomicReference<>(buildIndex(initialData, initialRatio));
    }

    /** 使用当前索引回答查询；采样比例 &lt; 1 时结果标记为"可能不完整"。 */
    public QueryResult query(String key) {
        InvertedIndex index = activeIndex.get();
        Set<DataRecord> results = index.lookup(key);
        if (index.sampleRatio() >= 1.0) {
            return QueryResult.complete(results);
        }
        return QueryResult.sampled(results, index.sampleRatio(), index.strategy());
    }

    /**
     * 用新数据与新采样比例重建索引。新索引构建期间，旧索引继续服务查询；
     * 构建完成后原子切换。
     */
    public void rebuild(List<DataRecord> newData, double newRatio) {
        Sampler.checkRatio(newRatio);
        List<DataRecord> data = List.copyOf(newData);
        InvertedIndex nextIndex = buildIndex(data, newRatio);
        dataset.set(data);
        currentRatio.set(newRatio);
        activeIndex.set(nextIndex);
        rebuildCount.incrementAndGet();
    }

    /** 数据分布未变、仅调整采样比例时重新抽样建索引。 */
    public void resample(double newRatio) {
        rebuild(dataset.get(), newRatio);
    }

    /** 登记一次误差评估的漏检率，用于统计平均漏检率。 */
    public void recordMissRate(double missRate) {
        if (Double.isNaN(missRate) || missRate < 0.0 || missRate > 1.0) {
            throw new IllegalArgumentException("missRate must be in [0, 1], got: " + missRate);
        }
        missRateSum.add(missRate);
        missRateEvaluations.incrementAndGet();
    }

    /** 用当前活跃索引与给定全量索引对一批查询做误差评估，并登记漏检率。 */
    public ErrorReport evaluate(InvertedIndex fullIndex, List<String> queries) {
        ErrorReport report = new ErrorEvaluator().evaluate(activeIndex.get(), fullIndex, queries);
        recordMissRate(report.missRate());
        return report;
    }

    public IndexStats stats() {
        InvertedIndex index = activeIndex.get();
        long evaluations = missRateEvaluations.get();
        double averageMissRate = evaluations == 0 ? 0.0 : missRateSum.sum() / evaluations;
        return new IndexStats(index.indexedRecordCount(),
                index.estimatedSizeBytes(),
                averageMissRate,
                rebuildCount.get(),
                currentRatio.get(),
                sampler.name());
    }

    public InvertedIndex activeIndex() {
        return activeIndex.get();
    }

    private InvertedIndex buildIndex(List<DataRecord> data, double ratio) {
        Random random = new Random(seed + buildGeneration.getAndIncrement());
        List<DataRecord> sample = sampler.sample(data, ratio, random);
        return InvertedIndex.build(sample, ratio, sampler.name());
    }
}
