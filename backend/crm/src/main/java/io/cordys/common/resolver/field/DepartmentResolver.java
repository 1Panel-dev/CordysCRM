package io.cordys.common.resolver.field;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.DepartmentField;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class DepartmentResolver extends AbstractModuleFieldResolver<DepartmentField> {

	private static final ExtDepartmentMapper extdepartmentMapper;

	static {
		extdepartmentMapper = CommonBeanFactory.getBean(ExtDepartmentMapper.class);
	}

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


	@Override
	public Object trans2Value(DepartmentField departmentField, String value) {
		if(StringUtils.isBlank(value)) {
			return StringUtils.EMPTY;
		}

		List<String> names = extdepartmentMapper.getNameByIds(List.of(value));
		if (CollectionUtils.isNotEmpty(names)) {
			return String.join(",", JSON.parseArray(JSON.toJSONString(names)));
		}

		return StringUtils.EMPTY;
	}
}
