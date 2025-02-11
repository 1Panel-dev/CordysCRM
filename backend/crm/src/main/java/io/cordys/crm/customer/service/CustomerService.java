package io.cordys.crm.customer.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.common.dto.ModuleFieldValueDTO;
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
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
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

    public List<CustomerListResponse> list(CustomerPageRequest request, String orgId) {
        List<CustomerListResponse> list = extCustomerMapper.list(request, orgId);
        return buildListData(list);
    }

    private List<CustomerListResponse> buildListData(List<CustomerListResponse> list) {
        List<String> customerIds = list.stream().map(CustomerListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<CustomerField>> caseCustomFiledMap = getCaseCustomFiledMap(customerIds);
        list.forEach(customerListResponse -> {
            // 获取自定义字段
            List<CustomerField> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            if (CollectionUtils.isNotEmpty(customerFields)) {
                List<ModuleFieldValueDTO> moduleFieldValues = customerFields.stream().map(customerField -> {
                    ModuleFieldValueDTO moduleFieldValue = new ModuleFieldValueDTO();
                    moduleFieldValue.setId(customerField.getFieldId());
                    moduleFieldValue.setValue(customerField.getFieldValue());
                    return moduleFieldValue;
                }).collect(Collectors.toList());
                customerListResponse.setModuleFields(moduleFieldValues);

                // 将毫秒数转换为天数, 并向上取整
                int days = (int) Math.ceil(customerListResponse.getCreateTime() * 1.0 / 86400000);
                customerListResponse.setReservedDays(days);
            }
        });

        return baseService.setCreateAndUpdateUserName(list);
    }

    public Map<String, List<CustomerField>> getCaseCustomFiledMap(List<String> customerIds) {
        if (CollectionUtils.isEmpty(customerIds)) {
            return Map.of();
        }
        LambdaQueryWrapper<CustomerField> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CustomerField::getCustomerId, customerIds);
        List<CustomerField> customerFields = customerFieldMapper.selectListByLambda(wrapper);
        return customerFields.stream().collect(Collectors.groupingBy(CustomerField::getCustomerId));
    }

    public CustomerGetResponse get(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        CustomerGetResponse customerGetResponse = BeanUtils.copyBean(new CustomerGetResponse(), customer);
        // do something...
        return baseService.setCreateAndUpdateUserName(customerGetResponse);
    }

    public Customer add(CustomerAddRequest request, String userId, String orgId) {
        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setCreateTime(System.currentTimeMillis());
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser(userId);
        customer.setCreateUser(userId);
        customer.setOrganizationId(orgId);
        customer.setId(IDGenerator.nextStr());
        customer.setInSharedPool(false);
        // 校验名称重复
        checkAddExist(customer);
        customerMapper.insert(customer);
        return customer;
    }

    public Customer update(CustomerUpdateRequest request, String userId) {
        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser(userId);
        // 校验名称重复
        checkUpdateExist(customer);
        customerMapper.update(customer);
        return customerMapper.selectByPrimaryKey(customer.getId());
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
        customerMapper.deleteByPrimaryKey(id);
    }
}