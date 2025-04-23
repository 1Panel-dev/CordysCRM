package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.DatasourceMultipleField;

public class DatasourceMultipleResolver extends AbstractModuleFieldResolver<DatasourceMultipleField> {

	@Override
	public void validate(DatasourceMultipleField customField, Object value) {

	}

	@Override
	public Object parse2Value(DatasourceMultipleField customField, String value) {
		return parse2Array(value);
	}

	@Override
	public String parse2String(DatasourceMultipleField customField, Object value) {
		return getJsonString(value);
	}
}
