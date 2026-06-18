package cn.cordys.crm.contract.constants;

import cn.cordys.crm.system.constants.FieldSourceType;
import lombok.Getter;

/**
 * 系统字段枚举
 */
@Getter
public enum SystemFieldConstants {

	/**
	 * 报价数据源 (状态, 审批状态)
	 */
	QUOTATION_INVALID("invalid", FieldSourceType.QUOTATION),
	QUOTATION_APPROVAL_STATUS("approvalStatus", FieldSourceType.QUOTATION),

    /**
     * 合同数据源 (合同阶段, 审批状态)
     */
	CONTRACT_STAGE("stage", FieldSourceType.CONTRACT),
    CONTRACT_APPROVAL_STATUS("approvalStatus", FieldSourceType.CONTRACT),

	/**
	 * 订单数据源 (订单阶段, 审批状态)
	 */
	ORDER_STAGE("stage", FieldSourceType.ORDER),
	ORDER_APPROVAL_STATUS("approvalStatus", FieldSourceType.ORDER),

	/**
	 * 发票数据源 (审批状态)
	 */
	INVOICE_APPROVAL_STATUS("approvalStatus", FieldSourceType.INVOICE);

    private final String key;
	private final FieldSourceType sourceType;

    SystemFieldConstants(String key, FieldSourceType sourceType) {
        this.key = key;
		this.sourceType = sourceType;
	}
}
