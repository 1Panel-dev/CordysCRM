package cn.cordys.crm.integration.dingtalk.response;

import cn.cordys.crm.integration.dingtalk.service.DingTalkDepartmentService;
import lombok.Data;

@Data
public class SubDeptIdListResponse extends DingTalkResponseEntity{

    private SubDeptIdListResult result;
}
