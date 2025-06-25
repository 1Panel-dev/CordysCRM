package io.cordys.crm.system.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageDetailDTO implements Serializable {
    private String id;
    private String event;
    private String taskType;
    private Long createTime;
    private String template;
    private String subject;
    private String organizationId;
    private boolean emailEnable;
    private boolean sysEnable;

}
