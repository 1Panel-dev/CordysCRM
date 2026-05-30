package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.field.AttachmentField;
import cn.cordys.crm.system.mapper.ExtAttachmentMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Objects;

public class AttachmentFieldResolver extends AbstractModuleFieldResolver<AttachmentField> {


    private static final ExtAttachmentMapper extAttachmentMapper;

    static {
        extAttachmentMapper = CommonBeanFactory.getBean(ExtAttachmentMapper.class);
    }

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

    @Override
    public Object transformToValue(AttachmentField attachmentField, String value) {
        if (StringUtils.isBlank(value) || Strings.CS.equals(value, "[]")) {
            return StringUtils.EMPTY;
        }
        List<String> ids = JSON.parseArray(value, String.class);

        List<String> names = Objects.requireNonNull(extAttachmentMapper).selectNameByIds(ids);

        if (CollectionUtils.isNotEmpty(names)) {
            return String.join(",", JSON.parseArray(JSON.toJSONString(names), String.class));
        }

        return StringUtils.EMPTY;
    }

}
