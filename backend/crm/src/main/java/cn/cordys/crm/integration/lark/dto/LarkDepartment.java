package cn.cordys.crm.integration.lark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LarkDepartment {

  /** 部门名称 */
  private String name;

  /** 父部门的 department_id */
  @JsonProperty("parent_department_id")
  private String parentDepartmentId;

  /** 部门的 department_id */
  @JsonProperty("department_id")
  private String departmentId;

  /** 部门的 open_department_id */
  @JsonProperty("open_department_id")
  private String openDepartmentId;

  /** 部门主管的用户 ID */
  @JsonProperty("leader_user_id")
  private String leaderUserId;

  /** 部门的群 ID */
  @JsonProperty("chat_id")
  private String chatId;

  /** 部门的排序 */
  private Integer order;

  /** 部门的状态 */
  private Status status;

  @Data
  public static class Status {
    /** 是否已删除 */
    @JsonProperty("is_deleted")
    private boolean isDeleted;
  }
}
