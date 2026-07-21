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
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.dto.*;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.constants.ContractPaymentPlanStatus;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.domain.ContractPaymentPlanField;
import cn.cordys.crm.contract.domain.ContractPaymentPlanFieldBlob;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanGetResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.dto.response.CustomerPaymentPlanStatisticResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ImportRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ContractPaymentPlanService {
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private BaseMapper<ContractPaymentPlan> contractPaymentPlanMapper;
    @Resource
    private ExtContractPaymentPlanMapper extContractPaymentPlanMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ContractPaymentPlanFieldService contractPaymentPlanFieldService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private BaseService baseService;
    @Resource
    private LogService logService;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private BaseMapper<ContractPaymentPlanField> contractPaymentPlanFieldMapper;
    @Resource
    private BaseMapper<ContractPaymentPlanFieldBlob> contractPaymentPlanFieldBlobMapper;


    public PagerWithOption<List<ContractPaymentPlanListResponse>> list(ContractPaymentPlanPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ContractPaymentPlanListResponse> list = extContractPaymentPlanMapper.list(request, userId, orgId, deptDataPermission);
        list = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list);
        return PageUtils.setPageInfoWithOption(page, list, optionMap);
    }

    public ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), orgId);
    }

    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<ContractPaymentPlanListResponse> list) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ContractPaymentPlanListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(list,
                ContractPaymentPlanListResponse::getOwner, ContractPaymentPlanListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_PLAN_OWNER.getBusinessKey(), ownerFieldOption);

        // 合同
        List<OptionDTO> contractFieldOption = moduleFormService.getBusinessFieldOption(list,
                ContractPaymentPlanListResponse::getContractId, ContractPaymentPlanListResponse::getContractName);
        optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_PLAN_CONTRACT.getBusinessKey(), contractFieldOption);

        return optionMap;
    }

    public List<ContractPaymentPlanListResponse> buildListData(List<ContractPaymentPlanListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> planIds = list.stream().map(ContractPaymentPlanListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = contractPaymentPlanFieldService.getResourceFieldMap(planIds, true);
        Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = contractPaymentPlanFieldService.setBusinessRefFieldValue(list,
                moduleFormService.getFlattenFormFields(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), orgId), caseCustomFiledMap);


        List<String> ownerIds = list.stream()
                .map(ContractPaymentPlanListResponse::getOwner)
                .distinct()
                .toList();

        List<String> createUserIds = list.stream()
                .map(ContractPaymentPlanListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(ContractPaymentPlanListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        list.forEach(planListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> contractPaymentPlanFields = resolvefieldValueMap.get(planListResponse.getId());
            planListResponse.setModuleFields(contractPaymentPlanFields);

            UserDeptDTO userDeptDTO = userDeptMap.get(planListResponse.getOwner());
            if (userDeptDTO != null) {
                planListResponse.setDepartmentId(userDeptDTO.getDeptId());
                planListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            String createUserName = baseService.getAndCheckOptionName(userNameMap.get(planListResponse.getCreateUser()));
            planListResponse.setCreateUserName(createUserName);
            String updateUserName = baseService.getAndCheckOptionName(userNameMap.get(planListResponse.getUpdateUser()));
            planListResponse.setUpdateUserName(updateUserName);
            planListResponse.setOwnerName(userNameMap.get(planListResponse.getOwner()));
        });

        return list;
    }

    public ContractPaymentPlanGetResponse get(String id) {
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(id);
        ContractPaymentPlanGetResponse contractPaymentPlanGetResponse = BeanUtils.copyBean(new ContractPaymentPlanGetResponse(), contractPaymentPlan);
        contractPaymentPlanGetResponse = baseService.setCreateUpdateOwnerUserName(contractPaymentPlanGetResponse);
        Contract contract = contractMapper.selectByPrimaryKey(contractPaymentPlanGetResponse.getContractId());
        // 获取模块字段
        List<BaseModuleFieldValue> contractPaymentPlanFields = contractPaymentPlanFieldService.getModuleFieldValuesByResourceId(id);
        contractPaymentPlanFields = contractPaymentPlanFieldService.setBusinessRefFieldValue(List.of(contractPaymentPlanGetResponse),
                moduleFormService.getFlattenFormFields(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), contractPaymentPlan.getOrganizationId()), new HashMap<>(Map.of(id, contractPaymentPlanFields))).get(id);
        ModuleFormConfigDTO contractPaymentPlanFormConfig = getFormConfig(contractPaymentPlan.getOrganizationId());

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(contractPaymentPlanFormConfig, contractPaymentPlanFields);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(contractPaymentPlanGetResponse,
                ContractPaymentPlanGetResponse::getOwner, ContractPaymentPlanGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        if (contract != null) {
            contractPaymentPlanGetResponse.setContractName(contract.getName());
            // 合同
            List<OptionDTO> contractFieldOption = moduleFormService.getBusinessFieldOption(contractPaymentPlanGetResponse,
                    ContractPaymentPlanGetResponse::getContractId, ContractPaymentPlanGetResponse::getContractName);
            optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_PLAN_CONTRACT.getBusinessKey(), contractFieldOption);
        }

        contractPaymentPlanGetResponse.setOptionMap(optionMap);
        contractPaymentPlanGetResponse.setModuleFields(contractPaymentPlanFields);

        if (contractPaymentPlanGetResponse.getOwner() != null) {
            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(contractPaymentPlanGetResponse.getOwner(), contractPaymentPlan.getOrganizationId());
            if (userDeptDTO != null) {
                contractPaymentPlanGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                contractPaymentPlanGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        // 附件信息
        contractPaymentPlanGetResponse.setAttachmentMap(moduleFormService.getAttachmentMap(contractPaymentPlanFormConfig, contractPaymentPlanFields));

        return contractPaymentPlanGetResponse;
    }

    /**
     * 获取跟进计划详情 （⚠️反射调用; 勿修改入参, 返回, 方法名!）
     *
     * @param id 计划ID
     * @return 计划详情
     */
    public ContractPaymentPlanGetResponse getSimple(String id) {
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(id);
        if (contractPaymentPlan == null) {
            return null;
        }
        ContractPaymentPlanGetResponse response = BeanUtils.copyBean(new ContractPaymentPlanGetResponse(), contractPaymentPlan);
        List<BaseModuleFieldValue> fvs = contractPaymentPlanFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fvs);
        return response;
    }

    /**
     * 批量获取回款计划详情 (用于数据源批量查询优化)
     *
     * @param ids 计划ID集合
     * @return 回款计划详情列表
     */
    public List<ContractPaymentPlanGetResponse> batchGetSimpleByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ContractPaymentPlan> plans = contractPaymentPlanMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(plans)) {
            return Collections.emptyList();
        }
        Map<String, List<BaseModuleFieldValue>> fieldValueMap = contractPaymentPlanFieldService.getResourceFieldMap(ids, true);

        return plans.stream().map(plan -> {
            ContractPaymentPlanGetResponse response = BeanUtils.copyBean(new ContractPaymentPlanGetResponse(), plan);
            response.setModuleFields(fieldValueMap.get(plan.getId()));
            return response;
        }).toList();
    }

    @OperationLog(module = LogModule.CONTRACT_PAYMENT, type = LogType.ADD, operator = "{#userId}")
    public ContractPaymentPlan add(ContractPaymentPlanAddRequest request, String userId, String orgId) {
        ContractPaymentPlan contractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), request);
        if (StringUtils.isBlank(request.getOwner())) {
            contractPaymentPlan.setOwner(userId);
        }
        if (StringUtils.isBlank(request.getPlanStatus())) {
            contractPaymentPlan.setPlanStatus(ContractPaymentPlanStatus.PENDING.name());
        }
        contractPaymentPlan.setCreateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser(userId);
        contractPaymentPlan.setCreateUser(userId);
        contractPaymentPlan.setOrganizationId(orgId);
        contractPaymentPlan.setId(IDGenerator.nextStr());
        // 保存自定义字段
        contractPaymentPlanFieldService.saveModuleField(contractPaymentPlan, orgId, userId, request.getModuleFields(), false);
        contractPaymentPlanMapper.insert(contractPaymentPlan);
        // 日志
        baseService.handleAddLogWithResourceName(contractPaymentPlan, request.getModuleFields());
        return contractPaymentPlan;
    }

    @OperationLog(module = LogModule.CONTRACT_PAYMENT, type = LogType.UPDATE, resourceId = "{#request.id}")
    public ContractPaymentPlan update(ContractPaymentPlanUpdateRequest request, String userId, String orgId) {
        ContractPaymentPlan originContractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(request.getId());
        ContractPaymentPlan contractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), request);
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser(userId);

        // 获取模块字段
        List<BaseModuleFieldValue> originContractPaymentPlanFields = List.of();
        if (request.getModuleFields() != null) {
            originContractPaymentPlanFields = contractPaymentPlanFieldService.getModuleFieldValuesByResourceId(request.getId());
        }

        if (BooleanUtils.isTrue(request.getAgentInvoke())) {
            contractPaymentPlanFieldService.updateModuleFieldByAgent(contractPaymentPlan, originContractPaymentPlanFields, request.getModuleFields(), orgId, userId);
        } else {
            // 更新模块字段
            updateModuleField(contractPaymentPlan, request.getModuleFields(), orgId, userId);
        }

        contractPaymentPlanMapper.update(contractPaymentPlan);

        contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(request.getId());

        baseService.handleUpdateLog(originContractPaymentPlan, contractPaymentPlan, originContractPaymentPlanFields, request.getModuleFields(), originContractPaymentPlan.getId(), originContractPaymentPlan.getName());
        return contractPaymentPlan;
    }

    private void updateModuleField(ContractPaymentPlan contractPaymentPlan, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        contractPaymentPlanFieldService.deleteByResourceId(contractPaymentPlan.getId());
        // 再保存
        contractPaymentPlanFieldService.saveModuleField(contractPaymentPlan, orgId, userId, moduleFields, true);
    }

    @OperationLog(module = LogModule.CONTRACT_PAYMENT, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        ContractPaymentPlan originContractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(id);
        Contract contract = contractMapper.selectByPrimaryKey(originContractPaymentPlan.getContractId());

        String resourceName = contract == null ? originContractPaymentPlan.getContractId() : contract.getName();

        contractPaymentPlanMapper.deleteByPrimaryKey(id);

        // 设置操作对象
        OperationLogContext.setResourceName(resourceName);
    }

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CONTRACT_PAYMENT_PLAN_READ, rolePermissions);
    }

    public CustomerPaymentPlanStatisticResponse calculateCustomerPaymentPlanStatistic(String accountId, String userId, String organizationId, DeptDataPermissionDTO deptDataPermission) {
        return extContractPaymentPlanMapper.calculateCustomerPaymentPlanStatistic(accountId, userId, organizationId, deptDataPermission);
    }

    /**
     * 通过名称获取回款计划集合
     *
     * @param names 名称集合
     * @return 回款计划集合
     */
    public List<ContractPaymentPlan> getPlanListByNames(List<String> names) {
        LambdaQueryWrapper<ContractPaymentPlan> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(ContractPaymentPlan::getName, names);
        return contractPaymentPlanMapper.selectListByLambda(lambdaQueryWrapper);
    }

    public String getPlanName(String id) {
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(id);
        if (contractPaymentPlan != null) {
            return contractPaymentPlan.getName();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 通过ID集合获取回款计划名称
     *
     * @param ids id集合
     * @return 回款计划名称
     */
    public String getPlanNameByIds(List<String> ids) {
        List<ContractPaymentPlan> plans = contractPaymentPlanMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(plans)) {
            List<String> names = plans.stream().map(ContractPaymentPlan::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }


    /**
     * 下载导入模板
     *
     * @param response
     * @param currentOrg
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
        new EasyExcelExporter()
                .exportMultiSheetTplWithSharedHandler(response, moduleFormService.getCustomImportHeadsNoRef(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), currentOrg),
                        Translator.get("payment.plan.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
                        new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), currentOrg)),
                        new CustomHeadColWidthStyleStrategy());
    }


    /**
     * 导入检查
     *
     * @param file
     * @param currentOrg
     * @return
     */
    public ImportResponse importPreCheck(MultipartFile file, String importType, String currentOrg) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        return checkImportExcel(file, importType, currentOrg);
    }


    /**
     * 检查导入文件
     *
     * @param file
     * @param currentOrg
     * @return
     */
    private ImportResponse checkImportExcel(MultipartFile file, String importType, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), currentOrg);
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "contract_payment_plan", "contract_payment_plan_field", currentOrg, importType);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("customer import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }


    /**
     * 回款计划导出
     *
     * @param file
     * @param request
     * @param currentOrg
     * @param currentUser
     * @return
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResponse realImport(MultipartFile file, ImportRequest request, String currentOrg, String currentUser) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), currentOrg);
            CustomImportAfterDoConsumer<ContractPaymentPlan, BaseResourceSubField> afterDo = (paymentPlans, paymentPlanFields, paymentPlanFieldBlobs) -> {
                var logs = new ArrayList<LogDTO>();
                ImportType importType = EnumUtils.valueOf(ImportType.class, request.getImportType());
                switch (importType) {
                    case ADD -> {
                        paymentPlans.forEach(paymentPlan -> {
                            paymentPlan.setPlanStatus(ContractPaymentPlanStatus.PENDING.name());
                            logs.add(new LogDTO(currentOrg, paymentPlan.getId(), currentUser, LogType.ADD, LogModule.CONTRACT_PAYMENT, paymentPlan.getName()));
                        });
                        contractPaymentPlanMapper.batchInsert(paymentPlans);
                        contractPaymentPlanFieldMapper.batchInsert(paymentPlanFields.stream().map(field -> BeanUtils.copyBean(new ContractPaymentPlanField(), field)).toList());
                        contractPaymentPlanFieldBlobMapper.batchInsert(paymentPlanFieldBlobs.stream().map(field -> BeanUtils.copyBean(new ContractPaymentPlanFieldBlob(), field)).toList());
                        // record logs
                        logService.batchAdd(logs);
                    }
                    case UPDATE -> {
                        List<String> ids = paymentPlans.stream().map(ContractPaymentPlan::getId).toList();
                        if (CollectionUtils.isEmpty(ids)) {
                            break;
                        }
                        //原数据
                        List<ContractPaymentPlan> originPaymentPlanList = contractPaymentPlanMapper.selectByIds(ids);
                        if (CollectionUtils.isEmpty(originPaymentPlanList)) {
                            break;
                        }
                        Map<String, ContractPaymentPlan> originPaymentPlanMaps = originPaymentPlanList.stream().collect(Collectors.toMap(ContractPaymentPlan::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> originFieldValueMap = contractPaymentPlanFieldService.getResourceFieldMap(ids, true);

                        List<ContractPaymentPlanField> insertField = new ArrayList<>();
                        List<ContractPaymentPlanFieldBlob> insertFieldBlob = new ArrayList<>();
                        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
                        ExtContractPaymentPlanMapper paymentPlanBatchMapper = sqlSession.getMapper(ExtContractPaymentPlanMapper.class);
                        CommonMapper commonMapper = sqlSession.getMapper(CommonMapper.class);

                        if (CollectionUtils.isNotEmpty(paymentPlans)) {
                            paymentPlans.forEach(paymentPlan -> {
                                paymentPlanBatchMapper.updatePaymentPlan(paymentPlan);
                            });
                        }

                        if (CollectionUtils.isNotEmpty(paymentPlanFields)) {
                            List<ContractPaymentPlanField> fieldList = contractPaymentPlanFieldMapper.selectByIds(paymentPlanFields.stream().map(BaseResourceSubField::getId).toList());
                            Map<String, ContractPaymentPlanField> fieldMap = fieldList.stream().collect(Collectors.toMap(ContractPaymentPlanField::getId, Function.identity()));
                            paymentPlanFields.forEach(planField -> {
                                if (fieldMap.containsKey(planField.getId())) {
                                    commonMapper.updateCustomerField("contract_payment_plan_field", planField);
                                } else {
                                    insertField.add(BeanUtils.copyBean(new ContractPaymentPlanField(), planField));
                                }
                            });
                        }

                        if (CollectionUtils.isNotEmpty(paymentPlanFieldBlobs)) {
                            List<ContractPaymentPlanFieldBlob> blobList = contractPaymentPlanFieldBlobMapper.selectByIds(paymentPlanFieldBlobs.stream().map(BaseResourceSubField::getId).toList());
                            Map<String, ContractPaymentPlanFieldBlob> blobMap = blobList.stream().collect(Collectors.toMap(ContractPaymentPlanFieldBlob::getId, Function.identity()));
                            paymentPlanFieldBlobs.forEach(planFieldBlob -> {
                                if (blobMap.containsKey(planFieldBlob.getId())) {
                                    commonMapper.updateCustomerField("contract_payment_plan_field_blob", planFieldBlob);
                                } else {
                                    insertFieldBlob.add(BeanUtils.copyBean(new ContractPaymentPlanFieldBlob(), planFieldBlob));
                                }
                            });

                        }

                        sqlSession.flushStatements();
                        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);

                        if (CollectionUtils.isNotEmpty(insertField)) {
                            contractPaymentPlanFieldMapper.batchInsert(insertField);
                        }
                        if (CollectionUtils.isNotEmpty(insertFieldBlob)) {
                            contractPaymentPlanFieldBlobMapper.batchInsert(insertFieldBlob);
                        }

                        SqlSession currentSession =
                                SqlSessionUtils.getSqlSession(sqlSessionFactory);
                        currentSession.clearCache();

                        Map<String, ContractPaymentPlan> modifiedPaymentPlanMaps = contractPaymentPlanMapper.selectByIds(ids).stream().collect(Collectors.toMap(ContractPaymentPlan::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> modifiedFieldValueMap = contractPaymentPlanFieldService.getResourceFieldMap(ids, true);

                        ids.forEach(id -> {
                            ContractPaymentPlan originDate = originPaymentPlanMaps.get(id);
                            ContractPaymentPlan modifiedDate = modifiedPaymentPlanMaps.get(id);
                            baseService.handleUpdateLog(originDate, modifiedDate, originFieldValueMap.get(id), modifiedFieldValueMap.get(id), id, modifiedDate.getName());
                            LogContextInfo contextInfo = OperationLogContext.getContext();
                            if (contextInfo != null) {
                                LogDTO logDTO = new LogDTO(currentOrg, id, currentUser, LogType.UPDATE, LogModule.CONTRACT_PAYMENT, modifiedDate.getName());
                                logDTO.setOriginalValue(contextInfo.getOriginalValue());
                                logDTO.setModifiedValue(contextInfo.getModifiedValue());
                                logs.add(logDTO);
                                OperationLogContext.clear();
                            }
                        });
                        logService.batchAdd(logs);

                    }
                }
            };
            CustomFieldImportEventListener<ContractPaymentPlan> eventListener = new CustomFieldImportEventListener<>(fields, ContractPaymentPlan.class, currentOrg, currentUser,
                    "contract_payment_plan_field", "contract_payment_plan_field_blob", afterDo, 2000, null, null, request.getImportType());
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("customer import error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }

    }
}