package io.cordys.common.constants;

import lombok.Getter;

@Getter
public enum ModuleConstants {

	/**
	 * 首页
	 */
	HOME("module.home"),
	/**
	 * 客户管理
	 */
	CUSTOMER("module.customer"),
	/**
	 * 线索管理
	 */
	CLUE("module.clue"),
	/**
	 * 商机管理
	 */
	BUSINESS("module.business"),
	/**
	 * 数据管理
	 */
	DATA("module.data"),
	/**
	 * 产品管理
	 */
	PRODUCT("module.product");

	private final String key;

	ModuleConstants(String key) {
		this.key = key;
	}
}
