package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.AttachmentField;

public class AttachmentFieldResolver extends AbstractModuleFieldResolver<AttachmentField>{


	@Override
	public void validate(AttachmentField customField, Object value) {

	}

	@Override
	public String parse2String(AttachmentField attachmentField, Object value) {
		return getJsonString(value);
	}

	@Override
	public Object parse2Value(AttachmentField attachmentField, String value) {
		return parse2Array(value);
	}
}
