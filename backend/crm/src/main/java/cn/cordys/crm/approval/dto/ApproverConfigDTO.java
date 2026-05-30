package cn.cordys.crm.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批人配置DTO
 */
@Data
public class ApproverConfigDTO {

    @Schema(description = "审批人类型：MEMBER/SUPERIOR/DEPT_HEAD/ROLE/FORM_FIELD")
    private String type;

    @Schema(description = "审批人值")
    private String value;
}