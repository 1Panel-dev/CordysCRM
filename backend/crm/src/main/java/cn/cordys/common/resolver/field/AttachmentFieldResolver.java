package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.AttachmentField;

public class AttachmentFieldResolver extends AbstractModuleFieldResolver<AttachmentField> {


    @Override
    public void validate(AttachmentField customField, Object value) {

    }

    @Override
    public String convertToString(AttachmentField attachmentField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object convertToValue(AttachmentField attachmentField, String value) {
        return parse2Array(value);
    }
}
