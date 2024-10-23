package ru.paskal.laba1.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.paskal.laba1.utils.JsonPair;
import ru.paskal.laba1.utils.Utils;

import java.util.*;

@SuppressWarnings("LoggingSimilarMessage")
@Component
@Slf4j
@RequiredArgsConstructor
public class Handler {

    private String lastPlainUrl;

    private JsonPair serializedJson;

    private final Dao dao;

    private final ObjectMapper objectMapper;

    public void handleResponse(String plainUrl, String json) {
        lastPlainUrl = plainUrl;
        JsonNode jsonNode;
        try {
            log.info("Get json from {}:\n{}", plainUrl, Utils.getShortStr(json));
            jsonNode = objectMapper.readTree(json);
            log.info("Successfully parsed JSON");
        } catch (Exception e) {
            log.error("Error parsing JSON: {}", e.getMessage());
            return;
        }
        serializedJson = recognizeAndHandleType(jsonNode);
        dao.save(lastPlainUrl, serializedJson);

    }

    /**
     * Виды Json:
     * 1. [ {obj1}, {obj2}, {obj3} ]
     * 2. { "meta or metadata or info": {}, "other": [ {obj}, {obj} ] }
     * 3. { "key": "value", "key2": "value2" }
     * 4. [ {meta}, [ {obj}, {obj} ] ]
     * 5. { "key": [ {obj}, {obj} ] }
     * 6. { "result": {obj} }
     */
    private JsonPair recognizeAndHandleType(JsonNode jsonNode) {
        if (jsonNode.isArray()) {
            if (jsonNode.size() > 1 && jsonNode.get(0).isObject() && jsonNode.get(1).isArray()) {
                return handleType4(jsonNode);
            } else {
                return handleType1(jsonNode);
            }
        } else if (jsonNode.isObject()) {
            if (jsonNode.has("meta") || jsonNode.has("metadata") || jsonNode.has("info")) {
                return handleType2(jsonNode);
            } else if (jsonNode.size() == 1) {
                JsonNode value = jsonNode.elements().next();
                if (value.isArray()) {
                    return handleType5(jsonNode);
                } else if (value.isObject()) {
                    return handleType6(jsonNode);
                } else {
                    return handleType3(jsonNode);
                }
            } else {
                return handleType3(jsonNode);
            }
        } else {
            return handleInvalid(jsonNode);
        }
    }


    /**
     * @param json recognized as [ {obj1}, {obj2}, {obj3} ] like
     */
    private JsonPair handleType1(JsonNode json) {
        log.info("Recognized as Type 1 JSON:\n{}", Utils.getShortJsonStr(json));
        return handleJsonArray(json);
    }

    /**
     * @param json recognized as { "meta or metadata or info": {}, "other": [ {obj}, {obj} ] } like
     */
    private JsonPair handleType2(JsonNode json) {
        log.info("Recognized as Type 2 JSON:\n{}", Utils.getShortJsonStr(json));

        String arrayKey = null;
        Iterator<String> fieldNames = json.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!fieldName.equals("meta") && !fieldName.equals("metadata") && !fieldName.equals("info")) {
                arrayKey = fieldName;
                break;
            }
        }

        if (arrayKey != null && json.has(arrayKey) && json.get(arrayKey).isArray()) {
            return handleJsonArray(json.get(arrayKey));
        } else {
            log.warn("No valid array key found in the JSON object.");
            return new JsonPair();
        }
    }

    /**
     * @param json recognized as { "key": "value", "key2": "value2" } like
     */
    private JsonPair handleType3(JsonNode json) {
        log.info("Recognized as Type 3 JSON:\n{}", Utils.getShortJsonStr(json));
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(json);
        return handleJsonArray(arrayNode);
    }

    /**
     * @param json recognized as [ {meta}, [ {obj}, {obj} ] ] like
     */
    private JsonPair handleType4(JsonNode json) {
        log.info("Recognized as Type 4 JSON:\n{}", Utils.getShortJsonStr(json));
        if (json.isArray() && json.size() > 1) {
            JsonNode secondElement = json.get(1);
            if (secondElement.isArray()) {
                return handleJsonArray(secondElement);
            } else {
                log.warn("The second element is not an array.");
            }
        } else {
            log.warn("The JSON array does not have enough elements.");
        }
        return new JsonPair();
    }

    /**
     * @param json recognized as { "key": [ {obj}, {obj} ] } like
     */
    private JsonPair handleType5(JsonNode json) {
        log.info("Recognized as Type 5 JSON:\n{}", Utils.getShortJsonStr(json));

        String arrayKey = null;
        Iterator<String> fieldNames = json.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (json.get(fieldName).isArray()) {
                arrayKey = fieldName;
                break;
            }
        }

        if (arrayKey != null) {
            JsonNode arrayNode = json.get(arrayKey);
            return handleJsonArray(arrayNode);
        } else {
            log.warn("No valid array key found in the JSON object.");
        }
        return new JsonPair();
    }

    /**
     * @param json recognized as { "result": {obj} } like
     */
    private JsonPair handleType6(JsonNode json) {
        log.info("Recognized as Type 6 JSON:\n{}", Utils.getShortJsonStr(json));
        String objKey = null;
        Iterator<String> fieldNames = json.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (json.get(fieldName).isObject()) {
                objKey = fieldName;
                break;
            }
        }

        if (objKey != null) {
            JsonNode objNode = json.get(objKey);
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add(objNode);
            return handleJsonArray(arrayNode);
        } else {
            log.warn("No valid object key found in the JSON object.");
        }
        return new JsonPair();
    }

    /**
     * @param json unrecognized JSON
     */
    private JsonPair handleInvalid(JsonNode json) {
        log.info("Unknown JSON:\n{}", Utils.getShortJsonStr(json));
        return new JsonPair();
    }


    private JsonPair handleJsonArray(JsonNode json) {
        Map<String, JsonNodeType> keyTypeMap = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();

        if (json.isArray() && !json.isEmpty()) {
            for (JsonNode element : json) {
                Map<String, Object> map = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = element.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String fieldName = field.getKey().equals("id") ? "from_json_id" : field.getKey();
                    if (field.getValue().getNodeType() != JsonNodeType.NULL) {
                        keyTypeMap.put(fieldName, field.getValue().getNodeType());
                    }
                    if (field.getValue().getNodeType() == JsonNodeType.OBJECT) {
                        map.put(fieldName, field.getValue().toString());
                    } else {
                        map.put(fieldName, field.getValue());
                    }
                }
                list.add(map);
            }
        } else {
            log.warn("The JSON array is empty or not valid.");
        }
        log.info("Json successfully serialized.");
        return new JsonPair(keyTypeMap, list);
    }
}
