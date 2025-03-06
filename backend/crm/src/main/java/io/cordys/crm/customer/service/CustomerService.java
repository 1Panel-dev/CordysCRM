package io.cordys.crm.customer.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.customer.constants.CustomerResultCode;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.CustomerUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerGetResponse;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
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
    private BaseService baseService;
    @Resource
    private CustomerFieldService customerFieldService;
    @Resource
    private CustomerCollaborationService customerCollaborationService;

    public List<CustomerListResponse> list(CustomerPageRequest request, String userId, String orgId,
                                           DeptDataPermissionDTO deptDataPermission) {
        List<CustomerListResponse> list = extCustomerMapper.list(request, orgId, userId, deptDataPermission);
        return buildListData(list, orgId);
    }

    private List<CustomerListResponse> buildListData(List<CustomerListResponse> list, String orgId) {
        List<String> customerIds = list.stream().map(CustomerListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = customerFieldService.getResourceFieldMap(customerIds);

        List<String> ownerIds = list.stream()
                .map(CustomerListResponse::getOwner)
                .distinct()
                .collect(Collectors.toList());

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        list.forEach(customerListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            customerListResponse.setModuleFields(customerFields);

            if (customerListResponse.getCollectionTime() != null) {
                // 将毫秒数转换为天数, 并向上取整
                int days = (int) Math.ceil(customerListResponse.getCollectionTime() * 1.0 / 86400000);
                customerListResponse.setReservedDays(days);
            }
            UserDeptDTO userDeptDTO = userDeptMap.get(customerListResponse.getOwner());
            if (userDeptDTO != null) {
                customerListResponse.setDepartmentId(userDeptDTO.getDeptId());
                customerListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        });

        return baseService.setCreateAndUpdateUserName(list);
    }

    public CustomerGetResponse get(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        CustomerGetResponse customerGetResponse = BeanUtils.copyBean(new CustomerGetResponse(), customer);

        // 获取模块字段
        List<BaseModuleFieldValue> customerFields = customerFieldService.getModuleFieldValuesByResourceId(id);

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

        // 校验名称重复
        checkAddExist(customer);

        customerMapper.insert(customer);

        //保存自定义字段
        customerFieldService.saveModuleField(customer.getId(), request.getModuleFields());
        return customer;
    }

    public Customer update(CustomerUpdateRequest request, String userId) {
        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser(userId);

        // 校验名称重复
        checkUpdateExist(customer);

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
        customerFieldService.deleteByResourceId(customerId);
        // 再保存
        customerFieldService.saveModuleField(customerId, moduleFields);
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
        customerFieldService.deleteByResourceId(id);
        // 删除客户协作人
        customerCollaborationService.deleteByCustomerId(id);
    }

    public void batchTransfer(CustomerBatchTransferRequest request) {
        extCustomerMapper.batchTransfer(request);
    }

    public void batchDelete(List<String> ids) {
        // 删除客户
        customerMapper.deleteByIds(ids);
        // 删除客户模块字段
        customerFieldService.deleteByResourceIds(ids);
        // 删除客户协作人
        customerCollaborationService.deleteByCustomerIds(ids);
    }
}