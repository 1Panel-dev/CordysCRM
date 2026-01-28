package cn.cordys.crm.integration.lark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * @author guoyuqi
 * @date 2024-07-26
 */
@Data
public class LarkUser {

  @JsonProperty("union_id")
  private String unionId;

  @JsonProperty("user_id")
  private String userId;

  @JsonProperty("open_id")
  private String openId;

  @JsonProperty("name")
  private String name;

  @JsonProperty("en_name")
  private String enName;

  @JsonProperty("email")
  private String email;

  @JsonProperty("mobile")
  private String mobile;

  /** 性别 1: 男 2: 女 0: 未知 */
  @JsonProperty("gender")
  private Integer gender;

  @JsonProperty("avatar")
  private LarkUserAvatar avatar;

  @JsonProperty("status")
  private LarkUserStatus status;

  @JsonProperty("department_ids")
  private List<String> departmentIds;

  @JsonProperty("leader_user_id")
  private String leaderUserId;

  @JsonProperty("city")
  private String city;

  @JsonProperty("country")
  private String country;

  @JsonProperty("work_station")
  private String workStation;

  @JsonProperty("join_time")
  private Long joinTime;

  @JsonProperty("is_tenant_manager")
  private Boolean isTenantManager;

  @JsonProperty("employee_no")
  private String employeeNo;

  /** 员工类型 1: 正式员工 2: 实习生 3: 顾问 4: 外部人员 */
  @JsonProperty("employee_type")
  private Integer employeeType;

  @JsonProperty("orders")
  private List<LarkUserOrder> orders;

  @Data
  public static class LarkUserAvatar {
    @JsonProperty("avatar_72")
    private String avatar72;

    @JsonProperty("avatar_240")
    private String avatar240;

    @JsonProperty("avatar_640")
    private String avatar640;

    @JsonProperty("avatar_original")
    private String avatarOriginal;
  }

  @Data
  public static class LarkUserStatus {
    @JsonProperty("is_activated")
    private Boolean isActivated;

    @JsonProperty("is_frozen")
    private Boolean isFrozen;

    @JsonProperty("is_resigned")
    private Boolean isResigned;

    @JsonProperty("is_unjoined")
    private Boolean isUnjoined;
  }

  @Data
  public static class LarkUserOrder {
    @JsonProperty("department_id")
    private String departmentId;

    @JsonProperty("user_order")
    private String userOrder;

    @JsonProperty("department_order")
    private String departmentOrder;

    @JsonProperty("is_primary_dept")
    private Boolean isPrimaryDept;
  }
}
