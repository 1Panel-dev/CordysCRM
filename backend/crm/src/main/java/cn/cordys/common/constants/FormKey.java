package cn.cordys.common.constants;

import lombok.Getter;

@Getter
public enum FormKey {

	/**
	 * 线索
	 */
	CLUE("clue"),
	/**
	 * 客户
	 */
	CUSTOMER("customer"),
	/**
	 * 联系人
	 */
	CONTACT("contact"),
	/**
	 * 跟进记录
	 */
	FOLLOW_RECORD("record"),
	/**
	 * 跟进计划
	 */
	FOLLOW_PLAN("plan"),
	/**
	 * 商机
	 */
	OPPORTUNITY("opportunity"),
	/**
	 * 产品
	 */
	PRODUCT("product");

	private final String key;

	FormKey(String key) {
		this.key = key;
	}
}
