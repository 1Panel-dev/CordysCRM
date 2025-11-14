package cn.cordys.crm.integration.sqlbot.handler;


import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理有数据权限的表
 */
public abstract class DataScopeTablePermissionHandler extends ModuleFieldTablePermissionHandler {

    public static final String DATA_SCOPE_SQL_TEMPLATE = """
            select {0},
            sys_department.name as department_name,
            sys_user.name as owner
            {1}
            from {2} c
            left join sys_user on sys_user.id = c.owner
            left join sys_organization_user sou on sou.user_id = sys_user.id
            left join sys_department sys_department on sys_department.id = sou.department_id
            where c.organization_id = ''{3}''
            """;

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam, ModuleFormConfigDTO formConfig) {
        List<BaseField> moduleFields = filterFields(formConfig.getFields());
        List<FieldDTO> tableFields = table.getFields();
        String sql = getTableSql(tableFields, tableHandleParam, moduleFields);
        table.setSql(sql);

        List<FieldDTO> appendFields = parse2SQLBotFields(moduleFields);
        tableFields.addAll(appendFields);
        tableFields.add(getDepartmentNameField());
    }

    private FieldDTO getDepartmentNameField() {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setType("varchar(255)");
        fieldDTO.setComment("部门名称");
        fieldDTO.setName("department_name");
        return fieldDTO;
    }

    protected String getProductsFieldSql() {
        return "(select JSON_ARRAYAGG(p.name) from product p where c.products like concat('%', p.id, '%')) as products";
    }

    protected String getTableSql(List<FieldDTO> sqlBotFields, TableHandleParam tableHandleParam, List<BaseField> moduleFields) {
        String fieldsSql = parseFieldsSql(tableHandleParam.getTableInfo().getTableName() + "_field", moduleFields);
        sqlBotFields = sqlBotFields.stream()
                .filter(field -> !Strings.CS.equals(field.getName(), "owner"))
                .collect(Collectors.toList());
        String dataScopeSql = MessageFormat.format(DATA_SCOPE_SQL_TEMPLATE,
                getSelectSystemFileSql(sqlBotFields),
                fieldsSql,
                tableHandleParam.getTableInfo().getTableName(),
                tableHandleParam.getOrgId()
        );

        DeptDataPermissionDTO dataPermission = tableHandleParam.getDataPermission();
        if (Strings.CS.equals(tableHandleParam.getUserId(), InternalUser.ADMIN.getValue()) || dataPermission.getAll()) {
            return dataScopeSql;
        } else if (CollectionUtils.isNotEmpty(dataPermission.getDeptIds())) {
            String deptIdStr = getInConditionStr(dataPermission.getDeptIds());
            return dataScopeSql + String.format(" and (sou.department_id in (%s) or sou.user_id = '%s')", deptIdStr, tableHandleParam.getUserId());
        } else {
            return dataScopeSql + String.format(" and c.owner = '%s'", tableHandleParam.getUserId());
        }
    }

    /**
     * 将数组转成IN的条件字符串
     *
     * @param ids
     *
     * @return
     */
    protected String getInConditionStr(Collection<String> ids) {
        return "'"
                + String.join("','", ids)
                + "'";
    }
}
