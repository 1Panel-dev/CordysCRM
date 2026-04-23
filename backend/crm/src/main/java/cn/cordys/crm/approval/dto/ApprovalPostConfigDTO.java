package cn.cordys.crm.approval.dto;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 审批通过后配置
 */
@Data
public class ApprovalPostConfigDTO {

    @Schema(description = "字段更新配置列表")
    private List<BaseModuleFieldValue> fieldUpdateConfigs;
}