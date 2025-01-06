package io.cordys.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cordys.common.dto.JsonDifferenceDTO;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonDifferenceUtils {

    public static List<JsonDifferenceDTO> compareJson(String oldJson, String newJson, List<JsonDifferenceDTO> JsonDifferenceDTO) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode oldNode = oldJson != null ? mapper.readTree(oldJson) : mapper.createObjectNode();
        JsonNode newNode = newJson != null ? mapper.readTree(newJson) : mapper.createObjectNode();
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
                removed.setOldValue(oldValue.toString());
                removed.setType("removed");
                JsonDifferenceDTOList.add(removed);
            } else if (!oldValue.equals(newValue)) {
                if (oldValue.isObject() && newValue.isObject()) {
                    //递归比较子节点
                    List<JsonDifferenceDTO> children = new ArrayList<>();
                    compareJsonNodes(oldValue, newValue, children);
                    JsonDifferenceDTOList.addAll(children);
                } else {
                    //更新的属性
                    JsonDifferenceDTO diff = new JsonDifferenceDTO();
                    diff.setColumn(fieldName);
                    diff.setOldValue(oldValue.toString());
                    diff.setNewValue(newValue.toString());
                    diff.setType("modified");
                    JsonDifferenceDTOList.add(diff);
                }
            }
        }

        //遍历查找新增的属性
        Iterator<String> fieldNames2 = newNode.fieldNames();
        while (fieldNames2.hasNext()) {
            String fieldName = fieldNames2.next();
            if (!oldNode.has(fieldName)) {
                JsonDifferenceDTO add = new JsonDifferenceDTO();
                add.setColumn(fieldName);
                add.setNewValue(newNode.get(fieldName).toString());
                add.setType("add");
                JsonDifferenceDTOList.add(add);
            }
        }
    }

}