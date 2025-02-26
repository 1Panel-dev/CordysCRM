package io.cordys.crm.system.constants;

import lombok.Getter;

@Getter
public enum FieldType {

	/**
	 * 单行输入
	 */
	INPUT,
	/**
	 * 多行输入
	 */
	TEXTAREA,
	/**
	 * 数字
	 */
	NUMBER,
	/**
	 * 日期时间
	 */
	DATETIME,
	/**
	 * 单选
	 */
	RADIO,
	/**
	 * 多选
	 */
	CHECKBOX,
	/**
	 * 单选下拉
	 */
	SELECT,
	/**
	 * 成员
	 */
	MEMBER,
	/**
	 * 部门
	 */
	DEPARTMENT,
	/**
	 * 分割线
	 */
	DIVIDER,
	/**
	 * 数据源
	 */
	DATA_SOURCE
}
