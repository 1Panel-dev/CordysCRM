package cn.cordys.crm.contract.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.*;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.dto.stage.StageSortRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.service.BaseService;
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
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.contract.constants.ContractApprovalStatus;
import cn.cordys.crm.contract.constants.ContractStage;
import cn.cordys.crm.contract.domain.*;
import cn.cordys.crm.contract.dto.request.ContractAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.request.ContractStageRequest;
import cn.cordys.crm.contract.dto.request.ContractUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractGetResponse;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.dto.response.ContractStatisticResponse;
import cn.cordys.crm.contract.dto.response.CustomerContractStatisticResponse;
import cn.cordys.crm.contract.mapper.ExtContractInvoiceMapper;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractStageConfigMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTaskConfig;
import cn.cordys.crm.system.dto.MessageTaskConfigDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
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
public class ContractService {

    @Resource
    private ContractFieldService contractFieldService;
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<ContractSnapshot> snapshotBaseMapper;
    @Resource
    private ExtContractMapper extContractMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private BaseMapper<Customer> customerBaseMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private BaseMapper<MessageTaskConfig> messageTaskConfigMapper;
    @Resource
    private BaseMapper<ContractPaymentRecord> contractPaymentRecordMapper;
    @Resource
    private ExtContractInvoiceMapper extContractInvoiceMapper;
    @Resource
    private DictService dictService;
    @Resource
    private ExtContractStageConfigMapper extContractStageConfigMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ApprovalResourceService approvalResourceService;
	@Resource
	private LogService logService;

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");
    public static final Long DEFAULT_POS = 1L;

    /**
     * 新建合同
     *
     * @param request
     * @param operatorId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.ADD, resourceName = "{#request.name}")
    @HitApproval(formKey = FormKey.CONTRACT, executeType = ExecuteTimingEnum.CREATE, operatorId = "{#operatorId}")
    public Contract add(ContractAddRequest request, String operatorId, String orgId) {
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("contract.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("contract.form.config.required"));
        }
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        List<StageConfigResponse> stageConfigList = extContractStageConfigMapper.getStageConfigList(orgId);
        Long nextPos = getNextPos(orgId, stageConfigList.getFirst().getId());
        Contract contract = new Contract();
        String id = IDGenerator.nextStr();
        contract.setId(id);
        contract.setName(request.getName());
        contract.setCustomerId(request.getCustomerId());
        contract.setOwner(request.getOwner());
        contract.setNumber(request.getNumber());
        contract.setStage(stageConfigList.getFirst().getId());
        contract.setPos(nextPos);
        contract.setOrganizationId(orgId);
        contract.setApprovalStatus(ApprovalStatus.NONE.name());
        contract.setStartTime(request.getStartTime());
        contract.setEndTime(request.getEndTime());
        contract.setCreateTime(System.currentTimeMillis());
        contract.setCreateUser(operatorId);
        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(operatorId);

        if (!dictService.isDictConfigEnable(DictModule.CONTRACT_APPROVAL.name(), orgId)) {
            contract.setApprovalStatus(ContractApprovalStatus.NONE.name());
        }

        //判断总金额
        setAmount(request.getAmount(), contract);

        // 设置子表格字段值
        moduleFields.add(new BaseModuleFieldValue("products", request.getProducts()));
        //自定义字段
        contractFieldService.saveModuleField(contract, orgId, operatorId, moduleFields, false);
        contractMapper.insert(contract);

        baseService.handleAddLogWithSubTable(contract, moduleFields, Translator.get("products_info"), moduleFormConfigDTO);

        // 保存表单配置快照
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, contractFieldService, contract.getId());
        ContractGetResponse response = get(contract, resolveFieldValues, moduleFormConfigDTO);
        saveSnapshot(contract, saveModuleFormConfigDTO, response);

        return contract;
    }

    private Long getNextPos(String orgId, String stage) {
        Long pos = extContractMapper.selectNextPos(orgId, stage);
        return pos == null ? 1 : pos + 1;
    }


    /**
     * 保存合同快照
     *
     * @param contract
     * @param moduleFormConfigDTO
     * @param response
     */
    private void saveSnapshot(Contract contract, ModuleFormConfigDTO moduleFormConfigDTO, ContractGetResponse response) {
        //移除response中moduleFields 集合里 的 BaseModuleFieldValue 的 fieldId="products"的数据，避免快照数据过大
        if (CollectionUtils.isNotEmpty(response.getModuleFields())) {
            response.setModuleFields(response.getModuleFields().stream()
                    .filter(field -> (field.getFieldValue() != null && StringUtils.isNotBlank(field.getFieldValue().toString()) && !"[]".equals(field.getFieldValue().toString()))).toList());
        }
        ContractSnapshot snapshot = new ContractSnapshot();
        snapshot.setId(IDGenerator.nextStr());
        snapshot.setContractId(contract.getId());
        snapshot.setContractProp(JSON.toJSONString(moduleFormConfigDTO));
        snapshot.setContractValue(JSON.toJSONString(response));
        snapshotBaseMapper.insert(snapshot);

    }

