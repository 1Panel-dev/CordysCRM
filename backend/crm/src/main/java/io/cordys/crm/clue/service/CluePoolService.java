package io.cordys.crm.clue.service;

import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolPickRule;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.domain.CluePoolRelation;
import io.cordys.crm.clue.dto.CluePoolDTO;
import io.cordys.crm.clue.dto.CluePoolPickRuleDTO;
import io.cordys.crm.clue.dto.CluePoolRecycleRuleDTO;
import io.cordys.crm.clue.dto.request.CluePoolAddRequest;
import io.cordys.crm.clue.dto.request.CluePoolUpdateRequest;
import io.cordys.crm.clue.mapper.ExtCluePoolMapper;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.RuleConditionDTO;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class CluePoolService {

    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<Role> roleMapper;
    @Resource
    private BaseMapper<Department> departmentMapper;
    @Resource
    private BaseMapper<CluePoolPickRule> cluePoolPickRuleMapper;
    @Resource
    private BaseMapper<CluePoolRecycleRule> cluePoolRecycleRuleMapper;
    @Resource
    private BaseMapper<CluePoolRelation> cluePoolRelationMapper;
    @Resource
    private ExtCluePoolMapper extCluePoolMapper;
    @Resource
    private UserExtendService userExtendService;

    /**
     * 分页获取线索池
     *
     * @param request 分页参数
     * @return 线索池列表
     */
    public List<CluePoolDTO> page(BasePageRequest request) {
        List<CluePoolDTO> pools = extCluePoolMapper.list(request, OrganizationContext.getOrganizationId());
        if (CollectionUtils.isEmpty(pools)) {
            return new ArrayList<>();
        }

        List<String> userIds = new ArrayList<>();
        List<String> scopeIds = new ArrayList<>();
        List<String> ownerIds = new ArrayList<>();

        pools.forEach(pool -> {
            userIds.add(pool.getCreateUser());
            userIds.add(pool.getUpdateUser());
            scopeIds.addAll(JSON.parseArray(pool.getScopeId(), String.class));
            ownerIds.addAll(JSON.parseArray(pool.getOwnerId(), String.class));
        });

        List<String> unionIds = ListUtils.union(scopeIds, ownerIds)
                .stream()
                .distinct()
                .toList();

        List<User> users = userMapper.selectByIds(unionIds.toArray(new String[0]));
        List<Role> roles = roleMapper.selectByIds(unionIds.toArray(new String[0]));
        List<Department> departments = departmentMapper.selectByIds(unionIds.toArray(new String[0]));
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

        pools.forEach(pool -> {
            pool.setMembers(userExtendService.getScope(users, roles, departments,
                    JSON.parseArray(pool.getScopeId(), String.class)));
            pool.setOwners(userExtendService.getScope(users, roles, departments,
                    JSON.parseArray(pool.getOwnerId(), String.class)));
            pool.setCreateUserName(userMap.get(pool.getCreateUser()));
            pool.setUpdateUserName(userMap.get(pool.getUpdateUser()));

            CluePoolPickRuleDTO pickRule = new CluePoolPickRuleDTO();
            BeanUtils.copyBean(pickRule, pickRuleMap.get(pool.getId()));
            CluePoolRecycleRuleDTO recycleRule = new CluePoolRecycleRuleDTO();
            CluePoolRecycleRule cluePoolRecycleRule = recycleRuleMap.get(pool.getId());
            BeanUtils.copyBean(recycleRule, cluePoolRecycleRule);
            recycleRule.setConditions(JSON.parseArray(cluePoolRecycleRule.getCondition(), RuleConditionDTO.class));

            pool.setPickRule(pickRule);
            pool.setRecycleRule(recycleRule);
        });

        return pools;
    }

    /**
     * 添加线索池
     *
     * @param request 添加参数
     */
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
        recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
        recycleRule.setUpdateTime(System.currentTimeMillis());
        recycleRule.setUpdateUser(currentUserId);

        cluePoolRecycleRuleMapper.insert(recycleRule);
    }

    /**
     * 修改线索池
     *
     * @param request 修改参数
     */
    public void update(CluePoolUpdateRequest request, String currentUserId, String currentOrgId) {
        CluePool oldPool = checkPoolExist(request.getId());
        checkPoolOwner(oldPool, currentUserId);

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
        recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
        recycleRule.setUpdateTime(System.currentTimeMillis());
        recycleRule.setUpdateUser(currentUserId);

        extCluePoolMapper.updateRecycleRule(recycleRule);
    }

    /**
     * 线索池是否存在未领取线索
     *
     * @param id 线索池ID
     */
    public boolean checkNoPick(String id) {
        LambdaQueryWrapper<CluePoolRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CluePoolRelation::getPoolId, id)
                .eq(CluePoolRelation::getPicked, false);
        List<CluePoolRelation> cluePoolRelations = cluePoolRelationMapper.selectListByLambda(wrapper);
        return CollectionUtils.isNotEmpty(cluePoolRelations);
    }

    /**
     * 删除线索池
     *
     * @param id 线索池ID
     */
    public void delete(String id, String currentUserId) {
        CluePool pool = checkPoolExist(id);
        checkPoolOwner(pool, currentUserId);

        cluePoolMapper.deleteByPrimaryKey(id);

        CluePoolPickRule pickRule = new CluePoolPickRule();
        pickRule.setPoolId(id);

        cluePoolPickRuleMapper.delete(pickRule);

        CluePoolRecycleRule recycleRule = new CluePoolRecycleRule();
        recycleRule.setPoolId(id);

        cluePoolRecycleRuleMapper.delete(recycleRule);
    }

    /**
     * 启用/禁用线索池
     *
     * @param id 线索池ID
     */
    public void switchStatus(String id, String currentUserId) {
        CluePool pool = checkPoolExist(id);
        checkPoolOwner(pool, currentUserId);

        pool.setEnable(!pool.getEnable());
        pool.setUpdateTime(System.currentTimeMillis());
        pool.setUpdateUser(currentUserId);

        cluePoolMapper.updateById(pool);
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
        List<String> ownerIds = JSON.parseArray(pool.getOwnerId(), String.class);
        List<String> ownerUserIds = extUserMapper.getUserIdsByScope(ownerIds, pool.getOrganizationId());

        if (!ownerUserIds.contains(accessUserId)) {
            throw new GenericException(Translator.get("clue_pool_access_fail"));
        }
    }
}
