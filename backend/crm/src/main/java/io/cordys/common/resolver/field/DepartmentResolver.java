package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.DepartmentField;

public class DepartmentResolver extends AbstractModuleFieldResolver<DepartmentField> {

	@Override
	public void validate(DepartmentField departmentField, Object value) {
		validateRequired(departmentField, value);
		validateString(departmentField.getName(), value);
	}

	@Override
	public String parse2String(DepartmentField selectField, Object value) {
		return getStringValue(value);
	}

	@Override
	public Object parse2Value(DepartmentField selectField, String value) {
		return getStringValue(value);
	}
}
