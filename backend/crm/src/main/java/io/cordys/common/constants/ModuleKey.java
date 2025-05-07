package io.cordys.common.constants;

import lombok.Getter;

@Getter
public enum ModuleKey {

	/**
	 * 首页
	 */
	HOME("home"),
	/**
	 * 线索管理
	 */
	CLUE("clue"),
	/**
	 * 客户管理
	 */
	CUSTOMER("customer"),
	/**
	 * 商机管理
	 */
	BUSINESS("business"),
	/**
	 * 产品管理
	 */
	PRODUCT("product"),
	/**
	 * 系统设置
	 */
	SETTING("setting");

	private final String key;

	ModuleKey(String key) {
		this.key = key;
	}
}
