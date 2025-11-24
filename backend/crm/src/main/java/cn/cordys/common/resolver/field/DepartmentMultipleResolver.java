package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.field.DepartmentMultipleField;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Objects;

public class DepartmentMultipleResolver extends AbstractModuleFieldResolver<DepartmentMultipleField> {

    private static final ExtDepartmentMapper extDepartmentMapper;

    static {
        extDepartmentMapper = CommonBeanFactory.getBean(ExtDepartmentMapper.class);
    }

    @Override
    public void validate(DepartmentMultipleField departmentField, Object value) {
        validateRequired(departmentField, value);
        validateArray(departmentField.getName(), value);
    }

    @Override
    public String convertToString(DepartmentMultipleField departmentField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object convertToValue(DepartmentMultipleField departmentField, String value) {
        return parse2Array(value);
    }

    @Override
    public Object transformToValue(DepartmentMultipleField departmentMultipleField, String value) {
        if (StringUtils.isBlank(value) || Strings.CS.equals(value, "[]")) {
            return StringUtils.EMPTY;
        }
        List<String> ids = JSON.parseArray(value, String.class);

        List<String> names = Objects.requireNonNull(extDepartmentMapper).getNameByIds(ids);

        if (CollectionUtils.isNotEmpty(names)) {
            return String.join(",", JSON.parseArray(JSON.toJSONString(names), String.class));
        }

        return StringUtils.EMPTY;
    }

    @Override
    public Object textToValue(DepartmentMultipleField field, String text) {
        if (StringUtils.isBlank(text) || Strings.CS.equals(text, "[]")) {
            return StringUtils.EMPTY;
        }
        List<String> names = parseFakeJsonArray(text);
        List<String> ids = Objects.requireNonNull(extDepartmentMapper).getIdsByNames(names);
        if (CollectionUtils.isNotEmpty(ids)) {
            return ids;
        }
        return names;
    }
}
