package cn.cordys.crm.approval.dto;

import cn.cordys.common.dto.EnableFieldValue;
import cn.cordys.crm.approval.constants.ApprovalResubmitModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 审批通过后配置
 */
@Data
public class ApprovalPostConfigDTO {

    @Schema(description = "字段更新配置列表")
    private List<EnableFieldValue> fieldUpdateConfigs;

    @Schema(description = "webhook配置")
    private WebHookConfig webHookConfig;

    @Schema(description = "默认重审路径：SUBMITTER/CURRENT_NODE/SPECIFIED_NODE，仅用于驳回后配置")
    private String defaultResubmitMode = ApprovalResubmitModeEnum.SUBMITTER.name();
}
