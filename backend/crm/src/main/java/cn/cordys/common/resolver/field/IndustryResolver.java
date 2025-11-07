package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.IndustryField;

/**
 * @author song-cc-rock
 */
public class IndustryResolver extends AbstractModuleFieldResolver<IndustryField> {

	@Override
	public void validate(IndustryField customField, Object value) {

	}

	@Override
	public Object trans2Value(IndustryField selectField, String value) {
		return value;
	}

	@Override
	public Object text2Value(IndustryField field, String text) {
		return text;
	}
}
