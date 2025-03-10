package io.cordys.crm.customer.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.dto.request.CustomerContactAddRequest;
import io.cordys.crm.customer.dto.request.CustomerContactDisableRequest;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.request.CustomerContactUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.cordys.crm.customer.constants.CustomerResultCode.CUSTOMER_CONTACT_EXIST;

/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerContactService {
    @Resource
    private BaseMapper<CustomerContact> customerContactMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private CustomerContactFieldService customerContactFieldService;

    public List<CustomerContactListResponse> list(CustomerContactPageRequest request, String orgId) {
        List<CustomerContactListResponse> list = extCustomerContactMapper.list(request, orgId);
        return buildListData(list, orgId);
    }

    private List<CustomerContactListResponse> buildListData(List<CustomerContactListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        List<String> customerIds = list.stream().map(CustomerContactListResponse::getId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = customerContactFieldService.getResourceFieldMap(customerIds);

        Map<String, String> customNameMap = extCustomerMapper.selectOptionByIds(customerIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

        List<String> ownerIds = list.stream()
                .map(CustomerContactListResponse::getOwner)
                .distinct()
                .toList();

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        list.forEach(customerListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            customerListResponse.setModuleFields(customerFields);

            UserDeptDTO userDeptDTO = userDeptMap.get(customerListResponse.getOwner());
            if (userDeptDTO != null) {
                customerListResponse.setDepartmentId(userDeptDTO.getDeptId());
                customerListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            customerListResponse.setCustomerName(customNameMap.get(customerListResponse.getCustomerId()));
        });

        return baseService.setCreateUpdateOwnerUserName(list);
    }

    public CustomerContactGetResponse get(String id, String orgId) {
        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(id);
        CustomerContactGetResponse customerContactGetResponse = BeanUtils.copyBean(new CustomerContactGetResponse(), customerContact);

        List<OptionDTO> customers = extCustomerMapper.selectOptionByIds(List.of(customerContact.getCustomerId()));
        if (CollectionUtils.isNotEmpty(customers)) {
            customerContactGetResponse.setCustomerName(customers.getFirst().getName());
        }

        UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(customerContact.getOwner(), orgId);
        if (userDeptDTO != null) {
            customerContactGetResponse.setDepartmentId(userDeptDTO.getDeptId());
            customerContactGetResponse.setDepartmentName(userDeptDTO.getDeptName());
        }

        return baseService.setCreateUpdateOwnerUserName(customerContactGetResponse);
    }

    public CustomerContact add(CustomerContactAddRequest request, String userId, String orgId) {
        CustomerContact customerContact = BeanUtils.copyBean(new CustomerContact(), request);
        customerContact.setCreateTime(System.currentTimeMillis());
        customerContact.setUpdateTime(System.currentTimeMillis());
        customerContact.setUpdateUser(userId);
        customerContact.setCreateUser(userId);
        customerContact.setOrganizationId(orgId);
        customerContact.setId(IDGenerator.nextStr());
        customerContact.setEnable(true);
        // 校验名称重复
        checkAddExist(customerContact);
        customerContactMapper.insert(customerContact);

        //保存自定义字段
        customerContactFieldService.saveModuleField(customerContact.getId(), request.getModuleFields());
        return customerContact;
    }

    public CustomerContact update(CustomerContactUpdateRequest request, String userId) {
        CustomerContact customerContact = BeanUtils.copyBean(new CustomerContact(), request);
        customerContact.setUpdateTime(System.currentTimeMillis());
        customerContact.setUpdateUser(userId);
        // 校验名称重复
        checkUpdateExist(customerContact);
        customerContactMapper.update(customerContact);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields());
        return customerContactMapper.selectByPrimaryKey(customerContact.getId());
    }

    private void updateModuleField(String customerId, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        customerContactFieldService.deleteByResourceId(customerId);
        // 再保存
        customerContactFieldService.saveModuleField(customerId, moduleFields);
    }

    private void checkAddExist(CustomerContact customerContact) {
        if (extCustomerContactMapper.checkAddExist(customerContact)) {
            throw new GenericException(CUSTOMER_CONTACT_EXIST);
        }
    }

    private void checkUpdateExist(CustomerContact customerContact) {
        if (extCustomerContactMapper.checkUpdateExist(customerContact)) {
            throw new GenericException(CUSTOMER_CONTACT_EXIST);
        }
    }

    public void delete(String id) {
        customerContactMapper.deleteByPrimaryKey(id);
        customerContactFieldService.deleteByResourceId(id);
    }

    public void enable(String id) {
        CustomerContact customerContact = new CustomerContact();
        customerContact.setEnable(true);
        customerContact.setId(id);
        customerContact.setDisableReason(StringUtils.EMPTY);
        customerContactMapper.updateById(customerContact);
    }

    public void disable(String id, CustomerContactDisableRequest request) {
        CustomerContact customerContact = new CustomerContact();
        customerContact.setEnable(false);
        customerContact.setId(id);
        customerContact.setDisableReason(request.getReason());
        customerContactMapper.updateById(customerContact);
    }
}