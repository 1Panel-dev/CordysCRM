package io.cordys.crm.customer.service;

import com.alibaba.excel.util.StringUtils;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.constants.CustomerRelationType;
import io.cordys.crm.customer.domain.CustomerRelation;
import io.cordys.crm.customer.dto.request.CustomerRelationSaveRequest;
import io.cordys.crm.customer.dto.request.CustomerRelationUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerRelationListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerRelationService {
    @Resource
    private BaseMapper<CustomerRelation> customerRelationMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;

    public List<CustomerRelationListResponse> list(String customerId) {
        CustomerRelation example = new CustomerRelation();
        example.setTargetCustomerId(customerId);
        List<CustomerRelation> sourceRelations = customerRelationMapper.select(example);

        List<String> customerIds = new ArrayList<>();

        // 查询上级集团的客户关系
        List<CustomerRelationListResponse> result = sourceRelations
                .stream().map(item -> {
                    CustomerRelationListResponse listResponse = new CustomerRelationListResponse();
                    listResponse.setRelationType(CustomerRelationType.GROUP.name());
                    listResponse.setId(item.getId());
                    listResponse.setCustomerId(item.getSourceCustomerId());
                    customerIds.add(listResponse.getCustomerId());
                    return listResponse;
                }).collect(Collectors.toList());

        example = new CustomerRelation();
        example.setSourceCustomerId(customerId);
        List<CustomerRelation> targetRelations = customerRelationMapper.select(example);

        // 查询下级子公司的客户关系
        List<CustomerRelationListResponse> subsidiaryCustomer = targetRelations.stream()
                .map(item -> {
                    CustomerRelationListResponse listResponse = new CustomerRelationListResponse();
                    listResponse.setRelationType(CustomerRelationType.SUBSIDIARY.name());
                    listResponse.setId(item.getId());
                    listResponse.setCustomerId(item.getTargetCustomerId());
                    customerIds.add(listResponse.getCustomerId());
                    return listResponse;
                }).collect(Collectors.toList());

        result.addAll(subsidiaryCustomer);

        if (CollectionUtils.isNotEmpty(customerIds)) {
            // 设置客户名称
            Map<String, String> customerMap = extCustomerMapper.selectOptionByIds(customerIds)
                    .stream()
                    .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            result.forEach(item -> item.setCustomerName(customerMap.get(item.getCustomerId())));
        }

        return result;
    }

    public void save(String customerId, List<CustomerRelationSaveRequest> requests) {
        // 先删除
        deleteByCustomerId(customerId);
        // 再创建
        List<CustomerRelation> relations = requests.stream()
                .filter(request -> StringUtils.isNotBlank(request.getCustomerId()) && StringUtils.isNotBlank(request.getRelationType()))
                .map(item -> {
                    CustomerRelation customerRelation = getCustomerRelation(item, customerId);
                    return customerRelation;
                }).toList();

        checkTargetCustomer(customerId, relations);

        if (CollectionUtils.isNotEmpty(relations)) {
            customerRelationMapper.batchInsert(relations);
        }
    }

    /**
     * 校验添加的子公司是否已经有所属的集团了
     * 一个公司只能有一个集团
     * @param customerId
     * @param relations
     */
    private void checkTargetCustomer(String customerId, List<CustomerRelation> relations) {
        List<String> targetCustomerIds = relations.stream()
                .filter(item -> !StringUtils.equals(item.getTargetCustomerId(), customerId))
                .map(CustomerRelation::getTargetCustomerId).toList();

        if (CollectionUtils.isEmpty(targetCustomerIds)) {
            return;
        }

        LambdaQueryWrapper<CustomerRelation> customerRelation = new LambdaQueryWrapper<>();
        customerRelation.in(CustomerRelation::getSourceCustomerId, targetCustomerIds);
        List<CustomerRelation> customerRelations = customerRelationMapper.selectListByLambda(customerRelation)
                .stream()
                .filter(item -> !StringUtils.equals(item.getSourceCustomerId(), customerId))
                .collect(Collectors.toList());
        if (!customerRelations.isEmpty()) {
            List<String> sourceIds = customerRelations.stream()
                    .map(CustomerRelation::getSourceCustomerId)
                    .collect(Collectors.toList());
            List<OptionDTO> customers = extCustomerMapper.getCustomerOptionsByIds(sourceIds);
            String userNames = customers.stream()
                    .map(OptionDTO::getName)
                    .distinct()
                    .collect(Collectors.joining(","));
            throw new GenericException(Translator.getWithArgs("customer.relation.target_customer.exist", userNames));
        }
    }

    public void deleteByCustomerId(String customerId) {
       deleteByCustomerIds(List.of(customerId));
    }

    public void deleteByCustomerIds(List<String> customerIds) {
        if (CollectionUtils.isEmpty(customerIds)) {
            return;
        }
        LambdaQueryWrapper<CustomerRelation> wrapper = new LambdaQueryWrapper();
        wrapper.in(CustomerRelation::getSourceCustomerId, customerIds);
        customerRelationMapper.deleteByLambda(wrapper);

        wrapper = new LambdaQueryWrapper();
        wrapper.in(CustomerRelation::getTargetCustomerId, customerIds);
        customerRelationMapper.deleteByLambda(wrapper);
    }

    public CustomerRelation add(CustomerRelationSaveRequest request, String customerId) {
        CustomerRelation customerRelation = getCustomerRelation(request, customerId);
        checkTargetCustomer(customerId, List.of(customerRelation));
        customerRelationMapper.insert(customerRelation);
        return customerRelation;
    }

    private CustomerRelation getCustomerRelation(CustomerRelationSaveRequest request, String customerId) {
        CustomerRelation customerRelation = new CustomerRelation();
        customerRelation.setId(IDGenerator.nextStr());
        customerRelation.setCreateTime(System.currentTimeMillis());
        if (StringUtils.equals(request.getRelationType(), CustomerRelationType.GROUP.name())) {
            customerRelation.setSourceCustomerId(request.getCustomerId());
            customerRelation.setTargetCustomerId(customerId);
        } else {
            customerRelation.setSourceCustomerId(customerId);
            customerRelation.setTargetCustomerId(request.getCustomerId());
        }
        return customerRelation;
    }

    public CustomerRelation update(CustomerRelationUpdateRequest request, String customerId) {
        CustomerRelation customerRelation = getCustomerRelation(request, customerId);
        customerRelation.setId(request.getId());
        customerRelation.setCreateTime(null);
        if (StringUtils.equals(request.getRelationType(), CustomerRelationType.GROUP.name())) {
            customerRelation.setSourceCustomerId(request.getCustomerId());
            customerRelation.setTargetCustomerId(customerId);
        } else {
            customerRelation.setSourceCustomerId(customerId);
            customerRelation.setTargetCustomerId(request.getCustomerId());
        }
        checkTargetCustomer(customerId, List.of(customerRelation));
        customerRelationMapper.update(customerRelation);
        return customerRelationMapper.selectByPrimaryKey(request.getId());
    }

    public void delete(String id) {
        customerRelationMapper.deleteByPrimaryKey(id);
    }
}