package com.example.gsb.sampledindex;

/**
 * 运行期统计快照。
 *
 * @param sampleSize       当前索引样本量
 * @param indexSizeBytes   当前索引大小估算（字节）
 * @param averageMissRate  历次误差评估登记的平均（宏平均）漏检率
 * @param rebuildCount     索引重建次数（首次构建不计入）
 * @param currentRatio     当前采样比例
 * @param strategy         当前抽样策略
 */
public record IndexStats(int sampleSize,
                         long indexSizeBytes,
                         double averageMissRate,
                         int rebuildCount,
                         double currentRatio,
                         String strategy) {
}
