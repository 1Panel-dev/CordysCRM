package cn.cordys.crm.integration.dingtalk.response;

import cn.cordys.crm.integration.dingtalk.dto.DingTalkDepartment;
import lombok.Data;

@Data
public class DepartmentDetailsResponse extends DingTalkResponseEntity {
    private DingTalkDepartment result;
}
