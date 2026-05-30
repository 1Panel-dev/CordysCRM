package cn.cordys.crm.contract.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Data
public class ContractPaymentPlanGetResponse extends ContractPaymentPlan {
    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "归属部门")
    private String departmentId;

    @Schema(description = "归属部门名称")
    private String departmentName;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;

    @Schema(description = "附件集合")
    private Map<String, List<Attachment>> attachmentMap;
}
