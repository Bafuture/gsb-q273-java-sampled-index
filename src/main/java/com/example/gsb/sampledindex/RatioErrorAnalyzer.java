package com.example.gsb.sampledindex;

import java.util.ArrayList;
import java.util.List;

/**
 * 采样比例与误差关系分析：在同一批数据与查询上，对多个采样比例分别建采样索引并评估，
 * 输出每个比例下的样本量与漏检率，用于选择“可接受漏检率下最省索引成本”的比例。
 *
 * <p>{@link RandomSampler} 采用确定性哈希且样本随比例嵌套，
 * 因此漏检率随比例单调不增；分层抽样因为有每层保底条数，结果同样给出但不保证嵌套。</p>
 */
public final class RatioErrorAnalyzer {

    private final ErrorEvaluator evaluator;

    public RatioErrorAnalyzer() {
        this(new ErrorEvaluator());
    }

    public RatioErrorAnalyzer(ErrorEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public List<RatioErrorPoint> analyze(List<Document> population,
                                         List<List<String>> queries,
                                         List<Double> ratios,
                                         long seed) {
        InvertedIndex fullIndex = InvertedIndex.build(population);
        List<RatioErrorPoint> points = new ArrayList<>();
        for (double ratio : ratios) {
            SampledIndex sampledIndex = SampledIndex.build(population, new RandomSampler(ratio, seed));
            ErrorReport report = evaluator.evaluate(sampledIndex, fullIndex, queries);
            points.add(new RatioErrorPoint(ratio, sampledIndex.sampleSize(),
                    report.hitRate(), report.missRate()));
        }
        return List.copyOf(points);
    }

    public List<RatioErrorPoint> analyze(List<Document> population,
                                         List<List<String>> queries,
                                         List<Double> ratios,
                                         SamplerFactory samplerFactory) {
        InvertedIndex fullIndex = InvertedIndex.build(population);
        List<RatioErrorPoint> points = new ArrayList<>();
        for (double ratio : ratios) {
            SampledIndex sampledIndex = SampledIndex.build(population, samplerFactory.create(ratio));
            ErrorReport report = evaluator.evaluate(sampledIndex, fullIndex, queries);
            points.add(new RatioErrorPoint(ratio, sampledIndex.sampleSize(),
                    report.hitRate(), report.missRate()));
        }
        return List.copyOf(points);
    }

    @FunctionalInterface
    public interface SamplerFactory {
        Sampler create(double ratio);
    }
}
