package io.cordys.crm.integration.sqlbot.handler;


import io.cordys.common.constants.InternalUser;
import io.cordys.common.util.JSON;
import io.cordys.crm.clue.constants.ClueStatus;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import io.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import io.cordys.crm.integration.sqlbot.dto.FieldDTO;
import io.cordys.crm.integration.sqlbot.dto.TableDTO;
import io.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class PoolCluePermissionHandler extends DataScopeTablePermissionHandler {

    {
        TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.POOL_CLUE, this);
    }

    public static final String POOL_CLUE_SCOPE_SQL_TEMPLATE = """
            select {0}
            from clue c
            where c.organization_id = ''{1}''
            and in_shared_pool is true
            and pool_id in ({2})
            """;

    @Resource
    private BaseMapper<CluePool> poolMapper;
    @Resource
    private UserExtendService userExtendService;

    @Override
    public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
        List<FieldDTO> filterFields = filterSystemFields(table.getFields(),
                Set.of("owner", "collection_time", "contact", "phone", "follower", "follow_time",
                        "in_shared_pool", "organization_id", "reason_id", "transition_type", "transition_id", "last_stage"));

        table.setFields(filterFields);

        String sql = getTableSql(filterFields, tableHandleParam);
        table.setSql(sql);
    }

    protected String getTableSql(List<FieldDTO> sqlBotFields, TableHandleParam tableHandleParam) {
        List<String> clueIds = getClueIds(tableHandleParam.getUserId(), tableHandleParam.getOrgId());
        if (clueIds.isEmpty()) {
            return "select 1 where 1=0"; // 返回空结果集
        }
        return MessageFormat.format(POOL_CLUE_SCOPE_SQL_TEMPLATE,
                getSelectSystemFileSql(sqlBotFields),
                tableHandleParam.getOrgId(),
                getInConditionStr(clueIds)

        );
    }

    @Override
    protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
        String fieldName = sqlBotField.getName();
        if (StringUtils.equals(fieldName, "stage")) {
            return getSystemOptionFileSql(Arrays.stream(ClueStatus.values()),
                    ClueStatus::getKey,
                    ClueStatus::getName,
                    fieldName);
        } else if (StringUtils.equals(fieldName, "products")) {
            return getProductsFieldSql();
        } else if (StringUtils.equals(fieldName, "pool_id")) {
            sqlBotField.setName("pool_name");
            sqlBotField.setComment("线索池名称");
            return "(select pool.name from clue_pool pool where c.pool_id = pool.id limit 1) as pool_name" ;
        } else {
            return getDefaultFieldSql(sqlBotField);
        }
    }

    private List<String> getClueIds(String currentUser, String currentOrgId) {
        LambdaQueryWrapper<CluePool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(CluePool::getEnable, true).eq(CluePool::getOrganizationId, currentOrgId);
        poolWrapper.orderByDesc(CluePool::getUpdateTime);
        List<CluePool> cluePools = poolMapper.selectListByLambda(poolWrapper);

        List<String> clueIds = new ArrayList<>();
        cluePools.forEach(pool -> {
            List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), currentOrgId);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), currentOrgId);
            if (scopeIds.contains(currentUser) || ownerIds.contains(currentUser) || StringUtils.equals(currentUser, InternalUser.ADMIN.getValue())) {
                clueIds.add(pool.getId());
            }
        });
        return clueIds;
    }
}
