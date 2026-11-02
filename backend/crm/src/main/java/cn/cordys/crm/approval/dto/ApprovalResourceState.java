package cn.cordys.crm.approval.dto;

import lombok.Data;

/**
 * 加锁读取的业务资源审批状态。
 */
@Data
public class ApprovalResourceState {

    private String approvalStatus;
    private Boolean approved;
    private String createUser;
    private String owner;
}
