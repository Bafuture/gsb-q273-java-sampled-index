package com.example.gsb.sampledindex;

/**
 * 某个采样比例下的误差数据点。
 *
 * @param ratio     配置的采样比例
 * @param sampleSize 实际样本量（分层抽样可能高于 ratio * 数据量）
 * @param hitRate   命中率（召回率）
 * @param missRate  漏检率
 */
public record RatioErrorPoint(double ratio, int sampleSize, double hitRate, double missRate) {
}
