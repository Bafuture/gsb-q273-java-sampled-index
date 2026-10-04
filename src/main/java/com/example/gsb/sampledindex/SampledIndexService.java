package com.example.gsb.sampledindex;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 采样索引服务：持有当前生效的采样索引，支持动态调整采样比例 / 适配数据分布变化后重建。
 *
 * <p>重建采用“先构建、后切换”：新索引在后台完整构建完成后通过 {@link AtomicReference}
 * 原子替换，构建期间所有查询仍由旧索引回答，不会出现索引空窗或半构建状态。</p>
 */
public final class SampledIndexService {

    private final Executor executor;
    private final AtomicReference<SampledIndex> current = new AtomicReference<>();
    private final AtomicInteger rebuildCount = new AtomicInteger(0);
    private volatile ErrorReport lastErrorReport;

    public SampledIndexService() {
        this(ForkJoinPool.commonPool());
    }

    public SampledIndexService(Executor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /** 同步构建/重建，完成后新索引立即生效。 */
    public SampledIndex rebuild(List<Document> population, Sampler sampler) {
        SampledIndex next = SampledIndex.build(population, sampler);
        current.set(next);
        rebuildCount.incrementAndGet();
        return next;
    }

    /**
     * 异步重建：立即返回 future。future 完成前 {@link #query(List)} 仍返回旧索引结果，
     * 完成后原子切换到新索引。
     */
    public CompletableFuture<SampledIndex> rebuildAsync(List<Document> population, Sampler sampler) {
        return CompletableFuture.supplyAsync(() -> rebuild(population, sampler), executor);
    }

    public QueryResult query(List<String> terms) {
        SampledIndex index = current.get();
        if (index == null) {
            throw new IllegalStateException("index has not been built yet; call rebuild() first");
        }
        return index.query(terms);
    }

    /** 用全量索引对当前采样索引做一次误差评估并记录，结果进入统计。 */
    public ErrorReport evaluateAgainst(List<List<String>> queries, InvertedIndex fullIndex) {
        SampledIndex index = current.get();
        if (index == null) {
            throw new IllegalStateException("index has not been built yet; call rebuild() first");
        }
        ErrorReport report = new ErrorEvaluator().evaluate(index, fullIndex, queries);
        lastErrorReport = report;
        return report;
    }

    public IndexStats stats() {
        SampledIndex index = current.get();
        if (index == null) {
            return new IndexStats(0, 0, 0, 0, -1.0, rebuildCount.get());
        }
        ErrorReport report = lastErrorReport;
        double avgMissRate = report == null ? -1.0 : report.missRate();
        return new IndexStats(index.sampleSize(), index.populationSize(), index.postingCount(),
                index.termCount(), avgMissRate, rebuildCount.get());
    }

    public SampledIndex currentIndex() {
        SampledIndex index = current.get();
        if (index == null) {
            throw new IllegalStateException("index has not been built yet; call rebuild() first");
        }
        return index;
    }
}
