package cn.cordys.crm.integration.dingtalk.response;

import cn.cordys.crm.integration.dingtalk.dto.DingTalkDepartment;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkUser;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 钉钉组织架构和用户数据响应 */
@Data
public class DingTalkOrgDataResponse {

  /** 部门列表 */
  private List<DingTalkDepartment> departments;

  /** 用户列表 */
  private Map<Long, List<DingTalkUser>> users;
}
