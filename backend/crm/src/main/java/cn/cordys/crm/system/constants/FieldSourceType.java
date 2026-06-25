package cn.cordys.crm.system.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
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
	ORDER("sales_order"),
	/**
	 * 自定义表单
	 */
	CUSTOM_FORM("custom_form_data"),
	/**
	 * 发票
	 */
	INVOICE("contract_invoice");

	private final String tableName;

	private static final Set<String> SOURCE_TYPE_NAMES = Arrays.stream(values())
			.map(Enum::name)
			.collect(Collectors.toSet());

	FieldSourceType(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * 如果是系统数据源类型，返回对应的枚举值, 否则返回自定义表单类型.
	 * @param type 数据源类型字符串
	 * @return 数据源类型枚举值
	 */
	public static FieldSourceType safeValueOf(String type) {
		if (type != null && SOURCE_TYPE_NAMES.contains(type)) {
			return valueOf(type);
		}
		return CUSTOM_FORM;
	}

}
