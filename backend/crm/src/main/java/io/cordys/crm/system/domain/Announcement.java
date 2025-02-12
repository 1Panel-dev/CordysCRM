package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_announcement")
public class Announcement extends BaseModel {
    @Schema(description = "公告标题")
    private String subject;

    @Schema(description = "公告内容")
    private byte[] content;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;

    @Schema(description = "链接")
    private String url;

    @Schema(description = "接收者")
    private byte[] receiver;

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "接收类型组合")
    private byte[] receiveType;

}
