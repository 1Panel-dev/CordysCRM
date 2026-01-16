package cn.cordys.crm.contract.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.cordys.crm.contract.domain.ContractInvoice;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-12-29 18:22:59
 */
@Data
public class ContractInvoiceListResponse extends ContractInvoice {

    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "部门id")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "工商抬头名称")
    private String businessTitleName;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}
