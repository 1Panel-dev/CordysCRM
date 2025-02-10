package io.cordys.crm.customer.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.service.BaseService;
import io.cordys.crm.customer.constants.CustomerResultCode;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import io.cordys.crm.customer.dto.request.*;
import io.cordys.crm.customer.dto.response.*;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.customer.domain.Customer;

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
    private BaseService baseService;

    public List<CustomerListResponse> list(CustomerPageRequest request, String orgId) {
        List<CustomerListResponse> list = extCustomerMapper.list(request, orgId);
        // do something...

        return baseService.setCreateAndUpdateUserName(list);
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