package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_resource_data")
public class ApprovalResourceData {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "表单类型")
    private String formType;

    @Schema(description = "资源ID")
    private String resourceId;

    @Schema(description = "执行时机：CREATE/UPDATE/DELETE")
    private String executeTime;

    @Schema(description = "执行时机为UPDATE时，有修改的字段列表")
    private String updateFields;
}
