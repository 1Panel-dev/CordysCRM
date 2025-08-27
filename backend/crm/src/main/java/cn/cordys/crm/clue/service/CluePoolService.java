package cn.cordys.crm.clue.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.RecycleConditionUtils;
import cn.cordys.crm.clue.domain.*;
import cn.cordys.crm.clue.dto.CluePoolDTO;
import cn.cordys.crm.clue.dto.CluePoolFieldConfigDTO;
import cn.cordys.crm.clue.dto.CluePoolPickRuleDTO;
import cn.cordys.crm.clue.dto.CluePoolRecycleRuleDTO;
import cn.cordys.crm.clue.dto.request.CluePoolAddRequest;
import cn.cordys.crm.clue.dto.request.CluePoolUpdateRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.mapper.ExtCluePoolMapper;
import cn.cordys.crm.system.constants.RecycleConditionColumnKey;
import cn.cordys.crm.system.constants.RecycleConditionOperator;
import cn.cordys.crm.system.constants.RecycleConditionScopeKey;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.RuleConditionDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class CluePoolService {

    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private BaseMapper<CluePoolHiddenField> cluePoolHiddenFieldMapper;
    @Resource
    private BaseMapper<CluePoolPickRule> cluePoolPickRuleMapper;
    @Resource
    private BaseMapper<CluePoolRecycleRule> cluePoolRecycleRuleMapper;
    @Resource
    private ExtCluePoolMapper extCluePoolMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    /**
     * 分页获取线索池
     *
     * @param request 分页参数
     * @return 线索池列表
     */
    public List<CluePoolDTO> page(BasePageRequest request, String organizationId) {
        List<CluePoolDTO> pools = extCluePoolMapper.list(request, organizationId);
        if (CollectionUtils.isEmpty(pools)) {
            return new ArrayList<>();
        }

        List<String> userIds = pools.stream().flatMap(pool -> Stream.of(pool.getCreateUser(), pool.getUpdateUser())).toList();
        List<User> createOrUpdateUsers = userMapper.selectByIds(userIds.toArray(new String[0]));
        Map<String, String> userMap = createOrUpdateUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<String> poolIds = pools.stream()
                .map(CluePoolDTO::getId)
                .toList();

        LambdaQueryWrapper<CluePoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
        pickRuleWrapper.in(CluePoolPickRule::getPoolId, poolIds);

        List<CluePoolPickRule> pickRules = cluePoolPickRuleMapper.selectListByLambda(pickRuleWrapper);
        Map<String, CluePoolPickRule> pickRuleMap = pickRules.stream()
                .collect(Collectors.toMap(CluePoolPickRule::getPoolId, pickRule -> pickRule));

        LambdaQueryWrapper<CluePoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
        recycleRuleWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);

        List<CluePoolRecycleRule> recycleRules = cluePoolRecycleRuleMapper.selectListByLambda(recycleRuleWrapper);
        Map<String, CluePoolRecycleRule> recycleRuleMap = recycleRules.stream()
                .collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, recycleRule -> recycleRule));

        Map<String, List<CluePoolHiddenField>> hiddenFieldMap = getCluePoolHiddenFieldsByPoolIds(poolIds)
                .stream()
                .collect(Collectors.groupingBy(CluePoolHiddenField::getPoolId));

        List<BaseField> fields = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), organizationId).getFields();

        pools.forEach(pool -> {
            pool.setMembers(userExtendService.getScope(JSON.parseArray(pool.getScopeId(), String.class)));
            pool.setOwners(userExtendService.getScope(JSON.parseArray(pool.getOwnerId(), String.class)));
            pool.setCreateUserName(userMap.get(pool.getCreateUser()));
            pool.setUpdateUserName(userMap.get(pool.getUpdateUser()));

            CluePoolPickRuleDTO pickRule = new CluePoolPickRuleDTO();
            BeanUtils.copyBean(pickRule, pickRuleMap.get(pool.getId()));
            CluePoolRecycleRuleDTO recycleRule = new CluePoolRecycleRuleDTO();
            CluePoolRecycleRule cluePoolRecycleRule = recycleRuleMap.get(pool.getId());
            BeanUtils.copyBean(recycleRule, cluePoolRecycleRule);
            recycleRule.setConditions(JSON.parseArray(cluePoolRecycleRule.getCondition(), RuleConditionDTO.class));
            delOldTime(recycleRule);
            pool.setPickRule(pickRule);
            pool.setRecycleRule(recycleRule);

            Set<String> hiddenFieldIds;
            if (hiddenFieldMap.get(pool.getId()) != null) {
                hiddenFieldIds = hiddenFieldMap.get(pool.getId()).stream()
                        .map(CluePoolHiddenField::getFieldId)
                        .collect(Collectors.toSet());
            } else {
                hiddenFieldIds = Set.of();
            }

            pool.setFieldConfigs(getCluePoolFieldConfigs(fields, hiddenFieldIds));
        });

        return pools;
    }

    private void delOldTime(CluePoolRecycleRuleDTO recycleRule) {
        recycleRule.getConditions().forEach(condition -> {
            if (Strings.CS.equals(condition.getColumn(), RecycleConditionColumnKey.STORAGE_TIME)
                    && Strings.CS.equals(condition.getOperator(), RecycleConditionOperator.DYNAMICS.name())) {
                String[] split = condition.getValue().split(",");
                if (StringUtils.isNotBlank(condition.getValue()) && split.length == 2 ) {
                    String dateValue = split[0];
                    String dateUnit = split[1];
                    dateUnit = switch (dateUnit) {
                        case "day" -> "BEFORE_DAY";
                        case "month" -> "BEFORE_MONTH";
                        case "week" -> "BEFORE_WEEK";
                        default -> dateUnit;
                    };
                    condition.setValue("CUSTOM,"+ dateValue + "," + dateUnit);
                }
            }
        });
    }

    public List<CluePoolFieldConfigDTO> getCluePoolFieldConfigs(List<BaseField> fields, Set<String> hiddenFieldIds) {
        List<CluePoolFieldConfigDTO> hiddenFields = fields.stream().map(field -> {
            CluePoolFieldConfigDTO hiddenFieldDTO = new CluePoolFieldConfigDTO();
            hiddenFieldDTO.setFieldId(field.getId());
            hiddenFieldDTO.setFieldName(field.getName());
            hiddenFieldDTO.setEnable(!hiddenFieldIds.contains(field.getId()));
            hiddenFieldDTO.setEditable(!Strings.CS.equals(field.getInternalKey(), BusinessModuleField.CLUE_NAME.getKey()));
            return hiddenFieldDTO;
        }).toList();
        return hiddenFields;
    }

    public List<CluePoolHiddenField> getCluePoolHiddenFieldsByPoolIds(List<String> poolIds) {
        LambdaQueryWrapper<CluePoolHiddenField> hiddenFieldWrapper = new LambdaQueryWrapper<>();
        hiddenFieldWrapper.in(CluePoolHiddenField::getPoolId, poolIds);
        return cluePoolHiddenFieldMapper.selectListByLambda(hiddenFieldWrapper);
    }

    /**
     * 添加线索池
     *
     * @param request 添加参数
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
    public void add(CluePoolAddRequest request, String currentUserId, String currentOrgId) {
        CluePool pool = new CluePool();
        BeanUtils.copyBean(pool, request);
        pool.setId(IDGenerator.nextStr());
        pool.setOrganizationId(currentOrgId);
        pool.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
        pool.setScopeId(JSON.toJSONString(request.getScopeIds()));
        pool.setCreateTime(System.currentTimeMillis());
        pool.setCreateUser(currentUserId);
        pool.setUpdateTime(System.currentTimeMillis());
        pool.setUpdateUser(currentUserId);

        cluePoolMapper.insert(pool);

        CluePoolPickRule pickRule = new CluePoolPickRule();
        BeanUtils.copyBean(pickRule, request.getPickRule());
        pickRule.setId(IDGenerator.nextStr());
        pickRule.setPoolId(pool.getId());
        pickRule.setCreateTime(System.currentTimeMillis());
        pickRule.setCreateUser(currentUserId);
        pickRule.setUpdateTime(System.currentTimeMillis());
        pickRule.setUpdateUser(currentUserId);

        cluePoolPickRuleMapper.insert(pickRule);

        CluePoolRecycleRule recycleRule = new CluePoolRecycleRule();
        BeanUtils.copyBean(recycleRule, request.getRecycleRule());
        recycleRule.setId(IDGenerator.nextStr());
        recycleRule.setPoolId(pool.getId());
        recycleRule.setCreateTime(System.currentTimeMillis());
        recycleRule.setCreateUser(currentUserId);
        try {
            recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
        } catch (Exception e) {
            throw new GenericException(Translator.get("customer_rule_condition_error"));
        }
        recycleRule.setUpdateTime(System.currentTimeMillis());
        recycleRule.setUpdateUser(currentUserId);

        cluePoolRecycleRuleMapper.insert(recycleRule);

        batchInsertCluePoolHiddenFields(pool.getId(), request.getHiddenFieldIds());

        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(pool)
                .resourceId(pool.getId())
                .resourceName(Translator.get("module.clue.pool.setting"))
                .build());
    }

    /**
     * 修改线索池
     *
     * @param request 修改参数
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void update(CluePoolUpdateRequest request, String currentUserId, String currentOrgId) {
        CluePool originPool = checkPoolExist(request.getId());

        CluePool pool = new CluePool();
        BeanUtils.copyBean(pool, request);
        pool.setOrganizationId(currentOrgId);
        pool.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
        pool.setScopeId(JSON.toJSONString(request.getScopeIds()));
        pool.setUpdateTime(System.currentTimeMillis());
        pool.setUpdateUser(currentUserId);

        cluePoolMapper.update(pool);

        CluePoolPickRule pickRule = new CluePoolPickRule();
        BeanUtils.copyBean(pickRule, request.getPickRule());
        pickRule.setPoolId(pool.getId());
        pickRule.setUpdateTime(System.currentTimeMillis());
        pickRule.setUpdateUser(currentUserId);

        extCluePoolMapper.updatePickRule(pickRule);

        CluePoolRecycleRule recycleRule = new CluePoolRecycleRule();
        BeanUtils.copyBean(recycleRule, request.getRecycleRule());
        recycleRule.setPoolId(pool.getId());
        try {
            recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
        } catch (Exception e) {
            throw new GenericException(Translator.get("customer_rule_condition_error"));
        }
        recycleRule.setUpdateTime(System.currentTimeMillis());
        recycleRule.setUpdateUser(currentUserId);

        extCluePoolMapper.updateRecycleRule(recycleRule);

        if (request.getHiddenFieldIds() != null) {
            deleteCluePoolHiddenFieldByPoolId(pool.getId());
            batchInsertCluePoolHiddenFields(pool.getId(), request.getHiddenFieldIds());
        }

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(pool.getId())
                        .resourceName(Translator.get("module.clue.pool.setting"))
                        .originalValue(originPool)
                        .modifiedValue(cluePoolMapper.selectByPrimaryKey(request.getId()))
                        .build()
        );
    }


    private void batchInsertCluePoolHiddenFields(String poolId, Set<String> fieldIds) {
        if (CollectionUtils.isEmpty(fieldIds)) {
            return;
        }
        List<CluePoolHiddenField> cluePoolHiddenFields = fieldIds.stream()
                .map(fieldId -> {
                    CluePoolHiddenField cluePoolHiddenField = new CluePoolHiddenField();
                    cluePoolHiddenField.setFieldId(fieldId);
                    cluePoolHiddenField.setPoolId(poolId);
                    return cluePoolHiddenField;
                }).toList();
        cluePoolHiddenFieldMapper.batchInsert(cluePoolHiddenFields);
    }

    private void deleteCluePoolHiddenFieldByPoolId(String poolId) {
        CluePoolHiddenField cluePoolHiddenField = new CluePoolHiddenField();
        cluePoolHiddenField.setPoolId(poolId);
        cluePoolHiddenFieldMapper.delete(cluePoolHiddenField);
    }

    /**
     * 线索池是否存在未领取线索
     *
     * @param id 线索池ID
     */
    public boolean checkNoPick(String id) {
        LambdaQueryWrapper<Clue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Clue::getPoolId, id)
                .eq(Clue::getInSharedPool, true);
        List<Clue> relations = clueMapper.selectListByLambda(wrapper);
        return CollectionUtils.isNotEmpty(relations);
    }

    /**
     * 删除线索池
     *
     * @param id 线索池ID
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        checkPoolExist(id);
        cluePoolMapper.deleteByPrimaryKey(id);
        CluePoolPickRule pickRule = new CluePoolPickRule();
        pickRule.setPoolId(id);
        cluePoolPickRuleMapper.delete(pickRule);
        CluePoolRecycleRule recycleRule = new CluePoolRecycleRule();
        recycleRule.setPoolId(id);
        cluePoolRecycleRuleMapper.delete(recycleRule);
        deleteCluePoolHiddenFieldByPoolId(id);

        // 设置操作对象
        OperationLogContext.setResourceName(Translator.get("module.clue.pool.setting"));
    }

    /**
     * 启用/禁用线索池
     *
     * @param id 线索池ID
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, resourceId = "{#id}")
    public void switchStatus(String id, String currentUserId) {
        CluePool pool = checkPoolExist(id);

        pool.setEnable(!pool.getEnable());
        pool.setUpdateTime(System.currentTimeMillis());
        pool.setUpdateUser(currentUserId);

        cluePoolMapper.updateById(pool);

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(Translator.get("module.clue.pool.setting"))
                        .originalValue(pool)
                        .modifiedValue(cluePoolMapper.selectByPrimaryKey(id))
                        .build()
        );
    }

    /**
     * 获取负责人默认线索池ID
     * @param ownerIds 负责人ID集合
     * @param organizationId 组织ID
     * @return 默认线索池
     */
    public Map<String, CluePool> getOwnersDefaultPoolMap(List<String> ownerIds, String organizationId) {
        Map<String, CluePool> poolMap = new HashMap<>(4);
        List<CluePool> pools = extCluePoolMapper.getAllPool(organizationId);
        Map<String, List<String>> ownerScopeMap = userExtendService.getMultiScopeMap(ownerIds, organizationId);
        ownerIds.forEach(ownerId -> {
            List<CluePool> matchPools = matchMultiScope(ownerScopeMap.get(ownerId), pools);
            if (CollectionUtils.isEmpty(matchPools)) {
                // not found pool for owner
                return;
            }
            poolMap.put(ownerId, matchPools.getFirst());
        });

        return poolMap;
    }

    /**
     * 匹配多个范围的线索池
     * @param scopeIds 范围ID集合
     * @param pools 线索池列表
     * @return 命中范围的线索池列表
     */
    public List<CluePool> matchMultiScope(List<String> scopeIds, List<CluePool> pools) {
        /*
         * 命中线索池任意范围即返回(默认按照创建时间作为优先级)
         */
        if (CollectionUtils.isEmpty(scopeIds) || CollectionUtils.isEmpty(pools)) {
            return new ArrayList<>();
        }
        return pools.stream()
                .filter(pool -> {
                    List<String> poolScopes = JSON.parseArray(pool.getScopeId(), String.class);
                    return CollectionUtils.isNotEmpty(poolScopes) && CollectionUtils.containsAny(scopeIds, poolScopes);
                })
                .sorted(Comparator.comparing(CluePool::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 计算剩余归属天数
     *
     * @param pool 线索池
     * @param clue 线索
     * @return 剩余归属天数
     */
    public Integer calcReservedDay(CluePool pool, CluePoolRecycleRule recycleRule, ClueListResponse clue) {
        if (pool == null || !pool.getAuto() || recycleRule == null) {
            return null;
        }

        // 判断线索池是否存在入库条件
        List<RuleConditionDTO> conditions = JSON.parseArray(recycleRule.getCondition(), RuleConditionDTO.class);
        return RecycleConditionUtils.calcRecycleDays(conditions, Math.min(clue.getCollectionTime(), clue.getCreateTime()));
    }

    /**
     * 校验线索池是否存在
     *
     * @param id 线索池ID
     * @return 线索池
     */
    private CluePool checkPoolExist(String id) {
        CluePool pool = cluePoolMapper.selectByPrimaryKey(id);
        if (pool == null) {
            throw new GenericException(Translator.get("clue_pool_not_exist"));
        }
        return pool;
    }

    /**
     * 校验是否线索池的管理员
     *
     * @param pool         线索池
     * @param accessUserId 访问用户ID
     */
    private void checkPoolOwner(CluePool pool, String accessUserId) {
        if (Strings.CS.equals(accessUserId, InternalUser.ADMIN.getValue())) {
            return;
        }
        List<String> ownerIds = JSON.parseArray(pool.getOwnerId(), String.class);
        List<String> ownerUserIds = userExtendService.getScopeOwnerIds(ownerIds, pool.getOrganizationId());

        if (!ownerUserIds.contains(accessUserId)) {
            throw new GenericException(Translator.get("clue_pool_access_fail"));
        }
    }

    /**
     * 获取负责人最佳匹配公海
     * @param pools 公海列表
     * @return 公海集合
     */
    public Map<List<String>, CluePool> getOwnersBestMatchPoolMap(List<CluePool> pools) {
        Map<List<String>, CluePool> poolMap = new HashMap<>(4);
        pools.sort(Comparator.comparing(CluePool::getCreateTime).reversed());
        for (CluePool pool : pools) {
            List<String> exitOwnerIds = poolMap.keySet().stream().flatMap(List::stream).toList();
            List<String> scopeIds = JSON.parseArray(pool.getScopeId(), String.class);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(scopeIds, pool.getOrganizationId());
            List<String> defaultOwnerIds = ownerIds.stream().distinct().filter(ownerId -> !exitOwnerIds.contains(ownerId)).toList();
            if (CollectionUtils.isEmpty(defaultOwnerIds)) {
                continue;
            }
            poolMap.put(defaultOwnerIds, pool);
        }
        return poolMap;
    }

    /**
     * 校验线索是否符合回收规则
     * @param clue 线索
     * @param recycleRule 回收规则
     * @return 是否符合回收规则
     */
    public boolean checkRecycled(Clue clue, CluePoolRecycleRule recycleRule) {
        boolean allMatch = Strings.CS.equals(CombineSearch.SearchMode.AND.name(), recycleRule.getOperator());
        List<RuleConditionDTO> conditions = JSON.parseArray(recycleRule.getCondition(), RuleConditionDTO.class);
        if (allMatch) {
            return conditions.stream().allMatch(condition -> matchTime(condition, clue));
        } else {
            return conditions.stream().anyMatch(condition -> matchTime(condition, clue));
        }
    }

    /**
     * 是否匹配时间规则
     * @param condition 规则
     * @param clue 线索
     * @return 是否匹配
     */
    private boolean matchTime(RuleConditionDTO condition, Clue clue) {
        if (Strings.CS.equals(condition.getColumn(), RecycleConditionColumnKey.STORAGE_TIME)) {
            if (condition.getScope().contains(RecycleConditionScopeKey.CREATED)) {
                return RecycleConditionUtils.matchTime(condition, clue.getCreateTime());
            } else if (condition.getScope().contains(RecycleConditionScopeKey.PICKED)) {
                return RecycleConditionUtils.matchTime(condition, clue.getCollectionTime());
            } else {
                return RecycleConditionUtils.matchTime(condition, clue.getCreateTime()) || RecycleConditionUtils.matchTime(condition, clue.getCollectionTime());
            }
        } else {
            return RecycleConditionUtils.matchTime(condition, clue.getFollowTime());
        }
    }
}
