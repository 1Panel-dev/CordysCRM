package cn.cordys.crm.integration.sqlbot.handler;


import cn.cordys.common.constants.FormKey;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class CluePermissionHandler extends DataScopeTablePermissionHandler {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.CLUE, this);
    }

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), OrganizationContext.getOrganizationId());
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(),
                Set.of("pool_id", "in_shared_pool", "organization_id", "reason_id", "transition_type", "transition_id", "last_stage"));
        table.setFields(filterFields);

        super.handleTable(table, tableHandleParam, formConfig);

        String sql = table.getSql() + " and in_shared_pool is false and (c.transition_id is null or c.transition_id = '')";
        table.setSql(sql);
    }


    @Override
    protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
        String fieldName = sqlBotField.getName();
        if (Strings.CS.equals(fieldName, "stage")) {
            return getSystemOptionFileSql(Arrays.stream(ClueStatus.values()),
                    ClueStatus::getKey,
                    ClueStatus::getName,
                    fieldName);
        } else if (Strings.CS.equals(fieldName, "products")) {
            return getProductsFieldSql();
        } else {
            return getDefaultFieldSql(sqlBotField);
        }
    }
}
