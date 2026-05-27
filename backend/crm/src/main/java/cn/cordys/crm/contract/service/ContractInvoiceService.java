package cn.cordys.crm.contract.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.*;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.dto.ResourceApprovalFieldUpdateParam;
import cn.cordys.crm.approval.dto.ResourceApprovalPostUpdateParam;
import cn.cordys.crm.approval.dto.ResourceSnapshotApprovalParam;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.contract.constants.BusinessTitleConstants;
import cn.cordys.crm.contract.domain.*;
import cn.cordys.crm.contract.dto.request.ContractInvoiceAddRequest;
import cn.cordys.crm.contract.dto.request.ContractInvoicePageRequest;
import cn.cordys.crm.contract.dto.request.ContractInvoiceUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractInvoiceGetResponse;
import cn.cordys.crm.contract.dto.response.ContractInvoiceListResponse;
import cn.cordys.crm.contract.mapper.ExtContractInvoiceMapper;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.DictService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ContractInvoiceService {

    @Resource
    private ContractInvoiceFieldService invoiceFieldService;
    @Resource
    private BaseMapper<ContractInvoice> invoiceMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<ContractInvoiceSnapshot> snapshotBaseMapper;
    @Resource
    private ExtContractInvoiceMapper extContractInvoiceMapper;
    @Resource
    private BaseMapper<ContractInvoice> contractInvoiceMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private LogService logService;
    @Resource
    private BusinessTitleService businessTitleService;
    @Resource
    private DictService dictService;
    @Resource
    private ApprovalFlowService approvalFlowService;

    /**
     * 合同列表
     *
     * @param request
     * @param userId
     * @param orgId
     * @param deptDataPermission
     * @return
     */
    public PagerWithOption<List<ContractInvoiceListResponse>> list(ContractInvoicePageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ContractInvoiceListResponse> list = extContractInvoiceMapper.list(request, orgId, userId, deptDataPermission);
        List<ContractInvoiceListResponse> results = buildList(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(list, results, orgId);

        return PageUtils.setPageInfoWithOption(page, results, optionMap);
    }

    private Map<String, List<OptionDTO>> buildOptionMap(List<ContractInvoiceListResponse> list, List<ContractInvoiceListResponse> buildList, String orgId) {
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ContractInvoiceListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFieldValues);
        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                ContractInvoiceListResponse::getOwner, ContractInvoiceListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.INVOICE_OWNER.getBusinessKey(), ownerFieldOption);
        // 补充工商抬头选项
        List<OptionDTO> businessTitleFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                ContractInvoiceListResponse::getBusinessTitleId, ContractInvoiceListResponse::getBusinessTitleName);
        optionMap.put(BusinessModuleField.INVOICE_BUSINESS_TITLE_ID.getBusinessKey(), businessTitleFieldOption);
        return optionMap;
    }

    /**
     * 新建合同
     *
     * @param request
     * @param operatorId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.CONTRACT_INVOICE, type = LogType.ADD)
	@HitApproval(formKey = FormKey.INVOICE, executeType = ExecuteTimingEnum.CREATE, resourceId = "#{request.id}")
    public ContractInvoice add(ContractInvoiceAddRequest request, String operatorId, String orgId) {
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("invoice.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("invoice.form.config.required"));
        }

        Contract contract = contractMapper.selectByPrimaryKey(request.getContractId());
        BigDecimal contractInvoiceValidAmount = extContractInvoiceMapper.calculateContractInvoiceValidAmount(request.getContractId(), operatorId, orgId, null);
        if (request.getAmount() != null && contract != null && request.getAmount().compareTo(contract.getAmount().subtract(contractInvoiceValidAmount)) > 0) {
            // 校验发票金额
            throw new GenericException(Translator.get("invoice.amount.exceed"));
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            // 金额 > 0
            throw new GenericException(Translator.get("invoice.amount.illegal"));
        }

        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        ContractInvoice invoice = BeanUtils.copyBean(new ContractInvoice(), request);
        String id = IDGenerator.nextStr();
        invoice.setId(id);
        invoice.setOrganizationId(orgId);
        invoice.setCreateTime(System.currentTimeMillis());
        invoice.setCreateUser(operatorId);
        invoice.setUpdateTime(System.currentTimeMillis());
        invoice.setUpdateUser(operatorId);
        invoice.setApprovalStatus(ApprovalStatus.NONE.name());

        if (StringUtils.isBlank(request.getOwner())) {
            invoice.setOwner(operatorId);
        }

        //自定义字段
        invoiceFieldService.saveModuleField(invoice, orgId, operatorId, moduleFields, false);
        invoiceMapper.insert(invoice);

        baseService.handleAddLog(invoice, request.getModuleFields());
        OperationLogContext.getContext().setResourceName(invoice.getName());
        OperationLogContext.getContext().setResourceId(invoice.getId());

        // 保存表单配置快照
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, invoiceFieldService, invoice.getId());
        ContractInvoiceGetResponse response = get(invoice, resolveFieldValues, moduleFormConfigDTO);
        saveSnapshot(invoice, saveModuleFormConfigDTO, response);

        return invoice;
    }

    /**
     * 保存合同快照
     *
     * @param invoice
     * @param moduleFormConfigDTO
     * @param response
     */
    private void saveSnapshot(ContractInvoice invoice, ModuleFormConfigDTO moduleFormConfigDTO, ContractInvoiceGetResponse response) {
        ContractInvoiceSnapshot snapshot = new ContractInvoiceSnapshot();
        snapshot.setId(IDGenerator.nextStr());
        snapshot.setInvoiceId(invoice.getId());
        snapshot.setInvoiceProp(JSON.toJSONString(moduleFormConfigDTO));
        snapshot.setInvoiceValue(JSON.toJSONString(response));
        snapshotBaseMapper.insert(snapshot);
    }

    /**
     * 编辑合同
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.CONTRACT_INVOICE, type = LogType.UPDATE, resourceId = "{#request.id}")
	@HitApproval(formKey = FormKey.INVOICE, executeType = ExecuteTimingEnum.EDIT, resourceId = "{#request.id}", updateType = "{#request.updateType}")
    public ContractInvoice update(ContractInvoiceUpdateRequest request, String userId, String orgId) {
        ContractInvoice originContractInvoice = invoiceMapper.selectByPrimaryKey(request.getId());
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("invoice.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("invoice.form.config.required"));
        }

        String contractId = request.getContractId() == null ? originContractInvoice.getContractId() : request.getContractId();
        Contract contract = contractMapper.selectByPrimaryKey(contractId);
        BigDecimal contractInvoiceValidAmount = extContractInvoiceMapper.calculateContractInvoiceValidAmount(request.getContractId(), userId, orgId, request.getId());
        if (request.getAmount() != null && contract != null && request.getAmount().compareTo(contract.getAmount().subtract(contractInvoiceValidAmount)) > 0) {
            // 校验发票金额
            throw new GenericException(Translator.get("invoice.amount.exceed"));
        }

        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            // 金额 > 0
            throw new GenericException(Translator.get("invoice.amount.illegal"));
        }

        dataScopeService.checkDataPermission(userId, orgId, originContractInvoice.getOwner(), PermissionConstants.CONTRACT_INVOICE_UPDATE);
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        Optional.of(originContractInvoice).ifPresentOrElse(item -> {
            List<BaseModuleFieldValue> originFields = invoiceFieldService.getModuleFieldValuesByResourceId(request.getId());
            ContractInvoice invoice = BeanUtils.copyBean(new ContractInvoice(), request);
            invoice.setUpdateTime(System.currentTimeMillis());
            invoice.setUpdateUser(userId);
            // 保留不可更改的字段
            invoice.setCreateUser(originContractInvoice.getCreateUser());
            invoice.setCreateTime(originContractInvoice.getCreateTime());
			invoice.setApprovalStatus(originContractInvoice.getApprovalStatus());

            updateFields(moduleFields, invoice, orgId, userId);
            invoiceMapper.update(invoice);
            //删除快照
            LambdaQueryWrapper<ContractInvoiceSnapshot> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(ContractInvoiceSnapshot::getInvoiceId, request.getId());
            List<ContractInvoiceSnapshot> invoiceSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
            if (CollectionUtils.isNotEmpty(invoiceSnapshots)) {
                ContractInvoiceSnapshot first = invoiceSnapshots.getFirst();
                if (first != null) {
                    ContractInvoiceGetResponse response = JSON.parseObject(first.getInvoiceValue(), ContractInvoiceGetResponse.class);
                    List<BaseModuleFieldValue> originModuleFields = response.getModuleFields();
                    if (CollectionUtils.isNotEmpty(originModuleFields)) {
                        originFields.addAll(originModuleFields);
                    }
                }
            }
            snapshotBaseMapper.deleteByLambda(delWrapper);
            //保存快照
            List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, invoiceFieldService, invoice.getId());
            // get 方法需要使用orgId
            invoice.setOrganizationId(orgId);
            ContractInvoiceGetResponse response = get(invoice, resolveFieldValues, moduleFormConfigDTO);
            saveSnapshot(invoice, saveModuleFormConfigDTO, response);

            // 处理日志上下文
            baseService.handleUpdateLog(originContractInvoice, invoice, originFields, moduleFields, request.getId(), invoice.getName());
        }, () -> {
            throw new GenericException(Translator.get("invoice.not.exist"));
        });
        return invoiceMapper.selectByPrimaryKey(request.getId());
    }


    /**
     * 更新自定义字段
     *
     * @param moduleFields
     * @param invoice
     * @param orgId
     * @param userId
     */
    private void updateFields(List<BaseModuleFieldValue> moduleFields, ContractInvoice invoice, String orgId, String userId) {
        if (moduleFields == null) {
            return;
        }
        invoiceFieldService.deleteByResourceId(invoice.getId());
        invoiceFieldService.saveModuleField(invoice, orgId, userId, moduleFields, true);
    }


    /**
     * 删除合同
     *
     * @param id
     */
    @OperationLog(module = LogModule.CONTRACT_INVOICE, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        ContractInvoice invoice = invoiceMapper.selectByPrimaryKey(id);
        if (invoice == null) {
            throw new GenericException(Translator.get("invoice.not.exist"));
        }

        dataScopeService.checkDataPermission(userId, orgId, invoice.getOwner(), PermissionConstants.CONTRACT_INVOICE_DELETE);


        invoiceFieldService.deleteByResourceId(id);
        invoiceMapper.deleteByPrimaryKey(id);

        //删除快照
        LambdaQueryWrapper<ContractInvoiceSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractInvoiceSnapshot::getInvoiceId, id);
        snapshotBaseMapper.deleteByLambda(wrapper);

        // 添加日志上下文
        OperationLogContext.setResourceName(invoice.getName());
    }

    public ContractInvoiceGetResponse getWithDataPermissionCheck(String id, String userId, String orgId) {
        ContractInvoiceGetResponse getResponse = get(id);
        if (getResponse == null) {
            throw new GenericException(Translator.get("resource.not.exist"));
        }
        dataScopeService.checkDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.CONTRACT_INVOICE_READ);
		if (Strings.CI.equals(getResponse.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
			Map<String, Boolean> firstNodeApproved = baseService.getApprovingResourceFirstNodeApproved(List.of(getResponse.getId()), orgId);
			getResponse.setFirstApproved(firstNodeApproved.get(getResponse.getId()));
		}
        return getResponse;
    }

    public ContractInvoiceGetResponse getSnapshotWithDataPermissionCheck(String id, String userId, String orgId) {
        ContractInvoiceGetResponse getResponse = getSnapshot(id);
        if (getResponse == null) {
            throw new GenericException(Translator.get("resource.not.exist"));
        }
        dataScopeService.checkDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.CONTRACT_INVOICE_READ);
		if (Strings.CI.equals(getResponse.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
			Map<String, Boolean> firstNodeApproved = baseService.getApprovingResourceFirstNodeApproved(List.of(getResponse.getId()), orgId);
			getResponse.setFirstApproved(firstNodeApproved.get(getResponse.getId()));
		}
        return getResponse;
    }

    private ContractInvoiceGetResponse get(ContractInvoice contractInvoice, List<BaseModuleFieldValue> contractInvoiceFields, ModuleFormConfigDTO contractInvoiceFormConfig) {
        ContractInvoiceGetResponse contractInvoiceGetResponse = BeanUtils.copyBean(new ContractInvoiceGetResponse(), contractInvoice);
        contractInvoiceGetResponse = baseService.setCreateUpdateOwnerUserName(contractInvoiceGetResponse);

        // 获取模块字段
        moduleFormService.processBusinessFieldValues(contractInvoiceGetResponse, contractInvoiceFields, contractInvoiceFormConfig);
        contractInvoiceFields = invoiceFieldService.setBusinessRefFieldValue(List.of(contractInvoiceGetResponse),
                moduleFormService.getFlattenFormFields(FormKey.INVOICE.getKey(), contractInvoice.getOrganizationId()),
                new HashMap<>(Map.of(contractInvoice.getId(), contractInvoiceFields))).get(contractInvoice.getId());

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(contractInvoiceFormConfig, contractInvoiceFields);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(contractInvoiceGetResponse,
                ContractInvoiceGetResponse::getOwner, ContractInvoiceGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.INVOICE_OWNER.getBusinessKey(), ownerFieldOption);

        Contract contract = contractMapper.selectByPrimaryKey(contractInvoice.getContractId());
        if (contract != null) {
            contractInvoiceGetResponse.setContractName(contract.getName());
            optionMap.put(BusinessModuleField.INVOICE_CONTRACT_ID.getBusinessKey(), Collections.singletonList(new OptionDTO(contract.getId(), contract.getName())));
        }

        BusinessTitle businessTitle = businessTitleService.selectById(contractInvoiceGetResponse.getBusinessTitleId());
        if (businessTitle != null) {
            contractInvoiceGetResponse.setBusinessTitleName(businessTitle.getName());
            optionMap.put(BusinessModuleField.INVOICE_BUSINESS_TITLE_ID.getBusinessKey(), Collections.singletonList(new OptionDTO(businessTitle.getId(), businessTitle.getName())));
        }

        contractInvoiceGetResponse.setOptionMap(optionMap);
        contractInvoiceGetResponse.setModuleFields(contractInvoiceFields);

        if (contractInvoiceGetResponse.getOwner() != null) {
            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(contractInvoiceGetResponse.getOwner(), contractInvoice.getOrganizationId());
            if (userDeptDTO != null) {
                contractInvoiceGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                contractInvoiceGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        // 附件信息
        contractInvoiceGetResponse.setAttachmentMap(moduleFormService.getAttachmentMap(contractInvoiceFormConfig, contractInvoiceFields));

        return contractInvoiceGetResponse;
    }

    /**
     * 获取合同详情
     *
     * @param id
     * @return
     */
    public ContractInvoiceGetResponse get(String id) {
        ContractInvoice contractInvoice = contractInvoiceMapper.selectByPrimaryKey(id);
        // 获取模块字段
        ModuleFormConfigDTO contractInvoiceFormConfig = getFormConfig(contractInvoice.getOrganizationId());
        List<BaseModuleFieldValue> contractInvoiceFields = invoiceFieldService.getModuleFieldValuesByResourceId(id);
        return get(contractInvoice, contractInvoiceFields, contractInvoiceFormConfig);
    }

    /**
     * 从快照中获取合同详情
     *
     * @param id 合同ID
     * @return 合同详情
     */
    public ContractInvoiceGetResponse getSnapshot(String id) {
        LambdaQueryWrapper<ContractInvoiceSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractInvoiceSnapshot::getInvoiceId, id);
        ContractInvoiceSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            return JSON.parseObject(snapshot.getInvoiceValue(), ContractInvoiceGetResponse.class);
        }
        return null;
    }

    private ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.INVOICE.getKey(), orgId);
    }

    public List<ContractInvoiceListResponse> buildList(List<ContractInvoiceListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        List<String> invoiceIds = list.stream().map(ContractInvoiceListResponse::getId)
                .collect(Collectors.toList());

        List<String> businessTitleIds = list.stream().map(ContractInvoiceListResponse::getBusinessTitleId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> invoiceFiledMap = invoiceFieldService.getResourceFieldMap(invoiceIds, true);
        Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = invoiceFieldService.setBusinessRefFieldValue(list, moduleFormService.getFlattenFormFields(FormKey.INVOICE.getKey(), orgId), invoiceFiledMap);
        Map<String, String> businessTitleNameMap = businessTitleService.selectByIds(businessTitleIds).stream()
                .collect(Collectors.toMap(BusinessTitle::getId, BusinessTitle::getName));

        List<String> ownerIds = list.stream()
                .map(ContractInvoiceListResponse::getOwner)
                .distinct()
                .toList();

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

		List<String> approvingResourceIds = list.stream().filter(item -> Strings.CI.contains(item.getApprovalStatus(), ApprovalStatus.APPROVING.name())).map(ContractInvoiceListResponse::getId).toList();
		Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(approvingResourceIds, orgId);

        list.forEach(item -> {
            UserDeptDTO userDeptDTO = userDeptMap.get(item.getOwner());
            if (userDeptDTO != null) {
                item.setDepartmentId(userDeptDTO.getDeptId());
                item.setDepartmentName(userDeptDTO.getDeptName());
            }
            String businessTitleName = businessTitleNameMap.get(item.getBusinessTitleId());
            if (StringUtils.isNotBlank(businessTitleName)) {
                item.setBusinessTitleName(businessTitleName);
            }
            // 获取自定义字段
            List<BaseModuleFieldValue> invoiceFields = resolvefieldValueMap.get(item.getId());
            item.setModuleFields(invoiceFields);
			item.setFirstApproved(firstNodeApprovedMap.get(item.getId()));
        });
        return baseService.setCreateUpdateOwnerUserName(list);
    }

    /**
     * 获取表单快照
     *
     * @param id
     * @param orgId
     * @return
     */
    public ModuleFormConfigDTO getFormSnapshot(String id, String orgId) {
        ContractInvoice invoice = invoiceMapper.selectByPrimaryKey(id);
        if (invoice == null) {
            throw new GenericException(Translator.get("resource.not.exist"));
        }
        LambdaQueryWrapper<ContractInvoiceSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractInvoiceSnapshot::getInvoiceId, id);
        ContractInvoiceSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            return JSON.parseObject(snapshot.getInvoiceProp(), ModuleFormConfigDTO.class);
        } else {
            return moduleFormCacheService.getBusinessFormConfig(FormKey.INVOICE.getKey(), orgId);
        }
    }

	/**
	 * 获取发票详情（⚠️反射调用; 勿修改入参, 返回, 方法名!）
	 * @param id 发票ID
	 * @return 发票详情
	 */
	public ContractInvoiceGetResponse getSimple(String id) {
		ContractInvoice contractInvoice = contractInvoiceMapper.selectByPrimaryKey(id);
		if (contractInvoice == null) {
			return null;
		}
		ContractInvoiceGetResponse response = BeanUtils.copyBean(new ContractInvoiceGetResponse(), contractInvoice);
		List<BaseModuleFieldValue> fvs = invoiceFieldService.getModuleFieldValuesByResourceId(id);
		ModuleFormConfigDTO formConfig = getFormConfig(contractInvoice.getOrganizationId());
		moduleFormService.processBusinessFieldValues(response, fvs, formConfig);
		return response;
	}

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CONTRACT_INVOICE_READ, rolePermissions);
    }

    public void batchDelete(List<String> ids, String userId, String orgId) {
        List<ContractInvoice> invoices = contractInvoiceMapper.selectByIds(ids);

        // 校验状态权限，过滤出有权限操作的发票
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.INVOICE.getValue(),
                invoices,
                PermissionConstants.CONTRACT_INVOICE_DELETE,
				orgId,
                ContractInvoice::getId,
                ContractInvoice::getApprovalStatus
        );

        if (CollectionUtils.isEmpty(permittedIds)) {
            return;
        }

        List<ContractInvoice> permittedInvoices = invoices.stream()
                .filter(i -> permittedIds.contains(i.getId()))
                .collect(Collectors.toList());

        List<String> owners = getOwners(permittedInvoices);
        dataScopeService.checkDataPermission(userId, orgId, owners, PermissionConstants.CONTRACT_INVOICE_DELETE);

        // 删除发票
        contractInvoiceMapper.deleteByIds(permittedIds);

        List<LogDTO> logs = permittedInvoices.stream()
                .map(invoice ->
                        new LogDTO(orgId, invoice.getId(), userId, LogType.DELETE, LogModule.CONTRACT_INVOICE, invoice.getName())
                )
                .toList();
        logService.batchAdd(logs);

        // 消息通知 todo