    private ContractGetResponse get(Contract contract, List<BaseModuleFieldValue> contractFields, ModuleFormConfigDTO contractFormConfig) {
        ContractGetResponse contractGetResponse = BeanUtils.copyBean(new ContractGetResponse(), contract);
        contractGetResponse = baseService.setCreateUpdateOwnerUserName(contractGetResponse);

        String id = contract.getId();
        // 获取模块字段
        moduleFormService.processBusinessFieldValues(contractGetResponse, contractFields, contractFormConfig);
        contractFields = contractFieldService.setBusinessRefFieldValue(List.of(contractGetResponse),
                moduleFormService.getFlattenFormFields(FormKey.CONTRACT.getKey(), contract.getOrganizationId()), new HashMap<>(Map.of(id, contractFields))).get(id);

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(contractFormConfig, contractFields);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(contractGetResponse,
                ContractGetResponse::getOwner, ContractGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CONTRACT_OWNER.getBusinessKey(), ownerFieldOption);

        Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
        if (customer != null) {
            contractGetResponse.setCustomerName(customer.getName());
            optionMap.put(BusinessModuleField.CONTRACT_CUSTOMER_NAME.getBusinessKey(), Collections.singletonList(new OptionDTO(customer.getId(), customer.getName())));
        }

        contractGetResponse.setOptionMap(optionMap);
        contractGetResponse.setModuleFields(contractFields);

        if (contractGetResponse.getOwner() != null) {
            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(contractGetResponse.getOwner(), contract.getOrganizationId());
            if (userDeptDTO != null) {
                contractGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                contractGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        // 附件信息
        contractGetResponse.setAttachmentMap(moduleFormService.getAttachmentMap(contractFormConfig, contractFields));
        contractGetResponse.setAlreadyPayAmount(sumContractRecordAmount(id));

        return contractGetResponse;
    }

    /**
     * 获取合同详情
     *
     * @param id
     * @return
     */
    public ContractGetResponse get(String id, String orgId) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        // 获取模块字段
        ModuleFormConfigDTO contractFormConfig = getFormConfig(contract.getOrganizationId());
        List<BaseModuleFieldValue> contractFields = contractFieldService.getModuleFieldValuesByResourceId(id);
        ContractGetResponse getResponse = get(contract, contractFields, contractFormConfig);

        if (Strings.CI.equals(getResponse.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
            Map<String, Boolean> firstNodeApproved = baseService.getApprovingResourceFirstNodeApproved(List.of(getResponse.getId()), orgId);
            getResponse.setFirstApproved(firstNodeApproved.get(getResponse.getId()));
        }
        return getResponse;
    }

    /**
     * 获取合同详情（⚠️反射调用; 勿修改入参, 返回, 方法名!）
     *
     * @param id 合同ID
     * @return 合同详情
     */
    public ContractGetResponse getSimple(String id) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            return null;
        }
        ContractGetResponse response = BeanUtils.copyBean(new ContractGetResponse(), contract);
        List<BaseModuleFieldValue> fvs = contractFieldService.getModuleFieldValuesByResourceId(id);
        ModuleFormConfigDTO contractFormConfig = getFormConfig(contract.getOrganizationId());
        moduleFormService.processBusinessFieldValues(response, fvs, contractFormConfig);
        return response;
    }


