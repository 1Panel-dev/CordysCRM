package cn.cordys.crm.integration.dataease.dto;

import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class SysVariableDTO {
    private String id;

    private String type = "text";

    private String name;

    private String min;

    private String max;

    private String startTime;

    private String endTime;

    private boolean root;

    private boolean disabled;
}
