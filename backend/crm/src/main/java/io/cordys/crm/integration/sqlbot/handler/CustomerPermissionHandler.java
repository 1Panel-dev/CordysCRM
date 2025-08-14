package io.cordys.crm.integration.sqlbot.handler;


import io.cordys.common.constants.FormKey;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.integration.sqlbot.dto.TableDTO;
import io.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 处理有数据权限的表
 */
@Component
public class CustomerPermissionHandler extends DataScopeTablePermissionHandler {

    {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.CUSTOMER, this);
    }

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), OrganizationContext.getOrganizationId());
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(), Set.of("pool_id", "in_shared_pool", "organization_id", "reason_id"));
        table.setFields(filterFields);

        super.handleTable(table, tableHandleParam, formConfig);

        String sql = table.getSql();
        sql += " and in_shared_pool is false";
        table.setSql(sql);
    }
}