    /**
     * 获取字段详情 (⚠️反射调用; 勿修改入参, 返回, 方法名!)
     * @param id 合同ID
     * @return 合同详情
     */
    public ContractGetResponse getFieldValues(String id) {
        ContractGetResponse response = new ContractGetResponse();
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            return null;
        }
        LambdaQueryWrapper<ContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractSnapshot::getContractId, id);
        ContractSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getContractValue(), ContractGetResponse.class);
            Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
            if (customer != null) {
                response.setInCustomerPool(customer.getInSharedPool());
                response.setPoolId(customer.getPoolId());
            }
            response.setAlreadyPayAmount(sumContractRecordAmount(id));
        }
        return response;
    }

    /**
     * 批量获取合同详情 (用于数据源批量查询优化)
     *
     * @param ids 合同ID集合
     * @return 合同详情列表
     */
    public List<ContractGetResponse> batchGetSimpleByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<Contract> contracts = contractMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(contracts)) {
            return Collections.emptyList();
        }
        Map<String, List<BaseModuleFieldValue>> fieldValueMap = contractFieldService.getResourceFieldMap(ids, true);

        return contracts.stream().map(contract -> {
            ContractGetResponse response = BeanUtils.copyBean(new ContractGetResponse(), contract);
            response.setModuleFields(fieldValueMap.get(contract.getId()));
            return response;
        }).toList();
    }


    /**
     * 编辑合同
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    @HitApproval(formKey = FormKey.CONTRACT, executeType = ExecuteTimingEnum.EDIT, resourceId = "{#request.id}", updateType = "{#request.updateType}", operatorId = "{#userId}")
    public Contract update(ContractUpdateRequest request, String userId, String orgId) {
        Contract oldContract = contractMapper.selectByPrimaryKey(request.getId());
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("contract.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("contract.form.config.required"));
        }
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        Optional.ofNullable(oldContract).ifPresentOrElse(item -> {

            List<BaseModuleFieldValue> originFields = contractFieldService.getModuleFieldValuesByResourceId(request.getId());
            Contract contract = BeanUtils.copyBean(new Contract(), request);
            contract.setStartTime(request.getStartTime());
            contract.setEndTime(request.getEndTime());
            contract.setUpdateTime(System.currentTimeMillis());
            contract.setUpdateUser(userId);
            // 保留不可更改的字段
            contract.setNumber(oldContract.getNumber());
            contract.setCreateUser(oldContract.getCreateUser());
            contract.setCreateTime(oldContract.getCreateTime());
            contract.setStage(oldContract.getStage());
            contract.setApprovalStatus(oldContract.getApprovalStatus());

            //判断总金额
            setAmount(request.getAmount(), contract);
            moduleFields.add(new BaseModuleFieldValue("products", request.getProducts()));
            updateFields(moduleFields, contract, orgId, userId);
            contractMapper.update(contract);
            //删除快照
            LambdaQueryWrapper<ContractSnapshot> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(ContractSnapshot::getContractId, request.getId());
            List<ContractSnapshot> contractSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
            if (CollectionUtils.isNotEmpty(contractSnapshots)) {
                ContractSnapshot first = contractSnapshots.getFirst();
                if (first != null) {
                    ContractGetResponse response = JSON.parseObject(first.getContractValue(), ContractGetResponse.class);
                    List<BaseModuleFieldValue> originModuleFields = response.getModuleFields();
                    originModuleFields.add(new BaseModuleFieldValue("products", response.getProducts()));
                    originFields.addAll(originModuleFields);
                }
            }
            snapshotBaseMapper.deleteByLambda(delWrapper);
            //保存快照
            List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, contractFieldService, contract.getId());
            // get 方法需要使用orgId
            contract.setOrganizationId(orgId);
            ContractGetResponse response = get(contract, resolveFieldValues, moduleFormConfigDTO);
            saveSnapshot(contract, saveModuleFormConfigDTO, response);
            // 处理日志上下文
            baseService.handleUpdateLogWithSubTable(oldContract, contract, originFields, moduleFields, request.getId(), contract.getName(), Translator.get("products_info"), moduleFormConfigDTO);
        }, () -> {
            throw new GenericException(Translator.get("contract.not.exist"));
        });
        return contractMapper.selectByPrimaryKey(request.getId());
    }

    private void setAmount(String amount, Contract contract) {
        if (StringUtils.isNotBlank(amount)) {
            contract.setAmount(new BigDecimal(amount));
            if (contract.getAmount().compareTo(MAX_AMOUNT) > 0) {
                throw new GenericException(Translator.get("contract.amount.exceed.max"));
            }
        } else {
            contract.setAmount(BigDecimal.ZERO);
        }
    }


    /**
     * 更新自定义字段
     *
     * @param moduleFields
     * @param contract
     * @param orgId
     * @param userId
     */
    private void updateFields(List<BaseModuleFieldValue> moduleFields, Contract contract, String orgId, String userId) {
        if (moduleFields == null) {
            return;
        }
        contractFieldService.deleteByResourceId(contract.getId());
        contractFieldService.saveModuleField(contract, orgId, userId, moduleFields, true);
    }


    /**
     * 删除合同
     *
     * @param id 合同ID
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        checkContractRelated(id);

        contractFieldService.deleteByResourceId(id);
        contractMapper.deleteByPrimaryKey(id);

        //删除快照
        LambdaQueryWrapper<ContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractSnapshot::getContractId, id);
        snapshotBaseMapper.deleteByLambda(wrapper);
        // 添加日志上下文
        OperationLogContext.setResourceName(contract.getName());
    }


    /**
     * ⚠️反射调用; 勿修改入参, 返回, 方法名!
     *
     * @param id 合同ID
     * @return 合同详情
     */
    public ContractGetResponse getSnapshot(String id, String orgId) {
        ContractGetResponse response = new ContractGetResponse();
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            return null;
        }
        LambdaQueryWrapper<ContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractSnapshot::getContractId, id);
        ContractSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getContractValue(), ContractGetResponse.class);
            Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
            if (customer != null) {
                response.setInCustomerPool(customer.getInSharedPool());
                response.setPoolId(customer.getPoolId());
            }
            response.setAlreadyPayAmount(sumContractRecordAmount(id));
            if (Strings.CI.equals(response.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
                Map<String, Boolean> firstNodeApproved = baseService.getApprovingResourceFirstNodeApproved(List.of(response.getId()), orgId);
                response.setFirstApproved(firstNodeApproved.get(response.getId()));
            }
        }
        return response;
    }


    /**
     * 合同列表
     *
     * @param request
     * @param userId
     * @param orgId
     * @param deptDataPermission
     * @return
     */
    public PagerWithOption<List<ContractListResponse>> list(ContractPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission, Boolean source) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ContractListResponse> list = extContractMapper.list(request, orgId, userId, deptDataPermission, source);
        List<ContractListResponse> results = buildList(list, orgId);
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(list, results, customerFormConfig);

        return PageUtils.setPageInfoWithOption(page, results, optionMap);
    }

    private Map<String, List<OptionDTO>> buildOptionMap(List<ContractListResponse> list, List<ContractListResponse> buildList,
                                                        ModuleFormConfigDTO formConfig) {
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ContractListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFieldValues);
        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                ContractListResponse::getOwner, ContractListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CONTRACT_OWNER.getBusinessKey(), ownerFieldOption);
        return optionMap;
    }

    private ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT.getKey(), orgId);
    }

    public List<ContractListResponse> buildList(List<ContractListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        List<String> contractIds = list.stream().map(ContractListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> contractFiledMap = contractFieldService.getResourceFieldMap(contractIds, true);
        Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = contractFieldService.setBusinessRefFieldValue(list, moduleFormService.getFlattenFormFields(FormKey.CONTRACT.getKey(), orgId), contractFiledMap);


        List<String> ownerIds = list.stream()
                .map(ContractListResponse::getOwner)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(ownerIds);
        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        Map<String, String> stageNameMap = extContractStageConfigMapper.getStageConfigList(orgId).stream()
                .collect(Collectors.toMap(StageConfigResponse::getId,
                        StageConfigResponse::getName));

        List<String> approvingResourceIds = list.stream().filter(item -> Strings.CI.contains(item.getApprovalStatus(), ApprovalStatus.APPROVING.name())).map(ContractListResponse::getId).toList();
        Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(approvingResourceIds, orgId);

        list.forEach(item -> {
            item.setOwnerName(userNameMap.get(item.getOwner()));
            UserDeptDTO userDeptDTO = userDeptMap.get(item.getOwner());
            if (userDeptDTO != null) {
                item.setDepartmentId(userDeptDTO.getDeptId());
                item.setDepartmentName(userDeptDTO.getDeptName());
            }
            item.setStageName(stageNameMap.get(item.getStage()));
            // 获取自定义字段
            List<BaseModuleFieldValue> contractFields = resolvefieldValueMap.get(item.getId());
            item.setModuleFields(contractFields);
            item.setFirstApproved(firstNodeApprovedMap.get(item.getId()));
        });
        return baseService.setCreateAndUpdateUserName(list);
    }


    /**
     * 获取表单快照
     *
     * @param id
     * @param orgId
     * @return
     */
    public ModuleFormConfigDTO getFormSnapshot(String id, String orgId) {
        ModuleFormConfigDTO moduleFormConfigDTO = new ModuleFormConfigDTO();
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        LambdaQueryWrapper<ContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractSnapshot::getContractId, id);
        ContractSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            moduleFormConfigDTO = JSON.parseObject(snapshot.getContractProp(), ModuleFormConfigDTO.class);
        } else {
            moduleFormConfigDTO = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT.getKey(), orgId);
        }
        return moduleFormConfigDTO;

    }


    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CONTRACT_READ, rolePermissions);
    }


    /**
     * 更新合同状态
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void updateStage(ContractStageRequest request, String userId, String orgId) {
        Contract contract = contractMapper.selectByPrimaryKey(request.getId());
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }

        List<StageConfigResponse> stageConfigList = extContractStageConfigMapper.getStageConfigList(orgId);

        Map<String, String> stageMap = stageConfigList.stream()
                .collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName));

        final Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("contractStage", stageMap.get(contract.getStage()));

        contract.setStage(request.getStage());
        if (StringUtils.isNotBlank(request.getVoidReason())) {
            contract.setVoidReason(request.getVoidReason());
        }

        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(userId);
        contractMapper.update(contract);

        updateStatusSnapshot(request.getId(), request.getStage(), null);

        if (Strings.CI.equals(request.getStage(), ContractStage.VOID.name()) || Strings.CI.equals(request.getStage(), ContractStage.ARCHIVED.name())) {
            String event = Strings.CI.equals(request.getStage(), ContractStage.VOID.name()) ?
                    NotificationConstants.Event.CONTRACT_VOID : NotificationConstants.Event.CONTRACT_ARCHIVED;
            Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
            sendNotice(contract, userId, orgId, event, customer.getName());
        }


        final Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("contractStage", stageMap.get(request.getStage()));
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(contract.getName())
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );

    }

    /**
     * 发送通知
     *
     * @param contract 合同实体
     * @param userId   用户ID
     * @param orgId    组织ID
     * @param event    事件类型
     */
    private void sendNotice(Contract contract, String userId, String orgId, String event, String customerName) {
        //查询通知配置的接收范围
        List<String> receiveUserIds = new ArrayList<>();
        List<MessageTaskConfig> messageTaskConfigList = messageTaskConfigMapper.selectListByLambda(new LambdaQueryWrapper<MessageTaskConfig>()
                .eq(MessageTaskConfig::getOrganizationId, orgId)
                .eq(MessageTaskConfig::getTaskType, NotificationConstants.Module.CONTRACT)
                .eq(MessageTaskConfig::getEvent, event));
        if (CollectionUtils.isNotEmpty(messageTaskConfigList)) {
            MessageTaskConfig messageTaskConfig = messageTaskConfigList.getFirst();
            MessageTaskConfigDTO messageTaskConfigDTO = JSON.parseObject(messageTaskConfig.getValue(), MessageTaskConfigDTO.class);
            receiveUserIds = commonNoticeSendService.getNoticeReceiveUserIds(messageTaskConfigDTO, contract.getCreateUser(), contract.getOwner(), orgId);
        } else {
            //默认通知创建人
            receiveUserIds.add(contract.getOwner());
        }

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("customerName", customerName);
        paramMap.put("name", contract.getName());
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CONTRACT, event,
                paramMap, userId, orgId, receiveUserIds, true);
    }

    private void updateStatusSnapshot(String id, String stage, String approvalStatus) {
        LambdaQueryWrapper<ContractSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(ContractSnapshot::getContractId, id);
        List<ContractSnapshot> contractSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
        ContractSnapshot first = contractSnapshots.getFirst();
        if (first != null) {
            ContractGetResponse response = JSON.parseObject(first.getContractValue(), ContractGetResponse.class);
            if (StringUtils.isNotBlank(stage)) {
                response.setStage(stage);
            }
            if (StringUtils.isNotBlank(approvalStatus)) {
                response.setApprovalStatus(approvalStatus);
            }
            first.setContractValue(JSON.toJSONString(response));
            snapshotBaseMapper.update(first);
        }
    }

    /**
     * ⚠️反射调用: 由审批执行操作统一调用, 勿修改
     *
     * @param param 参数
     */
    public void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param) {
        ContractSnapshot snapshotCriteria = new ContractSnapshot();
        snapshotCriteria.setContractId(param.getResourceId());
        ContractSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
        if (snapshot != null) {
            ContractGetResponse response = JSON.parseObject(snapshot.getContractValue(), ContractGetResponse.class);
            response.setApprovalStatus(param.getApprovalStatus());
            snapshot.setContractValue(JSON.toJSONString(response));
            snapshotBaseMapper.update(snapshot);
        }
    }

    /**
     * ⚠️反射调用: 由审批执行后置操作统一调用, 勿修改
     *
     * @param postFieldParam 参数
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void updateApprovalPostField(ResourceApprovalPostUpdateParam postFieldParam) {
        ModuleFormConfigDTO formConfig = getFormConfig(OrganizationContext.getOrganizationId());
        List<BaseField> fields = formConfig.getFields();
        Map<String, BaseField> fieldConfigMap = fields.stream().collect(Collectors.toMap(BaseField::getId, f -> f));
        Contract contract = contractMapper.selectByPrimaryKey(postFieldParam.getResourceId());
        // 保存原始数据用于日志记录
        Contract originContract = BeanUtils.copyBean(new Contract(), contract);
        List<BaseModuleFieldValue> originFields = contractFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId());
        List<ContractField> contractFields = new ArrayList<>();
        List<ContractFieldBlob> contractFieldBlobs = new ArrayList<>();
        ContractSnapshot snapshotCriteria = new ContractSnapshot();
        snapshotCriteria.setContractId(postFieldParam.getResourceId());
        ContractSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
        ContractGetResponse response = new ContractGetResponse();
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getContractValue(), ContractGetResponse.class);
        }
        for (ResourceApprovalFieldUpdateParam fieldUpdateParam : postFieldParam.getFields()) {
            if (!fieldConfigMap.containsKey(fieldUpdateParam.getFieldId()) || fieldUpdateParam.getFieldValue() == null) {
                return;
            }
            BaseField fieldConfig = fieldConfigMap.get(fieldUpdateParam.getFieldId());
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
            if (fieldConfig.hasBusinessKey()) {
                // 业务主表字段
                contractFieldService.setResourceFieldValue(contract, fieldConfig.getBusinessKey(), fieldUpdateParam.getFieldValue());
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
                    contractFieldService.getResourceFieldBlobMapper().deleteByLambda(new LambdaQueryWrapper<ContractFieldBlob>()
                            .eq(ContractFieldBlob::getFieldId, fieldUpdateParam.getFieldId()).eq(ContractFieldBlob::getResourceId, postFieldParam.getResourceId()));
                    ContractFieldBlob field = new ContractFieldBlob();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
                    field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    contractFieldBlobs.add(field);
                } else {
                    // 自定义表
                    contractFieldService.getResourceFieldMapper().deleteByLambda(new LambdaQueryWrapper<ContractField>()
                            .eq(ContractField::getFieldId, fieldUpdateParam.getFieldId()).eq(ContractField::getResourceId, postFieldParam.getResourceId()));
                    ContractField field = new ContractField();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
                    field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    contractFields.add(field);
                }
            }
        }
        contractMapper.updateById(contract);
        if (CollectionUtils.isNotEmpty(contractFields)) {
            contractFieldService.getResourceFieldMapper().batchInsert(contractFields);
        }
        if (CollectionUtils.isNotEmpty(contractFieldBlobs)) {
            contractFieldService.getResourceFieldBlobMapper().batchInsert(contractFieldBlobs);
        }
        // 更新快照
        if (snapshot != null) {
            ContractGetResponse snapshotRes = get(contract, response.getModuleFields(), formConfig);
            snapshot.setContractValue(JSON.toJSONString(snapshotRes));
            snapshotBaseMapper.update(snapshot);
        }
        // 记录审批后置字段更新日志
        baseService.handleUpdateLogWithSubTable(originContract, contract, originFields, contractFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId()),
                postFieldParam.getResourceId(), contract.getName(), Translator.get("products_info"), formConfig);
        // 从 OperationLogContext 中获取日志信息并手动记录
        LogContextInfo contextInfo = OperationLogContext.getContext();
        if (contextInfo != null) {
            String orgId = OrganizationContext.getOrganizationId();
            LogDTO logDTO = new LogDTO(orgId, postFieldParam.getResourceId(), postFieldParam.getOperator(), LogType.UPDATE, LogModule.CONTRACT_INDEX, contract.getName());
            logDTO.setOriginalValue(contextInfo.getOriginalValue());
            logDTO.setModifiedValue(contextInfo.getModifiedValue());
            logService.add(logDTO);
            OperationLogContext.clear();
        }
    }


    public CustomerContractStatisticResponse calculateContractStatisticByCustomerId(String customerId, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        return extContractMapper.calculateContractStatisticByCustomerId(customerId, userId, orgId, deptDataPermission);
    }

    public String getContractName(String id) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(contract).map(Contract::getName).orElse(null);
    }

    public Contract selectByPrimaryKey(String id) {
        return contractMapper.selectByPrimaryKey(id);
    }

    /**
     * 通过名称获取合同集合
     *
     * @param names 名称集合
     * @return 合同集合
     */
    public List<Contract> getContractListByNames(List<String> names) {
        LambdaQueryWrapper<Contract> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Contract::getName, names);
        return contractMapper.selectListByLambda(lambdaQueryWrapper);
    }

    /**
     * 计算合同已回款金额
     *
     * @param contractId 合同ID
     * @return 已回款金额
     */
    private BigDecimal sumContractRecordAmount(String contractId) {
        LambdaQueryWrapper<ContractPaymentRecord> paymentRecordWrapper = new LambdaQueryWrapper<>();
        paymentRecordWrapper.eq(ContractPaymentRecord::getContractId, contractId);
        List<ContractPaymentRecord> contractPaymentRecords = contractPaymentRecordMapper.selectListByLambda(paymentRecordWrapper);
        if (CollectionUtils.isNotEmpty(contractPaymentRecords)) {
            return contractPaymentRecords.stream()
                    .map(ContractPaymentRecord::getRecordAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 批量更新合同
     *
     * @param request        批量编辑参数
     * @param userId         当前用户ID
     * @param organizationId 当前组织ID
     */
    public BatchAffectReasonResponse batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = contractFieldService.getAndCheckField(request.getFieldId(), organizationId);
        // getAndCheckField 走的是 getConfig()，不会设置 businessKey，需要手动补充
        moduleFormService.setFieldBusinessParam(field);
        List<Contract> originContracts = contractMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(originContracts)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0).errorMessages(Translator.get("contract.not.exist")).build();
        }

        // 校验状态权限，过滤出有权限操作的合同
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.CONTRACT.getValue(),
                originContracts,
                PermissionConstants.CONTRACT_UPDATE,
                organizationId,
                Contract::getId,
                Contract::getApprovalStatus
        );

        if (CollectionUtils.isEmpty(permittedIds)) {
            return BatchAffectReasonResponse.builder().success(0).fail(originContracts.size()).skip(0).errorMessages(Translator.get("no.operation.permission")).build();
        }
        approvalResourceService.batchEditTriggerApproval(permittedIds, FormKey.CONTRACT, organizationId, userId);
        List<Contract> permittedContracts = originContracts.stream()
                .filter(c -> permittedIds.contains(c.getId()))
                .collect(Collectors.toList());

        ResourceBatchEditRequest filteredRequest = new ResourceBatchEditRequest();
        filteredRequest.setIds(permittedIds);
        filteredRequest.setFieldId(request.getFieldId());
        filteredRequest.setFieldValue(request.getFieldValue());

        contractFieldService.batchUpdate(filteredRequest, field, permittedContracts, Contract.class, LogModule.CONTRACT_INDEX, extContractMapper::batchUpdate, userId, organizationId);

        // 批量更新后重建每条合同的快照
        ModuleFormConfigDTO moduleFormConfigDTO = getFormConfig(organizationId);
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);

        // 批量删除旧快照（1次）
        LambdaQueryWrapper<ContractSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.in(ContractSnapshot::getContractId, permittedIds);
        snapshotBaseMapper.deleteByLambda(delWrapper);

        // 批量重新获取最新合同数据，因为业务字段已更新（1次替代N次）
        List<Contract> latestContracts = contractMapper.selectByIds(permittedIds);
        Map<String, Contract> latestContractMap = latestContracts.stream()
                .collect(Collectors.toMap(Contract::getId, c -> c));

        // 批量获取所有合同的自定义字段值（1次替代N次）
        Map<String, List<BaseModuleFieldValue>> fieldMap = contractFieldService.getResourceFieldMap(permittedIds, true);

        // 逐条构建快照，批量写入
        List<ContractSnapshot> snapshots = new ArrayList<>();
        for (String id : permittedIds) {
            Contract contract = latestContractMap.get(id);
            if (contract == null) continue;
            List<BaseModuleFieldValue> contractFields = fieldMap.getOrDefault(id, Collections.emptyList());
            List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(contractFields, moduleFormConfigDTO, contractFieldService, id);
            ContractGetResponse response = get(contract, resolveFieldValues, moduleFormConfigDTO);
            // 过滤空值（与 saveSnapshot 保持一致）
            if (CollectionUtils.isNotEmpty(response.getModuleFields())) {
                response.setModuleFields(response.getModuleFields().stream()
                        .filter(f -> f.getFieldValue() != null && StringUtils.isNotBlank(f.getFieldValue().toString()) && !"[]".equals(f.getFieldValue().toString()))
                        .toList());
            }
            ContractSnapshot snapshot = new ContractSnapshot();
            snapshot.setId(IDGenerator.nextStr());
            snapshot.setContractId(id);
            snapshot.setContractProp(JSON.toJSONString(saveModuleFormConfigDTO));
            snapshot.setContractValue(JSON.toJSONString(response));
            snapshots.add(snapshot);
        }
        if (CollectionUtils.isNotEmpty(snapshots)) {
            snapshotBaseMapper.batchInsert(snapshots);
        }

        return BatchAffectReasonResponse.builder().success(permittedIds.size()).fail(originContracts.size() - permittedIds.size()).skip(0).errorMessages(Translator.get("batch.update.reason")).build();
    }

    /**
     * 校验合同是否存在关联数据
     *
     * @param contractId 合同ID
     */
    private void checkContractRelated(String contractId) {
        LambdaQueryWrapper<ContractPaymentRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ContractPaymentRecord::getContractId, contractId);
        List<ContractPaymentRecord> contractPaymentRecords = contractPaymentRecordMapper.selectListByLambda(recordWrapper);
        if (CollectionUtils.isNotEmpty(contractPaymentRecords)) {
            throw new GenericException(Translator.get("contract.has.payment.record"));
        }
        if (extContractInvoiceMapper.hasContractInvoice(contractId)) {
            throw new GenericException(Translator.get("contract.has.invoice.cannot.delete"));
        }
    }


    /**
     * 统计
     *
     * @param request
     * @param userId
     * @param orgId
     * @param deptDataPermission
     * @return
     */
    public ContractStatisticResponse searchStatistic(BaseCondition request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ContractStatisticResponse response = extContractMapper.searchStatistic(request, orgId, userId, deptDataPermission);
        return Optional.ofNullable(response).orElse(new ContractStatisticResponse());
    }

    /**
     * 通过ID集合获取合同名称
     *
     * @param ids id集合
     * @return 合同名称
     */
    public Object getContractNameByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return StringUtils.EMPTY;
        }
        List<Contract> contracts = contractMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(contracts)) {
            List<String> names = contracts.stream().map(Contract::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }


    /**
     * 阶段看板排序
     *
     * @param request
     * @param userId
     */
    public void sort(StageSortRequest request, String userId) {
        //拖拽节点
        Contract contract = contractMapper.selectByPrimaryKey(request.getDragNodeId());
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        Long pos = DEFAULT_POS;
        if (StringUtils.isNotBlank(request.getDropNodeId())) {
            //放入节点
            Contract dropNode = contractMapper.selectByPrimaryKey(request.getDropNodeId());
            pos = dropNode.getPos();
            if (request.getDropPosition() == -1) {

                extContractMapper.moveUpStageContract(pos, request.getStage(), DEFAULT_POS);
                pos = pos + 1;
            } else {
                extContractMapper.moveDownStageContract(pos, request.getStage(), DEFAULT_POS);
            }
        }
        Contract dragContract = new Contract();
        dragContract.setId(request.getDragNodeId());
        dragContract.setPos(pos);
        dragContract.setStage(request.getStage());
        dragContract.setUpdateUser(userId);
        dragContract.setUpdateTime(System.currentTimeMillis());
        contractMapper.updateById(dragContract);

        updateStatusSnapshot(request.getDragNodeId(), request.getStage(), null);

    }

    /**
     * 处理旧版本审批状态 (APPROVING => NONE)
     */
    public void handleOldApprovalData() {
        List<Contract> contracts = contractMapper.selectListByLambda(new LambdaQueryWrapper<Contract>().eq(Contract::getApprovalStatus, ApprovalStatus.APPROVING.name()));
        contracts.forEach(contract -> {
            ResourceSnapshotApprovalParam param = ResourceSnapshotApprovalParam.builder().resourceId(contract.getId()).approvalStatus(ApprovalStatus.NONE.name()).build();
            updateSnapshotApprovalStatus(param);
        });
        extContractMapper.updateOldApprovalStatusNone();
    }
}
