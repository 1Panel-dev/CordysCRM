package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.*;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.dto.*;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.constants.CustomerCollaborationType;
import cn.cordys.crm.customer.domain.*;
import cn.cordys.crm.customer.dto.request.*;
import cn.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerContactListAllResponse;
import cn.cordys.crm.customer.dto.response.CustomerContactListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.Product;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

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
    private PermissionCache permissionCache;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private BaseMapper<CustomerContactField> customerContactFieldMapper;
    @Resource
    private BaseMapper<CustomerContactFieldBlob> customerContactFieldBlobMapper;
    @Resource
    private LogService logService;

    public PagerWithOption<List<CustomerContactListResponse>> list(CustomerContactPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomerContactListResponse> list = extCustomerContactMapper.list(request, userId, orgId, deptDataPermission);
        list = buildListData(list, orgId);

        Map<String, List<OptionDTO>> optionMap = getListOptionMap(orgId, list);

        return PageUtils.setPageInfoWithOption(page, list, optionMap);
    }

    public Map<String, List<OptionDTO>> getListOptionMap(String orgId, List<CustomerContactListResponse> list) {
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

    public List<CustomerContactListResponse> buildListData(List<CustomerContactListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        List<String> customerContactIds = list.stream().map(CustomerContactListResponse::getId)
                .distinct()
                .collect(Collectors.toList());

        List<String> customerIds = list.stream().map(CustomerContactListResponse::getCustomerId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = customerContactFieldService.getResourceFieldMap(customerContactIds, true);

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
        if (StringUtils.isBlank(request.getOwner())) {
            customerContact.setOwner(userId);
        }

        //保存自定义字段
        customerContactFieldService.saveModuleField(customerContact, orgId, userId, request.getModuleFields(), false);

        customerContactMapper.insert(customerContact);

        baseService.handleAddLog(customerContact, request.getModuleFields());

        // 添加联系人通知
        Customer customer = customerMapper.selectByPrimaryKey(request.getCustomerId());
        if (customer != null) {
            Map<String, String> userNameMap = baseService.getUserNameMap(List.of(userId));
            Map<String, Object> paramMap = new HashMap<>(4);
            paramMap.put("useTemplate", "true");
            paramMap.put("template", Translator.get("message.customer.contact.add.text"));
            paramMap.put("operator", userNameMap.getOrDefault(userId, userId));
            paramMap.put("cName", customerContact.getName());
            paramMap.put("name", customer.getName());
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER, NotificationConstants.Event.CUSTOMER_CONCAT_ADD,
                    paramMap, userId, orgId, List.of(customer.getOwner()), true);
        }
        return customerContact;
    }

    @OperationLog(module = LogModule.CUSTOMER_CONTACT, type = LogType.UPDATE, resourceId = "{#request.id}")
    public CustomerContact update(CustomerContactUpdateRequest request, String userId, String orgId) {
        CustomerContact customerContact = BeanUtils.copyBean(new CustomerContact(), request);
        customerContact.setUpdateTime(System.currentTimeMillis());
        customerContact.setUpdateUser(userId);

        CustomerContact originCustomerContact = customerContactMapper.selectByPrimaryKey(customerContact.getId());
        // 获取模块字段
        List<BaseModuleFieldValue> originCustomerFields = customerContactFieldService.getModuleFieldValuesByResourceId(request.getId());

        // 更新模块字段
        updateModuleField(customerContact, request.getModuleFields(), orgId, userId);

        customerContactMapper.update(customerContact);

        customerContact = customerContactMapper.selectByPrimaryKey(customerContact.getId());
        baseService.handleUpdateLog(originCustomerContact, customerContact, originCustomerFields, request.getModuleFields(), originCustomerContact.getId(), originCustomerContact.getName());
        return customerContact;
    }

    private void updateModuleField(CustomerContact customerContact, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        customerContactFieldService.deleteByResourceId(customerContact.getId());
        // 再保存
        customerContactFieldService.saveModuleField(customerContact, orgId, userId, moduleFields, true);
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
        changeEnable(id, true, StringUtils.EMPTY);
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
            originResourceLog.setEnable(!enable);
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
        if (deptDataPermission.getAll() || Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
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
                .filter(collaboration -> Strings.CS.equals(collaboration.getCollaborationType(), CustomerCollaborationType.COLLABORATION.name()))
                .map(CustomerCollaboration::getUserId)
                .collect(Collectors.toSet());

        boolean isCustomerOwner = Strings.CS.equals(customer.getOwner(), userId);
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
                        .filter(item -> Strings.CS.equals(item.getOwner(), userId))
                        .collect(Collectors.toList());
            }
        }

        if (deptDataPermission.getSelf()) {
            list = extCustomerContactMapper.listByCustomerId(customerId);
            if (isCustomerOwner) {
                // 本人数据权限，则过滤协作人的联系人
                list = list.stream()
                        .filter(item -> !collaborationUserIds.contains(item.getOwner()) || Strings.CS.equals(item.getOwner(), userId))
                        .collect(Collectors.toList());
            } else if (isCollaborationUser) {
                // 没有权限，只是协作人，则只能看自己的
                list = list.stream()
                        .filter(item -> Strings.CS.equals(item.getOwner(), userId))
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

    public String getContactName(String id) {
        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(customerContact).map(CustomerContact::getName).orElse(null);
    }

    public CustomerContactListAllResponse getOpportunityContactList(String contactId, String orgId) {
        CustomerContactListAllResponse response = new CustomerContactListAllResponse();
        List<CustomerContactListResponse> list = extCustomerContactMapper.getById(contactId);
        list = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = getListOptionMap(orgId, list);
        response.setList(list);
        response.setOptionMap(optionMap);
        return response;
    }

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, rolePermissions);
    }

    /**
     * 检查联系人和电话是否唯一
     *
     * @param contact    联系人姓名
     * @param phone      联系人电话
     * @param customerId 客户ID
     * @param orgId      组织ID
     * @return 是否唯一
     */
    public boolean checkCustomerContactUnique(String contact, String phone, String customerId, String orgId) {
        LambdaQueryWrapper<ModuleForm> formQueryWrapper = new LambdaQueryWrapper<>();
        formQueryWrapper.eq(ModuleForm::getOrganizationId, orgId).eq(ModuleForm::getFormKey, FormKey.CONTACT.getKey());
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(formQueryWrapper);
        List<String> formIds = forms.stream().map(ModuleForm::getId).toList();

        LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ModuleField::getInternalKey, List.of(BusinessModuleField.CUSTOMER_CONTACT_NAME.getKey(),
                BusinessModuleField.CUSTOMER_CONTACT_PHONE.getKey())).in(ModuleField::getFormId, formIds);
        List<String> fieldIds = moduleFieldMapper.selectListByLambda(queryWrapper).stream().map(ModuleField::getId).toList();
        List<ModuleFieldBlob> blobs = moduleFieldBlobMapper.selectByIds(fieldIds);

        boolean nameUnique = false, phoneUnique = false;
        for (ModuleFieldBlob blob : blobs) {
            BaseField baseField = JSON.parseObject(blob.getProp(), BaseField.class);
            String internalKey = baseField.getInternalKey();
            boolean hasUnique = baseField.getRules().stream().anyMatch(rule -> RuleValidatorConstants.UNIQUE.equals(rule.getKey()));
            if (Strings.CI.equals(internalKey, BusinessModuleField.CUSTOMER_CONTACT_NAME.getKey())) {
                nameUnique = hasUnique;
            }
            if (Strings.CI.equals(internalKey, BusinessModuleField.CUSTOMER_CONTACT_PHONE.getKey())) {
                phoneUnique = hasUnique;
            }
        }
        if (!nameUnique && !phoneUnique) {
            return true;
        }
        ContactUniqueRequest request = ContactUniqueRequest.builder().name(contact).phone(phone).nameUnique(nameUnique).phoneUnique(phoneUnique).build();
        return extCustomerContactMapper.getUniqueContactCount(request, customerId, orgId) == 0;
    }


    /**
     * 更新责任人
     *
     * @param customerId
     * @param newOwner
     * @param oldOwner
     * @param orgId
     */
    public void updateContactOwner(String customerId, String newOwner, String oldOwner, String orgId) {
        extCustomerContactMapper.updateContactOwner(customerId, newOwner, oldOwner, orgId);
    }

    public List<CustomerContact> getContactListByNames(List<String> names) {
        LambdaQueryWrapper<CustomerContact> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(CustomerContact::getName, names);
        return customerContactMapper.selectListByLambda(lambdaQueryWrapper);
    }

    /**
     * 下载导入的模板
     * @param response 响应
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
        // 客户表单字段
        List<BaseField> fields = moduleFormService.getCustomImportHeads(FormKey.CONTACT.getKey(), currentOrg);

        new EasyExcelExporter(Objects.class)
                .exportMultiSheetTplWithSharedHandler(response, fields.stream().map(field -> Collections.singletonList(field.getName())).toList(),
                        Translator.get("contact.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT), new CustomTemplateWriteHandler(fields), new CustomHeadColWidthStyleStrategy());
    }

    /**
     * 导入检查
     * @param file 导入文件
     * @param currentOrg 当前组织
     * @return 导入检查信息
     */
    public ImportResponse importPreCheck(MultipartFile file, String currentOrg) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        return checkImportExcel(file, currentOrg);
    }

    /**
     * 联系人导入
     * @param file 导入文件
     * @param currentOrg 当前组织
     * @param currentUser 当前用户
     * @return 导入返回信息
     */
    public ImportResponse realImport(MultipartFile file, String currentOrg, String currentUser) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CONTACT.getKey(), currentOrg);
            CustomImportAfterDoConsumer<CustomerContact, BaseResourceField> afterDo = (contacts, contactFields, contactFieldBlobs) -> {
                List<LogDTO> logs = new ArrayList<>();
                contacts.forEach(contact -> {
                    contact.setEnable(true);
                    logs.add(new LogDTO(currentOrg, contact.getId(), currentUser, LogType.ADD, LogModule.CUSTOMER_CONTACT, contact.getName()));
                });
                customerContactMapper.batchInsert(contacts);
                customerContactFieldMapper.batchInsert(contactFields.stream().map(field -> BeanUtils.copyBean(new CustomerContactField(), field)).toList());
                customerContactFieldBlobMapper.batchInsert(contactFieldBlobs.stream().map(field -> BeanUtils.copyBean(new CustomerContactFieldBlob(), field)).toList());
                // record logs
                logService.batchAdd(logs);
            };
            CustomFieldImportEventListener<CustomerContact, CustomerContactField> eventListener = new CustomFieldImportEventListener<>(fields, CustomerContact.class, currentOrg, currentUser,
                    customerContactFieldMapper, afterDo, 2000);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getDataList().size()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("contact import error: ", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 检查导入的文件
     * @param file 文件
     * @param currentOrg 当前组织
     * @return 检查信息
     */
    private ImportResponse checkImportExcel(MultipartFile file, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getCustomImportHeads(FormKey.CONTACT.getKey(), currentOrg);
            CustomFieldCheckEventListener<CustomerContactField> eventListener = new CustomFieldCheckEventListener<>(fields, "customer_contact", currentOrg, customerContactFieldMapper);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("contact import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    public String getContactNameByIds(List<String> ids) {
        List<CustomerContact> contacts = customerContactMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(contacts)) {
            List<String> names = contacts.stream().map(CustomerContact::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }
}