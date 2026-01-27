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
import cn.cordys.common.dto.condition.FilterCondition;
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
import cn.cordys.crm.contract.constants.ContractStage;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractPaymentRecord;
import cn.cordys.crm.contract.domain.ContractSnapshot;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.ContractGetResponse;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.dto.response.CustomerContractStatisticResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractSnapshotMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.opportunity.constants.ApprovalState;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTaskConfig;
import cn.cordys.crm.system.dto.MessageTaskConfigDTO;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.BatchAffectSkipResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
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
    private LogService logService;
    @Resource
    private SerialNumGenerator serialNumGenerator;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private BaseMapper<MessageTaskConfig> messageTaskConfigMapper;
    @Resource
    private DataScopeService dataScopeService;
	@Resource
	private BaseMapper<ContractPaymentRecord> contractPaymentRecordMapper;


    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");

    /**
     * 新建合同
     *
     * @param request
     * @param operatorId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.ADD, resourceName = "{#request.name}")
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
        Contract contract = new Contract();
        String id = IDGenerator.nextStr();
        contract.setId(id);
        contract.setName(request.getName());
        contract.setCustomerId(request.getCustomerId());
        contract.setOwner(request.getOwner());
        contract.setNumber(createContractNumber(moduleFormConfigDTO, orgId));
        contract.setStage(ContractStage.PENDING_SIGNING.name());
        contract.setOrganizationId(orgId);
        contract.setApprovalStatus(ContractApprovalStatus.APPROVING.name());
        contract.setStartTime(request.getStartTime());
        contract.setEndTime(request.getEndTime());
        contract.setCreateTime(System.currentTimeMillis());
        contract.setCreateUser(operatorId);
        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(operatorId);

        //计算子产品总金额
        setAmount(request.getProducts(), contract);

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


    private String createContractNumber(ModuleFormConfigDTO moduleFormConfigDTO, String orgId) {
        BaseField numberField = moduleFormConfigDTO.getFields().stream()
                .filter(field -> field.isSerialNumber() && StringUtils.isNotEmpty(field.getBusinessKey())).findFirst().orElse(null);

        if (numberField != null) {
            BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
            fieldValue.setFieldId(numberField.getId());
            return serialNumGenerator.generateByRules(((SerialNumberField) numberField).getSerialNumberRules(), orgId, FormKey.CONTRACT.getKey());
        }
        return null;
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

    public ContractGetResponse getWithDataPermissionCheck(String id, String userId, String orgId) {
        ContractGetResponse getResponse = get(id);
        if (getResponse == null) {
            throw new GenericException(Translator.get("resource.not.exist"));
        }
        dataScopeService.checkDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.CONTRACT_READ);
        return getResponse;
    }

    public ContractGetResponse getSnapshotWithDataPermissionCheck(String id, String userId, String orgId) {
        ContractGetResponse getResponse = getSnapshot(id);
        if (getResponse == null) {
            throw new GenericException(Translator.get("resource.not.exist"));
        }
        dataScopeService.checkDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.CONTRACT_READ);
        return getResponse;
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
    public ContractGetResponse get(String id) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        // 获取模块字段
        ModuleFormConfigDTO contractFormConfig = getFormConfig(contract.getOrganizationId());
        List<BaseModuleFieldValue> contractFields = contractFieldService.getModuleFieldValuesByResourceId(id);
        return get(contract, contractFields, contractFormConfig);
    }


    /**
     * 计算子产品总金额
     *
     * @param products 子产品列表
     * @param contract 合同对象
     */
    private void setAmount(List<Map<String, Object>> products, Contract contract) {
        BigDecimal totalAmount = products.stream()
                .map(product -> new BigDecimal(product.get("amount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new GenericException(Translator.get("contract.amount.exceed.max"));
        }
        contract.setAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
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
            contract.setAmount(oldContract.getAmount());
            contract.setStage(oldContract.getStage());
            contract.setApprovalStatus(ContractApprovalStatus.APPROVING.name());
            //计算子产品总金额
            setAmount(request.getProducts(), contract);
            // 设置子表格字段值
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
     * @param id
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }

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
    public ContractGetResponse getSnapshot(String id) {
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

        list.forEach(item -> {
            item.setOwnerName(userNameMap.get(item.getOwner()));
            UserDeptDTO userDeptDTO = userDeptMap.get(item.getOwner());
            if (userDeptDTO != null) {
                item.setDepartmentId(userDeptDTO.getDeptId());
                item.setDepartmentName(userDeptDTO.getDeptName());
            }
            // 获取自定义字段
            List<BaseModuleFieldValue> contractFields = resolvefieldValueMap.get(item.getId());
            item.setModuleFields(contractFields);
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
    public void updateStage(ContractStageRequest request, String userId, String orgId) {
        Contract contract = contractMapper.selectByPrimaryKey(request.getId());
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        if (!Strings.CI.equals(contract.getApprovalStatus(), ContractApprovalStatus.APPROVED.name())) {
            throw new GenericException(Translator.get("contract.unapproved.cannot.edit"));
        }

        Map<String, String> oldMap = new HashMap<>();
        oldMap.put("contractStage", Translator.get("contract.stage." + contract.getStage().toLowerCase()));

        contract.setStage(request.getStage());
        if (StringUtils.isNotBlank(request.getVoidReason())) {
            contract.setVoidReason(request.getVoidReason());
        }

        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(userId);
        contractMapper.update(contract);

        updateStatusSnapshot(request.getId(), request.getStage(), null);

        LogDTO logDTO = new LogDTO(orgId, request.getId(), userId, LogType.UPDATE, LogModule.CONTRACT_INDEX, contract.getName());
        Map<String, String> newMap = new HashMap<>();
        newMap.put("contractStage", Translator.get("contract.stage." + request.getStage().toLowerCase()));
        logDTO.setOriginalValue(oldMap);
        logDTO.setModifiedValue(newMap);
        logService.add(logDTO);

        if (Strings.CI.equals(request.getStage(), ContractStage.VOID.name()) || Strings.CI.equals(request.getStage(), ContractStage.ARCHIVED.name())) {
            String event = Strings.CI.equals(request.getStage(), ContractStage.VOID.name()) ?
                    NotificationConstants.Event.CONTRACT_VOID : NotificationConstants.Event.CONTRACT_ARCHIVED;
            Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
            sendNotice(contract, userId, orgId, event, customer.getName());
        }

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

    public CustomerContractStatisticResponse calculateContractStatisticByCustomerId(String customerId, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        return extContractMapper.calculateContractStatisticByCustomerId(customerId, userId, orgId, deptDataPermission);
    }


    /**
     * 审核通过/不通过
     *
     * @param request
     * @param userId
     */
    public void approvalContract(ContractApprovalRequest request, String userId, String orgId) {
        Contract contract = contractMapper.selectByPrimaryKey(request.getId());
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        String state = contract.getApprovalStatus();
        contract.setApprovalStatus(request.getApprovalStatus());
        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(userId);
        contractMapper.update(contract);

        updateStatusSnapshot(request.getId(), null, request.getApprovalStatus());

        // 添加日志上下文
        LogDTO logDTO = getApprovalLogDTO(orgId, request.getId(), userId, contract.getName(), state, request.getApprovalStatus());
        logService.add(logDTO);
    }

    public String revoke(String id, String userId, String orgId) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        String originApprovalStatus = contract.getApprovalStatus();
        if (!Strings.CI.equals(contract.getCreateUser(), userId) || !Strings.CI.equals(contract.getApprovalStatus(), ApprovalState.APPROVING.toString())) {
            return contract.getApprovalStatus();
        }
        contract.setApprovalStatus(ApprovalState.REVOKED.toString());
        contract.setUpdateUser(userId);
        contract.setUpdateTime(System.currentTimeMillis());
        contractMapper.update(contract);

        //更新快照
        updateStatusSnapshot(id, null, ApprovalState.REVOKED.toString());

        // 添加日志上下文
        LogDTO logDTO = getApprovalLogDTO(orgId, id, userId, contract.getName(), originApprovalStatus, ApprovalState.REVOKED.toString());
        logService.add(logDTO);

        return contract.getApprovalStatus();
    }


    /**
     * 批量审核
     *
     * @param request
     * @param userId
     * @param orgId
     */
    public BatchAffectSkipResponse batchApprovalContract(ContractApprovalBatchRequest request, String userId, String orgId) {
        List<String> ids = extContractMapper.selectByStatusAndIds(request.getIds(), ContractApprovalStatus.APPROVING.name());

        if (CollectionUtils.isEmpty(ids)) {
            return BatchAffectSkipResponse.builder().success(0).fail(0).skip(request.getIds().size()).build();
        }
        LambdaQueryWrapper<ContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ContractSnapshot::getContractId, ids);
        List<ContractSnapshot> contractSnapshots = snapshotBaseMapper.selectListByLambda(wrapper);
        Map<String, ContractSnapshot> snapshotsMaps = contractSnapshots.stream().collect(Collectors.toMap(ContractSnapshot::getContractId, Function.identity()));

        List<LogDTO> logs = new ArrayList<>();
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtContractMapper batchUpdateMapper = sqlSession.getMapper(ExtContractMapper.class);
        ExtContractSnapshotMapper snapshotMapper = sqlSession.getMapper(ExtContractSnapshotMapper.class);

        ids.forEach(id -> {
            batchUpdateMapper.updateStatus(id, request.getApprovalStatus(), userId, System.currentTimeMillis());
            ContractSnapshot contractSnapshot = snapshotsMaps.get(id);
            ContractGetResponse response = JSON.parseObject(contractSnapshot.getContractValue(), ContractGetResponse.class);
            String state = response.getApprovalStatus();
            response.setApprovalStatus(request.getApprovalStatus());
            contractSnapshot.setContractValue(JSON.toJSONString(response));
            snapshotMapper.update(contractSnapshot);
            LogDTO logDTO = getApprovalLogDTO(orgId, id, userId, response.getName(), state, request.getApprovalStatus());
            logs.add(logDTO);
        });
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
        logService.batchAdd(logs);

        return BatchAffectSkipResponse.builder().success(ids.size()).fail(0).skip(request.getIds().size() - ids.size()).build();
    }

    private LogDTO getApprovalLogDTO(String orgId, String id, String userId, String response, String state, String newState) {
        LogDTO logDTO = new LogDTO(orgId, id, userId, LogType.APPROVAL, LogModule.CONTRACT_INDEX, response);
        Map<String, String> oldMap = new HashMap<>();
        oldMap.put("approvalStatus", Translator.get("contract.approval_status." + state.toLowerCase()));
        logDTO.setOriginalValue(oldMap);
        Map<String, String> newMap = new HashMap<>();
        newMap.put("approvalStatus", Translator.get("contract.approval_status." + newState.toLowerCase()));
        logDTO.setModifiedValue(newMap);
        return logDTO;
    }

    public String getContractName(String id) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(contract).map(Contract::getName).orElse(null);
    }

    public Contract selectByPrimaryKey(String id) {
        return contractMapper.selectByPrimaryKey(id);
    }

    public ModuleFormConfigDTO getBusinessFormConfig(String orgId) {
        ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT.getKey(), orgId);
        // 添加合同金额字段
        formConfig.getFields().add(moduleFormService.getContractAmountField());
        return formConfig;
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
	 * 设置默认的数据源搜索条件
	 * @return 搜索条件
	 */
	public List<FilterCondition> getDefaultSourceFilters() {
		// 只展示状态为通过且非作废/归档阶段的合同
		List<FilterCondition> conditions = new ArrayList<>();

		FilterCondition statusCondition = new FilterCondition();
		statusCondition.setMultipleValue(false);
		statusCondition.setName("approvalStatus");
		statusCondition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
		statusCondition.setValue(List.of(ContractApprovalStatus.APPROVED.name()));
		conditions.add(statusCondition);

		FilterCondition stageCondition = new FilterCondition();
		stageCondition.setMultipleValue(false);
		stageCondition.setName("stage");
		stageCondition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
		stageCondition.setValue(List.of(ContractStage.PENDING_SIGNING.name(), ContractStage.SIGNED.name(),
				ContractStage.IN_PROGRESS.name(), ContractStage.COMPLETED_PERFORMANCE.name()));
		conditions.add(stageCondition);

		return conditions;
	}

	/**
	 * 计算合同已回款金额
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
}
