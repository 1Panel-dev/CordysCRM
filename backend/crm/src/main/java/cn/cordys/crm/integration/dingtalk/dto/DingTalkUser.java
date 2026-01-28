package cn.cordys.crm.integration.dingtalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/** 钉钉用户 DTO */
@Data
public class DingTalkUser {

  /** 员工的userid */
  @JsonProperty("userid")
  private String userId;

  /** 员工在当前开发者企业账号范围内的唯一标识 */
  @JsonProperty("unionid")
  private String unionId;

  /** 员工名称 */
  private String name;

  /** 头像 */
  private String avatar;

  /** 国际电话区号 */
  @JsonProperty("state_code")
  private String stateCode;

  /** 手机号码 */
  private String mobile;

  /** 是否号码隐藏 */
  @JsonProperty("hide_mobile")
  private Boolean hideMobile;

  /** 分机号 */
  private String telephone;

  /** 员工工号 */
  @JsonProperty("job_number")
  private String jobNumber;

  /** 职位 */
  private String title;

  /** 员工邮箱 */
  private String email;

  /** 办公地点 */
  @JsonProperty("work_place")
  private String workPlace;

  /** 备注 */
  private String remark;

  /** 所属部门id列表 */
  @JsonProperty("dept_id_list")
  private List<Long> deptIdList;

  /** 员工在部门中的排序 */
  @JsonProperty("dept_order")
  private Long deptOrder;

  /** 扩展属性 */
  private String extension;

  /** 入职时间 */
  @JsonProperty("hired_date")
  private Long hiredDate;

  /** 是否激活 */
  private Boolean active;

  /** 是否实名认证 */
  @JsonProperty("real_authed")
  private Boolean realAuthed;

  /** 是否高管 */
  private Boolean senior;

  /** 是否管理员 */
  private Boolean admin;

  /** 是否老板 */
  private Boolean boss;

  /** 是否是部门主管 */
  private Boolean leader;
}
