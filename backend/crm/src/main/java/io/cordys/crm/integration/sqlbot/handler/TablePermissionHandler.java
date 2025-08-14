package io.cordys.crm.integration.sqlbot.handler;


import io.cordys.crm.integration.sqlbot.dto.TableDTO;
import io.cordys.crm.integration.sqlbot.dto.TableHandleParam;

/**
 * 表的权限接口
 */
public interface TablePermissionHandler {
    /**
     * 处理表
     * @param table 表信息
     * @param tableHandleParam 处理参数
     */
    void handleTable(TableDTO table, TableHandleParam tableHandleParam);
}
