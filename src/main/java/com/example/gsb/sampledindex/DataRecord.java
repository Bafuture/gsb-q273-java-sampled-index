package com.example.gsb.sampledindex;

import java.util.Objects;

/**
 * 一条待索引的数据记录。
 *
 * @param id      全局唯一 id
 * @param key     索引键（查询按 key 精确匹配）
 * @param stratum 分层维度取值（分层抽样按此字段分组）
 */
public record DataRecord(long id, String key, String stratum) {

    public DataRecord {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(stratum, "stratum must not be null");
    }
}
