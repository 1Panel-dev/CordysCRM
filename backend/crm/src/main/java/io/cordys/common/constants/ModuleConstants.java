package io.cordys.common.constants;

import lombok.Getter;

@Getter
public enum ModuleConstants {

	/**
	 * 首页
	 */
	HOME("home"),
	/**
	 * 客户管理
	 */
	CUSTOMER("customer"),
	/**
	 * 线索管理
	 */
	CLUE("clue"),
	/**
	 * 商机管理
	 */
	BUSINESS("business"),
	/**
	 * 数据管理
	 */
	DATA("data"),
	/**
	 * 产品管理
	 */
	PRODUCT("product");

	private final String key;

	ModuleConstants(String key) {
		this.key = key;
	}
}
