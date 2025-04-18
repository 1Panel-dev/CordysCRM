package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.DepartmentField;
import org.apache.commons.lang3.BooleanUtils;

public class DepartmentResolver extends AbstractModuleFieldResolver<DepartmentField>{

	@Override
	public void validate(DepartmentField departmentField, Object value) {
		if (BooleanUtils.isTrue(departmentField.getMultiple())) {
			ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_DEPARTMENT)
					.validate(departmentField, value);

		}
		validateRequired(departmentField, value);
		validateArray(departmentField.getName(), value);
	}

	@Override
	public String parse2String(DepartmentField selectField, Object value) {
		if (BooleanUtils.isTrue(selectField.getMultiple())) {
			return ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_DEPARTMENT)
					.parse2String(selectField, value);
		}
		return getStringValue(value);
	}

	@Override
	public Object parse2Value(DepartmentField selectField, String value) {
		if (BooleanUtils.isTrue(selectField.getMultiple())) {
			return ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_DEPARTMENT)
					.parse2Value(selectField, value);
		}
		return getStringValue(value);
	}
}
