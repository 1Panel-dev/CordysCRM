package cn.cordys.crm.system.constants;

public enum FieldSourceType {

    /**
     * 客户来源
     */
    CUSTOMER("customer"),
    /**
     * 线索
     */
    CLUE("clue"),
    /**
     * 联系人来源
     */
    CONTACT("customer_contact"),
    /**
     * 商机来源
     */
    OPPORTUNITY("opportunity"),
    /**
     * 产品来源
     */
    PRODUCT("product"),
	/**
	 * 价格来源
	 */
	PRICE("product_price"),
	/**
	 * 合同来源
	 */
	CONTRACT("contract"),
	/**
	 * 报价单来源
	 */
	QUOTATION("opportunity_quotation"),
	/**
	 * 回款计划
	 */
	PAYMENT_PLAN("contract_payment_plan"),
	/**
	 * 工商抬头
	 */
	BUSINESS_TITLE("business_title"),
	/**
	 * 回款记录
	 */
	CONTRACT_PAYMENT_RECORD("contract_payment_record"),
	/**
	 * 订单
	 */
	ORDER("sales_order");

	private String tableName;

	FieldSourceType(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}
}
