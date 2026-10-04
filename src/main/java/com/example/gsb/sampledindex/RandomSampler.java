package com.example.gsb.sampledindex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 简单随机抽样：对全量数据无差别抽取。
 *
 * <p>优点：实现简单、无偏。缺点：占比极小的类别可能完全抽不到，
 * 若查询集中在稀有类别上，漏检率会显著偏高——这种场景应使用
 * {@link StratifiedSampler}。
 */
public final class RandomSampler implements Sampler {

    @Override
    public List<DataRecord> sample(List<DataRecord> data, double ratio, Random random) {
        Sampler.checkRatio(ratio);
        if (data.isEmpty()) {
            return List.of();
        }
        List<DataRecord> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, random);
        int sampleSize = Math.min(data.size(), Math.max(1, (int) Math.round(data.size() * ratio)));
        return List.copyOf(shuffled.subList(0, sampleSize));
    }

    @Override
    public String name() {
        return "random";
    }
}
