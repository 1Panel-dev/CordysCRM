package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Table(name = "approval_flow_version")
public class ApprovalFlowVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private String id;

    @Schema(description = "审批流ID")
    private String flowId;

    @Schema(description = "新建时执行")
    private Boolean createExecute;

    @Schema(description = "编辑时执行")
    private Boolean updateExecute;

    @Schema(description = "允许提交人撤销")
    private Boolean submitterCanRevoke;

    @Schema(description = "允许批量处理")
    private Boolean allowBatchProcess;

    @Schema(description = "允许撤回")
    private Boolean allowWithdraw;

    @Schema(description = "允许加签")
    private Boolean allowAddSign;

    @Schema(description = "重复审批人规则：FIRST_ONLY/SEQUENTIAL_ALL/EACH")
    private String duplicateApproverRule;

    @Schema(description = "是否必须填写审批意见")
    private Boolean requireComment;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "状态权限配置（JSON格式）")
    private String statusPermissions;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建时间")
    private Long createTime;
}
