package cn.cordys.crm.contract.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.contract.domain.ContractPaymentRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author song-cc-rock
 */
@Data
public class ContractPaymentRecordResponse extends ContractPaymentRecord {

	@Schema(description = "创建人名称")
	private String createUserName;

	@Schema(description = "更新人名称")
	private String updateUserName;

	@Schema(description = "合同名称")
	private String contractName;

	@Schema(description = "回款计划名称")
	private String paymentPlanName;

	@Schema(description = "负责人名称")
	private String ownerName;

	@Schema(description = "归属部门")
	private String departmentId;

	@Schema(description = "归属部门名称")
	private String departmentName;

	@Schema(description = "自定义字段集合")
	private List<BaseModuleFieldValue> moduleFields;
}
