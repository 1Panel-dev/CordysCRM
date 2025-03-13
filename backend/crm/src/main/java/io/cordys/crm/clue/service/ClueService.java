package io.cordys.crm.clue.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.FollowUpRecordDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.clue.constants.ClueResultCode;
import io.cordys.crm.clue.constants.ClueStatus;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.dto.request.*;
import io.cordys.crm.clue.dto.response.ClueGetResponse;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ClueService {

    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;
    @Resource
    private ClueOwnerHistoryService clueOwnerHistoryService;

    public List<ClueListResponse> list(CluePageRequest request, String userId, String orgId,
                                       DeptDataPermissionDTO deptDataPermission) {
        List<ClueListResponse> list = extClueMapper.list(request, orgId, userId, deptDataPermission);
        return buildListData(list, orgId);
    }

    private List<ClueListResponse> buildListData(List<ClueListResponse> list, String orgId) {
        if(CollectionUtils.isEmpty(list)){
            return list;
        }
        List<String> clueIds = list.stream().map(ClueListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = clueFieldService.getResourceFieldMap(clueIds);

        List<String> ownerIds = list.stream()
                .map(ClueListResponse::getOwner)
                .distinct()
                .toList();

        // 获取线索池信息
        List<String> poolIds = list.stream().map(ClueListResponse::getPoolId).distinct().toList();
        List<CluePool> pools = cluePoolService.getPoolsByIds(poolIds);
        Map<String, CluePool> poolMap = pools.stream().collect(Collectors.toMap(CluePool::getId, pool -> pool));
        Map<String, CluePool> ownersDefaultPoolMap = cluePoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        List<String> allPoolIds = ListUtils.union(poolMap.values().stream().map(CluePool::getId).toList(),
                ownersDefaultPoolMap.values().stream().map(CluePool::getId).toList()).stream().distinct().toList();
        Map<String, CluePoolRecycleRule> recycleRuleMap;
        if (CollectionUtils.isEmpty(allPoolIds)) {
            recycleRuleMap = Map.of();
        } else {
            LambdaQueryWrapper<CluePoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
            recycleRuleWrapper.in(CluePoolRecycleRule::getPoolId, allPoolIds);
            List<CluePoolRecycleRule> recycleRules = recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
            recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, rule -> rule));
        }

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);
        Map<String, FollowUpRecordDTO> recordMap = baseService.getOpportunityFollowRecord(clueIds, "CLUE", "CLUE");
        list.forEach(clueListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> clueFields = caseCustomFiledMap.get(clueListResponse.getId());
            clueListResponse.setModuleFields(clueFields);

            // 设置回收公海
            CluePool reservePool;
            if (poolMap.containsKey(clueListResponse.getPoolId())) {
                reservePool = poolMap.get(clueListResponse.getPoolId());
            } else {
                reservePool = ownersDefaultPoolMap.get(clueListResponse.getOwner());
            }
            clueListResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            // 计算剩余归属天数
            clueListResponse.setReservedDays(cluePoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    clueListResponse));

            UserDeptDTO userDeptDTO = userDeptMap.get(clueListResponse.getOwner());
            if (userDeptDTO != null) {
                clueListResponse.setDepartmentId(userDeptDTO.getDeptId());
                clueListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
            if (recordMap.containsKey(clueListResponse.getId())) {
                FollowUpRecordDTO followUpRecordDTO = recordMap.get(clueListResponse.getId());
                clueListResponse.setLastFollower(followUpRecordDTO.getFollower());
                clueListResponse.setLastFollowerName(followUpRecordDTO.getFollowerName());
                clueListResponse.setLastFollowTime(followUpRecordDTO.getFollowTime());
            }
        });

        return baseService.setCreateUpdateOwnerUserName(list);
    }

    public ClueGetResponse get(String id, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        ClueGetResponse clueGetResponse = BeanUtils.copyBean(new ClueGetResponse(), clue);

        // 获取模块字段
        List<BaseModuleFieldValue> clueFields = clueFieldService.getModuleFieldValuesByResourceId(id);
        clueGetResponse.setModuleFields(clueFields);

        UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(clueGetResponse.getOwner(), orgId);
        if (userDeptDTO != null) {
            clueGetResponse.setDepartmentId(userDeptDTO.getDeptId());
            clueGetResponse.setDepartmentName(userDeptDTO.getDeptName());
        }

        return baseService.setCreateUpdateOwnerUserName(clueGetResponse);
    }

    public Clue add(ClueAddRequest request, String userId, String orgId) {
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setCreateTime(System.currentTimeMillis());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setCollectionTime(clue.getCreateTime());
        clue.setUpdateUser(userId);
        clue.setCreateUser(userId);
        clue.setOrganizationId(orgId);
        clue.setId(IDGenerator.nextStr());
        clue.setStatus(ClueStatus.NEW.name());
        clue.setInSharedPool(false);

        // 校验名称重复
        checkAddExist(clue);

        clueMapper.insert(clue);

        //保存自定义字段
        clueFieldService.saveModuleField(clue.getId(), request.getModuleFields());
        return clue;
    }

    public Clue update(ClueUpdateRequest request, String userId) {
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);

        // 校验名称重复
        checkUpdateExist(clue);

        if (StringUtils.isNotBlank(request.getOwner())) {
            Clue originCustomer = clueMapper.selectByPrimaryKey(request.getId());
            if (!StringUtils.equals(request.getOwner(), originCustomer.getOwner())) {
                // 如果责任人有修改，则添加责任人历史
                clueOwnerHistoryService.add(originCustomer, userId);
            }
        }

        clueMapper.update(clue);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields());
        return clueMapper.selectByPrimaryKey(clue.getId());
    }

    public void updateStatus(ClueStatusUpdateRequest request, String userId) {
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        clueMapper.update(clue);
    }

    private void updateModuleField(String clueId, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        clueFieldService.deleteByResourceId(clueId);
        // 再保存
        clueFieldService.saveModuleField(clueId, moduleFields);
    }

    private void checkAddExist(Clue clue) {
        if (extClueMapper.checkAddExist(clue)) {
            throw new GenericException(ClueResultCode.CLUE_EXIST);
        }
    }

    private void checkUpdateExist(Clue clue) {
        if (extClueMapper.checkUpdateExist(clue)) {
            throw new GenericException(ClueResultCode.CLUE_EXIST);
        }
    }

    public void delete(String id) {
        // 删除客户
        clueMapper.deleteByPrimaryKey(id);
        // 删除客户模块字段
        clueFieldService.deleteByResourceId(id);
        // 删除责任人历史
        clueOwnerHistoryService.deleteByClueIds(List.of(id));
    }

    public void batchTransfer(ClueBatchTransferRequest request) {
        extClueMapper.batchTransfer(request);
    }

    public void batchDelete(List<String> ids) {
        // 删除客户
        clueMapper.deleteByIds(ids);
        // 删除客户模块字段
        clueFieldService.deleteByResourceIds(ids);
        // 删除责任人历史
        clueOwnerHistoryService.deleteByClueIds(ids);
    }
}