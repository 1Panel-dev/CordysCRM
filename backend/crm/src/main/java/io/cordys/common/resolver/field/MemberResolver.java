package io.cordys.common.resolver.field;


import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.MemberField;
import io.cordys.crm.system.mapper.ExtUserMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author jianxing
 */
public class MemberResolver extends AbstractModuleFieldResolver<MemberField> {

    private static final ExtUserMapper extUserMapper;

    static {
        // 从 CommonBeanFactory 获取 ExtUserMapper 实例
        extUserMapper = CommonBeanFactory.getBean(ExtUserMapper.class);
    }

    @Override
    public void validate(MemberField memberField, Object value) {
        validateRequired(memberField, value);
        validateString(memberField.getName(), value);
    }

    @Override
    public String parse2String(MemberField memberField, Object value) {
        return getStringValue(value);
    }

    @Override
    public Object parse2Value(MemberField memberField, String value) {
        return getStringValue(value);
    }

    @Override
    public Object trans2Value(MemberField memberField, String value) {
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }

        List<String> names = extUserMapper.selectUserNameByIds(List.of(value));

        if (CollectionUtils.isNotEmpty(names)) {
            return String.join(",", JSON.parseArray(JSON.toJSONString(names)));
        }

        return StringUtils.EMPTY;
    }

    @Override
    public Object text2Value(MemberField field, String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        List<String> ids = extUserMapper.selectUserIdsByNames(List.of(text));
        if (CollectionUtils.isNotEmpty(ids)) {
            return ids.getFirst();
        }
        return text;
    }
}
