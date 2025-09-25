package cn.cordys.common.dto;
import cn.cordys.common.util.JSON;
import lombok.Data;

import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2025-09-25  11:37
 */
@Data
public class BatchUpdateDbParam {
    private List<String> ids;
    private String fieldName;
    private Object fieldValue;
    private String updateUser;
    private Long updateTime;

    public Object getFieldValue() {
        if (fieldValue != null && fieldValue instanceof List) {
            return JSON.toJSONString(fieldValue);
        }
        return fieldValue;
    }
}
