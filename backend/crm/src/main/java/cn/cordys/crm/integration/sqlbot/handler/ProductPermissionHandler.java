package cn.cordys.crm.integration.sqlbot.handler;


import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 处理有数据权限的表
 */
@Component
public class ProductPermissionHandler extends ModuleFieldTablePermissionHandler {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @PostConstruct
    public void registerHandler() {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.PRODUCT, this);
    }

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRODUCT.getKey(), OrganizationContext.getOrganizationId());
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(), Set.of("organization_id", "pos"));
        table.setFields(filterFields);
        super.handleTable(table, tableHandleParam, formConfig);

    }

    @Override
    protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
        String fieldName = sqlBotField.getName();
        if (Strings.CS.equals(fieldName, "status")) {
            List<OptionDTO> options = List.of(new OptionDTO("1", "上架"), new OptionDTO("2", "下架"));
            return getSystemOptionFileSql(options.stream(),
                    OptionDTO::getId,
                    OptionDTO::getName,
                    fieldName);
        } else {
            return getDefaultFieldSql(sqlBotField);
        }
    }
}
