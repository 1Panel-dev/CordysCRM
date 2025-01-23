package io.cordys.crm.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class MessageDetailDTO implements Serializable {
    private List<String> receiverIds = new ArrayList<>();
    private String id;
    private String event;
    private String taskType;
    private String webhook;
    private String resourceId;
    private Long createTime;
    private String subject;
    private String template;
    private String appKey;
    private String appSecret;
    private String organizationId;
    private String receiveType;
}
