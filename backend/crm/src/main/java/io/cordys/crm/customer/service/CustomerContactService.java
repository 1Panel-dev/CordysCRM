package io.cordys.crm.customer.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseModuleFieldValueService;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.domain.CustomerContactField;
import io.cordys.crm.customer.dto.request.CustomerContactAddRequest;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.request.CustomerContactUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
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
    private BaseMapper<CustomerContactField> customerContactFieldMapper;
    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private BaseModuleFieldValueService baseModuleFieldValueService;

    public List<CustomerContactListResponse> list(CustomerContactPageRequest request, String orgId) {
        List<CustomerContactListResponse> list = extCustomerContactMapper.list(request, orgId);
        // do something...

        return buildListData(list);
    }

    private List<CustomerContactListResponse> buildListData(List<CustomerContactListResponse> list) {
        List<String> customerIds = list.stream().map(CustomerContactListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<CustomerContactField>> caseCustomFiledMap =
                baseModuleFieldValueService.getResourceFiledMap(customerIds,
                        CustomerContactField::getCustomerContactId, customerContactFieldMapper);
        list.forEach(customerListResponse -> {
            // 获取自定义字段
            List<CustomerContactField> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            customerListResponse.setModuleFields(customerFields);
        });

        return baseService.setCreateAndUpdateUserName(list);
    }

    public CustomerContactGetResponse get(String id) {
        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(id);
        CustomerContactGetResponse customerContactGetResponse = BeanUtils.copyBean(new CustomerContactGetResponse(), customerContact);
        // do something...
        return baseService.setCreateAndUpdateUserName(customerContactGetResponse);
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
        return customerContact;
    }

    public CustomerContact update(CustomerContactUpdateRequest request, String userId) {
        CustomerContact customerContact = BeanUtils.copyBean(new CustomerContact(), request);
        customerContact.setUpdateTime(System.currentTimeMillis());
        customerContact.setUpdateUser(userId);
        // 校验名称重复
        checkUpdateExist(customerContact);
        customerContactMapper.update(customerContact);
        return customerContactMapper.selectByPrimaryKey(customerContact.getId());
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
    }
}