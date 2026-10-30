package com.hongik.graduate.ai.common;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Versioned template; inserts data literally (never re-interprets input as a template). */
public record AiPromptTemplate(String name, String version, String body) {
    private static final Pattern VARIABLE =
            Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]*)}");

    public AiPromptTemplate {
        if (name == null || !name.matches("[a-z][a-z0-9-]{0,79}")
                || version == null || !version.matches("v[1-9][0-9]*")
                || body == null || body.isBlank()) {
            throw new IllegalArgumentException("invalid prompt template");
        }
        String withoutValidVariables = VARIABLE.matcher(body).replaceAll("");
        if (withoutValidVariables.contains("${")) {
            throw new IllegalArgumentException("malformed prompt placeholder");
        }
    }

    public Set<String> requiredVariables() {
        Matcher matcher = VARIABLE.matcher(body);
        Set<String> names = new LinkedHashSet<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return Set.copyOf(names);
    }

    public String render(AiContext context) {
        Matcher matcher = VARIABLE.matcher(body);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = context.values().get(name);
            if (!(value instanceof String text) || text.isBlank()) {
                throw new AiProviderException(
                        AiProviderException.Code.INVALID_REQUEST,
                        "missing or non-text prompt variable: " + name);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(text));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
