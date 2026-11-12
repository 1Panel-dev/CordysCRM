package cn.cordys.crm.form.service;

import java.util.List;

/** 自定义表单系统视图解析后的数据范围。 */
record CustomFormViewScope(boolean ownerOnly, List<String> departmentIds) {
}
