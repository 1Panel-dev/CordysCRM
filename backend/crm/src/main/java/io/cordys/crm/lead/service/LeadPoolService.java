package io.cordys.crm.lead.service;

import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.lead.domain.LeadPool;
import io.cordys.crm.lead.domain.LeadPoolPickRule;
import io.cordys.crm.lead.domain.LeadPoolRecycleRule;
import io.cordys.crm.lead.domain.LeadPoolRelation;
import io.cordys.crm.lead.dto.LeadPoolDTO;
import io.cordys.crm.lead.dto.LeadPoolRuleDTO;
import io.cordys.crm.lead.dto.request.LeadPoolAddRequest;
import io.cordys.crm.lead.dto.request.LeadPoolUpdateRequest;
import io.cordys.crm.lead.mapper.ExtLeadPoolMapper;
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
public class LeadPoolService {

    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<LeadPool> leadPoolMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<Role> roleMapper;
    @Resource
    private BaseMapper<Department> departmentMapper;
    @Resource
    private BaseMapper<LeadPoolPickRule> leadPoolPickRuleMapper;
    @Resource
    private BaseMapper<LeadPoolRecycleRule> leadPoolRecycleRuleMapper;
    @Resource
    private BaseMapper<LeadPoolRelation> leadPoolRelationMapper;
    @Resource
    private ExtLeadPoolMapper extLeadPoolMapper;
    @Resource
    private UserExtendService userExtendService;

    /**
     * 分页获取线索池
     *
     * @param request 分页参数
     * @return 线索池列表
     */
    public List<LeadPoolDTO> page(BasePageRequest request) {
        List<LeadPoolDTO> pools = extLeadPoolMapper.list(request, OrganizationContext.getOrganizationId());
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
                .map(LeadPoolDTO::getId)
                .toList();

        LambdaQueryWrapper<LeadPoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
        pickRuleWrapper.in(LeadPoolPickRule::getPoolId, poolIds);

        List<LeadPoolPickRule> pickRules = leadPoolPickRuleMapper.selectListByLambda(pickRuleWrapper);
        Map<String, LeadPoolPickRule> pickRuleMap = pickRules.stream()
                .collect(Collectors.toMap(LeadPoolPickRule::getPoolId, pickRule -> pickRule));

        LambdaQueryWrapper<LeadPoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
        recycleRuleWrapper.in(LeadPoolRecycleRule::getPoolId, poolIds);

        List<LeadPoolRecycleRule> recycleRules = leadPoolRecycleRuleMapper.selectListByLambda(recycleRuleWrapper);
        Map<String, LeadPoolRecycleRule> recycleRuleMap = recycleRules.stream()
                .collect(Collectors.toMap(LeadPoolRecycleRule::getPoolId, recycleRule -> recycleRule));

        pools.forEach(pool -> {
            pool.setMembers(userExtendService.getScope(users, roles, departments,
                    JSON.parseArray(pool.getScopeId(), String.class)));
            pool.setOwners(userExtendService.getScope(users, roles, departments,
                    JSON.parseArray(pool.getOwnerId(), String.class)));
            pool.setCreateUserName(userMap.get(pool.getCreateUser()));
            pool.setUpdateUserName(userMap.get(pool.getUpdateUser()));

            LeadPoolPickRule pickRule = pickRuleMap.get(pool.getId());
            LeadPoolRecycleRule recycleRule = recycleRuleMap.get(pool.getId());

            LeadPoolRuleDTO rule = LeadPoolRuleDTO.builder()
                    .limitOnNumber(pickRule.getLimitOnNumber())
                    .pickNumber(pickRule.getPickNumber())
                    .limitPreOwner(pickRule.getLimitPreOwner())
                    .pickIntervalDays(pickRule.getPickIntervalDays())
                    .expireNotice(recycleRule.getExpireNotice())
                    .noticeDays(recycleRule.getNoticeDays())
                    .operator(recycleRule.getOperator())
                    .conditions(JSON.parseArray(recycleRule.getCondition(), RuleConditionDTO.class))
                    .build();

            pool.setRule(rule);
        });

        return pools;
    }

    /**
     * 添加线索池
     *
     * @param request 添加参数
     */
    public void add(LeadPoolAddRequest request, String currentUserId, String currentOrgId) {
        LeadPool pool = new LeadPool();
        BeanUtils.copyBean(pool, request);
        pool.setId(IDGenerator.nextStr());
        pool.setOrganizationId(currentOrgId);
        pool.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
        pool.setScopeId(JSON.toJSONString(request.getScopeIds()));
        pool.setCreateTime(System.currentTimeMillis());
        pool.setCreateUser(currentUserId);
        pool.setUpdateTime(System.currentTimeMillis());
        pool.setUpdateUser(currentUserId);

        leadPoolMapper.insert(pool);

        LeadPoolPickRule pickRule = new LeadPoolPickRule();
        BeanUtils.copyBean(pickRule, request.getPickRule());
        pickRule.setId(IDGenerator.nextStr());
        pickRule.setPoolId(pool.getId());
        pickRule.setCreateTime(System.currentTimeMillis());
        pickRule.setCreateUser(currentUserId);
        pickRule.setUpdateTime(System.currentTimeMillis());
        pickRule.setUpdateUser(currentUserId);

        leadPoolPickRuleMapper.insert(pickRule);

        LeadPoolRecycleRule recycleRule = new LeadPoolRecycleRule();
        BeanUtils.copyBean(recycleRule, request.getRecycleRule());
        recycleRule.setId(IDGenerator.nextStr());
        recycleRule.setPoolId(pool.getId());
        recycleRule.setCreateTime(System.currentTimeMillis());
        recycleRule.setCreateUser(currentUserId);
        recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
        recycleRule.setUpdateTime(System.currentTimeMillis());
        recycleRule.setUpdateUser(currentUserId);

        leadPoolRecycleRuleMapper.insert(recycleRule);
    }

