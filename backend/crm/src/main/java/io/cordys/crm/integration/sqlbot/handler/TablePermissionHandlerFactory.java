package io.cordys.crm.integration.sqlbot.handler;

import io.cordys.crm.integration.sqlbot.constant.SQLBotTable;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-05  09:59
 */
public class TablePermissionHandlerFactory {
    private static final Map<SQLBotTable, TablePermissionHandler> tableHandlerMap = new HashMap<>();

    public static void registerTableHandler(SQLBotTable sqlBotTable, TablePermissionHandler tablePermissionHandler) {
        tableHandlerMap.put(sqlBotTable, tablePermissionHandler);
    }

    public static TablePermissionHandler getTableHandler(SQLBotTable sqlBotTable) {
        return tableHandlerMap.get(sqlBotTable);
    }
}
