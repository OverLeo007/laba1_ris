package ru.paskal.laba1.utils;

import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.util.List;
import java.util.Map;

public class JsonPair extends Pair<Map<String, JsonNodeType>, List<Map<String, Object>>> {
    public JsonPair(Map<String, JsonNodeType> left, List<Map<String, Object>> right) {
        super(left, right);
    }

    public JsonPair() {
        super(null, null);
    }
}