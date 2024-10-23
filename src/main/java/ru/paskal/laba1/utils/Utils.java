package ru.paskal.laba1.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.commons.text.StringEscapeUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Utils {

    public static InputStream getResourceAsStream(String resourcePath) {
        return Utils.class.getClassLoader().getResourceAsStream(resourcePath);
    }

    public static boolean isResourceUrl(String url) {
        return url.startsWith("res://");
    }

    public static String getResourcePath(String url) {
        return url.replace("res://", "");
    }

    public static String getShortJsonStr(JsonNode jsonNode) {
        var jsonStr = jsonNode.toPrettyString();
        return jsonStr.length() > 300 ? jsonStr.substring(0, 300) + "..." : jsonStr;
    }

    public static String getShortStr(String jsonStr) {
        return jsonStr.length() > 300 ? jsonStr.substring(0, 300) + "..." : jsonStr;
    }

    public static String toPlainUrl(String url) {
        if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        if (url.startsWith("res://")) {
            url = url.substring(6);
        }
        if (url.endsWith(".json")) {
            url = url.substring(0, url.length() - 5);
        }
        // Replace invalid characters with underscores
        url = url.replaceAll("[^a-zA-Z0-9]", "_");
        // Ensure the table name starts with a letter
        if (!Character.isLetter(url.charAt(0))) {
            url = "t_" + url;
        }
        return url;
    }

    public static String getSqlType(JsonNodeType jsonNodeType) {
        return switch (jsonNodeType) {
            case ARRAY, OBJECT, STRING, POJO -> "TEXT";
            case BINARY -> "BYTEA";
            case BOOLEAN -> "BOOLEAN";
            case MISSING, NULL -> "NULL";
            case NUMBER -> "NUMERIC";
        };
    }

    public static Object wrapInSqlType(Object value, String sqlType) {
        return switch (sqlType.toUpperCase()) {
            case "TEXT", "BYTEA" -> !Objects.equals(value.toString(), "\"\"") ? escapeWithQuotes(value.toString().replaceAll("\"", "").replaceAll("'", "''")) : "NULL";
            case "BOOLEAN" -> Boolean.parseBoolean(value.toString()) ? "TRUE" : "FALSE";
            case "NUMERIC" -> Double.parseDouble(value.toString());
            case "NULL" -> "NULL";
            default -> throw new IllegalArgumentException("Unsupported SQL type: " + sqlType);
        };
    }

    public static String escapeWithQuotes(String string) {
        return "'" + string + "'";
    }

    public static String escapeWithDoubleQuotes(String string) {
        return "\"" + string + "\"";
    }

    public static Map<String, String> mapTypesToSql(Map<String, JsonNodeType> types) {
        return types.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), getSqlType(e.getValue())), Map::putAll);
    }

    public static String escapeJsonString(String jsonString) {
        return StringEscapeUtils.escapeJson(jsonString.replaceAll("'", "\"\""));
    }
}
