package io.cordys.common.resolver.field;

import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.DatasourceField;

public class DatasourceResolver extends AbstractModuleFieldResolver<DatasourceField> {

	@Override
	public void validate(DatasourceField customField, Object value) {

	}

	@Override
	public Object parse2Value(DatasourceField selectField, String value) {
		return parse2Array(value);
	}

	@Override
	public String parse2String(DatasourceField selectField, Object value) {
		return JSON.toJSONString(value);
	}
}
