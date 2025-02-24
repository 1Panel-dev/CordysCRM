package io.cordys.crm.customer.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseModuleFieldValueService;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.crm.customer.constants.CustomerResultCode;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerField;
import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.CustomerUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerGetResponse;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
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
public class CustomerService {

    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private BaseMapper<CustomerField> customerFieldMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private BaseModuleFieldValueService baseModuleFieldValueService;

    public List<CustomerListResponse> list(CustomerPageRequest request, String orgId) {
        List<CustomerListResponse> list = extCustomerMapper.list(request, orgId);
        return buildListData(list);
    }

    private List<CustomerListResponse> buildListData(List<CustomerListResponse> list) {
        List<String> customerIds = list.stream().map(CustomerListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<CustomerField>> caseCustomFiledMap = baseModuleFieldValueService.getResourceFiledMap(customerIds,
                CustomerField::getCustomerId, customerFieldMapper);
        list.forEach(customerListResponse -> {
            // 获取自定义字段
            List<CustomerField> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            customerListResponse.setModuleFields(customerFields);

            if (customerListResponse.getCollectionTime() != null) {
                // 将毫秒数转换为天数, 并向上取整
                int days = (int) Math.ceil(customerListResponse.getCollectionTime() * 1.0 / 86400000);
                customerListResponse.setReservedDays(days);
            }
        });

        return baseService.setCreateAndUpdateUserName(list);
    }

    public CustomerGetResponse get(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        CustomerGetResponse customerGetResponse = BeanUtils.copyBean(new CustomerGetResponse(), customer);

        // 获取模块字段
        List<CustomerField> customerFields = baseModuleFieldValueService.getModuleFieldValuesByResourceIds(List.of(id),
                CustomerField::getCustomerId, customerFieldMapper);
        customerGetResponse.setModuleFields(customerFields);
        return baseService.setCreateAndUpdateUserName(customerGetResponse);
    }

    public Customer add(CustomerAddRequest request, String userId, String orgId) {
        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setCreateTime(System.currentTimeMillis());
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setCollectionTime(customer.getCreateTime());
        customer.setUpdateUser(userId);
        customer.setCreateUser(userId);
        customer.setOrganizationId(orgId);
        customer.setId(IDGenerator.nextStr());
        customer.setInSharedPool(false);

        // 校验名称重复 todo
//        checkAddExist(customer);

        customerMapper.insert(customer);

        //保存自定义字段
        saveModuleField(customer.getId(), request.getModuleFields());
        return customer;
    }

    /**
     * @param moduleFieldValues
     */
    public void saveModuleField(String customerId, List<BaseModuleFieldValue> moduleFieldValues) {
        if (CollectionUtils.isEmpty(moduleFieldValues)) {
            return;
        }

        //  todo 字段的校验
        List<CustomerField> customerFields = moduleFieldValues.stream().map(custom -> {
            CustomerField customField = new CustomerField();
            customField.setCustomerId(customerId);
            customField.setFieldId(custom.getFieldId());
            String valueStr = custom.getFieldValue() instanceof String ? custom.getFieldValue() : JSON.toJSONString(custom.getFieldValue());
            customField.setFieldValue(valueStr);
            customField.setId(IDGenerator.nextStr());
            return customField;
        }).toList();
        customerFieldMapper.batchInsert(customerFields);
    }

    public Customer update(CustomerUpdateRequest request, String userId) {
        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser(userId);

        // 校验名称重复 todo
//        checkUpdateExist(customer);

        customerMapper.update(customer);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields());
        return customerMapper.selectByPrimaryKey(customer.getId());
    }

    private void updateModuleField(String customerId, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        deleteCustomerFieldByCustomerId(customerId);
        // 再保存
        saveModuleField(customerId, moduleFields);
    }

    private void deleteCustomerFieldByCustomerId(String customerId) {
        CustomerField example = new CustomerField();
        example.setCustomerId(customerId);
        customerFieldMapper.delete(example);
    }

    private void checkAddExist(Customer customer) {
        if (extCustomerMapper.checkAddExist(customer)) {
            throw new GenericException(CustomerResultCode.CUSTOMER_EXIST);
        }
    }

    private void checkUpdateExist(Customer customer) {
        if (extCustomerMapper.checkUpdateExist(customer)) {
            throw new GenericException(CustomerResultCode.CUSTOMER_EXIST);
        }
    }

    public void delete(String id) {
        // 删除客户
        customerMapper.deleteByPrimaryKey(id);
        // 删除客户模块字段
        deleteCustomerFieldByCustomerId(id);
    }
}