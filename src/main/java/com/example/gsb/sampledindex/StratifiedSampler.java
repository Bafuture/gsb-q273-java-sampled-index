package com.example.gsb.sampledindex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 分层抽样：按 {@link DataRecord#stratum()} 分组，每层独立按比例抽样，
 * 且每层至少保留 1 条样本，保证稀有类别也必然进入样本。
 *
 * <p>分层键选择的影响：
 * <ul>
 *   <li>分层键应与查询的过滤维度相关：若查询常按某维度过滤，按该维度分层
 *       能显著降低这些查询的漏检率；</li>
 *   <li>分层键基数过高（接近每行一个值）时，"每层至少 1 条"的保证会使实际
 *       采样率远高于配置比例，失去降本意义；</li>
 *   <li>分层键与数据分布无关时，效果退化为随机抽样，还额外付出分组开销。</li>
 * </ul>
 */
public final class StratifiedSampler implements Sampler {

    @Override
    public List<DataRecord> sample(List<DataRecord> data, double ratio, Random random) {
        Sampler.checkRatio(ratio);
        if (data.isEmpty()) {
            return List.of();
        }
        Map<String, List<DataRecord>> strata = new LinkedHashMap<>();
        for (DataRecord record : data) {
            strata.computeIfAbsent(record.stratum(), k -> new ArrayList<>()).add(record);
        }
        List<DataRecord> sample = new ArrayList<>();
        for (List<DataRecord> group : strata.values()) {
            List<DataRecord> shuffled = new ArrayList<>(group);
            Collections.shuffle(shuffled, random);
            int stratumSampleSize =
                    Math.min(group.size(), Math.max(1, (int) Math.round(group.size() * ratio)));
            sample.addAll(shuffled.subList(0, stratumSampleSize));
        }
        return List.copyOf(sample);
    }

    @Override
    public String name() {
        return "stratified";
    }
}
