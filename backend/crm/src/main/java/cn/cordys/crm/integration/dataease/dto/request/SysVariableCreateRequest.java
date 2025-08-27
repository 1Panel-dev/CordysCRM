package cn.cordys.crm.integration.dataease.dto.request;

import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-15  16:30
 */
@Data
public class SysVariableCreateRequest {
    private String name;
    private String type = "text";
    private String desc;
}
