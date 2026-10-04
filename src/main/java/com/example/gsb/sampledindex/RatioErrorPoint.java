package com.example.gsb.sampledindex;

/**
 * 某个采样比例下的误差数据点。
 *
 * @param ratio         采样比例
 * @param sampleSize    样本量
 * @param indexSizeBytes 索引大小估算（字节）
 * @param hitRate       命中率
 * @param missRate      漏检率
 */
public record RatioErrorPoint(double ratio,
                              int sampleSize,
                              long indexSizeBytes,
                              double hitRate,
                              double missRate) {
}
