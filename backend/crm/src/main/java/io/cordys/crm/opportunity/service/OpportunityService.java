package io.cordys.crm.opportunity.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.FollowUpRecordDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.opportunity.constants.StageType;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.domain.OpportunityField;
import io.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityTransferRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityUpdateRequest;
import io.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.service.LogService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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


    public List<OpportunityListResponse> list(OpportunityPageRequest request, String userId, String orgId,
                                              DeptDataPermissionDTO deptDataPermission) {
        List<OpportunityListResponse> list = extOpportunityMapper.list(request, orgId, userId, deptDataPermission);
        return buildListData(list);
    }

    private List<OpportunityListResponse> buildListData(List<OpportunityListResponse> list) {
        if(CollectionUtils.isEmpty(list)){
            return list;
        }
        List<String> opportunityIds = list.stream().map(OpportunityListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<OpportunityField>> opportunityFiledMap = getOpportunityFiledMap(opportunityIds);
        Map<String, FollowUpRecordDTO> recordMap = baseService.getOpportunityFollowRecord(opportunityIds, "OPPORTUNITY", "CUSTOMER");

        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            List<OpportunityField> opportunityFields = opportunityFiledMap.get(opportunityListResponse.getId());

            if (CollectionUtils.isNotEmpty(opportunityFields)) {
                // 将毫秒数转换为天数, 并向上取整
                int days = (int) Math.ceil(opportunityListResponse.getCreateTime() * 1.0 / 86400000);
                opportunityListResponse.setReservedDays(days);
            }
            if (recordMap.containsKey(opportunityListResponse.getId())) {
                FollowUpRecordDTO followUpRecordDTO = recordMap.get(opportunityListResponse.getId());
                opportunityListResponse.setLastFollower(followUpRecordDTO.getFollower());
                opportunityListResponse.setLastFollowerName(followUpRecordDTO.getFollowerName());
                opportunityListResponse.setLastFollowTime(followUpRecordDTO.getFollowTime());
            }

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
     */
    public void batchDelete(List<String> ids) {
        opportunityMapper.deleteByIds(ids);
        opportunityFieldService.deleteByResourceIds(ids);
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
}
