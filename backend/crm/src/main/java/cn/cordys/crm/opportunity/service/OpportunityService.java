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
import cn.cordys.common.dto.stage.StageSortRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseChartService;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalResourceUpdateType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.dto.ResourceApprovalFieldUpdateParam;
import cn.cordys.crm.approval.dto.ResourceApprovalPostUpdateParam;
import cn.cordys.crm.approval.dto.ResourceSnapshotApprovalParam;
import cn.cordys.crm.approval.handler.ApprovalResourceHandler;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
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
import cn.cordys.crm.opportunity.dto.response.OpportunityStageResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.*;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.request.ImportRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldMergeCellEventListener;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.*;
import cn.cordys.crm.system.service.StatisticFieldService.StatisticHostScope;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.enums.CellExtraTypeEnum;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class OpportunityService extends BaseExportService implements ApprovalResourceHandler {

    public static final String SUCCESS = "SUCCESS";
    public static final Long DEFAULT_POS = 1L;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private StatisticFieldService statisticFieldService;
    @Resource
    private BaseService baseService;
    @Resource
    private OpportunityFieldService opportunityFieldService;
    @Resource
    private BaseChartService baseChartService;
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
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private StageAdvancedConfigService stageAdvancedConfigService;
    @Resource
    private ApprovalFlowService approvalFlowService;

    public PagerWithOption<List<OpportunityListResponse>> list(OpportunityPageRequest request, String userId, String orgId,
                                                               DeptDataPermissionDTO deptDataPermission, Boolean source) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<OpportunityListResponse> list = extOpportunityMapper.list(request, orgId, userId, deptDataPermission, source);
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
        if (CollectionUtils.isNotEmpty(contactFieldOption)) {
            optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);
        }

        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        if (CollectionUtils.isNotEmpty(productOption)) {
            optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);
        }

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
        Map<String, List<BaseModuleFieldValue>> fvMap = opportunityFieldService.setBusinessRefFieldValue(list, moduleFormService.getFlattenFormFields(FormKey.OPPORTUNITY.getKey(), orgId), opportunityFiledMap);

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

        List<OpportunityStageResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        Map<String, OpportunityStageResponse> endConfigMaps = stageConfigList.stream().filter(config ->
                Strings.CI.equals(config.getType(), OpportunityStageType.END.name())
        ).collect(Collectors.toMap(OpportunityStageResponse::getId, Function.identity()));

        // 失败原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.OPPORTUNITY_FAIL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        // 审批中才需要提审人与首节点是否已通过, 其余状态不查审批实例, 统一留空
        List<String> approvingResourceIds = list.stream()
                .filter(item -> Strings.CI.equals(item.getApprovalStatus(), ApprovalStatus.APPROVING.name()))
                .map(OpportunityListResponse::getId).toList();
        Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(approvingResourceIds, orgId);
        Map<String, String> submitterIdMap = baseService.getApprovingResourceSubmitterIds(approvingResourceIds);

        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> opportunityFields = fvMap.get(opportunityListResponse.getId());
            // 计算保留天数(成功失败阶段不计算)
            opportunityListResponse.setReservedDays(endConfigMaps.containsKey(opportunityListResponse.getStage()) ?
                    null : opportunityRuleService.calcReservedDay(ownersDefaultRuleMap.get(opportunityListResponse.getOwner()), opportunityListResponse.getCreateTime()));
            opportunityListResponse.setModuleFields(opportunityFields);

            opportunityListResponse.setFollowerName(userNameMap.get(opportunityListResponse.getFollower()));
            opportunityListResponse.setCreateUserName(userNameMap.get(opportunityListResponse.getCreateUser()));
            opportunityListResponse.setUpdateUserName(userNameMap.get(opportunityListResponse.getUpdateUser()));
            opportunityListResponse.setOwnerName(userNameMap.get(opportunityListResponse.getOwner()));
            opportunityListResponse.setContactName(contactMap.get(opportunityListResponse.getContactId()));

            opportunityListResponse.setFirstApproved(firstNodeApprovedMap.get(opportunityListResponse.getId()));
            opportunityListResponse.setSubmitterId(submitterIdMap.get(opportunityListResponse.getId()));

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
    @OperationLog(module = LogModule.OPPORTUNITY_INDEX, type = LogType.ADD)
    @HitApproval(formKey = FormKey.OPPORTUNITY, executeType = ExecuteTimingEnum.CREATE, operatorId = "{#operatorId}")
    public Opportunity add(OpportunityAddRequest request, String operatorId, String orgId) {
        productService.checkProductList(request.getProducts());
        List<OpportunityStageResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
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
        opportunity.setApprovalStatus(ApprovalStatus.NONE.name());
        opportunity.setApproved(false);
        if (StringUtils.isBlank(request.getOwner())) {
            opportunity.setOwner(operatorId);
        }

        //自定义字段
        opportunityFieldService.saveModuleField(opportunity, orgId, operatorId, request.getModuleFields(), false);
        opportunityMapper.insert(opportunity);

        // 统计字段: 本条记录刚建好, 先按各统计字段的空值口径把值行落一次
        statisticFieldService.refreshDataStatisticFields(FormKey.OPPORTUNITY.getKey(), opportunity.getId(), orgId);
        // 统计字段: 新数据可能关联到了别的表单记录, 被关联记录的统计值要跟着重算
        statisticFieldService.refreshByRelatedDataChange(FormKey.OPPORTUNITY.getKey(), opportunity.getId(), orgId);
        baseService.handleAddLogWithSubTable(opportunity, request.getModuleFields(), Translator.get("products_info"), getFormConfig(orgId));

        // 消息通知
        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                NotificationConstants.Event.BUSINESS_ADD, opportunity.getName(), operatorId,
                orgId, List.of(opportunity.getOwner()), true);
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
    @HitApproval(formKey = FormKey.OPPORTUNITY, executeType = ExecuteTimingEnum.UPDATE, resourceId = "{#request.id}", updateType = "{#request.updateType}", operatorId = "{#userId}", comment = "{#request.comment}")
    public Opportunity update(OpportunityUpdateRequest request, String userId, String orgId) {
        Opportunity oldOpportunity = opportunityMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(oldOpportunity).ifPresentOrElse(item -> {
            Opportunity newOpportunity = BeanUtils.copyBean(new Opportunity(), item);
            productService.checkProductList(request.getProducts());
            // 负责人变更不是普通字段改动, 与商机转移(OpportunityService#transfer)保持一致:
            // 关联联系人的负责人要跟着走, 并通知新负责人
            if (StringUtils.isNotBlank(request.getOwner()) && !Strings.CS.equals(request.getOwner(), item.getOwner())) {
                syncContactOwner(request.getContactId(), request.getOwner());
                sendTransferNotice(List.of(item), request.getOwner(), userId, orgId);
            }
            //更新商机
            Opportunity updateOpportunity = newOpportunity(newOpportunity, request, userId);
            // 保留审批状态, 编辑不改变审批状态
            updateOpportunity.setApprovalStatus(item.getApprovalStatus());
            updateOpportunity.setApproved(item.getApproved());
            // 获取模块字段
            List<BaseModuleFieldValue> originCustomerFields = opportunityFieldService.getModuleFieldValuesByResourceId(request.getId());
            // 统计字段: 关联字段在下面会被覆盖, 改之前先把它当前指向的宿主捕下来 ——
            // 改成别的关联对象时, 变更前那条宿主的统计值会偏大, 而改完就再也查不出它了
            StatisticHostScope statisticScope = statisticFieldService.captureRelatedHosts(
                    FormKey.OPPORTUNITY.getKey(), List.of(request.getId()), orgId);
            if (BooleanUtils.isTrue(request.getAgentInvoke())) {
                opportunityFieldService.updateModuleFieldByAgent(updateOpportunity, originCustomerFields, request.getModuleFields(), orgId, userId);
            } else {
                // 更新模块字段
                updateModuleField(updateOpportunity, request.getModuleFields(), orgId, userId);
            }
            extOpportunityMapper.updateIncludeNullById(updateOpportunity);
            // 统计字段: 改前改后关联到的宿主记录都要重算(关联没动时这两批是同一批, 去重后只算一次)
            statisticFieldService.refreshAfterRelatedChange(statisticScope, List.of(request.getId()));
            baseService.handleUpdateLogWithSubTable(oldOpportunity, newOpportunity, originCustomerFields, request.getModuleFields(),
                    oldOpportunity.getId(), oldOpportunity.getName(), Translator.get("products_info"), getFormConfig(orgId));
        }, () -> {
            throw new GenericException("opportunity_not_found");
        });
        return opportunityMapper.selectByPrimaryKey(request.getId());
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
     * 删除商机（带审批校验）
     *
     * @param id     商机ID
     * @param userId 用户ID
     * @param orgId  组织ID
     */
    @HitApproval(formKey = FormKey.OPPORTUNITY, executeType = ExecuteTimingEnum.DELETE, resourceId = "{#id}", operatorId = "{#userId}")
    public void deleteWithApprovalCheck(String id, String userId, String orgId) {
        delete(id, userId, orgId);
    }

    /**
     * 删除商机
     *
     * @param id
     */
    @OperationLog(module = LogModule.OPPORTUNITY_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(id);
        // 删除会同时毁掉关联字段的值, 所以「这条商机关联了谁」只能删前先捕; 重算又要等删完才准。
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHosts(
                FormKey.OPPORTUNITY.getKey(), List.of(id), orgId);
        Optional.ofNullable(opportunity).ifPresentOrElse(item -> {
            opportunityMapper.deleteByPrimaryKey(opportunity.getId());
            opportunityFieldService.deleteByResourceId(opportunity.getId());
        }, () -> {
            throw new GenericException("opportunity_not_found");
        });
        // 删完再重算, 此时被删的那条已经不在, 不会被统计进去。
        statisticFieldService.refreshAfterRelatedDelete(statisticScope);
        // 添加日志上下文
        OperationLogContext.setResourceName(opportunity.getName());

        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                NotificationConstants.Event.BUSINESS_DELETED, opportunity.getName(), userId,
                orgId, List.of(opportunity.getOwner()), true);
    }


    /**
     * 商机转移 (与商机阶段无关, 都可转移)
     *
     * @param request 请求参数
     * @param userId  用户ID
     * @param orgId   组织ID
     */
    public BatchAffectReasonResponse transfer(OpportunityTransferRequest request, String userId, String orgId) {
        List<Opportunity> opportunityList = opportunityMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(opportunityList)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0)
                    .errorMessages(Translator.get("opportunity.not.exist")).build();
        }
        // 转移 SQL 只写负责人真正发生变化的商机, 审批同样只对这些商机触发, 避免给原本就是这个负责人的商机凭空建一条审批
        List<String> changedIds = opportunityList.stream()
                .filter(opportunity -> !Strings.CS.equals(opportunity.getOwner(), request.getOwner()))
                .map(Opportunity::getId)
                .toList();
        // 快照须在转移前落库, 否则审批驳回/撤回时回退到的是转移后的负责人
        CommonBeanFactory.getBean(ApprovalResourceService.class).batchTransferTriggerApproval(
                changedIds, BusinessModuleField.OPPORTUNITY_OWNER, FormKey.OPPORTUNITY, orgId, userId, request.getOwner());

        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtOpportunityMapper batchUpdateMapper = sqlSession.getMapper(ExtOpportunityMapper.class);
        for (int i = 0; i < changedIds.size(); i++) {
            batchUpdateMapper.transfer(request.getOwner(), userId, changedIds.get(i), System.currentTimeMillis());
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
        });

        // 商机负责人变更, 联系人负责人要跟着走; 只处理负责人真正变化的商机, 与转移 SQL 的范围保持一致
        opportunityList.stream()
                .filter(opportunity -> !Strings.CS.equals(opportunity.getOwner(), request.getOwner()))
                .forEach(opportunity -> syncContactOwner(opportunity.getContactId(), request.getOwner()));

        logService.batchAdd(logs);
        sendTransferNotice(opportunityList, request.getOwner(), userId, orgId);

        // success: 真的换了负责人; skip: 负责人本来就是目标负责人; fail: 入参里有查不到的商机
        return BatchAffectReasonResponse.builder()
                .success(changedIds.size())
                .fail(CollectionUtils.size(request.getIds()) - opportunityList.size())
                .skip(opportunityList.size() - changedIds.size())
                .errorMessages(Translator.get("batch.transfer.reason"))
                .build();
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
        List<Opportunity> selectedList = opportunityMapper.selectByIds(ids);
        // 状态权限校验: 过滤出当前用户有权删除的商机
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.OPPORTUNITY.getValue(),
                selectedList,
                PermissionConstants.OPPORTUNITY_MANAGEMENT_DELETE,
                orgId,
                Opportunity::getId,
                Opportunity::getApprovalStatus
        );
        if (CollectionUtils.isEmpty(permittedIds)) {
            return;
        }

        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Opportunity::getId, permittedIds);
        wrapper.nq(Opportunity::getStage, SUCCESS);
        List<Opportunity> opportunityList = opportunityMapper.selectListByLambda(wrapper);
        List<String> toDoIds = opportunityList.stream().map(Opportunity::getId).toList();
        if (CollectionUtils.isEmpty(toDoIds)) {
            return;
        }

        // 命中删除审批流的商机不直接删除, 走审批
        Map<String, String> nameMap = opportunityList.stream()
                .collect(Collectors.toMap(Opportunity::getId, Opportunity::getName, (a, b) -> a));
        ApprovalResourceService approvalResourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
        List<String> approvalIds = approvalResourceService.batchDeleteTriggerApproval(
                toDoIds, FormKey.OPPORTUNITY, orgId, userId, nameMap);
        List<String> deleteIds = toDoIds.stream().filter(id -> !approvalIds.contains(id)).toList();
        if (CollectionUtils.isEmpty(deleteIds)) {
            return;
        }

        // 捕的是审批分流之后真正要删的 deleteIds, 不是 toDoIds —— 走审批的那批此刻并没删掉,
        // 捕了就是白捕, 而且审批通过后还会由审批侧再删一次, 那次自会重算。
        // 删除会同时毁掉关联字段的值, 所以只能删前先捕; 重算又要等删完才准。
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHosts(
                FormKey.OPPORTUNITY.getKey(), deleteIds, orgId);
        opportunityMapper.deleteByIds(deleteIds);
        opportunityFieldService.deleteByResourceIds(deleteIds);
        // 删完再重算, 此时被删的那批已经不在, 不会被统计进去。
        statisticFieldService.refreshAfterRelatedDelete(statisticScope);
        List<LogDTO> logs = new ArrayList<>();
        opportunityList.stream()
                .filter(opportunity -> deleteIds.contains(opportunity.getId()))
                .forEach(opportunity -> {
                    LogDTO logDTO = new LogDTO(opportunity.getOrganizationId(), opportunity.getId(), userId, LogType.DELETE, LogModule.OPPORTUNITY_INDEX, opportunity.getName());
                    logDTO.setOriginalValue(opportunity);
                    logs.add(logDTO);
                });
        logService.batchAdd(logs);

        // 消息通知
        opportunityList.stream()
                .filter(opportunity -> deleteIds.contains(opportunity.getId()))
                .forEach(opportunity ->
                        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                                NotificationConstants.Event.BUSINESS_DELETED, opportunity.getName(), userId,
                                orgId, List.of(opportunity.getOwner()), true)
                );
    }


    public OpportunityDetailResponse getWithDataPermissionCheck(String id, String userId, String orgId) {
        OpportunityDetailResponse getResponse = get(id);
        if (getResponse == null) {
            throw new GenericException(Translator.get("opportunity_not_found"));
        }
        dataScopeService.hasDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
        return getResponse;
    }


    /**
     * @param id 商机ID
     * @return 商机详情
     */
    public OpportunityDetailResponse get(String id) {
        OpportunityDetailResponse response = extOpportunityMapper.getDetail(id);
        if (response == null) {
            return null;
        }
        List<BaseModuleFieldValue> fieldValueList = opportunityFieldService.getModuleFieldValuesByResourceId(id);
        fieldValueList = opportunityFieldService.setBusinessRefFieldValue(List.of(response),
                moduleFormService.getFlattenFormFields(FormKey.OPPORTUNITY.getKey(), response.getOrganizationId()), new HashMap<>(Map.of(id, fieldValueList))).get(id);
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

        List<OpportunityStageResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(response.getOrganizationId());
        Map<String, OpportunityStageResponse> endConfigMaps = stageConfigList.stream().filter(config ->
                Strings.CI.equals(config.getType(), OpportunityStageType.END.name())
        ).collect(Collectors.toMap(OpportunityStageResponse::getId, Function.identity()));

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

        // 审批中才需要提审人与首节点是否已通过, 其余状态不查审批实例
        if (Strings.CI.equals(response.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
            Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(List.of(id), response.getOrganizationId());
            response.setFirstApproved(firstNodeApprovedMap.get(id));
            response.setSubmitterId(baseService.getApprovingResourceSubmitterId(id));
        }


        ModuleFormConfigDTO customerFormConfig = getFormConfig(response.getOrganizationId());
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, fieldValueList);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getOwner, OpportunityDetailResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getContactId, OpportunityDetailResponse::getContactName);
        if (CollectionUtils.isNotEmpty(contactFieldOption)) {
            optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);
        }

        List<OptionDTO> customerOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getCustomerId, OpportunityDetailResponse::getCustomerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CUSTOMER_NAME.getBusinessKey(), customerOption);

        List<OptionDTO> productOption = extProductMapper.getOptions(response.getOrganizationId());
        if (CollectionUtils.isNotEmpty(productOption)) {
            optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);
        }

        response.setOptionMap(optionMap);

        // 附件信息
        response.setAttachmentMap(moduleFormService.getAttachmentMap(customerFormConfig, fieldValueList));

        return response;
    }

    /**
     * 获取商机详情 (⚠️反射调用; 勿修改入参, 返回, 方法名!)
     *
     * @param id 商机ID
     * @return 商机详情
     */
    public OpportunityDetailResponse getSimple(String id) {
        OpportunityDetailResponse response = extOpportunityMapper.getDetail(id);
        if (response == null) {
            return null;
        }
        List<BaseModuleFieldValue> fvs = opportunityFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fvs);
        return response;
    }

    /**
     * 批量获取商机详情 (用于数据源批量查询优化)
     *
     * @param ids 商机ID集合
     * @return 商机详情列表
     */
    public List<OpportunityDetailResponse> batchGetSimpleByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        // 批量查询资源基本信息
        List<Opportunity> opportunities = opportunityMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(opportunities)) {
            return Collections.emptyList();
        }
        // 批量查询自定义字段值
        Map<String, List<BaseModuleFieldValue>> fieldValueMap = opportunityFieldService.getResourceFieldMap(ids, true);

        // 组装结果
        return opportunities.stream().map(opportunity -> {
            OpportunityDetailResponse response = BeanUtils.copyBean(new OpportunityDetailResponse(), opportunity);
            response.setModuleFields(fieldValueMap.get(opportunity.getId()));
            return response;
        }).toList();
    }


    /**
     * 标记商机阶段
     *
     * @param request
     * @param userId
     * @param orgId
     */
    @OperationLog(module = LogModule.OPPORTUNITY_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    @HitApproval(formKey = FormKey.OPPORTUNITY, executeType = ExecuteTimingEnum.UPDATE, resourceId = "{#request.id}", operatorId = "{#userId}")
    public void updateStage(OpportunityStageRequest request, String userId, String orgId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(orgId)
                || !PermissionUtils.hasPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        final Opportunity oldOpportunity = opportunityMapper.selectByPrimaryKey(request.getId());
        if (oldOpportunity == null) {
            throw new GenericException(Translator.get("opportunity_not_found"));
        }

        // 使用实际商机的组织和负责人校验，避免依赖资源 provider 的选择结果。
        if (!orgId.equals(oldOpportunity.getOrganizationId()) || StringUtils.isBlank(oldOpportunity.getOwner())
                || !dataScopeService.hasDataPermission(userId, orgId, oldOpportunity.getOwner(),
                PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        final List<OpportunityStageResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(orgId);
        if (stageConfigList.stream().noneMatch(cfg -> Objects.equals(cfg.getId(), request.getStage()))) {
            throw new GenericException(Translator.get("opportunity_stage_not_exist"));
        }

        final Optional<OpportunityStageResponse> successOpt = stageConfigList.stream()
                .filter(cfg -> Strings.CI.equals(cfg.getType(), OpportunityStageType.END.name())
                        && Strings.CI.equals(cfg.getRate(), "100"))
                .findFirst();

        final Optional<OpportunityStageResponse> failOpt = stageConfigList.stream()
                .filter(cfg -> Strings.CI.equals(cfg.getType(), OpportunityStageType.END.name())
                        && Strings.CI.equals(cfg.getRate(), "0"))
                .findFirst();

        final Map<String, String> stageMap = stageConfigList.stream()
                .collect(Collectors.toMap(OpportunityStageResponse::getId, OpportunityStageResponse::getName));

        final Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("stage", stageMap.get(oldOpportunity.getStage()));

        if (!stageAdvancedConfigService.checkStage(oldOpportunity.getStage(), request.getStage(), FormKey.OPPORTUNITY.getKey())) {
            return;
        }


        oldOpportunity.setLastStage(oldOpportunity.getStage());
        oldOpportunity.setStage(request.getStage());

        final boolean isSuccessStage = successOpt.map(cfg -> Strings.CI.equals(request.getStage(), cfg.getId())).orElse(false);
        final boolean isFailStage = failOpt.map(cfg -> Strings.CI.equals(request.getStage(), cfg.getId())).orElse(false);

        if (isSuccessStage || isFailStage) {
            oldOpportunity.setActualEndTime(System.currentTimeMillis());
        }
        if (isFailStage) {
            oldOpportunity.setFailureReason(request.getFailureReason());
        }

        final Long nextPos = getNextPos(oldOpportunity.getOrganizationId(), request.getStage());
        oldOpportunity.setPos(nextPos);

        opportunityMapper.update(oldOpportunity);

        updateField(oldOpportunity, request.getFields(), userId);

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

    private void updateField(Opportunity opportunity, List<BaseModuleFieldValue> requestFields, String userId) {
        if (CollectionUtils.isNotEmpty(requestFields)) {
            ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), opportunity.getOrganizationId());
            List<BaseField> fields = businessFormConfig.getFields();
            requestFields.forEach(field -> {
                BaseField baseField = fields.stream().filter(customField -> customField.getId().equals(field.getFieldId())).findFirst().orElse(null);
                ResourceBatchEditRequest updateRequest = new ResourceBatchEditRequest();
                updateRequest.setIds(List.of(opportunity.getId()));
                updateRequest.setFieldId(field.getFieldId());
                updateRequest.setFieldValue(field.getFieldValue());
                opportunityFieldService.batchUpdate(updateRequest, baseField, List.of(opportunity), Opportunity.class, LogModule.OPPORTUNITY_INDEX, extOpportunityMapper::batchUpdate, userId, opportunity.getOrganizationId());
            });
        }
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
                .exportMultiSheetTplWithSharedHandler(response, processDuplicateLastLevelHeads(moduleFormService.getCustomImportHeadsNoRef(FormKey.OPPORTUNITY.getKey(), currentOrg)),
                        Translator.get("opportunity.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
                        new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(FormKey.OPPORTUNITY.getKey(), currentOrg)), new CustomHeadColWidthStyleStrategy());
    }

    /**
     * 导入检查
     *
     * @param file       导入文件
     * @param currentOrg 当前组织
     * @return 导入检查信息
     */
    public ImportResponse importPreCheck(MultipartFile file, String importType, String currentOrg) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        return checkImportExcel(file, importType, currentOrg);
    }

    /**
     * 商机导入
     *
     * @param file        导入文件
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     * @return 导入返回信息
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResponse realImport(MultipartFile file, ImportRequest request, String currentOrg, String currentUser) {
        try {
            List<OpportunityStageResponse> stageConfigList = extOpportunityStageConfigMapper.getStageConfigList(currentOrg);

            List<BaseField> fields = moduleFormService.getAllFields(FormKey.OPPORTUNITY.getKey(), currentOrg);
            boolean supportSubHead = moduleFormService.supportSubHead(fields);
            int headRowNumber = supportSubHead ? 2 : 1;
            // 1 读取合并单元格信息
            CustomFieldMergeCellEventListener mergeCellEventListener =
                    new CustomFieldMergeCellEventListener();

            FastExcelFactory.read(file.getInputStream(), mergeCellEventListener)
                    .extraRead(CellExtraTypeEnum.MERGE)
                    .headRowNumber(headRowNumber)
                    .ignoreEmptyRow(true)
                    .sheet()
                    .doRead();

            ModuleFormConfigDTO moduleFormConfigDTO = getFormConfig(currentOrg);
            long nextPos = getNextPos(currentOrg, stageConfigList.getFirst().getId());
            CustomImportAfterDoConsumer<Opportunity, BaseResourceSubField> afterDo = (opportunities, opportunityFields, opportunityFieldBlobs) -> {
                List<LogDTO> logs = new ArrayList<>();
                ImportType importType = EnumUtils.valueOf(ImportType.class, request.getImportType());
                switch (importType) {
                    case ADD -> {
                        for (int i = 0; i < opportunities.size(); i++) {
                            Opportunity opportunity = opportunities.get(i);
                            opportunity.setStage(stageConfigList.getFirst().getId());
                            opportunity.setPos(nextPos + i);
                            logs.add(new LogDTO(currentOrg, opportunity.getId(), currentUser, LogType.ADD, LogModule.OPPORTUNITY_INDEX, opportunity.getName()));
                            commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                                    NotificationConstants.Event.BUSINESS_ADD, opportunity.getName(), currentUser,
                                    currentOrg, List.of(opportunity.getOwner()), true);
                        }
                        opportunityMapper.batchInsert(opportunities);
                        opportunityFieldMapper.batchInsert(opportunityFields.stream().map(field -> BeanUtils.copyBean(new OpportunityField(), field)).toList());
                        opportunityFieldBlobMapper.batchInsert(opportunityFieldBlobs.stream().map(field -> BeanUtils.copyBean(new OpportunityFieldBlob(), field)).toList());
                        // record logs
                        logService.batchAdd(logs);
                    }
                    case UPDATE -> {
                        List<String> ids = opportunities.stream().map(Opportunity::getId).toList();
                        if (CollectionUtils.isEmpty(ids)) {
                            break;
                        }
                        //原数据
                        List<Opportunity> originOpportunityList = opportunityMapper.selectByIds(ids);
                        if (CollectionUtils.isEmpty(originOpportunityList)) {
                            break;
                        }
                        Map<String, Opportunity> originOpportunityMaps = originOpportunityList.stream().collect(Collectors.toMap(Opportunity::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> originFieldValueMap = opportunityFieldService.getResourceFieldMap(ids, true);

                        List<OpportunityField> insertField = new ArrayList<>();
                        List<OpportunityFieldBlob> insertFieldBlob = new ArrayList<>();
                        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
                        ExtOpportunityMapper batchMapper = sqlSession.getMapper(ExtOpportunityMapper.class);
                        CommonMapper commonMapper = sqlSession.getMapper(CommonMapper.class);

                        if (CollectionUtils.isNotEmpty(opportunities)) {
                            opportunities.forEach(opportunity -> {
                                batchMapper.updateOpportunity(opportunity);
                            });
                        }

                        if (CollectionUtils.isNotEmpty(opportunityFields)) {
                            List<OpportunityField> fieldList = opportunityFieldMapper.selectByIds(opportunityFields.stream().map(BaseResourceSubField::getId).toList());
                            Map<String, OpportunityField> fieldMap = fieldList.stream().collect(Collectors.toMap(OpportunityField::getId, Function.identity()));
                            opportunityFields.forEach(opportunityField -> {
                                if (fieldMap.containsKey(opportunityField.getId())) {
                                    commonMapper.updateCustomerField("opportunity_field", opportunityField);
                                } else {
                                    insertField.add(BeanUtils.copyBean(new OpportunityField(), opportunityField));
                                }
                            });
                        }

                        if (CollectionUtils.isNotEmpty(opportunityFieldBlobs)) {
                            List<OpportunityFieldBlob> blobList = opportunityFieldBlobMapper.selectByIds(opportunityFieldBlobs.stream().map(BaseResourceSubField::getId).toList());
                            Map<String, OpportunityFieldBlob> blobMap = blobList.stream().collect(Collectors.toMap(OpportunityFieldBlob::getId, Function.identity()));
                            opportunityFieldBlobs.forEach(opportunityFieldBlob -> {
                                if (blobMap.containsKey(opportunityFieldBlob.getId())) {
                                    commonMapper.updateCustomerField("opportunity_field_blob", opportunityFieldBlob);
                                } else {
                                    insertFieldBlob.add(BeanUtils.copyBean(new OpportunityFieldBlob(), opportunityFieldBlob));
                                }
                            });
                        }

                        sqlSession.flushStatements();
                        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);

                        if (CollectionUtils.isNotEmpty(insertField)) {
                            opportunityFieldMapper.batchInsert(insertField);
                        }
                        if (CollectionUtils.isNotEmpty(insertFieldBlob)) {
                            opportunityFieldBlobMapper.batchInsert(insertFieldBlob);
                        }

                        SqlSession currentSession =
                                SqlSessionUtils.getSqlSession(sqlSessionFactory);
                        currentSession.clearCache();

                        Map<String, Opportunity> modifiedOpportunityMaps = opportunityMapper.selectByIds(ids).stream().collect(Collectors.toMap(Opportunity::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> modifiedFieldValueMap = opportunityFieldService.getResourceFieldMap(ids, true);

                        ids.forEach(id -> {
                            Opportunity originDate = originOpportunityMaps.get(id);
                            Opportunity modifiedDate = modifiedOpportunityMaps.get(id);
                            baseService.handleUpdateLogWithSubTable(originDate, modifiedDate, originFieldValueMap.get(id), modifiedFieldValueMap.get(id),
                                    id, modifiedDate.getName(), Translator.get("products_info"), moduleFormConfigDTO);
                            LogContextInfo contextInfo = OperationLogContext.getContext();
                            if (contextInfo != null) {
                                LogDTO logDTO = new LogDTO(currentOrg, id, currentUser, LogType.UPDATE, LogModule.OPPORTUNITY_INDEX, modifiedDate.getName());
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
            CustomFieldImportEventListener<Opportunity> eventListener = new CustomFieldImportEventListener<>(fields, Opportunity.class, currentOrg, currentUser,
                    "opportunity_field", "opportunity_field_blob", afterDo, 2000, mergeCellEventListener.getMergeCellMap(), mergeCellEventListener.getMergeRowDataMap(), request.getImportType());
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(headRowNumber).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("opportunity import error: ", e.getMessage());
            throw new GenericException("导入异常，请检查文件数据！" + e);
        }
    }

    /**
     * 检查导入的文件
     *
     * @param file       文件
     * @param currentOrg 当前组织
     * @return 检查信息
     */
    private ImportResponse checkImportExcel(MultipartFile file, String importType, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(FormKey.OPPORTUNITY.getKey(), currentOrg);

            boolean supportSubHead = moduleFormService.supportSubHead(fields);
            int headRowNumber = supportSubHead ? 2 : 1;

            // 1 先读取合并单元格信息
            CustomFieldMergeCellEventListener mergeCellEventListener =
                    new CustomFieldMergeCellEventListener();

            FastExcelFactory.read(file.getInputStream(), mergeCellEventListener)
                    .extraRead(CellExtraTypeEnum.MERGE)
                    .headRowNumber(headRowNumber)
                    .ignoreEmptyRow(true)
                    .sheet()
                    .doRead();

            // 2 校验数据
            CustomFieldCheckEventListener eventListener =
                    new CustomFieldCheckEventListener(
                            fields,
                            "opportunity",
                            "opportunity_field",
                            currentOrg,
                            mergeCellEventListener.getMergeCellMap(),
                            mergeCellEventListener.getMergeRowDataMap(),
                            importType
                    );

            FastExcelFactory.read(file.getInputStream(), eventListener)
                    .headRowNumber(headRowNumber)
                    .ignoreEmptyRow(true)
                    .sheet()
                    .doRead();

            return ImportResponse.builder()
                    .errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess())
                    .failCount(eventListener.getErrList().size())
                    .build();

        } catch (Exception e) {
            log.error("opportunity import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    public BatchAffectReasonResponse batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = opportunityFieldService.getAndCheckField(request.getFieldId(), organizationId);
        List<Opportunity> originOpportunities = opportunityMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(originOpportunities)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0)
                    .errorMessages(Translator.get("opportunity.not.exist")).build();
        }
        // 状态权限校验: 过滤出当前用户有权编辑的商机。放在各分支之前, 是因为下面还有一条走批量转移
        // 的路径, 它同样是一次编辑 —— 否则处在无权编辑状态(如审批中)的商机会从这条路径绕过去
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.OPPORTUNITY.getValue(),
                originOpportunities,
                PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE,
                organizationId,
                Opportunity::getId,
                Opportunity::getApprovalStatus
        );
        if (CollectionUtils.isEmpty(permittedIds)) {
            return BatchAffectReasonResponse.builder().success(0).fail(originOpportunities.size()).skip(0)
                    .errorMessages(Translator.get("no.operation.permission")).build();
        }

        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey())) {
            // 修改负责人，走批量转移接口
            OpportunityTransferRequest batchTransferRequest = new OpportunityTransferRequest();
            batchTransferRequest.setIds(permittedIds);
            batchTransferRequest.setOwner(request.getFieldValue().toString());
            return transfer(batchTransferRequest, userId, organizationId);
        }

        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey())) {
            productService.checkProductList((List<String>) request.getFieldValue());
        }

        ApprovalResourceService approvalResourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
        approvalResourceService.batchEditTriggerApproval(permittedIds, request.getFieldId(), FormKey.OPPORTUNITY, organizationId, userId, field.getName(), request.getFieldValue());
        List<Opportunity> permittedOpportunities = originOpportunities.stream()
                .filter(opportunity -> permittedIds.contains(opportunity.getId()))
                .toList();

        ResourceBatchEditRequest filteredRequest = new ResourceBatchEditRequest();
        filteredRequest.setIds(permittedIds);
        filteredRequest.setFieldId(request.getFieldId());
        filteredRequest.setFieldValue(request.getFieldValue());

        // 统计字段: 批量编辑只改一个字段, 改的若是关联字段, 下面这批商机的关联关系会整批换人 ——
        // 换之前它们指向的宿主得先捕下来, 否则那些宿主的统计值会一直偏大; 改的不是关联字段时
        // 这一步在服务内部直接短路, 只多一次反查。用 permittedIds: 没权限的那些根本没被写
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHostsForFieldChange(
                FormKey.OPPORTUNITY.getKey(), request.getFieldId(), permittedIds, organizationId);
        opportunityFieldService.batchUpdate(filteredRequest, field, permittedOpportunities, Opportunity.class, LogModule.OPPORTUNITY_INDEX, extOpportunityMapper::batchUpdate, userId, organizationId);
        // 统计字段: 改前改后关联到的宿主记录都要重算(关联没动时这两批是同一批, 去重后只算一次)
        statisticFieldService.refreshAfterRelatedChange(statisticScope, permittedIds);

        return BatchAffectReasonResponse.builder()
                .success(permittedIds.size())
                .fail(originOpportunities.size() - permittedIds.size())
                .skip(0)
                .errorMessages(Translator.get("batch.update.reason"))
                .build();
    }


    /**
     * 阶段看板拖拽排序
     * <p>
     * 阶段未变化时只是同列排序, 不触发审批; 跨列换阶段属于业务阶段变更, 走编辑审批流。
     *
     * @param request
     * @param userId
     */
    public void sort(StageSortRequest request, String userId) {
        //拖拽节点
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(request.getDragNodeId());
        if (opportunity == null) {
            throw new GenericException(Translator.get("opportunity_not_found"));
        }
        if (Strings.CI.equals(opportunity.getStage(), request.getStage())) {
            doSort(request, opportunity, userId);
            return;
        }
        // 自调用不走代理, 需从容器取代理对象才能命中审批切面
        CommonBeanFactory.getBean(OpportunityService.class).sortWithApproval(request, userId);
    }

    /**
     * 跨列换阶段的看板排序: 阶段变更需命中编辑审批流, 排序本身照常写入
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.OPPORTUNITY_INDEX, type = LogType.UPDATE, resourceId = "{#request.dragNodeId}")
    @HitApproval(formKey = FormKey.OPPORTUNITY, executeType = ExecuteTimingEnum.UPDATE, resourceId = "{#request.dragNodeId}", operatorId = "{#userId}")
    public void sortWithApproval(StageSortRequest request, String userId) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(request.getDragNodeId());
        if (opportunity == null) {
            throw new GenericException(Translator.get("opportunity_not_found"));
        }
        final Map<String, String> stageMap = extOpportunityStageConfigMapper
                .getStageConfigList(opportunity.getOrganizationId()).stream()
                .collect(Collectors.toMap(OpportunityStageResponse::getId, OpportunityStageResponse::getName));

        doSort(request, opportunity, userId);

        // 阶段变更记录: 审批实例按变更字段匹配节点条件时依赖此处写入的日志上下文
        final Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("stage", stageMap.get(opportunity.getStage()));
        final Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("stage", stageMap.get(request.getStage()));

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(opportunity.getName())
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
    }

    private void doSort(StageSortRequest request, Opportunity opportunity, String userId) {
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

        if (!stageAdvancedConfigService.checkStage(opportunity.getStage(), request.getStage(), FormKey.OPPORTUNITY.getKey())) {
            return;
        }

        Opportunity dragOpportunity = new Opportunity();
        dragOpportunity.setId(request.getDragNodeId());
        dragOpportunity.setPos(pos);
        dragOpportunity.setStage(request.getStage());
        dragOpportunity.setLastStage(opportunity.getStage());
        dragOpportunity.setUpdateUser(userId);
        dragOpportunity.setUpdateTime(System.currentTimeMillis());
        opportunityMapper.updateById(dragOpportunity);
        updateField(dragOpportunity, request.getFields(), userId);
    }

    @Override
    public FormKey getFormKey() {
        return FormKey.OPPORTUNITY;
    }

    /**
     * 更新业务快照审批状态
     * <p>
     * 商机没有业务快照表，编辑回退统一走框架的 {@code approval_resource_snapshot}，此处无需处理。
     *
     * @param param 参数
     */
    @Override
    public void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param) {
        // 商机无业务快照
    }

    @Override
    public String getPreUpdateSnapshotData(String resourceId, String userId, String orgId) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(resourceId);
        if (opportunity == null) {
            return null;
        }
        List<BaseModuleFieldValue> opportunityFields = opportunityFieldService.getModuleFieldValuesByResourceId(resourceId);
        OpportunityApprovalSnapshotRequest snapshotReq = BeanUtils.copyBean(new OpportunityApprovalSnapshotRequest(), opportunity);
        snapshotReq.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());
        ModuleFormConfigDTO opportunityFormConfig = getFormConfig(opportunity.getOrganizationId());
        // 获取模块字段
        moduleFormService.processBusinessFieldValues(snapshotReq, opportunityFields, opportunityFormConfig);
        return JSON.toJSONString(snapshotReq);
    }

    @Override
    public void revertToSnapshot(String resourceId, String userId, String orgId, String snapshotData) {
        try {
            OpportunityApprovalSnapshotRequest request = JSON.parseObject(snapshotData, OpportunityApprovalSnapshotRequest.class);
            if (request == null) {
                return;
            }
            CommonBeanFactory.getBean(OpportunityService.class).update(request, userId, orgId);
            // 阶段变更不走编辑接口, 需按快照单独回退
            revertStage(request, userId, orgId);
        } catch (Exception e) {
            log.error("审批回退还原业务数据失败, resourceId:{}", resourceId, e);
        }
    }

    /**
     * 商机负责人变更时同步关联联系人的负责人
     * <p>
     * 商机只关联一个联系人, 因此跟随负责人走的就是商机当前关联的这一个, 不涉及"改派一批"。
     * 编辑与转移都走这里; 编辑回退也是负责人变更, 复用同一条路径即可把联系人还给原负责人,
     * 无需另做按 id 的精确还原。没关联联系人(历史数据)时无事可做。
     *
     * @param contactId 商机关联的联系人ID
     * @param owner     商机负责人
     */
    private void syncContactOwner(String contactId, String owner) {
        if (StringUtils.isBlank(contactId)) {
            return;
        }
        extCustomerContactMapper.updateContactById(contactId, owner);
    }

    /**
     * 回退阶段变更
     * <p>
     * 阶段不在编辑请求的可变更字段内, 编辑回退不会带上阶段, 故按编辑前快照单独还原;
     * 回到非结束阶段时同时清空结束时间与失败原因。
     *
     * @param request 编辑前快照
     * @param userId  用户ID
     * @param orgId   组织ID
     */
    private void revertStage(OpportunityApprovalSnapshotRequest request, String userId, String orgId) {
        if (request == null || StringUtils.isBlank(request.getStage())) {
            return;
        }
        Opportunity current = opportunityMapper.selectByPrimaryKey(request.getId());
        if (current == null || Strings.CI.equals(current.getStage(), request.getStage())) {
            return;
        }
        boolean endStage = extOpportunityStageConfigMapper.getStageConfigList(orgId).stream()
                .anyMatch(cfg -> Strings.CI.equals(cfg.getId(), request.getStage())
                        && Strings.CI.equals(cfg.getType(), OpportunityStageType.END.name()));
        extOpportunityMapper.revertStageByApproval(request.getId(), request.getStage(), current.getStage(),
                endStage ? current.getActualEndTime() : null,
                endStage ? current.getFailureReason() : null,
                userId, System.currentTimeMillis());
    }

    /**
     * ⚠️反射调用: 由审批执行后置操作统一调用, 勿修改
     *
     * @param postFieldParam 参数
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void updateApprovalPostField(ResourceApprovalPostUpdateParam postFieldParam) {
        ModuleFormConfigDTO formConfig = getFormConfig(OrganizationContext.getOrganizationId());
        Map<String, BaseField> fieldConfigMap = formConfig.getFields().stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity(), (a, b) -> a));
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(postFieldParam.getResourceId());
        if (opportunity == null) {
            return;
        }
        // 保存原始数据用于日志记录
        Opportunity originOpportunity = BeanUtils.copyBean(new Opportunity(), opportunity);
        List<BaseModuleFieldValue> originFields = opportunityFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId());
        List<OpportunityField> opportunityFields = new ArrayList<>();
        List<OpportunityFieldBlob> opportunityFieldBlobs = new ArrayList<>();

        for (ResourceApprovalFieldUpdateParam fieldUpdateParam : postFieldParam.getFields()) {
            if (!fieldConfigMap.containsKey(fieldUpdateParam.getFieldId()) || fieldUpdateParam.getFieldValue() == null) {
                continue;
            }
            BaseField fieldConfig = fieldConfigMap.get(fieldUpdateParam.getFieldId());
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
            if (fieldConfig.hasBusinessKey()) {
                // 业务主表字段
                opportunityFieldService.setResourceFieldValue(opportunity, fieldConfig.getBusinessKey(), fieldUpdateParam.getFieldValue());
            } else {
                // 自定义字段
                if (fieldConfig.isBlob()) {
                    opportunityFieldService.getResourceFieldBlobMapper().deleteByLambda(new LambdaQueryWrapper<OpportunityFieldBlob>()
                            .eq(OpportunityFieldBlob::getFieldId, fieldUpdateParam.getFieldId())
                            .eq(OpportunityFieldBlob::getResourceId, postFieldParam.getResourceId()));
                    OpportunityFieldBlob field = new OpportunityFieldBlob();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
                    field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    opportunityFieldBlobs.add(field);
                } else {
                    opportunityFieldService.getResourceFieldMapper().deleteByLambda(new LambdaQueryWrapper<OpportunityField>()
                            .eq(OpportunityField::getFieldId, fieldUpdateParam.getFieldId())
                            .eq(OpportunityField::getResourceId, postFieldParam.getResourceId()));
                    OpportunityField field = new OpportunityField();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
                    field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    opportunityFields.add(field);
                }
            }
        }
        opportunityMapper.updateById(opportunity);
        if (CollectionUtils.isNotEmpty(opportunityFields)) {
            opportunityFieldService.getResourceFieldMapper().batchInsert(opportunityFields);
        }
        if (CollectionUtils.isNotEmpty(opportunityFieldBlobs)) {
            opportunityFieldService.getResourceFieldBlobMapper().batchInsert(opportunityFieldBlobs);
        }
        // 记录审批后置字段更新日志
        baseService.handleUpdateLogWithSubTable(originOpportunity, opportunity, originFields,
                opportunityFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId()),
                postFieldParam.getResourceId(), opportunity.getName(), Translator.get("products_info"), formConfig);
        // 从 OperationLogContext 中获取日志信息并手动记录
        LogContextInfo contextInfo = OperationLogContext.getContext();
        if (contextInfo != null) {
            String orgId = OrganizationContext.getOrganizationId();
            LogDTO logDTO = new LogDTO(orgId, postFieldParam.getResourceId(), postFieldParam.getOperator(), LogType.UPDATE, LogModule.OPPORTUNITY_INDEX, opportunity.getName());
            logDTO.setOriginalValue(contextInfo.getOriginalValue());
            logDTO.setModifiedValue(contextInfo.getModifiedValue());
            logService.add(logDTO);
            OperationLogContext.clear();
        }
    }

    public List<ChartResult> chart(ChartAnalysisRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        formConfig.getFields().addAll(BaseChartService.getChartBaseFields());
        formConfig.getFields().addAll(getChartFields(orgId));
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request, formConfig);
        List<ChartResult> chartResults = extOpportunityMapper.chart(chartAnalysisDbRequest, userId, orgId, deptDataPermission);
        return baseChartService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
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
