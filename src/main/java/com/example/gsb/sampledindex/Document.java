package com.example.gsb.sampledindex;

import java.util.Map;
import java.util.Optional;

/**
 * 一条待索引的数据记录。
 *
 * @param id         全局唯一标识
 * @param text       可被分词并建立倒排索引的文本
 * @param attributes 维度属性，用于分层抽样的分层键
 */
public record Document(String id, String text, Map<String, String> attributes) {

    public Document {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static Document of(String id, String text) {
        return new Document(id, text, Map.of());
    }

    public static Document of(String id, String text, Map<String, String> attributes) {
        return new Document(id, text, attributes);
    }

    public Optional<String> attribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }
}
