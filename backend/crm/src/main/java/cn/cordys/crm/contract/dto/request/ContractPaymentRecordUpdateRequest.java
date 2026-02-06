package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author song-cc-rock
 */
@Data
public class ContractPaymentRecordUpdateRequest {

	@NotBlank
	@Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
	@Size(max = 32)
	private String id;

	@NotBlank
	@Size(max = 255)
	@Schema(description = "回款记录名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;

	@NotBlank
	@Size(max = 32)
	@Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
	private String owner;

	@NotBlank
	@Size(max = 32)
	@Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String contractId;

	@Size(max = 32)
	@Schema(description = "回款计划ID")
	private String paymentPlanId;

	@Schema(description = "回款金额")
	private BigDecimal recordAmount;

	@Schema(description = "回款时间")
	private Long recordEndTime;

	@Schema(description = "自定义字段值")
	private List<BaseModuleFieldValue> moduleFields;
}
