package io.cordys.crm.opportunity.service;

import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.domain.BaseModuleFieldValue;
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
import io.cordys.crm.opportunity.dto.request.OpportunityUpdateRequest;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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


    public List<OpportunityListResponse> list(OpportunityPageRequest request, String orgId) {
        List<OpportunityListResponse> list = extOpportunityMapper.list(request, orgId);
        return buildListData(list);
    }

    private List<OpportunityListResponse> buildListData(List<OpportunityListResponse> list) {
        List<String> opportunityIds = list.stream().map(OpportunityListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<OpportunityField>> opportunityFiledMap = getOpportunityFiledMap(opportunityIds);
        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            List<OpportunityField> opportunityFields = opportunityFiledMap.get(opportunityListResponse.getId());

            if (CollectionUtils.isNotEmpty(opportunityFields)) {
                // 将毫秒数转换为天数, 并向上取整
                int days = (int) Math.ceil(opportunityListResponse.getCreateTime() * 1.0 / 86400000);
                opportunityListResponse.setReservedDays(days);
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
        checkOpportunity(request, orgId);
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
        //TODO 商机规则 计算归属日期
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
     */
    private void checkOpportunity(OpportunityAddRequest request, String orgId) {
        List<String> products = extOpportunityMapper.selectByProducts(request, orgId);
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
}
