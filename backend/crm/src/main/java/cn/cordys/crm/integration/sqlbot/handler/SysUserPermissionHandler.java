package cn.cordys.crm.integration.sqlbot.handler;


import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

/**
 * 处理有数据权限的表
 */
@Component
public class SysUserPermissionHandler extends OrgTablePermissionHandler {

    private static final String SYS_USER_SQL_TEMPLATE = """
            select {0}
            from sys_user c
            join sys_organization_user sou on c.id = sou.user_id
            join sys_department sd on sd.id = sou.department_id
            where sou.organization_id = ''{1}'' and sou.enable is true
            """;

    private static final String SYS_ORGANIZATION_USER_PREFIX = "sou";

    @PostConstruct
    public void registerHandler() {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.SYS_USER, this);
    }

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(),
                Set.of("organization_id", "pos", "password", "last_organization_id", "language"));
        filterFields.add(getDepartmentNameField());
        filterFields.add(getPositionField());
        filterFields.add(getEmployeeIdField());
        filterFields.add(getEmployeeTypeField());
        filterFields.add(getOnboardingDateField());
        table.setFields(filterFields);

        String sql = getSysUserTableSql(tableHandleParam, table.getFields());
        table.setSql(sql);
    }

    protected String getSysUserTableSql(TableHandleParam tableHandleParam, List<FieldDTO> sqlBotFields) {
        return MessageFormat.format(SYS_USER_SQL_TEMPLATE,
                getSelectSystemFileSql(sqlBotFields),
                tableHandleParam.getOrgId());
    }

    protected FieldDTO getPositionField() {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setType("varchar(255)");
        fieldDTO.setComment("职位");
        fieldDTO.setName("position");
        return fieldDTO;
    }

    protected FieldDTO getEmployeeIdField() {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setType("varchar(255)");
        fieldDTO.setComment("工号");
        fieldDTO.setName("employee_id");
        return fieldDTO;
    }

    protected FieldDTO getEmployeeTypeField() {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setType("varchar(255)");
        fieldDTO.setComment("员工类型");
        fieldDTO.setName("employee_type");
        return fieldDTO;
    }

    protected FieldDTO getOnboardingDateField() {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setType("varchar(500)");
        fieldDTO.setComment("入职时间，时间格式：%Y-%m-%d");
        fieldDTO.setName("onboarding_date");
        return fieldDTO;
    }

    @Override
    protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
        String fieldName = sqlBotField.getName();
        if (Strings.CS.equals(fieldName, "employee_type")) {
            List<OptionDTO> options = List.of(new OptionDTO("formal", "正式"),
                    new OptionDTO("internship", "实习"), new OptionDTO("outsourcing", "外包"));
            return getSystemOptionFileSql(options.stream(),
                    OptionDTO::getId,
                    OptionDTO::getName,
                    fieldName, SYS_ORGANIZATION_USER_PREFIX);
        } else if (Strings.CS.equals(fieldName, "gender")) {
            return "if(c.gender, '女', '男') as '性别'";
        } else if (Strings.CS.equals(fieldName, "department_name")) {
            return "sd.name as department_name";
        } else if (Strings.CS.equalsAny(fieldName, "onboarding_date", "position", "employee_id")) {
            return getDefaultFieldSql(sqlBotField, SYS_ORGANIZATION_USER_PREFIX);
        } else {
            return getDefaultFieldSql(sqlBotField);
        }
    }
}
