package cn.cordys.crm.approval.log;

import cn.cordys.crm.approval.domain.ApprovalFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalFlowLogDTO extends ApprovalFlow {

    /**
     * 流程描述
     */
    private String description;

    /**
     * 节点配置
     */
    private List<String> nodes;
}