package cn.cordys.crm.opportunity.service;

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
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.dto.response.CustomerContactListAllResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.opportunity.constants.OpportunityStageType;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityField;
import cn.cordys.crm.opportunity.domain.OpportunityFieldBlob;
import cn.cordys.crm.opportunity.domain.OpportunityRule;
import cn.cordys.crm.opportunity.dto.request.*;
import cn.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunitySearchStatisticResponse;
import cn.cordys.crm.opportunity.dto.response.StageConfigResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.DictService;
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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityService {

    public static final String SUCCESS = "SUCCESS";
    public static final Long DEFAULT_POS = 1L;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private OpportunityFieldService opportunityFieldService;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;
    @Autowired
    private OpportunityRuleService opportunityRuleService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private ProductService productService;
    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private DictService dictService;
    @Resource
    private BaseMapper<OpportunityField> opportunityFieldMapper;
    @Resource
    private BaseMapper<OpportunityFieldBlob> opportunityFieldBlobMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private ExtOpportunityStageConfigMapper extOpportunityStageConfigMapper;

    public PagerWithOption<List<OpportunityListResponse>> list(OpportunityPageRequest request, String userId, String orgId,
                                                               DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<OpportunityListResponse> list = extOpportunityMapper.list(request, orgId, userId, deptDataPermission);
        List<OpportunityListResponse> buildList = buildListData(list, orgId);

        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);


        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public OpportunitySearchStatisticResponse searchStatistic(OpportunitySearchStatisticRequest request, String userId, String orgId,
                                                              DeptDataPermissionDTO deptDataPermission) {
        OpportunitySearchStatisticResponse response = extOpportunityMapper.searchStatistic(request, orgId, userId, deptDataPermission);
        return Optional.ofNullable(response).orElse(new OpportunitySearchStatisticResponse());
    }

    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<OpportunityListResponse> list, List<OpportunityListResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, OpportunityListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                OpportunityListResponse::getOwner, OpportunityListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                OpportunityListResponse::getContactId, OpportunityListResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);

        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);

        return optionMap;

    }

    private ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), orgId);
    }

    public List<OpportunityListResponse> buildListData(List<OpportunityListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> opportunityIds = list.stream().map(OpportunityListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> opportunityFiledMap = opportunityFieldService.getResourceFieldMap(opportunityIds, true);

        List<String> ownerIds = list.stream()
                .map(OpportunityListResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = list.stream()
                .map(OpportunityListResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = list.stream()
                .map(OpportunityListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(OpportunityListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        List<String> contactIds = list.stream()
                .map(OpportunityListResponse::getContactId)
                .distinct()
                .toList();
        Map<String, String> contactMap = baseService.getContactMap(contactIds);

        Map<String, OpportunityRule> ownersDefaultRuleMap = opportunityRuleService.getOwnersDefaultRuleMap(ownerIds, orgId);
        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        List<StageConfigResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        Map<String, StageConfigResponse> endConfigMaps = stageConfigList.stream().filter(config ->
                Strings.CI.equals(config.getType(), OpportunityStageType.END.name())
        ).collect(Collectors.toMap(StageConfigResponse::getId, Function.identity()));

        // 失败原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.OPPORTUNITY_FAIL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> opportunityFields = opportunityFiledMap.get(opportunityListResponse.getId());
            // 计算保留天数(成功失败阶段不计算)
            opportunityListResponse.setReservedDays(endConfigMaps.containsKey(opportunityListResponse.getStage()) ?
                    null : opportunityRuleService.calcReservedDay(ownersDefaultRuleMap.get(opportunityListResponse.getOwner()), opportunityListResponse.getCreateTime()));
            opportunityListResponse.setModuleFields(opportunityFields);

            opportunityListResponse.setFollowerName(userNameMap.get(opportunityListResponse.getFollower()));
            opportunityListResponse.setCreateUserName(userNameMap.get(opportunityListResponse.getCreateUser()));
            opportunityListResponse.setUpdateUserName(userNameMap.get(opportunityListResponse.getUpdateUser()));
            opportunityListResponse.setOwnerName(userNameMap.get(opportunityListResponse.getOwner()));
            opportunityListResponse.setContactName(contactMap.get(opportunityListResponse.getContactId()));

            UserDeptDTO userDeptDTO = userDeptMap.get(opportunityListResponse.getOwner());
            if (userDeptDTO != null) {
                opportunityListResponse.setDepartmentId(userDeptDTO.getDeptId());
                opportunityListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            opportunityListResponse.setFailureReason(dictMap.get(opportunityListResponse.getFailureReason()));
        });
        return baseService.setCreateAndUpdateUserName(list);
    }


    /**
     * 新建商机
     *
     * @param request
     * @param operatorId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.OPPORTUNITY_INDEX, type = LogType.ADD, resourceName = "{#request.name}")
    public Opportunity add(OpportunityAddRequest request, String operatorId, String orgId) {
        productService.checkProductList(request.getProducts());
        List<StageConfigResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        Long nextPos = getNextPos(orgId, stageConfigList.getFirst().getId());
        Opportunity opportunity = new Opportunity();
        String id = IDGenerator.nextStr();
        opportunity.setId(id);
        opportunity.setName(request.getName());
        opportunity.setCustomerId(request.getCustomerId());
        opportunity.setAmount(request.getAmount());
        opportunity.setPossible(request.getPossible());
        opportunity.setProducts(request.getProducts());
        opportunity.setOrganizationId(orgId);
        opportunity.setStage(stageConfigList.getFirst().getId());
        opportunity.setPos(nextPos);
        opportunity.setContactId(request.getContactId());
        opportunity.setOwner(request.getOwner());
        opportunity.setCreateTime(System.currentTimeMillis());
        opportunity.setCreateUser(operatorId);
        opportunity.setUpdateTime(System.currentTimeMillis());
        opportunity.setUpdateUser(operatorId);
        opportunity.setExpectedEndTime(request.getExpectedEndTime());
        opportunity.setFollower(request.getFollower());
        opportunity.setFollowTime(request.getFollowTime());
        if (StringUtils.isBlank(request.getOwner())) {
            opportunity.setOwner(operatorId);
        }

        //自定义字段
        opportunityFieldService.saveModuleField(opportunity, orgId, operatorId, request.getModuleFields(), false);
        opportunityMapper.insert(opportunity);

        baseService.handleAddLog(opportunity, request.getModuleFields());

        return opportunity;
    }

    private Long getNextPos(String orgId, String stage) {
        Long pos = extOpportunityMapper.selectNextPos(orgId, stage);
        return pos == null ? 1 : pos + 1;
    }


    /**
     * 更新商机
     *
     * @param request
     * @param userId
     * @param orgId
     */
    @OperationLog(module = LogModule.OPPORTUNITY_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public Opportunity update(OpportunityUpdateRequest request, String userId, String orgId) {
        Opportunity oldOpportunity = opportunityMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(oldOpportunity).ifPresentOrElse(item -> {
            Opportunity newOpportunity = BeanUtils.copyBean(new Opportunity(), item);
            productService.checkProductList(request.getProducts());
            //更新商机
            Opportunity updateOpportunity = newOpportunity(newOpportunity, request, userId);
            // 获取模块字段
            List<BaseModuleFieldValue> originCustomerFields = opportunityFieldService.getModuleFieldValuesByResourceId(request.getId());
            if (BooleanUtils.isTrue(request.getAgentInvoke())) {
                opportunityFieldService.updateModuleFieldByAgent(updateOpportunity, originCustomerFields, request.getModuleFields(), orgId, userId);
            } else {
                // 更新模块字段
                updateModuleField(updateOpportunity, request.getModuleFields(), orgId, userId);
            }
            extOpportunityMapper.updateIncludeNullById(updateOpportunity);
            baseService.handleUpdateLog(oldOpportunity, newOpportunity, originCustomerFields, request.getModuleFields(), oldOpportunity.getId(), oldOpportunity.getName());
        }, () -> {
            throw new GenericException("opportunity_not_found");
        });
        return oldOpportunity;
    }


    private Opportunity newOpportunity(Opportunity item, OpportunityUpdateRequest request, String userId) {
        item.setName(request.getName());
        item.setCustomerId(request.getCustomerId());
        item.setAmount(request.getAmount());
        item.setPossible(request.getPossible());
        item.setProducts(request.getProducts());
        item.setContactId(request.getContactId());
        item.setOwner(request.getOwner());
        item.setUpdateTime(System.currentTimeMillis());
        item.setUpdateUser(userId);
        item.setExpectedEndTime(request.getExpectedEndTime());
        return item;
    }


    private void updateModuleField(Opportunity opportunity, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        opportunityFieldService.deleteByResourceId(opportunity.getId());
        // 再保存
        opportunityFieldService.saveModuleField(opportunity, orgId, userId, moduleFields, true);
    }


    /**
     * 删除商机
     *
     * @param id
     */
    @OperationLog(module = LogModule.OPPORTUNITY_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(id);
        Optional.ofNullable(opportunity).ifPresentOrElse(item -> {
            opportunityMapper.deleteByPrimaryKey(opportunity.getId());
            opportunityFieldService.deleteByResourceId(opportunity.getId());
        }, () -> {
            throw new GenericException("opportunity_not_found");
        });
        // 添加日志上下文
        OperationLogContext.setResourceName(opportunity.getName());

        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                NotificationConstants.Event.BUSINESS_DELETED, opportunity.getName(), userId,
                orgId, List.of(opportunity.getOwner()), true);
    }


    /**
     * 商机转移
     */
    public void transfer(OpportunityTransferRequest request, String userId, String orgId) {
        List<StageConfigResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        // 过滤出成功阶段
        StageConfigResponse successConfig = stageConfigList.stream().filter(config ->
                Strings.CI.equals(config.getType(), OpportunityStageType.END.name()) && Strings.CI.equals(config.getRate(), "100")
        ).findFirst().get();

        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Opportunity::getId, request.getIds());
        wrapper.nq(Opportunity::getStage, successConfig.getId());
        List<Opportunity> opportunityList = opportunityMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(opportunityList)) {
            return;
        }
        List<String> ids = opportunityList.stream().map(Opportunity::getId).toList();

        long nextPos = getNextPos(orgId, stageConfigList.getFirst().getId());
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtOpportunityMapper batchUpdateMapper = sqlSession.getMapper(ExtOpportunityMapper.class);
        for (int i = 0; i < ids.size(); i++) {
            batchUpdateMapper.transfer(request.getOwner(), userId, ids.get(i), System.currentTimeMillis(), nextPos + i, stageConfigList.getFirst().getId());
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);

        // 记录日志
        List<LogDTO> logs = new ArrayList<>();
        opportunityList.forEach(opportunity -> {
            Customer originCustomer = new Customer();
            originCustomer.setOwner(opportunity.getOwner());
            Customer modifieCustomer = new Customer();
            modifieCustomer.setOwner(request.getOwner());
            LogDTO logDTO = new LogDTO(orgId, opportunity.getId(), userId, LogType.UPDATE, LogModule.OPPORTUNITY_INDEX, opportunity.getName());
            logDTO.setOriginalValue(originCustomer);
            logDTO.setModifiedValue(modifieCustomer);
            logs.add(logDTO);

            extCustomerContactMapper.updateContactById(opportunity.getContactId(), request.getOwner());
        });

        logService.batchAdd(logs);
        sendTransferNotice(opportunityList, request.getOwner(), userId, orgId);
    }

    private void sendTransferNotice(List<Opportunity> opportunityList, String toUser, String userId, String orgId) {
        opportunityList.forEach(opportunity -> commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                NotificationConstants.Event.BUSINESS_TRANSFER, opportunity.getName(), userId,
                orgId, List.of(toUser), true));
    }

    /**
     * 批量删除商机
     *
     * @param ids
     * @param userId
     */
    public void batchDelete(List<String> ids, String userId, String orgId) {
        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Opportunity::getId, ids);
        wrapper.nq(Opportunity::getStage, SUCCESS);
        List<Opportunity> opportunityList = opportunityMapper.selectListByLambda(wrapper);
        List<String> toDoIds = opportunityList.stream().map(Opportunity::getId).toList();
        if (CollectionUtils.isEmpty(toDoIds)) {
            return;
        }
        opportunityMapper.deleteByIds(toDoIds);
        opportunityFieldService.deleteByResourceIds(toDoIds);
        List<LogDTO> logs = new ArrayList<>();
        opportunityList.forEach(opportunity -> {
            LogDTO logDTO = new LogDTO(opportunity.getOrganizationId(), opportunity.getId(), userId, LogType.DELETE, LogModule.OPPORTUNITY_INDEX, opportunity.getName());
            logDTO.setOriginalValue(opportunity);
            logs.add(logDTO);
        });
        logService.batchAdd(logs);

        // 消息通知
        opportunityList.forEach(opportunity ->
                commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                        NotificationConstants.Event.BUSINESS_DELETED, opportunity.getName(), userId,
                        orgId, List.of(opportunity.getOwner()), true)
        );
    }


    /**
     * 商机详情
     *
     * @param id
     * @return
     */
    public OpportunityDetailResponse get(String id) {
        OpportunityDetailResponse response = extOpportunityMapper.getDetail(id);
        if (response == null) {
            throw new GenericException(Translator.get("opportunity_not_found"));
        }
        List<BaseModuleFieldValue> fieldValueList = opportunityFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fieldValueList);
        List<String> userIds = Stream.of(Arrays.asList(response.getCreateUser(), response.getUpdateUser(), response.getOwner(), response.getFollower()))
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);
        Map<String, String> contactMap = baseService.getContactMap(StringUtils.isEmpty(response.getContactId()) ? null : List.of(response.getContactId()));
        Map<String, OpportunityRule> ownersDefaultRuleMap = opportunityRuleService.getOwnersDefaultRuleMap(List.of(response.getOwner()), response.getOrganizationId());
        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(List.of(response.getOwner()), response.getOrganizationId());

        response.setCreateUserName(userNameMap.get(response.getCreateUser()));
        response.setUpdateUserName(userNameMap.get(response.getUpdateUser()));
        response.setOwnerName(userNameMap.get(response.getOwner()));
        response.setContactName(contactMap.get(response.getContactId()));
        response.setFollowerName(userNameMap.get(response.getFollower()));

        List<StageConfigResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(response.getOrganizationId());
        Map<String, StageConfigResponse> endConfigMaps = stageConfigList.stream().filter(config ->
                Strings.CI.equals(config.getType(), OpportunityStageType.END.name())
        ).collect(Collectors.toMap(StageConfigResponse::getId, Function.identity()));

        // 计算保留天数(成功失败阶段不计算)
        response.setReservedDays(endConfigMaps.containsKey(response.getStage()) ?
                null : opportunityRuleService.calcReservedDay(ownersDefaultRuleMap.get(response.getOwner()), response.getCreateTime()));
        UserDeptDTO userDeptDTO = userDeptMap.get(response.getOwner());
        if (userDeptDTO != null) {
            response.setDepartmentId(userDeptDTO.getDeptId());
            response.setDepartmentName(userDeptDTO.getDeptName());
        }

        // 失败原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.OPPORTUNITY_FAIL_RS.name(), response.getOrganizationId());
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));
        response.setFailureReason(dictMap.get(response.getFailureReason()));


        ModuleFormConfigDTO customerFormConfig = getFormConfig(response.getOrganizationId());
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, fieldValueList);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getOwner, OpportunityDetailResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getContactId, OpportunityDetailResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);

        List<OptionDTO> customerOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getCustomerId, OpportunityDetailResponse::getCustomerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CUSTOMER_NAME.getBusinessKey(), customerOption);

        List<OptionDTO> productOption = extProductMapper.getOptions(response.getOrganizationId());
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);

        response.setOptionMap(optionMap);

        // 附件信息
        response.setAttachmentMap(moduleFormService.getAttachmentMap(customerFormConfig, fieldValueList));

        return response;
    }


    /**
     * 标记商机阶段
     *
     * @param request
     * @param orgId
     */
    @OperationLog(module = LogModule.OPPORTUNITY_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void updateStage(OpportunityStageRequest request, String orgId) {
        final Opportunity oldOpportunity = opportunityMapper.selectByPrimaryKey(request.getId());
        if (oldOpportunity == null) {
            throw new GenericException(Translator.get("opportunity_not_found"));
        }

        final List<StageConfigResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);

        final Optional<StageConfigResponse> successOpt = stageConfigList.stream()
                .filter(cfg -> Strings.CI.equals(cfg.getType(), OpportunityStageType.END.name())
                        && Strings.CI.equals(cfg.getRate(), "100"))
                .findFirst();

        final Optional<StageConfigResponse> failOpt = stageConfigList.stream()
                .filter(cfg -> Strings.CI.equals(cfg.getType(), OpportunityStageType.END.name())
                        && Strings.CI.equals(cfg.getRate(), "0"))
                .findFirst();

        final Map<String, String> stageMap = stageConfigList.stream()
                .collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName));

        final Opportunity newOpportunity = new Opportunity();
        newOpportunity.setId(request.getId());
        newOpportunity.setLastStage(oldOpportunity.getStage());
        newOpportunity.setStage(request.getStage());

        final boolean isSuccessStage = successOpt.map(cfg -> Strings.CI.equals(request.getStage(), cfg.getId())).orElse(false);
        final boolean isFailStage = failOpt.map(cfg -> Strings.CI.equals(request.getStage(), cfg.getId())).orElse(false);

        if (isSuccessStage || isFailStage) {
            newOpportunity.setActualEndTime(System.currentTimeMillis());
        }
        if (isFailStage) {
            newOpportunity.setFailureReason(request.getFailureReason());
        }

        final Long nextPos = getNextPos(oldOpportunity.getOrganizationId(), request.getStage());
        newOpportunity.setPos(nextPos);

        opportunityMapper.update(newOpportunity);

        final Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("stage", stageMap.get(oldOpportunity.getStage()));
        final Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("stage", stageMap.get(request.getStage()));

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(oldOpportunity.getName())
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
    }

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, rolePermissions);
    }

    public CustomerContactListAllResponse getContactList(String opportunityId, String orgId) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(opportunityId);
        if (opportunity == null) {
            throw new GenericException(Translator.get("opportunity_not_found"));
        }
        return customerContactService.getOpportunityContactList(opportunity.getContactId(), orgId);
    }

    public String getOpportunityName(String id) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(opportunity).map(Opportunity::getName).orElse(null);
    }

    public List<Opportunity> getOpportunityListByNames(List<String> names) {
        LambdaQueryWrapper<Opportunity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Opportunity::getName, names);
        return opportunityMapper.selectListByLambda(lambdaQueryWrapper);
    }

    public String getOpportunityNameByIds(List<String> ids) {
        List<Opportunity> opportunityList = opportunityMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(opportunityList)) {
            List<String> names = opportunityList.stream().map(Opportunity::getName).toList();
            return String.join(",", JSON.parseArray(JSON.toJSONString(names), String.class));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 下载导入的模板
     *
     * @param response 响应
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
		new EasyExcelExporter()
                .exportMultiSheetTplWithSharedHandler(response, moduleFormService.getCustomImportHeads(FormKey.OPPORTUNITY.getKey(), currentOrg),
                        Translator.get("opportunity.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
						new CustomTemplateWriteHandler(moduleFormService.getCustomImportFields(FormKey.OPPORTUNITY.getKey(), currentOrg)), new CustomHeadColWidthStyleStrategy());
    }

    /**
     * 导入检查
     *
     * @param file       导入文件
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
     * 商机导入
     *
     * @param file        导入文件
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     * @return 导入返回信息
     */
    public ImportResponse realImport(MultipartFile file, String currentOrg, String currentUser) {
        try {
            List<StageConfigResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(currentOrg);

            List<BaseField> fields = moduleFormService.getAllFields(FormKey.OPPORTUNITY.getKey(), currentOrg);
            long nextPos = getNextPos(currentOrg, stageConfigList.getFirst().getId());
            CustomImportAfterDoConsumer<Opportunity, BaseResourceSubField> afterDo = (opportunities, opportunityFields, opportunityFieldBlobs) -> {
                List<LogDTO> logs = new ArrayList<>();
                for (int i = 0; i < opportunities.size(); i++) {
                    Opportunity opportunity = opportunities.get(i);
                    opportunity.setStage(stageConfigList.getFirst().getId());
                    opportunity.setPos(nextPos + i);
                    logs.add(new LogDTO(currentOrg, opportunity.getId(), currentUser, LogType.ADD, LogModule.OPPORTUNITY_INDEX, opportunity.getName()));
                }
                opportunityMapper.batchInsert(opportunities);
                opportunityFieldMapper.batchInsert(opportunityFields.stream().map(field -> BeanUtils.copyBean(new OpportunityField(), field)).toList());
                opportunityFieldBlobMapper.batchInsert(opportunityFieldBlobs.stream().map(field -> BeanUtils.copyBean(new OpportunityFieldBlob(), field)).toList());
                // record logs
                logService.batchAdd(logs);
            };
            CustomFieldImportEventListener<Opportunity> eventListener = new CustomFieldImportEventListener<>(fields, Opportunity.class, currentOrg, currentUser,
                    "opportunity_field", afterDo, 2000, null);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("opportunity import error: ", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 检查导入的文件
     *
     * @param file       文件
     * @param currentOrg 当前组织
     * @return 检查信息
     */
    private ImportResponse checkImportExcel(MultipartFile file, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getCustomImportFields(FormKey.OPPORTUNITY.getKey(), currentOrg);
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "opportunity", "opportunity_field", currentOrg, null);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("opportunity import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    public void batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = opportunityFieldService.getAndCheckField(request.getFieldId(), organizationId);
        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey())) {
            // 修改负责人，走批量转移接口
            OpportunityTransferRequest batchTransferRequest = new OpportunityTransferRequest();
            batchTransferRequest.setIds(request.getIds());
            batchTransferRequest.setOwner(request.getFieldValue().toString());
            transfer(batchTransferRequest, userId, organizationId);
            return;
        }

        List<Opportunity> originOpportunities = opportunityMapper.selectByIds(request.getIds());

        opportunityFieldService.batchUpdate(request, field, originOpportunities, Opportunity.class, LogModule.OPPORTUNITY_INDEX, extOpportunityMapper::batchUpdate, userId, organizationId);
    }


    /**
     * 阶段看板拖拽排序
     *
     * @param request
     * @param userId
     */
    public void sort(OpportunitySortRequest request, String userId) {
        //拖拽节点
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(request.getDragNodeId());
        if (opportunity == null) {
            throw new GenericException(Translator.get("opportunity_not_found"));
        }
        Long pos = DEFAULT_POS;
        if (StringUtils.isNotBlank(request.getDropNodeId())) {
            //放入节点
            Opportunity dropNode = opportunityMapper.selectByPrimaryKey(request.getDropNodeId());
            pos = dropNode.getPos();
            if (request.getDropPosition() == -1) {

                extOpportunityMapper.moveUpStageOpportunity(pos, request.getStage(), DEFAULT_POS);
                pos = pos + 1;
            } else {
                extOpportunityMapper.moveDownStageOpportunity(pos, request.getStage(), DEFAULT_POS);
            }
        }
        Opportunity dragOpportunity = new Opportunity();
        dragOpportunity.setId(request.getDragNodeId());
        dragOpportunity.setPos(pos);
        dragOpportunity.setStage(request.getStage());
        dragOpportunity.setLastStage(opportunity.getStage());
        dragOpportunity.setUpdateUser(userId);
        dragOpportunity.setUpdateTime(System.currentTimeMillis());
        opportunityMapper.updateById(dragOpportunity);
    }

    public List<ChartResult> chart(ChartAnalysisRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        formConfig.getFields().addAll(BaseResourceFieldService.getChartBaseFields());
        formConfig.getFields().addAll(getChartFields(orgId));
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request, formConfig);
        List<ChartResult> chartResults = extOpportunityMapper.chart(chartAnalysisDbRequest, userId, orgId, deptDataPermission);
        return opportunityFieldService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
    }

    public List<BaseField> getChartFields(String orgId) {
        SelectField stageField = new SelectField();
        stageField.setType(FieldType.SELECT.name());
        stageField.setId("stage");
        stageField.setBusinessKey("stage");
        List<OptionProp> options = extOpportunityStageConfigMapper.getStageConfigList(orgId)
                .stream()
                .map(config -> {
                    OptionProp optionDTO = new OptionProp();
                    optionDTO.setLabel(config.getName());
                    optionDTO.setValue(config.getId());
                    return optionDTO;
                }).collect(Collectors.toList());
        stageField.setOptions(options);

        return List.of(stageField);
    }
}