    /**
     * 修改线索池
     *
     * @param request 修改参数
     */
    public void update(LeadPoolUpdateRequest request, String currentUserId, String currentOrgId) {
        LeadPool oldPool = checkPoolExist(request.getId());
        checkPoolOwner(oldPool, currentUserId);

        LeadPool pool = new LeadPool();
        BeanUtils.copyBean(pool, request);
        pool.setOrganizationId(currentOrgId);
        pool.setOwnerId(JSON.toJSONString(request.getOwnerIds()));
        pool.setScopeId(JSON.toJSONString(request.getScopeIds()));
        pool.setUpdateTime(System.currentTimeMillis());
        pool.setUpdateUser(currentUserId);

        leadPoolMapper.update(pool);

        LeadPoolPickRule pickRule = new LeadPoolPickRule();
        BeanUtils.copyBean(pickRule, request.getPickRule());
        pickRule.setPoolId(pool.getId());
        pickRule.setUpdateTime(System.currentTimeMillis());
        pickRule.setUpdateUser(currentUserId);

        extLeadPoolMapper.updatePickRule(pickRule);

        LeadPoolRecycleRule recycleRule = new LeadPoolRecycleRule();
        BeanUtils.copyBean(recycleRule, request.getRecycleRule());
        recycleRule.setPoolId(pool.getId());
        recycleRule.setCondition(JSON.toJSONString(request.getRecycleRule().getConditions()));
        recycleRule.setUpdateTime(System.currentTimeMillis());
        recycleRule.setUpdateUser(currentUserId);

        extLeadPoolMapper.updateRecycleRule(recycleRule);
    }

    /**
     * 线索池是否存在未领取线索
     *
     * @param id 线索池ID
     */
    public boolean checkNoPick(String id) {
        LambdaQueryWrapper<LeadPoolRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeadPoolRelation::getPoolId, id)
                .eq(LeadPoolRelation::getPicked, false);
        List<LeadPoolRelation> leadPoolRelations = leadPoolRelationMapper.selectListByLambda(wrapper);
        return CollectionUtils.isNotEmpty(leadPoolRelations);
    }

    /**
     * 删除线索池
     *
     * @param id 线索池ID
     */
    public void delete(String id, String currentUserId) {
        LeadPool pool = checkPoolExist(id);
        checkPoolOwner(pool, currentUserId);

        leadPoolMapper.deleteByPrimaryKey(id);

        LeadPoolPickRule pickRule = new LeadPoolPickRule();
        pickRule.setPoolId(id);

        leadPoolPickRuleMapper.delete(pickRule);

        LeadPoolRecycleRule recycleRule = new LeadPoolRecycleRule();
        recycleRule.setPoolId(id);

        leadPoolRecycleRuleMapper.delete(recycleRule);
    }

    /**
     * 启用/禁用线索池
     *
     * @param id 线索池ID
     */
    public void switchStatus(String id, String currentUserId) {
        LeadPool pool = checkPoolExist(id);
        checkPoolOwner(pool, currentUserId);

        pool.setEnable(!pool.getEnable());
        pool.setUpdateTime(System.currentTimeMillis());
        pool.setUpdateUser(currentUserId);

        leadPoolMapper.updateById(pool);
    }

    /**
     * 校验线索池是否存在
     *
     * @param id 线索池ID
     * @return 线索池
     */
    private LeadPool checkPoolExist(String id) {
        LeadPool pool = leadPoolMapper.selectByPrimaryKey(id);
        if (pool == null) {
            throw new GenericException(Translator.get("lead_pool_not_exist"));
        }
        return pool;
    }

    /**
     * 校验是否线索池的管理员
     *
     * @param pool         线索池
     * @param accessUserId 访问用户ID
     */
    private void checkPoolOwner(LeadPool pool, String accessUserId) {
        List<String> ownerIds = JSON.parseArray(pool.getOwnerId(), String.class);
        List<String> ownerUserIds = extUserMapper.getUserIdsByScope(ownerIds, pool.getOrganizationId());

        if (!ownerUserIds.contains(accessUserId)) {
            throw new GenericException(Translator.get("lead_pool_access_fail"));
        }
    }
}
