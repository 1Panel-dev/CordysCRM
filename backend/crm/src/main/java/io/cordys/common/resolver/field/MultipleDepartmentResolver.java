package io.cordys.common.resolver.field;

import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.DepartmentField;

public class MultipleDepartmentResolver extends DepartmentResolver{

	@Override
	public void validate(DepartmentField departmentField, Object value) {
		validateRequired(departmentField, value);
		validateArray(departmentField.getName(), value);
	}

	@Override
	public String parse2String(DepartmentField departmentField, Object value) {
		return JSON.toJSONString(value);
	}

	@Override
	public Object parse2Value(DepartmentField departmentField, String value) {
		return parse2Array(value);
	}
}
