package cn.cordys.common.resolver.field;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.field.DepartmentField;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

public class DepartmentResolver extends AbstractModuleFieldResolver<DepartmentField> {

    private static final ExtDepartmentMapper extDepartmentMapper;

    static {
        extDepartmentMapper = CommonBeanFactory.getBean(ExtDepartmentMapper.class);
    }

    @Override
    public void validate(DepartmentField departmentField, Object value) {
        validateRequired(departmentField, value);
        validateString(departmentField.getName(), value);
    }

    @Override
    public String parse2String(DepartmentField selectField, Object value) {
        return getStringValue(value);
    }

    @Override
    public Object parse2Value(DepartmentField selectField, String value) {
        return getStringValue(value);
    }


    @Override
    public Object trans2Value(DepartmentField departmentField, String value) {
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }

        List<String> names = Objects.requireNonNull(extDepartmentMapper).getNameByIds(List.of(value));
        if (CollectionUtils.isNotEmpty(names)) {
            return String.join(",", JSON.parseArray(JSON.toJSONString(names), String.class));
        }

        return StringUtils.EMPTY;
    }

    @Override
    public Object text2Value(DepartmentField field, String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        List<String> ids = Objects.requireNonNull(extDepartmentMapper).getIdsByNames(List.of(text));
        if (CollectionUtils.isNotEmpty(ids)) {
            return ids.getFirst();
        }
        return text;
    }
}
