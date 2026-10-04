package com.example.gsb.sampledindex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 简单的内存倒排索引：term -&gt; 文档 id 集合。查询语义为 AND（文档须包含全部查询词）。
 * 既用于全量索引（误差评估的基准），也作为采样索引的底层结构。
 */
public final class InvertedIndex {

    private final Map<String, Set<String>> postings;
    private final int documentCount;

    private InvertedIndex(Map<String, Set<String>> postings, int documentCount) {
        this.postings = postings;
        this.documentCount = documentCount;
    }

    public static InvertedIndex build(Collection<Document> documents) {
        Map<String, Set<String>> postings = new TreeMap<>();
        Set<String> seenIds = new LinkedHashSet<>();
        for (Document doc : documents) {
            if (!seenIds.add(doc.id())) {
                throw new IllegalArgumentException("duplicate document id: " + doc.id());
            }
            for (String term : tokenize(doc.text())) {
                postings.computeIfAbsent(term, k -> new TreeSet<>()).add(doc.id());
            }
        }
        Map<String, Set<String>> immutable = new TreeMap<>();
        postings.forEach((term, ids) -> immutable.put(term, Collections.unmodifiableSet(ids)));
        return new InvertedIndex(Collections.unmodifiableMap(immutable), documents.size());
    }

    /** 分词：小写化后按非字母数字切分。 */
    public static List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        if (text == null) {
            return terms;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{IsAlnum}]+")) {
            if (!token.isEmpty()) {
                terms.add(token);
            }
        }
        return terms;
    }

    /** AND 语义：返回包含全部查询词的文档 id（升序）。查询词为空时返回空集。 */
    public Set<String> query(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return Set.of();
        }
        Set<String> result = null;
        for (String raw : terms) {
            List<String> tokens = tokenize(raw);
            for (String token : tokens) {
                Set<String> ids = postings.getOrDefault(token, Set.of());
                if (result == null) {
                    result = new TreeSet<>(ids);
                } else {
                    result.retainAll(ids);
                }
                if (result.isEmpty()) {
                    return Set.of();
                }
            }
        }
        return result == null ? Set.of() : Collections.unmodifiableSet(result);
    }

    /** 索引大小：所有 posting list 的条目总数（近似占用）。 */
    public long postingCount() {
        return postings.values().stream().mapToLong(Set::size).sum();
    }

    public int termCount() {
        return postings.size();
    }

    public int documentCount() {
        return documentCount;
    }
}
