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
	INPUT_NUMBER,
	/**
	 * 日期时间
	 */
	DATE_TIME,
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
	 * 多值输入
	 */
	MULTIPLE_INPUT,
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
	 * 图片
	 */
	PICTURE,
	/**
	 * 地址
	 */
	LOCATION,
	/**
	 * 电话
	 */
	PHONE,
	/**
	 * 数据源
	 */
	DATA_SOURCE,
}
