package com.example.gsb.sampledindex;

import java.util.List;
import java.util.Random;

/**
 * 抽样策略：从全量数据中按给定比例抽取样本。
 */
public interface Sampler {

    /**
     * @param data   全量数据
     * @param ratio  采样比例，取值 (0, 1]
     * @param random 随机源（由调用方传入固定种子以保证可复现）
     * @return 样本列表；实现需保证 ratio &gt; 0 且 data 非空时样本非空
     */
    List<DataRecord> sample(List<DataRecord> data, double ratio, Random random);

    /** 策略名称，用于索引元信息与统计。 */
    String name();

    static void checkRatio(double ratio) {
        if (Double.isNaN(ratio) || ratio <= 0.0 || ratio > 1.0) {
            throw new IllegalArgumentException("ratio must be in (0, 1], got: " + ratio);
        }
    }
}
