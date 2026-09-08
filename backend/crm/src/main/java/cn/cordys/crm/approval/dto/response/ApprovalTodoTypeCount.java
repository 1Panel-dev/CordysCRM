package cn.cordys.crm.approval.dto.response;

import lombok.Data;

/**
 * 待我审批按资源类型分组的数量统计结果。
 */
@Data
public class ApprovalTodoTypeCount {

    private String type;

    private Integer count;
}
