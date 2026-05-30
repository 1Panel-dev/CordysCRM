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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class PoolCustomerPermissionHandler extends DataScopeTablePermissionHandler {

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

    @PostConstruct
    public void registerHandler() {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.POOL_CUSTOMER, this);
    }

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
            return "(select pool.name from customer_pool pool where c.pool_id = pool.id limit 1) as pool_name";
        } else {
            return getDefaultFieldSql(sqlBotField);
        }
    }

    private List<String> getClueIds(String currentUser, String currentOrgId) {
        // 构建查询条件
        var poolWrapper = new LambdaQueryWrapper<CustomerPool>()
                .eq(CustomerPool::getEnable, true)
                .eq(CustomerPool::getOrganizationId, currentOrgId)
                .orderByDesc(CustomerPool::getUpdateTime);

        var pools = poolMapper.selectListByLambda(poolWrapper);
        if (pools == null || pools.isEmpty()) {
            return List.of();
        }

        // 管理员直接拥有所有客户池
        boolean isAdmin = Strings.CS.equals(currentUser, InternalUser.ADMIN.getValue());

        return pools.stream()
                .filter(pool -> {
                    var scopeIds = Optional.ofNullable(pool.getScopeId())
                            .map(ids -> userExtendService.getScopeOwnerIds(JSON.parseArray(ids, String.class), currentOrgId))
                            .orElse(List.of());

                    var ownerIds = Optional.ofNullable(pool.getOwnerId())
                            .map(ids -> userExtendService.getScopeOwnerIds(JSON.parseArray(ids, String.class), currentOrgId))
                            .orElse(List.of());

                    return isAdmin || scopeIds.contains(currentUser) || ownerIds.contains(currentUser);
                })
                .map(CustomerPool::getId)
                .toList();
    }
}
