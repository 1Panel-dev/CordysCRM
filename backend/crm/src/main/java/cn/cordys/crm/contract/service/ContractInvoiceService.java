package cn.cordys.crm.contract.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
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
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.constants.ContractApprovalStatus;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractInvoice;
import cn.cordys.crm.contract.domain.ContractInvoiceSnapshot;
import cn.cordys.crm.contract.dto.request.ContractInvoiceAddRequest;
import cn.cordys.crm.contract.dto.request.ContractInvoicePageRequest;
import cn.cordys.crm.contract.dto.request.ContractInvoiceUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractInvoiceGetResponse;
import cn.cordys.crm.contract.dto.response.ContractInvoiceListResponse;
import cn.cordys.crm.contract.mapper.ExtContractInvoiceMapper;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
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
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private SerialNumGenerator serialNumGenerator;
    @Resource
    private DataScopeService dataScopeService;

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
    public ContractInvoice add(ContractInvoiceAddRequest request, String operatorId, String orgId) {
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("invoice.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("invoice.form.config.required"));
        }
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        ContractInvoice invoice = BeanUtils.copyBean(new ContractInvoice(), request);
        String id = IDGenerator.nextStr();
        invoice.setId(id);
        invoice.setNumber(createContractInvoiceNumber(moduleFormConfigDTO, orgId));
        invoice.setOrganizationId(orgId);
        invoice.setCreateTime(System.currentTimeMillis());
        invoice.setCreateUser(operatorId);
        invoice.setUpdateTime(System.currentTimeMillis());
        invoice.setUpdateUser(operatorId);

        if (StringUtils.isBlank(request.getOwner())) {
            invoice.setOwner(operatorId);
        }

        //自定义字段
        invoiceFieldService.saveModuleField(invoice, orgId, operatorId, moduleFields, false);
        invoiceMapper.insert(invoice);

        Contract contract = contractMapper.selectByPrimaryKey(invoice.getContractId());
        String resourceName = contract == null ? invoice.getContractId() : contract.getName();
        baseService.handleAddLog(invoice, request.getModuleFields());
        OperationLogContext.getContext().setResourceName(resourceName);
        OperationLogContext.getContext().setResourceId(invoice.getId());

        // 保存表单配置快照
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, invoiceFieldService, invoice.getId());
        ContractInvoiceGetResponse response = getContractInvoiceResponse(invoice, resolveFieldValues, moduleFormConfigDTO);
        saveSnapshot(invoice, saveModuleFormConfigDTO, response);

        return invoice;
    }


    private String createContractInvoiceNumber(ModuleFormConfigDTO moduleFormConfigDTO, String orgId) {
        BaseField numberField = moduleFormConfigDTO.getFields().stream()
                .filter(field -> field.isSerialNumber() && StringUtils.isNotEmpty(field.getBusinessKey())).findFirst().orElse(null);

        if (numberField != null) {
            BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
            fieldValue.setFieldId(numberField.getId());
            return serialNumGenerator.generateByRules(((SerialNumberField) numberField).getSerialNumberRules(), orgId, FormKey.INVOICE.getKey());
        }
        return null;
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
     * 获取合同详情
     *
     * @param invoice
     * @param moduleFields
     * @param moduleFormConfigDTO
     * @return
     */
    private ContractInvoiceGetResponse getContractInvoiceResponse(ContractInvoice invoice, List<BaseModuleFieldValue> moduleFields, ModuleFormConfigDTO moduleFormConfigDTO) {
        ContractInvoiceGetResponse response = BeanUtils.copyBean(new ContractInvoiceGetResponse(), invoice);
        moduleFormService.processBusinessFieldValues(response, moduleFields, moduleFormConfigDTO);
        List<BaseModuleFieldValue> fvs = invoiceFieldService.setBusinessRefFieldValue(List.of(response), moduleFormService.getFlattenFormFields(FormKey.INVOICE.getKey(), invoice.getOrganizationId()),
                new HashMap<>(Map.of(response.getId(), moduleFields))).get(response.getId());
        response.setModuleFields(fvs);

        response = baseService.setCreateAndUpdateOwnerUserName(response);

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(moduleFormConfigDTO, fvs);
        Contract contract = contractMapper.selectByPrimaryKey(invoice.getContractId());
        optionMap.put(BusinessModuleField.INVOICE_CONTRACT_ID.getBusinessKey(), Collections.singletonList(new OptionDTO(contract.getId(), contract.getName())));
        optionMap.put(BusinessModuleField.INVOICE_OWNER.getBusinessKey(), Collections.singletonList(new OptionDTO(response.getId(), response.getOwnerName())));
        response.setOptionMap(optionMap);
        response.setContractName(contract.getName());
        Map<String, List<Attachment>> attachmentMap = moduleFormService.getAttachmentMap(moduleFormConfigDTO, moduleFields);
        response.setAttachmentMap(attachmentMap);
        return response;
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

        dataScopeService.checkDataPermission(userId, orgId, originContractInvoice.getOwner(), PermissionConstants.CONTRACT_INVOICE_UPDATE);
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        Optional.ofNullable(originContractInvoice).ifPresentOrElse(item -> {
            List<BaseModuleFieldValue> originFields = invoiceFieldService.getModuleFieldValuesByResourceId(request.getId());
            ContractInvoice invoice = BeanUtils.copyBean(new ContractInvoice(), request);
            invoice.setUpdateTime(System.currentTimeMillis());
            invoice.setUpdateUser(userId);
            // 保留不可更改的字段
            invoice.setNumber(originContractInvoice.getNumber());
            invoice.setCreateUser(originContractInvoice.getCreateUser());
            invoice.setCreateTime(originContractInvoice.getCreateTime());
            invoice.setApprovalStatus(ContractApprovalStatus.APPROVING.name());

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
                    originFields.addAll(originModuleFields);
                }
            }
            snapshotBaseMapper.deleteByLambda(delWrapper);
            //保存快照
            List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, invoiceFieldService, invoice.getId());
            ContractInvoiceGetResponse response = getContractInvoiceResponse(invoice, resolveFieldValues, moduleFormConfigDTO);
            saveSnapshot(invoice, saveModuleFormConfigDTO, response);
            Contract contract = contractMapper.selectByPrimaryKey(invoice.getContractId());

            // 处理日志上下文
            baseService.handleUpdateLogWithSubTable(originContractInvoice, invoice, originFields, moduleFields, request.getId(), contract.getName(), Translator.get("products_info"), moduleFormConfigDTO);
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

        Contract contract = contractMapper.selectByPrimaryKey(invoice.getContractId());

        // 添加日志上下文
        OperationLogContext.setResourceName(contract.getName());
    }

    public ContractInvoiceGetResponse getWithDataPermissionCheck(String id, String userId, String orgId) {
        ContractInvoiceGetResponse getResponse = get(id);
        if (getResponse == null) {
            throw new GenericException(Translator.get("resource.not.exist"));
        }
        dataScopeService.checkDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.CONTRACT_INVOICE_READ);
        return getResponse;
    }

    /**
     * 从快照中获取合同详情
     * @param id 合同ID
     * @return 合同详情
     */
    public ContractInvoiceGetResponse get(String id) {
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
        Map<String, List<BaseModuleFieldValue>> invoiceFiledMap = invoiceFieldService.getResourceFieldMap(invoiceIds, true);
        Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = invoiceFieldService.setBusinessRefFieldValue(list, moduleFormService.getFlattenFormFields(FormKey.INVOICE.getKey(), orgId), invoiceFiledMap);

        List<String> ownerIds = list.stream()
                .map(ContractInvoiceListResponse::getOwner)
                .distinct()
                .toList();

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        list.forEach(item -> {
            UserDeptDTO userDeptDTO = userDeptMap.get(item.getOwner());
            if (userDeptDTO != null) {
                item.setDepartmentId(userDeptDTO.getDeptId());
                item.setDepartmentName(userDeptDTO.getDeptName());
            }
            // 获取自定义字段
            List<BaseModuleFieldValue> invoiceFields = resolvefieldValueMap.get(item.getId());
            item.setModuleFields(invoiceFields);
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

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CONTRACT_INVOICE_READ, rolePermissions);
    }
}
