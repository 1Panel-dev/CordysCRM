package cn.cordys.crm.integration.wecom.response;

import cn.cordys.crm.integration.wecom.dto.WeComDepartment;
import java.util.List;
import lombok.Data;

/** 企业微信部门 响应对象 */
@Data
public class WeComDepartmentListResponse extends WeComResponseEntity {
  /** 部门列表数据。 */
  private List<WeComDepartment> department;
}
