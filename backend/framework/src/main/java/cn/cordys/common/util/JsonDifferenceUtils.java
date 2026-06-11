package cn.cordys.common.util;

import cn.cordys.common.dto.JsonDifferenceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonDifferenceUtils {

    public static List<JsonDifferenceDTO> compareJson(String oldJson, String newJson, List<JsonDifferenceDTO> JsonDifferenceDTO) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode oldNode = oldJson != null ? mapper.readTree(oldJson) : mapper.createObjectNode();
        JsonNode newNode = newJson != null ? mapper.readTree(newJson) : mapper.createObjectNode();
        if (oldNode.isArray() && newNode.isArray()) {
            compareArrayNodesById(oldNode, newNode, JsonDifferenceDTO);
        }
        compareJsonNodes(oldNode, newNode, JsonDifferenceDTO);
        return JsonDifferenceDTO;
    }

    private static void compareJsonNodes(JsonNode oldNode, JsonNode newNode, List<JsonDifferenceDTO> JsonDifferenceDTOList) {
        Iterator<String> fieldNames1 = oldNode.fieldNames();
        //遍历
        while (fieldNames1.hasNext()) {
            String fieldName = fieldNames1.next();
            JsonNode oldValue = oldNode.get(fieldName);
            JsonNode newValue = newNode.get(fieldName);
            if (!newNode.has(fieldName)) {
                //删除的属性
                JsonDifferenceDTO removed = new JsonDifferenceDTO();
                removed.setColumn(fieldName);
                removed.setOldValue(getValue(oldValue));
                removed.setType("removed");
                JsonDifferenceDTOList.add(removed);
            } else if (!isNodeEquals(oldValue, newValue)) {
                //更新的属性
                JsonDifferenceDTO diff = new JsonDifferenceDTO();
                diff.setColumn(fieldName);
                diff.setOldValue(getValue(oldValue));
                diff.setNewValue(getValue(newValue));
                diff.setType("modified");
                JsonDifferenceDTOList.add(diff);
            }
        }

        //遍历查找新增的属性
        Iterator<String> fieldNames2 = newNode.fieldNames();
        while (fieldNames2.hasNext()) {
            String fieldName = fieldNames2.next();
            if (!oldNode.has(fieldName)) {
                JsonDifferenceDTO add = new JsonDifferenceDTO();
                add.setColumn(fieldName);
                add.setNewValue(getValue(newNode.get(fieldName)));
                add.setType("add");
                JsonDifferenceDTOList.add(add);
            }
        }
    }

    private static boolean isNodeEquals(JsonNode oldValue, JsonNode newValue) {
        if (oldValue.isNumber() && newValue.isNumber()) {
            // 避免小数点，科学计数法等格式导致的比较不一致
            return oldValue.asDouble() == newValue.asDouble();
        }
        // 数字与字符串的跨类型比较（如计算字段: 99 vs "99"）
        if (oldValue.isNumber() && newValue.isTextual()) {
            return isNumericTextEqual(oldValue, newValue);
        }
        if (oldValue.isTextual() && newValue.isNumber()) {
            return isNumericTextEqual(newValue, oldValue);
        }
        return oldValue.equals(newValue);
    }

    /**
     * 比较数值节点与文本节点是否相等
     * 解决计算字段等场景中，同一值因类型不同（数字 vs 字符串）导致的误判
     *
     * @param numberNode 数值节点
     * @param textNode   文本节点
     * @return 是否相等
     */
    private static boolean isNumericTextEqual(JsonNode numberNode, JsonNode textNode) {
        try {
            String text = textNode.asText();
            if (text.contains(".") || text.contains("e") || text.contains("E")) {
                return numberNode.asDouble() == Double.parseDouble(text);
            }
            return numberNode.asLong() == Long.parseLong(text);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 比较数组节点 (ID作为节点唯一性)
     *
     * @param oldNode           旧节点
     * @param newNode           新节点
     * @param jsonDifferenceDTO 差异属性集合  [节点1:属性1:值1 => 值2]
     */
    private static void compareArrayNodesById(JsonNode oldNode, JsonNode newNode, List<JsonDifferenceDTO> jsonDifferenceDTO) {
        List<String> oldNodeIds = new ArrayList<>();
        Iterator<JsonNode> elements = oldNode.elements();
        while (elements.hasNext()) {
            JsonNode oldElement = elements.next();
            boolean found = false;
            oldNodeIds.add(oldElement.get("id").asText());
            for (JsonNode newElement : newNode) {
                if (newElement.get("id").equals(oldElement.get("id"))) {
                    compareJsonNodes(oldElement, newElement, jsonDifferenceDTO);
                    found = true;
                    break;
                }
            }
            if (!found) {
                JsonDifferenceDTO removed = new JsonDifferenceDTO();
                removed.setOldValue(oldElement.get("name"));
                removed.setType("removed");
                jsonDifferenceDTO.add(removed);
            }
        }

        for (JsonNode newElement : newNode) {
            if (!oldNodeIds.contains(newElement.get("id").asText())) {
                JsonDifferenceDTO added = new JsonDifferenceDTO();
                added.setNewValue(newElement.get("name"));
                added.setType("add");
                jsonDifferenceDTO.add(added);
            }
        }
    }

    public static Object getValue(JsonNode jsonNode) {
        if (jsonNode.isArray() || jsonNode.isObject()) {
            return JSON.parseObject(jsonNode.toString());
        } else {
            return jsonNode.asText();
        }
    }

}