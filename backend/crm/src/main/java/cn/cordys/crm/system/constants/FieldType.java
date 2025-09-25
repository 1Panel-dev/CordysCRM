package cn.cordys.crm.system.constants;

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
     * 多选下拉
     */
    SELECT_MULTIPLE,
    /**
     * 多值输入
     */
    INPUT_MULTIPLE,
    /**
     * 成员
     */
    MEMBER,
    /**
     * 多选成员
     */
    MEMBER_MULTIPLE,
    /**
     * 部门
     */
    DEPARTMENT,
    /**
     * 多选部门
     */
    DEPARTMENT_MULTIPLE,
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
    /**
     * 多选数据源
     */
    DATA_SOURCE_MULTIPLE,
    /**
     * 流水号
     */
    SERIAL_NUMBER,
    /**
     * 附件
     */
    ATTACHMENT,
    /**
     * 链接
     */
    LINK
}
