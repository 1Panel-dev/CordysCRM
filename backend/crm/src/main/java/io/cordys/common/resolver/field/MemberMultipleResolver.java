package io.cordys.common.resolver.field;


import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.MemberMultipleField;
import io.cordys.crm.system.mapper.ExtUserMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;

/**
 * @author jianxing
 */
public class MemberMultipleResolver extends AbstractModuleFieldResolver<MemberMultipleField> {

    private static final ExtUserMapper extUserMapper;

    static {
        // 从 CommonBeanFactory 获取 ExtUserMapper 实例
        extUserMapper = CommonBeanFactory.getBean(ExtUserMapper.class);
    }

    @Override
    public void validate(MemberMultipleField memberField, Object value) {
        validateRequired(memberField, value);
        validateArray(memberField.getName(), value);
    }

    @Override
    public String parse2String(MemberMultipleField memberField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object parse2Value(MemberMultipleField memberField, String value) {
        return parse2Array(value);
    }


    @Override
    public Object trans2Value(MemberMultipleField memberMultipleField, String value) {
        if (StringUtils.isBlank(value) || Strings.CS.equals(value, "[]")) {
            return StringUtils.EMPTY;
        }
        List ids = JSON.parseArray(value,String.class);

        List names = extUserMapper.selectUserNameByIds(ids);

        if(CollectionUtils.isNotEmpty(names)) {
            return String.join(",", JSON.parseArray(JSON.toJSONString(names)));
        }

        return StringUtils.EMPTY;
    }

    @Override
    public Object text2Value(MemberMultipleField field, String text) {
        if (StringUtils.isBlank(text) || Strings.CS.equals(text, "[]")) {
            return StringUtils.EMPTY;
        }
        List<String> names = parseFakeJsonArray(text);
        List<String> ids = extUserMapper.selectUserIdsByNames(names);
        if(CollectionUtils.isNotEmpty(ids)) {
            return ids;
        }
        return names;
    }
}
