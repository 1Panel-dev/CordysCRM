package io.cordys.crm.integration.sqlbot.handler;


import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.OptionDTO;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.integration.sqlbot.dto.TableDTO;
import io.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ContactPermissionHandler extends DataScopeTablePermissionHandler {

    {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.CONTACT, this);
    }

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTACT.getKey(), OrganizationContext.getOrganizationId());
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(),
                Set.of("organization_id", "disable_reason"));
        table.setFields(filterFields);

        super.handleTable(table, tableHandleParam, formConfig);
    }

    @Override
    protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
        String fieldName = sqlBotField.getName();
        if (Strings.CS.equals(fieldName, "enable")) {
            List<OptionDTO> options = List.of(new OptionDTO("1", "启用"), new OptionDTO("2", "停用"));
            return getSystemOptionFileSql(options.stream(),
                    OptionDTO::getId,
                    OptionDTO::getName,
                    fieldName);
        } else {
            return getDefaultFieldSql(sqlBotField);
        }
    }
}
