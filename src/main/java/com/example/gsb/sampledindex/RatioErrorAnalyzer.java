package com.example.gsb.sampledindex;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 采样比例与误差关系分析：在多个采样比例下分别建采样索引，
 * 与同一个全量索引对比，输出各比例下的漏检率数据。
 */
public final class RatioErrorAnalyzer {

    private final ErrorEvaluator evaluator = new ErrorEvaluator();

    public List<RatioErrorPoint> analyze(List<DataRecord> data,
                                         List<String> queries,
                                         List<Double> ratios,
                                         Sampler sampler,
                                         long seed) {
        InvertedIndex fullIndex = InvertedIndex.build(data, 1.0, "full");
        List<RatioErrorPoint> points = new ArrayList<>();
        int index = 0;
        for (double ratio : ratios) {
            Random random = new Random(seed + index);
            List<DataRecord> sample = sampler.sample(data, ratio, random);
            InvertedIndex sampledIndex = InvertedIndex.build(sample, ratio, sampler.name());
            ErrorReport report = evaluator.evaluate(sampledIndex, fullIndex, queries);
            points.add(new RatioErrorPoint(ratio,
                    sampledIndex.indexedRecordCount(),
                    sampledIndex.estimatedSizeBytes(),
                    report.hitRate(),
                    report.missRate()));
            index++;
        }
        return List.copyOf(points);
    }
}
