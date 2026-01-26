package cn.cordys.crm.contract.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.contract.domain.Contract;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ContractListResponse extends Contract {

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "负责人名称")
    private String ownerName;

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;

    @Schema(description = "关联的客户是否在公海")
    private Boolean inCustomerPool;

    @Schema(description = "客户公海id")
    private String poolId;

    @Schema(description = "部门id")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

	@Schema(description = "已回款金额")
	private BigDecimal alreadyPayAmount;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}
