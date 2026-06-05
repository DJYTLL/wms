package com.example.wms.monitor;

import org.apache.ibatis.mapping.BoundSql;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

final class SqlTimingFormatter {
    private SqlTimingFormatter() {
    }

    static String normalizeSql(BoundSql boundSql) {
        if (boundSql == null || boundSql.getSql() == null) {
            return "";
        }
        return boundSql.getSql().replaceAll("\\s+", " ").trim();
    }

    static String summarizeParameter(Object parameter, boolean enabled) {
        if (!enabled) {
            return "[disabled]";
        }
        if (parameter == null) {
            return "null";
        }
        if (parameter instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), simplify(entry.getValue()));
            }
            return normalized.toString();
        }
        return String.valueOf(simplify(parameter));
    }

    private static Object simplify(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value.getClass().isEnum()) {
            return value;
        }
        if (value.getClass().isArray()) {
            return "[array]";
        }
        if (value instanceof Collection<?> collection) {
            return "[collection size=" + collection.size() + "]";
        }
        if (value instanceof Map<?, ?> map) {
            return "[map size=" + map.size() + "]";
        }
        return value.getClass().getSimpleName();
    }
}
