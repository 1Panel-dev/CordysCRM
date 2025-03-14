package io.cordys.crm.opportunity.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.opportunity.constants.StageType;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.domain.OpportunityField;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.dto.request.*;
import io.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.service.LogService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityService {

    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private BaseMapper<OpportunityField> opportunityFieldMapper;
    @Resource
    private OpportunityFieldService opportunityFieldService;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;
    @Resource
    private BaseMapper<Product> productMapper;
	@Autowired
	private OpportunityRuleService opportunityRuleService;


    public List<OpportunityListResponse> list(OpportunityPageRequest request, String userId, String orgId,
                                              DeptDataPermissionDTO deptDataPermission) {
        List<OpportunityListResponse> list = extOpportunityMapper.list(request, orgId, userId, deptDataPermission);
        return buildListData(list, orgId);
    }

    private List<OpportunityListResponse> buildListData(List<OpportunityListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> opportunityIds = list.stream().map(OpportunityListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<OpportunityField>> opportunityFiledMap = getOpportunityFiledMap(opportunityIds);

        List<String> ownerIds = list.stream()
                .map(OpportunityListResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = list.stream()
                .map(OpportunityListResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = list.stream()
                .map(OpportunityListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(OpportunityListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        List<String> contactIds = list.stream()
                .map(OpportunityListResponse::getContactId)
                .distinct()
                .toList();
        Map<String, String> contactMap = baseService.getContactMap(contactIds);

        Map<String, OpportunityRule> ownersDefaultRuleMap = opportunityRuleService.getOwnersDefaultRuleMap(ownerIds, orgId);

        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            List<OpportunityField> opportunityFields = opportunityFiledMap.get(opportunityListResponse.getId());

            opportunityListResponse.setReservedDays(opportunityRuleService.calcReservedDay(ownersDefaultRuleMap.get(opportunityListResponse.getOwner()), opportunityListResponse));
            opportunityListResponse.setModuleFields(opportunityFields);

            opportunityListResponse.setFollowerName(userNameMap.get(opportunityListResponse.getFollower()));
            opportunityListResponse.setCreateUserName(userNameMap.get(opportunityListResponse.getCreateUser()));
            opportunityListResponse.setUpdateUserName(userNameMap.get(opportunityListResponse.getUpdateUser()));
            opportunityListResponse.setOwnerName(userNameMap.get(opportunityListResponse.getOwner()));
            opportunityListResponse.setContactName(contactMap.get(opportunityListResponse.getContactId()));

        });
        return baseService.setCreateAndUpdateUserName(list);
    }


    public Map<String, List<OpportunityField>> getOpportunityFiledMap(List<String> opportunityIds) {
        if (CollectionUtils.isEmpty(opportunityIds)) {
            return Map.of();
        }
        List<OpportunityField> opportunityFields = getOpportunityFieldsByOpportunityIds(opportunityIds);
        return opportunityFields.stream().collect(Collectors.groupingBy(OpportunityField::getResourceId));
    }


    private List<OpportunityField> getOpportunityFieldsByOpportunityIds(List<String> opportunityIds) {
        LambdaQueryWrapper<OpportunityField> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OpportunityField::getResourceId, opportunityIds);
        return opportunityFieldMapper.selectListByLambda(wrapper);
    }


    /**
     * 新建商机
     *
     * @param request
     * @param operatorId
     * @param orgId
     * @return
     */
    public Opportunity add(OpportunityAddRequest request, String operatorId, String orgId) {
        checkOpportunity(request, orgId, null);
        Opportunity opportunity = new Opportunity();
        String id = IDGenerator.nextStr();
        opportunity.setId(id);
        opportunity.setName(request.getName());
        opportunity.setCustomerId(request.getCustomerId());
        opportunity.setAmount(request.getAmount());
        opportunity.setPossible(request.getPossible());
        opportunity.setProducts(request.getProducts());
        opportunity.setOrganizationId(orgId);
        opportunity.setStage(StageType.CREATE.name());
        opportunity.setContactId(request.getContactId());
        opportunity.setOwner(request.getOwner());
        opportunity.setCreateTime(System.currentTimeMillis());
        opportunity.setCreateUser(operatorId);
        opportunity.setUpdateTime(System.currentTimeMillis());
        opportunity.setUpdateUser(operatorId);
        opportunity.setStatus(true);
        opportunityMapper.insert(opportunity);

        //自定义字段
        opportunityFieldService.saveModuleField(id, request.getModuleFields());
        //日志
        LogDTO logDTO = new LogDTO(orgId, id, operatorId, LogType.ADD, LogModule.OPPORTUNITY, request.getName());
        logDTO.setOriginalValue(null);
        logDTO.setModifiedValue(opportunity);
        logService.add(logDTO);

        return opportunity;
    }


    /**
     * 校验商机
     *
     * @param request
     * @param orgId
     * @param id
     */
    private void checkOpportunity(OpportunityAddRequest request, String orgId, String id) {
        List<String> products = extOpportunityMapper.selectByProducts(request, orgId, id);
        if (CollectionUtils.isNotEmpty(products)) {
            List<String> ids = JSON.parseArray(products.getFirst(), String.class);
            String projectId = request.getProducts().stream()
                    .filter(ids::contains)
                    .toList().getFirst();
            Product product = productMapper.selectByPrimaryKey(projectId);

            throw new GenericException(String.format(Translator.get("opportunity_exist"), product.getName()));
        }
    }


    /**
     * 更新商机
     *
     * @param request
     * @param userId
     * @param orgId
     */
    public Opportunity update(OpportunityUpdateRequest request, String userId, String orgId) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(opportunity).ifPresentOrElse(item -> {
            checkOpportunity(request, orgId, request.getId());
            LogDTO logDTO = new LogDTO(orgId, item.getId(), userId, LogType.UPDATE, LogModule.OPPORTUNITY, Translator.get("update_opportunity"));
            logDTO.setOriginalValue(opportunity);
            //更新跟进计划
            updateOpportunity(item, request, userId);
            //更新模块字段
            updateModuleField(request.getId(), request.getModuleFields());
            logDTO.setModifiedValue(item);
            logService.add(logDTO);
        }, () -> {
            throw new GenericException("opportunity_not_found");
        });
        return opportunity;
    }


    private void updateOpportunity(Opportunity item, OpportunityUpdateRequest request, String userId) {
        item.setName(request.getName());
        item.setCustomerId(request.getCustomerId());
        item.setAmount(request.getAmount());
        item.setPossible(request.getPossible());
        item.setProducts(request.getProducts());
        item.setContactId(request.getContactId());
        item.setOwner(request.getOwner());
        item.setUpdateTime(System.currentTimeMillis());
        item.setUpdateUser(userId);
        opportunityMapper.update(item);
    }


    private void updateModuleField(String id, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        opportunityFieldService.deleteByResourceId(id);
        // 再保存
        opportunityFieldService.saveModuleField(id, moduleFields);
    }


    /**
     * 删除商机
     *
     * @param id
     */
    @OperationLog(module = LogModule.OPPORTUNITY, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(id);
        Optional.ofNullable(opportunity).ifPresentOrElse(item -> {
            opportunityMapper.deleteByPrimaryKey(opportunity.getId());
            opportunityFieldService.deleteByResourceId(opportunity.getId());
        }, () -> {
            throw new GenericException("opportunity_not_found");
        });
        // 添加日志上下文
        OperationLogContext.setResourceName(opportunity.getName());
    }


    /**
     * 商机转移
     *
     * @param request
     */
    public void transfer(OpportunityTransferRequest request) {
        extOpportunityMapper.batchTransfer(request);
    }

    /**
     * 批量删除商机
     *
     * @param ids
     * @param userId
     */
    public void batchDelete(List<String> ids, String userId) {
        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Opportunity::getId, ids);
        List<Opportunity> opportunityList = opportunityMapper.selectListByLambda(wrapper);
        opportunityMapper.deleteByIds(ids);
        opportunityFieldService.deleteByResourceIds(ids);
        List<LogDTO> logs = new ArrayList<>();
        opportunityList.forEach(opportunity -> {
            LogDTO logDTO = new LogDTO(opportunity.getOrganizationId(), opportunity.getId(), userId, LogType.DELETE, LogModule.OPPORTUNITY, opportunity.getName());
            logDTO.setOriginalValue(opportunity);
            logs.add(logDTO);
        });
        logService.batchAdd(logs);
    }


    /**
     * 商机详情
     *
     * @param id
     * @return
     */
    public OpportunityDetailResponse get(String id) {
        OpportunityDetailResponse response = extOpportunityMapper.getDetail(id);
        List<BaseModuleFieldValue> fieldValueList = opportunityFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fieldValueList);
        List<String> userIds = Stream.of(List.of(response.getCreateUser(), response.getUpdateUser(), response.getOwner()))
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);
        Map<String, String> contactMap = baseService.getContactMap(List.of(response.getContactId()));

        response.setCreateUserName(userNameMap.get(response.getCreateUser()));
        response.setUpdateUserName(userNameMap.get(response.getUpdateUser()));
        response.setOwnerName(userNameMap.get(response.getOwner()));
        response.setContactName(contactMap.get(response.getContactId()));
        return response;
    }


    /**
     * 标记商机阶段
     *
     * @param request
     */
    @OperationLog(module = LogModule.OPPORTUNITY, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void updateStage(OpportunityStageRequest request) {
        Opportunity oldOpportunity = opportunityMapper.selectByPrimaryKey(request.getId());
        Opportunity newOpportunity = new Opportunity();
        if (StringUtils.equalsAnyIgnoreCase(request.getStage(), StageType.SUCCESS.name(), StageType.FAIL.name())) {
            newOpportunity.setStatus(false);
        }
        newOpportunity.setId(request.getId());
        newOpportunity.setStage(request.getStage());
        opportunityMapper.update(newOpportunity);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(oldOpportunity.getName())
                        .originalValue(oldOpportunity)
                        .modifiedValue(opportunityMapper.selectByPrimaryKey(request.getId()))
                        .build()
        );
    }
}
