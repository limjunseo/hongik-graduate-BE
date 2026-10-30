package com.hongik.graduate.ai.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable snapshot of JSON-compatible context, including nested collections. */
public record AiContext(Map<String, Object> values) {
    private static final int MAX_DEPTH = 32;

    public AiContext {
        if (values == null) {
            throw new IllegalArgumentException("AI context is required");
        }
        values = copyMap(values, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    private static Map<String, Object> copyMap(
            Map<?, ?> source, Set<Object> active, int depth) {
        enter(source, active, depth);
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            for (var entry : source.entrySet()) {
                if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                    throw new IllegalArgumentException("AI context keys must be non-blank strings");
                }
                result.put(key, snapshot(entry.getValue(), active, depth + 1));
            }
            return Collections.unmodifiableMap(result);
        } finally {
            active.remove(source);
        }
    }

    private static Object snapshot(Object value, Set<Object> active, int depth) {
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long
                || value instanceof java.math.BigInteger
                || value instanceof java.math.BigDecimal) {
            return value;
        }
        if (value instanceof Double d && Double.isFinite(d)
                || value instanceof Float f && Float.isFinite(f)) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            return copyMap(map, active, depth);
        }
        if (value instanceof List<?> list) {
            enter(list, active, depth);
            try {
                List<Object> copy = new ArrayList<>(list.size());
                for (Object element : list) {
                    copy.add(snapshot(element, active, depth + 1));
                }
                return Collections.unmodifiableList(copy);
            } finally {
                active.remove(list);
            }
        }
        throw new IllegalArgumentException("unsupported AI context value type");
    }

    private static void enter(Object value, Set<Object> active, int depth) {
        if (depth > MAX_DEPTH || !active.add(value)) {
            throw new IllegalArgumentException("AI context is cyclic or too deeply nested");
        }
    }
}
