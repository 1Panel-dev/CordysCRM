package cn.cordys.crm.integration.sqlbot.handler;


import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.customer.domain.CustomerPool;
import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PoolCustomerPermissionHandler extends DataScopeTablePermissionHandler {

    {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.POOL_CUSTOMER, this);
    }

    public static final String POOL_CUSTOMER_SCOPE_SQL_TEMPLATE = """
            select {0}
            from customer c
            where c.organization_id = ''{1}''
            and in_shared_pool is true
            and pool_id in ({2})
            """;

    @Resource
    private BaseMapper<CustomerPool> poolMapper;
    @Resource
    private UserExtendService userExtendService;

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(),
                Set.of("owner", "collection_time", "follower", "follow_time",
                        "in_shared_pool", "organization_id", "reason_id"));

        table.setFields(filterFields);

        String sql = getTableSql(filterFields, tableHandleParam);
        table.setSql(sql);
    }

    protected String getTableSql(List<FieldDTO> sqlBotFields, TableHandleParam tableHandleParam) {
        List<String> clueIds = getClueIds(tableHandleParam.getUserId(), tableHandleParam.getOrgId());
        if (clueIds.isEmpty()) {
            return "select 1 where 1=0"; // 返回空结果集
        }
        return MessageFormat.format(POOL_CUSTOMER_SCOPE_SQL_TEMPLATE,
                getSelectSystemFileSql(sqlBotFields),
                tableHandleParam.getOrgId(),
                getInConditionStr(clueIds)

        );
    }

    @Override
    protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
        String fieldName = sqlBotField.getName();
        if (Strings.CS.equals(fieldName, "pool_id")) {
            sqlBotField.setName("pool_name");
            sqlBotField.setComment("公海名称");
            return "(select pool.name from customer_pool pool where c.pool_id = pool.id limit 1) as pool_name" ;
        } else {
            return getDefaultFieldSql(sqlBotField);
        }
    }

    private List<String> getClueIds(String currentUser, String currentOrgId) {
        LambdaQueryWrapper<CustomerPool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(CustomerPool::getEnable, true).eq(CustomerPool::getOrganizationId, currentOrgId);
        poolWrapper.orderByDesc(CustomerPool::getUpdateTime);
        List<CustomerPool> pools = poolMapper.selectListByLambda(poolWrapper);

        List<String> customerIds = new ArrayList<>();
        pools.forEach(pool -> {
            List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), currentOrgId);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), currentOrgId);
            if (scopeIds.contains(currentUser) || ownerIds.contains(currentUser) || Strings.CS.equals(currentUser, InternalUser.ADMIN.getValue())) {
                customerIds.add(pool.getId());
            }
        });
        return customerIds;
    }
}
