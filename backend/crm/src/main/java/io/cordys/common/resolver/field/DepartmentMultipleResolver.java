package io.cordys.common.resolver.field;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.DepartmentMultipleField;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class DepartmentMultipleResolver extends AbstractModuleFieldResolver<DepartmentMultipleField> {

    private static final ExtDepartmentMapper extdepartmentMapper;

    static {
        extdepartmentMapper = CommonBeanFactory.getBean(ExtDepartmentMapper.class);
    }

    @Override
    public void validate(DepartmentMultipleField departmentField, Object value) {
        validateRequired(departmentField, value);
        validateArray(departmentField.getName(), value);
    }

    @Override
    public String parse2String(DepartmentMultipleField departmentField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object parse2Value(DepartmentMultipleField departmentField, String value) {
        return parse2Array(value);
    }

    @Override
    public Object trans2Value(DepartmentMultipleField departmentMultipleField, String value) {
        if (StringUtils.isBlank(value) || StringUtils.equals(value, "[]")) {
            return StringUtils.EMPTY;
        }
        List ids = JSON.parseArray(value, String.class);

        List names = extdepartmentMapper.getNameByIds(ids);

        if (CollectionUtils.isNotEmpty(names)) {
            return String.join(",", JSON.parseArray(JSON.toJSONString(names)));
        }

        return StringUtils.EMPTY;
    }
}