//        invoices.forEach(invoice ->
//                commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
//                        NotificationConstants.Event.CUSTOMER_DELETED, invoice.getName(), userId,
//                        orgId, List.of(invoice.getOwner()), true)
//        );
    }

    private List<String> getOwners(List<ContractInvoice> invoices) {
        return invoices.stream().map(ContractInvoice::getOwner)
                .distinct()
                .toList();
    }

    public BigDecimal calculateCustomerInvoiceAmount(String customerId, String userId, String orgId) {
        if (dictService.isDictConfigEnable(DictModule.INVOICE_APPROVAL.name(), orgId)) {
            return extContractInvoiceMapper.calculateCustomerInvoiceAmount(customerId, userId, orgId, true);
        } else {
            return extContractInvoiceMapper.calculateCustomerInvoiceAmount(customerId, userId, orgId, false);
        }
    }

    public BigDecimal calculateContractInvoiceAmount(String contractId, String userId, String orgId) {
        if (dictService.isDictConfigEnable(DictModule.INVOICE_APPROVAL.name(), orgId)) {
            return extContractInvoiceMapper.calculateContractInvoiceAmount(contractId, userId, orgId, true);
        } else {
            return extContractInvoiceMapper.calculateContractInvoiceAmount(contractId, userId, orgId, false);
        }
    }

    public ModuleFormConfigDTO getBusinessFormConfig(String organizationId) {
        ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.INVOICE.getKey(), organizationId);
        Set<String> businessTitleKeySet = Arrays.stream(BusinessTitleConstants.values())
                .map(BusinessTitleConstants::getKey)
                .collect(Collectors.toSet());

        for (BaseField field : businessFormConfig.getFields()) {
            if (businessTitleKeySet.contains(field.getId()) || Strings.CS.equals(field.getId(), BusinessModuleField.CONTRACT_TOTAL_AMOUNT.getKey())) {
                // 特殊表单，设置可见
                field.setReadable(true);
            }
        }
        return businessFormConfig;
    }

	/**
	 * ⚠️反射调用: 由审批执行操作统一调用, 勿修改
	 * @param param 参数
	 */
	public void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param) {
		ContractInvoiceSnapshot snapshotCriteria = new ContractInvoiceSnapshot();
		snapshotCriteria.setInvoiceId(param.getResourceId());
		ContractInvoiceSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
		if (snapshot != null) {
			ContractInvoiceGetResponse response = JSON.parseObject(snapshot.getInvoiceValue(), ContractInvoiceGetResponse.class);
			response.setApprovalStatus(param.getApprovalStatus());
			snapshot.setInvoiceValue(JSON.toJSONString(response));
			snapshotBaseMapper.update(snapshot);
		}
	}

	/**
	 * ⚠️反射调用: 由审批执行后置操作统一调用, 勿修改
	 * @param postFieldParam 参数
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void updateApprovalPostField(ResourceApprovalPostUpdateParam postFieldParam) {
		ModuleFormConfigDTO formConfig = getFormConfig(OrganizationContext.getOrganizationId());
		List<BaseField> fields = formConfig.getFields();
		Map<String, BaseField> fieldConfigMap = fields.stream().collect(Collectors.toMap(BaseField::getId, f -> f));
		ContractInvoice contractInvoice = contractInvoiceMapper.selectByPrimaryKey(postFieldParam.getResourceId());
		List<ContractInvoiceField> contractInvoiceFields = new ArrayList<>();
		List<ContractInvoiceFieldBlob> contractInvoiceFieldBlobs = new ArrayList<>();
		ContractInvoiceSnapshot snapshotCriteria = new ContractInvoiceSnapshot();
		snapshotCriteria.setInvoiceId(postFieldParam.getResourceId());
		ContractInvoiceSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
		ContractInvoiceGetResponse response = new ContractInvoiceGetResponse();
		if (snapshot != null) {
			response = JSON.parseObject(snapshot.getInvoiceValue(), ContractInvoiceGetResponse.class);
		}
		for (ResourceApprovalFieldUpdateParam fieldUpdateParam : postFieldParam.getFields()) {
			if (!fieldConfigMap.containsKey(fieldUpdateParam.getFieldId()) || fieldUpdateParam.getFieldValue() == null) {
				return;
			}
			BaseField fieldConfig = fieldConfigMap.get(fieldUpdateParam.getFieldId());
			AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
			if (fieldConfig.hasBusinessKey()) {
				// 业务主表字段
				invoiceFieldService.setResourceFieldValue(contractInvoice, fieldConfig.getBusinessKey(), fieldUpdateParam.getFieldValue());
			} else {
				// 快照自定义字段
				Optional<BaseModuleFieldValue> findField = response.getModuleFields().stream().filter(fieldValue -> Strings.CI.equals(fieldValue.getFieldId(), fieldUpdateParam.getFieldId())).findAny();
				if (findField.isPresent()) {
					findField.get().setFieldValue(fieldUpdateParam.getFieldValue());
				} else {
					BaseModuleFieldValue fv = new BaseModuleFieldValue();
					fv.setFieldId(fieldUpdateParam.getFieldId());
					fv.setFieldValue(fieldUpdateParam.getFieldValue());
					response.getModuleFields().add(fv);
				}
				if (fieldConfig.isBlob()) {
					// 自定义大表
					invoiceFieldService.getResourceFieldBlobMapper().deleteByLambda(new LambdaQueryWrapper<ContractInvoiceFieldBlob>()
							.eq(ContractInvoiceFieldBlob::getFieldId, fieldUpdateParam.getFieldId()).eq(ContractInvoiceFieldBlob::getResourceId, postFieldParam.getResourceId()));
					ContractInvoiceFieldBlob field = new ContractInvoiceFieldBlob();
					field.setId(IDGenerator.nextStr());
					field.setResourceId(postFieldParam.getResourceId());
					field.setFieldId(fieldUpdateParam.getFieldId());
					field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
					contractInvoiceFieldBlobs.add(field);
				} else {
					// 自定义表
					invoiceFieldService.getResourceFieldMapper().deleteByLambda(new LambdaQueryWrapper<ContractInvoiceField>()
							.eq(ContractInvoiceField::getFieldId, fieldUpdateParam.getFieldId()).eq(ContractInvoiceField::getResourceId, postFieldParam.getResourceId()));
					ContractInvoiceField field = new ContractInvoiceField();
					field.setId(IDGenerator.nextStr());
					field.setResourceId(postFieldParam.getResourceId());
					field.setFieldId(fieldUpdateParam.getFieldId());
					field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
					contractInvoiceFields.add(field);
				}
			}
		}
		contractInvoiceMapper.updateById(contractInvoice);
		if (CollectionUtils.isNotEmpty(contractInvoiceFields)) {
			invoiceFieldService.getResourceFieldMapper().batchInsert(contractInvoiceFields);
		}
		if (CollectionUtils.isNotEmpty(contractInvoiceFieldBlobs)) {
			invoiceFieldService.getResourceFieldBlobMapper().batchInsert(contractInvoiceFieldBlobs);
		}
		// 更新快照
		if (snapshot != null) {
			ContractInvoiceGetResponse snapshotRes = get(contractInvoice, response.getModuleFields(), formConfig);
			snapshot.setInvoiceValue(JSON.toJSONString(snapshotRes));
			snapshotBaseMapper.update(snapshot);
		}
	}

	/**
	 * 处理旧版本审批状态 (APPROVING => NONE)
	 */
	public void handleOldApprovalData() {
		List<ContractInvoice> invoices = contractInvoiceMapper.selectListByLambda(new LambdaQueryWrapper<ContractInvoice>().eq(ContractInvoice::getApprovalStatus, ApprovalStatus.APPROVING.name()));
		invoices.forEach(invoice -> {
			ResourceSnapshotApprovalParam param = ResourceSnapshotApprovalParam.builder().resourceId(invoice.getId()).approvalStatus(ApprovalStatus.NONE.name()).build();
			updateSnapshotApprovalStatus(param);
		});
		extContractInvoiceMapper.updateOldApprovalStatusNone();
	}
}
