package io.cordys.crm.customer.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.customer.constants.CustomerCollaborationType;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerCollaboration;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.dto.request.CustomerContactAddRequest;
import io.cordys.crm.customer.dto.request.CustomerContactDisableRequest;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.request.CustomerContactUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import io.cordys.crm.customer.dto.response.CustomerContactListAllResponse;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.cordys.crm.customer.constants.CustomerResultCode.CUSTOMER_CONTACT_EXIST;

/**
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
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private CustomerContactFieldService customerContactFieldService;
    @Resource
    private CustomerCollaborationService customerCollaborationService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;
    @Resource
    private CustomerFieldService customerFieldService;

    public PagerWithOption<List<CustomerContactListResponse>> list(CustomerContactPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomerContactListResponse> list = extCustomerContactMapper.list(request, userId, orgId, deptDataPermission);
        list = buildListData(list, orgId);

        Map<String, List<OptionDTO>> optionMap = getListOptionMap(orgId, list);

        return PageUtils.setPageInfoWithOption(page, list, optionMap);
    }

    private Map<String, List<OptionDTO>> getListOptionMap(String orgId, List<CustomerContactListResponse> list) {
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTACT.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, CustomerContactListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(list,
                CustomerContactListResponse::getOwner, CustomerContactListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_CONTACT_OWNER.getBusinessKey(), ownerFieldOption);

        // 补充客户选项
        List<OptionDTO> customerFieldOption = moduleFormService.getBusinessFieldOption(list,
                CustomerContactListResponse::getCustomerId, CustomerContactListResponse::getCustomerName);
        optionMap.put(BusinessModuleField.CUSTOMER_CONTACT_CUSTOMER.getBusinessKey(), customerFieldOption);
        return optionMap;
    }

    public List<CustomerContactListResponse> sourceList(CustomerContactPageRequest request, String currentUser, String orgId) {
        List<CustomerContactListResponse> list = extCustomerContactMapper.sourceList(request, orgId, currentUser);
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

        Customer customer = customerMapper.selectByPrimaryKey(customerContact.getCustomerId());
        if (customer != null) {
            customerContactGetResponse.setCustomerName(customer.getName());
        }

        UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(customerContact.getOwner(), orgId);
        if (userDeptDTO != null) {
            customerContactGetResponse.setDepartmentId(userDeptDTO.getDeptId());
            customerContactGetResponse.setDepartmentName(userDeptDTO.getDeptName());
        }

        customerContactGetResponse = baseService.setCreateUpdateOwnerUserName(customerContactGetResponse);

        // 获取模块字段
        List<BaseModuleFieldValue> customerContactFields = customerContactFieldService.getModuleFieldValuesByResourceId(id);
        ModuleFormConfigDTO customerContactFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTACT.getKey(), orgId);

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerContactFormConfig, customerContactFields);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(customerContactGetResponse,
                CustomerContactGetResponse::getOwner, CustomerContactGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_CONTACT_OWNER.getBusinessKey(), ownerFieldOption);

        // 补充客户选项
        List<OptionDTO> customerFieldOption = moduleFormService.getBusinessFieldOption(customerContactGetResponse,
                CustomerContactGetResponse::getCustomerId, CustomerContactGetResponse::getCustomerName);
        optionMap.put(BusinessModuleField.CUSTOMER_CONTACT_CUSTOMER.getBusinessKey(), customerFieldOption);

        customerContactGetResponse.setOptionMap(optionMap);
        customerContactGetResponse.setModuleFields(customerContactFields);

        return customerContactGetResponse;
    }

    @OperationLog(module = LogModule.CUSTOMER_CONTACT, type = LogType.ADD, resourceName = "{#request.name}")
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
        customerContactFieldService.saveModuleField(customerContact.getId(), orgId, userId, request.getModuleFields());

        baseService.handleAddLog(customerContact, request.getModuleFields());
        return customerContact;
    }

    @OperationLog(module = LogModule.CUSTOMER_CONTACT, type = LogType.UPDATE, resourceId = "{#request.id}")
    public CustomerContact update(CustomerContactUpdateRequest request, String userId, String orgId) {
        CustomerContact customerContact = BeanUtils.copyBean(new CustomerContact(), request);
        customerContact.setUpdateTime(System.currentTimeMillis());
        customerContact.setUpdateUser(userId);
        // 校验名称重复
        checkUpdateExist(customerContact);

        CustomerContact originCustomerContact = customerContactMapper.selectByPrimaryKey(customerContact.getId());
        // 获取模块字段
        List<BaseModuleFieldValue> originCustomerFields = customerFieldService.getModuleFieldValuesByResourceId(request.getId());

        customerContactMapper.update(customerContact);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields(), orgId, userId);

        customerContact = customerContactMapper.selectByPrimaryKey(customerContact.getId());
        baseService.handleUpdateLog(originCustomerContact, customerContact, originCustomerFields, request.getModuleFields(), originCustomerContact.getId(), originCustomerContact.getName());
        return customerContact;
    }

    private void updateModuleField(String customerId, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        customerContactFieldService.deleteByResourceId(customerId);
        // 再保存
        customerContactFieldService.saveModuleField(customerId, orgId, userId, moduleFields);
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

    @OperationLog(module = LogModule.CUSTOMER_CONTACT, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        CustomerContact originCustomerContact = customerContactMapper.selectByPrimaryKey(id);
        if (originCustomerContact == null) {
            return;
        }
        customerContactMapper.deleteByPrimaryKey(id);
        customerContactFieldService.deleteByResourceId(id);

        // 设置操作对象
        OperationLogContext.setResourceName(originCustomerContact.getName());
    }

    @OperationLog(module = LogModule.CUSTOMER_CONTACT, type = LogType.UPDATE, resourceId = "{#id}")
    public void enable(String id) {
        changeEnable(id, true, null);
    }

    private void changeEnable(String id, boolean enable, String reason) {
        CustomerContact originCustomerContact = customerContactMapper.selectByPrimaryKey(id);

        CustomerContact customerContact = new CustomerContact();
        customerContact.setEnable(enable);
        customerContact.setId(id);
        customerContact.setDisableReason(reason);
        customerContactMapper.updateById(customerContact);

        if (!originCustomerContact.getEnable().equals(customerContact.getEnable())) {
            CustomerContact originResourceLog = new CustomerContact();
            originResourceLog.setEnable(originResourceLog.getEnable());
            CustomerContact modifiedResourceLog = new CustomerContact();
            modifiedResourceLog.setEnable(customerContact.getEnable());
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId(id)
                            .resourceName(originCustomerContact.getName())
                            .originalValue(originResourceLog)
                            .modifiedValue(modifiedResourceLog)
                            .build()
            );
        }
    }

    @OperationLog(module = LogModule.CUSTOMER_CONTACT, type = LogType.UPDATE, resourceId = "{#id}")
    public void disable(String id, CustomerContactDisableRequest request) {
        changeEnable(id, false, request.getReason());
    }

    public CustomerContactListAllResponse listByCustomerId(String customerId, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Customer customer = customerMapper.selectByPrimaryKey(customerId);
        CustomerContactListAllResponse response = new CustomerContactListAllResponse();
        if (deptDataPermission.getAll() || StringUtils.equals(userId, InternalUser.ADMIN.getValue())) {
            // 全部数据权限，直接返回
            List<CustomerContactListResponse> list = extCustomerContactMapper.listByCustomerId(customerId);
            list = buildListData(list, orgId);
            Map<String, List<OptionDTO>> optionMap = getListOptionMap(orgId, list);
            response.setList(list);
            response.setOptionMap(optionMap);
            return response;
        }

        List<CustomerContactListResponse> list = List.of();

        // 查询协作人信息
        List<CustomerCollaboration> collaborations = customerCollaborationService.selectByCustomerId(customerId);

        // 获取协作类型的协作的联系人
        Set<String> collaborationUserIds = collaborations.stream()
                .filter(collaboration -> StringUtils.equals(collaboration.getCollaborationType(), CustomerCollaborationType.COLLABORATION.name()))
                .map(CustomerCollaboration::getUserId)
                .collect(Collectors.toSet());

        boolean isCustomerOwner = StringUtils.equals(customer.getOwner(), userId);
        boolean isCollaborationUser = collaborationUserIds.contains(userId);

        // 部门数据权限
        if (CollectionUtils.isNotEmpty(deptDataPermission.getDeptIds())) {
            UserDeptDTO customerOwnerDept = baseService.getUserDeptMapByUserId(customer.getOwner(), orgId);
            // 部门权限是否有该客户的权限
            boolean hasDeptCustomerPermission = customerOwnerDept != null && deptDataPermission.getDeptIds().contains(customerOwnerDept.getDeptId());

            list = extCustomerContactMapper.listByCustomerId(customerId);
            if (hasDeptCustomerPermission) {
                Map<String, UserDeptDTO> userDeptMapByUserIds = baseService.getUserDeptMapByUserIds(collaborationUserIds, orgId);
                list = list.stream()
                        .filter(item -> {
                            if (!collaborationUserIds.contains(item.getOwner())) {
                                // 不是协作人的联系人，则不过滤
                                return true;
                            }
                            UserDeptDTO userDeptDTO = userDeptMapByUserIds.get(item.getOwner());
                            // 部门数据权限，则过滤掉非本部门的协作人的联系人
                            return userDeptDTO != null && deptDataPermission.getDeptIds().contains(userDeptDTO.getDeptId());
                        })
                        .collect(Collectors.toList());
            } else if (isCollaborationUser) {
                // 没有权限，只是协作人，则只能看自己的
                list = list.stream()
                        .filter(item -> StringUtils.equals(item.getOwner(), userId))
                        .collect(Collectors.toList());
            }
        }

        if (deptDataPermission.getSelf()) {
            list = extCustomerContactMapper.listByCustomerId(customerId);
            if (isCustomerOwner) {
                // 本人数据权限，则过滤协作人的联系人
                list = list.stream()
                        .filter(item -> !collaborationUserIds.contains(item.getOwner()) && !StringUtils.equals(item.getOwner(), userId))
                        .collect(Collectors.toList());
            } else if (isCollaborationUser) {
                // 没有权限，只是协作人，则只能看自己的
                list = list.stream()
                        .filter(item -> StringUtils.equals(item.getOwner(), userId))
                        .collect(Collectors.toList());
            }
        }

        list = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = getListOptionMap(orgId, list);
        response.setList(list);
        response.setOptionMap(optionMap);
        return response;
    }

    public boolean checkOpportunity(String id) {
        Opportunity example = new Opportunity();
        example.setContactId(id);
        return opportunityMapper.countByExample(example) > 0;
    }
}