package cn.cordys.crm.integration.sqlbot.handler;


import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 处理有数据权限的表
 */
@Component
public class SysDepartmentPermissionHandler extends OrgTablePermissionHandler {

    @PostConstruct
    public void registerHandler() {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.SYS_DEPARTMENT, this);
    }

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(), Set.of("organization_id", "pos", "resource", "resource_id"));
        table.setFields(filterFields);
        super.handleTable(table, tableHandleParam);
    }
}
