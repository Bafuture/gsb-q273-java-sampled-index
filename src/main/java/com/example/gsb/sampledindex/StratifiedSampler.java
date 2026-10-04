package com.example.gsb.sampledindex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分层抽样：按 {@code strataKey} 指定的维度把数据分成若干层，每层独立抽样，
 * 并保证每层至少保留 {@code minPerStratum} 条（层本身不足时全保留），
 * 从而保证稀有类别在样本中必有代表。
 *
 * <p>注意：由于每层有保底条数，实际采样比例可能高于配置比例，
 * 尤其当层数多、每层数据少时。分层键的选择影响见 README。</p>
 */
public final class StratifiedSampler implements Sampler {

    /** 缺失分层键的文档归入的层名。 */
    public static final String MISSING_STRATUM = "<missing>";

    private final double ratio;
    private final String strataKey;
    private final int minPerStratum;
    private final long seed;

    public StratifiedSampler(double ratio, String strataKey, int minPerStratum, long seed) {
        if (ratio < 0.0 || ratio > 1.0) {
            throw new IllegalArgumentException("ratio must be in [0, 1]: " + ratio);
        }
        if (strataKey == null || strataKey.isBlank()) {
            throw new IllegalArgumentException("strataKey must not be blank");
        }
        if (minPerStratum < 0) {
            throw new IllegalArgumentException("minPerStratum must be >= 0");
        }
        this.ratio = ratio;
        this.strataKey = strataKey;
        this.minPerStratum = minPerStratum;
        this.seed = seed;
    }

    @Override
    public List<Document> sample(List<Document> population) {
        Map<String, List<Document>> strata = new LinkedHashMap<>();
        for (Document doc : population) {
            strata.computeIfAbsent(stratumOf(doc), k -> new ArrayList<>()).add(doc);
        }
        List<Document> sample = new ArrayList<>();
        for (List<Document> stratum : strata.values()) {
            int target = (int) Math.max(minPerStratum, Math.round(ratio * stratum.size()));
            target = Math.min(target, stratum.size());
            // 层内按确定性哈希排序后取前 target 条：可复现，且比例增大时样本嵌套增长。
            stratum.stream()
                    .sorted(Comparator.comparingDouble(doc -> RandomSampler.unitInterval(doc.id(), seed)))
                    .limit(target)
                    .forEach(sample::add);
        }
        return sample;
    }

    private String stratumOf(Document doc) {
        return doc.attribute(strataKey).orElse(MISSING_STRATUM);
    }

    @Override
    public String describe() {
        return "StratifiedSampler(ratio=" + ratio + ", key=" + strataKey
                + ", minPerStratum=" + minPerStratum + ", seed=" + seed + ")";
    }
}
