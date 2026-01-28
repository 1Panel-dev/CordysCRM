package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_notification")
public class Notification {

  @Schema(description = "ID")
  private String id;

  @Schema(description = "通知类型")
  private String type;

  @Schema(description = "接收人")
  private String receiver;

  @Schema(description = "标题")
  private String subject;

  @Schema(description = "状态")
  private String status;

  @Schema(description = "操作人")
  private String operator;

  @Schema(description = "操作")
  private String operation;

  @Schema(description = "组织ID")
  private String organizationId;

  @Schema(description = "资源ID")
  private String resourceId;

  @Schema(description = "资源类型")
  private String resourceType;

  @Schema(description = "资源名称")
  private String resourceName;

  @Schema(description = "通知内容")
  private byte[] content;

  @Schema(description = "创建人")
  private String createUser;

  @Schema(description = "修改人")
  private String updateUser;

  @Schema(description = "创建时间")
  private Long createTime;

  @Schema(description = "更新时间")
  private Long updateTime;
}
