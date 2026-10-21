package cn.cordys.crm.integration.dataease.dto.request;

import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class SysVariableCreateRequest {
    private String name;
    private String type = "text";
    private String desc;
}
