package com.example.gsb.sampledindex;

import java.util.List;

/**
 * 均匀随机抽样。
 *
 * <p>实现上对每个文档 id 做确定性哈希（splitmix64），哈希值小于阈值即入选。
 * 相比逐条掷随机数，这带来两个工程上很重要的性质：</p>
 * <ul>
 *   <li>相同 seed + 相同比例 ⇒ 样本完全可复现；</li>
 *   <li>样本随比例嵌套：ratio1 &lt; ratio2 时 sample(ratio1) ⊆ sample(ratio2)，
 *       因此“采样比例 vs 漏检率”曲线单调不增，便于比例调参。</li>
 * </ul>
 */
public final class RandomSampler implements Sampler {

    private final double ratio;
    private final long seed;

    public RandomSampler(double ratio, long seed) {
        if (ratio < 0.0 || ratio > 1.0) {
            throw new IllegalArgumentException("ratio must be in [0, 1]: " + ratio);
        }
        this.ratio = ratio;
        this.seed = seed;
    }

    public double ratio() {
        return ratio;
    }

    @Override
    public List<Document> sample(List<Document> population) {
        return population.stream()
                .filter(doc -> unitInterval(doc.id(), seed) < ratio)
                .toList();
    }

    @Override
    public String describe() {
        return "RandomSampler(ratio=" + ratio + ", seed=" + seed + ")";
    }

    /** 将 (id, seed) 确定性地映射到 [0, 1)。 */
    static double unitInterval(String id, long seed) {
        long z = seed + id.hashCode() * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z >>> 11) * 0x1.0p-53;
    }
}
