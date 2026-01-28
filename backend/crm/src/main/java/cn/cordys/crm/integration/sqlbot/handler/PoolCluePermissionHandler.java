package cn.cordys.crm.integration.sqlbot.handler;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import cn.cordys.crm.integration.sqlbot.dto.FieldDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableDTO;
import cn.cordys.crm.integration.sqlbot.dto.TableHandleParam;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

@Component
public class PoolCluePermissionHandler extends DataScopeTablePermissionHandler {

  public static final String POOL_CLUE_SCOPE_SQL_TEMPLATE =
      """
      select {0}
      from clue c
      where c.organization_id = ''{1}''
      and in_shared_pool is true
      and pool_id in ({2})
      """;
  @Resource private BaseMapper<CluePool> poolMapper;
  @Resource private UserExtendService userExtendService;

  @PostConstruct
  public void registerHandler() {
    TablePermissionHandlerFactory.registerTableHandler(SQLBotTable.POOL_CLUE, this);
  }

  @Override
  public void handleTable(TableDTO table, TableHandleParam tableHandleParam) {
    List<FieldDTO> filterFields =
        filterSystemFields(
            table.getFields(),
            Set.of(
                "owner",
                "collection_time",
                "contact",
                "phone",
                "follower",
                "follow_time",
                "in_shared_pool",
                "organization_id",
                "reason_id",
                "transition_type",
                "transition_id",
                "last_stage"));

    table.setFields(filterFields);

    String sql = getTableSql(filterFields, tableHandleParam);
    table.setSql(sql);
  }

  protected String getTableSql(List<FieldDTO> sqlBotFields, TableHandleParam tableHandleParam) {
    List<String> clueIds = getClueIds(tableHandleParam.getUserId(), tableHandleParam.getOrgId());
    if (clueIds.isEmpty()) {
      return "select 1 where 1=0"; // 返回空结果集
    }
    return MessageFormat.format(
        POOL_CLUE_SCOPE_SQL_TEMPLATE,
        getSelectSystemFileSql(sqlBotFields),
        tableHandleParam.getOrgId(),
        getInConditionStr(clueIds));
  }

  @Override
  protected String getSelectSystemFileSql(FieldDTO sqlBotField) {
    String fieldName = sqlBotField.getName();
    if (Strings.CS.equals(fieldName, "stage")) {
      return getSystemOptionFileSql(
          Arrays.stream(ClueStatus.values()), ClueStatus::getKey, ClueStatus::getName, fieldName);
    } else if (Strings.CS.equals(fieldName, "products")) {
      return getProductsFieldSql();
    } else if (Strings.CS.equals(fieldName, "pool_id")) {
      sqlBotField.setName("pool_name");
      sqlBotField.setComment("线索池名称");
      return "(select pool.name from clue_pool pool where c.pool_id = pool.id limit 1) as"
                 + " pool_name";
    } else {
      return getDefaultFieldSql(sqlBotField);
    }
  }

  private List<String> getClueIds(String currentUser, String currentOrgId) {
    LambdaQueryWrapper<CluePool> query =
        new LambdaQueryWrapper<CluePool>()
            .eq(CluePool::getEnable, true)
            .eq(CluePool::getOrganizationId, currentOrgId)
            .orderByDesc(CluePool::getUpdateTime);

    List<CluePool> cluePools = poolMapper.selectListByLambda(query);
    if (cluePools == null || cluePools.isEmpty()) {
      return Collections.emptyList();
    }

    return cluePools.stream()
        .filter(
            pool -> {
              List<String> scopeIds =
                  Optional.ofNullable(pool.getScopeId())
                      .filter(StringUtils::isNotBlank)
                      .map(json -> JSON.parseArray(json, String.class))
                      .map(ids -> userExtendService.getScopeOwnerIds(ids, currentOrgId))
                      .orElse(Collections.emptyList());

              List<String> ownerIds =
                  Optional.ofNullable(pool.getOwnerId())
                      .filter(StringUtils::isNotBlank)
                      .map(json -> JSON.parseArray(json, String.class))
                      .map(ids -> userExtendService.getScopeOwnerIds(ids, currentOrgId))
                      .orElse(Collections.emptyList());

              return scopeIds.contains(currentUser)
                  || ownerIds.contains(currentUser)
                  || Strings.CS.equals(currentUser, InternalUser.ADMIN.getValue());
            })
        .map(CluePool::getId)
        .toList();
  }
}
