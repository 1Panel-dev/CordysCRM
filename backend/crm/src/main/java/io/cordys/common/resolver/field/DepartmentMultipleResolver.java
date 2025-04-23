package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.DepartmentMultipleField;

public class DepartmentMultipleResolver extends AbstractModuleFieldResolver<DepartmentMultipleField> {

	@Override
	public void validate(DepartmentMultipleField departmentField, Object value) {
		validateRequired(departmentField, value);
		validateArray(departmentField.getName(), value);
	}

	@Override
	public String parse2String(DepartmentMultipleField departmentField, Object value) {
		return getJsonString(value);
	}

	@Override
	public Object parse2Value(DepartmentMultipleField departmentField, String value) {
		return parse2Array(value);
	}
}
