package cn.cordys.common.resolver.field;


import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.field.MemberField;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

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
    public String convertToString(MemberField memberField, Object value) {
        return getStringValue(value);
    }

    @Override
    public Object convertToValue(MemberField memberField, String value) {
        return getStringValue(value);
    }

    @Override
    public Object transformToValue(MemberField memberField, String value) {
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }

        List<String> names = Objects.requireNonNull(extUserMapper).selectUserNameByIds(List.of(value));

        if (CollectionUtils.isNotEmpty(names)) {
            return String.join(",", JSON.parseArray(JSON.toJSONString(names), String.class));
        }

        return StringUtils.EMPTY;
    }

    @Override
    public Object textToValue(MemberField field, String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        List<String> ids = Objects.requireNonNull(extUserMapper).selectUserIdsByNames(List.of(text));
        if (CollectionUtils.isNotEmpty(ids)) {
            return ids.getFirst();
        }
        return text;
    }
}
