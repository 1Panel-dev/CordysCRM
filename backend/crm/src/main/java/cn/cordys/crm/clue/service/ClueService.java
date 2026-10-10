package cn.cordys.crm.clue.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.LinkScenarioKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.dto.*;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.service.BaseChartService;
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
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.domain.*;
import cn.cordys.crm.clue.dto.ClueFollowDTO;
import cn.cordys.crm.clue.dto.TransformCsAssociateDTO;
import cn.cordys.crm.clue.dto.request.*;
import cn.cordys.crm.clue.dto.response.ClueGetResponse;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.dto.request.*;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.follow.constants.FollowUpPlanType;
import cn.cordys.crm.follow.domain.*;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormLinkFill;
import cn.cordys.crm.system.dto.request.BatchPoolReasonRequest;
import cn.cordys.crm.system.dto.request.ImportRequest;
import cn.cordys.crm.system.dto.request.PoolReasonRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.BatchAffectResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.DictService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.StatisticFieldService;
import cn.cordys.crm.system.service.StatisticFieldService.StatisticHostScope;
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
import org.apache.commons.lang3.Strings;
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
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ClueService implements ApprovalResourceHandler {

    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private StatisticFieldService statisticFieldService;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private DictService dictService;
    @Resource
    private CustomerService customerService;
    @Resource
    private OpportunityService opportunityService;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private BaseChartService baseChartService;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private ClueOwnerHistoryService clueOwnerHistoryService;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private PoolClueService poolClueService;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<CustomerContact> customerContactMapper;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private ProductService productService;
    @Resource
    private PoolCustomerService poolCustomerService;
    @Resource
    private CustomerCollaborationService customerCollaborationService;
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<ClueField> clueFieldMapper;
    @Resource
    private BaseMapper<ClueFieldBlob> clueFieldBlobMapper;
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private BaseMapper<FollowUpRecordField> followUpRecordFieldMapper;
    @Resource
    private BaseMapper<FollowUpRecordFieldBlob> followUpRecordFieldBlobMapper;
    @Resource
    private BaseMapper<FollowUpPlan> followUpPlanMapper;
    @Resource
    private BaseMapper<FollowUpPlanField> followUpPlanFieldMapper;
    @Resource
    private BaseMapper<FollowUpPlanFieldBlob> followUpPlanFieldBlobMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private ApprovalFlowService approvalFlowService;

    public PagerWithOption<List<ClueListResponse>> list(CluePageRequest request, String userId, String orgId,
                                                        DeptDataPermissionDTO deptDataPermission, Boolean source) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ClueListResponse> list = extClueMapper.list(request, orgId, userId, deptDataPermission, source);
        List<ClueListResponse> buildList = buildListData(list, orgId);

        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);

        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<ClueListResponse> list, List<ClueListResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ClueListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                ClueListResponse::getOwner, ClueListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CLUE_OWNER.getBusinessKey(), ownerFieldOption);

        // 意向产品选项
        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);
        return optionMap;
    }

    public ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
    }

    public List<ClueListResponse> buildListData(List<ClueListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> clueIds = list.stream().map(ClueListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = clueFieldService.getResourceFieldMap(clueIds, true);

        List<String> ownerIds = list.stream()
                .map(ClueListResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = list.stream()
                .map(ClueListResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = list.stream()
                .map(ClueListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(ClueListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        // 获取负责人线索池信息
        Map<String, CluePool> ownersDefaultPoolMap = cluePoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        List<String> poolIds = ownersDefaultPoolMap.values().stream().map(CluePool::getId).distinct().toList();
        Map<String, CluePoolRecycleRule> recycleRuleMap;
        if (CollectionUtils.isEmpty(poolIds)) {
            recycleRuleMap = Map.of();
        } else {
            LambdaQueryWrapper<CluePoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
            recycleRuleWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);
            List<CluePoolRecycleRule> recycleRules = recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
            recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, rule -> rule));
        }

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        // 线索池原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CLUE_POOL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        // 审批中才需要提审人与首节点是否已通过, 其余状态不查审批实例, 统一留空
        List<String> approvingResourceIds = list.stream()
                .filter(item -> Strings.CI.equals(item.getApprovalStatus(), ApprovalStatus.APPROVING.name()))
                .map(ClueListResponse::getId).toList();
        Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(approvingResourceIds, orgId);
        Map<String, String> submitterIdMap = baseService.getApprovingResourceSubmitterIds(approvingResourceIds);

        list.forEach(clueListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> clueFields = caseCustomFiledMap.get(clueListResponse.getId());
            clueListResponse.setModuleFields(clueFields);

            // 设置回收公海
            CluePool reservePool = ownersDefaultPoolMap.get(clueListResponse.getOwner());
            clueListResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            // 计算剩余归属天数
            clueListResponse.setReservedDays(cluePoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    clueListResponse.getCollectionTime(), clueListResponse.getCreateTime()));

            UserDeptDTO userDeptDTO = userDeptMap.get(clueListResponse.getOwner());
            if (userDeptDTO != null) {
                clueListResponse.setDepartmentId(userDeptDTO.getDeptId());
                clueListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
            clueListResponse.setFollowerName(userNameMap.get(clueListResponse.getFollower()));
            clueListResponse.setCreateUserName(userNameMap.get(clueListResponse.getCreateUser()));
            clueListResponse.setUpdateUserName(userNameMap.get(clueListResponse.getUpdateUser()));
            clueListResponse.setOwnerName(userNameMap.get(clueListResponse.getOwner()));
            clueListResponse.setFirstApproved(firstNodeApprovedMap.get(clueListResponse.getId()));
            clueListResponse.setSubmitterId(submitterIdMap.get(clueListResponse.getId()));
            if (StringUtils.isNotBlank(clueListResponse.getReasonId())) {
                clueListResponse.setReasonName(dictMap.get(clueListResponse.getReasonId()));
            }
        });

        return list;
    }

    /**
     * @param id 线索ID
     * @return 线索详情
     */
    public ClueGetResponse get(String id) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        if (clue == null) {
            return null;
        }
        ClueGetResponse clueGetResponse = BeanUtils.copyBean(new ClueGetResponse(), clue);
        clueGetResponse = baseService.setCreateUpdateOwnerUserName(clueGetResponse);

        // 获取模块字段
        List<BaseModuleFieldValue> clueFields = clueFieldService.getModuleFieldValuesByResourceId(id);
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = getFormConfig(clue.getOrganizationId());
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, clueFields);
        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(clueGetResponse,
                ClueGetResponse::getOwner, ClueGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CLUE_OWNER.getBusinessKey(), ownerFieldOption);

        // 意向产品选项
        List<OptionDTO> productOption = extProductMapper.getOptions(clue.getOrganizationId());
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);


        clueGetResponse.setOptionMap(optionMap);
        clueGetResponse.setModuleFields(clueFields);


        // 线索池原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CLUE_POOL_RS.name(), clue.getOrganizationId());
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        if (clueGetResponse.getOwner() != null) {
            // 获取负责人线索池信息
            Map<String, CluePool> ownersDefaultPoolMap = cluePoolService.getOwnersDefaultPoolMap(List.of(clueGetResponse.getOwner()), clue.getOrganizationId());
            List<String> poolIds = ownersDefaultPoolMap.values().stream().map(CluePool::getId).distinct().toList();
            Map<String, CluePoolRecycleRule> recycleRuleMap;
            if (CollectionUtils.isEmpty(poolIds)) {
                recycleRuleMap = Map.of();
            } else {
                LambdaQueryWrapper<CluePoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
                recycleRuleWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);
                List<CluePoolRecycleRule> recycleRules = recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
                recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, rule -> rule));
            }
            // 设置回收公海
            CluePool reservePool = ownersDefaultPoolMap.get(clueGetResponse.getOwner());
            clueGetResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            // 计算剩余归属天数
            clueGetResponse.setReservedDays(cluePoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    clueGetResponse.getCollectionTime(), clueGetResponse.getCreateTime()));

            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(clueGetResponse.getOwner(), clue.getOrganizationId());
            if (userDeptDTO != null) {
                clueGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                clueGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        if (clueGetResponse.getFollower() != null) {
            Map<String, String> userNameMap = baseService.getUserNameMap(List.of(clueGetResponse.getFollower()));
            clueGetResponse.setFollowerName(userNameMap.get(clueGetResponse.getFollower()));
        }

        if (StringUtils.isNotBlank(clueGetResponse.getReasonId())) {
            clueGetResponse.setReasonName(dictMap.get(clueGetResponse.getReasonId()));
        }

        // 审批中才需要提审人与首节点是否已通过, 其余状态不查审批实例
        if (Strings.CI.equals(clueGetResponse.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
            Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(List.of(id), clue.getOrganizationId());
            clueGetResponse.setFirstApproved(firstNodeApprovedMap.get(id));
            clueGetResponse.setSubmitterId(baseService.getApprovingResourceSubmitterId(id));
        }

        // 附件信息
        clueGetResponse.setAttachmentMap(moduleFormService.getAttachmentMap(customerFormConfig, clueFields));

        return clueGetResponse;
    }

    /**
     * 获取线索详情 (⚠️反射调用; 勿修改入参, 返回, 方法名!)
     *
     * @param id 线索ID
     * @return 详情
     */
    public ClueGetResponse getSimple(String id) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        if (clue == null) {
            return null;
        }
        ClueGetResponse clueGetResponse = BeanUtils.copyBean(new ClueGetResponse(), clue);
        // 获取模块字段
        List<BaseModuleFieldValue> clueFields = clueFieldService.getModuleFieldValuesByResourceId(id);
        clueGetResponse.setModuleFields(clueFields);
        return clueGetResponse;
    }

    /**
     * 批量获取线索详情 (用于数据源批量查询优化)
     *
     * @param ids 线索ID集合
     * @return 线索详情列表
     */
    public List<ClueGetResponse> batchGetSimpleByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        // 批量查询资源基本信息
        List<Clue> clues = clueMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(clues)) {
            return Collections.emptyList();
        }
        // 批量查询自定义字段值
        Map<String, List<BaseModuleFieldValue>> fieldValueMap = clueFieldService.getResourceFieldMap(ids, true);

        // 组装结果
        return clues.stream().map(clue -> {
            ClueGetResponse response = BeanUtils.copyBean(new ClueGetResponse(), clue);
            response.setModuleFields(fieldValueMap.get(clue.getId()));
            return response;
        }).toList();
    }

    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.ADD)
    @HitApproval(formKey = FormKey.CLUE, executeType = ExecuteTimingEnum.CREATE, operatorId = "{#userId}")
    public Clue add(ClueAddRequest request, String userId, String orgId) {
        productService.checkProductList(request.getProducts());
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        if (StringUtils.isBlank(request.getOwner())) {
            clue.setOwner(userId);
        }
        poolClueService.validateCapacity(1, clue.getOwner(), orgId);
        clue.setCreateTime(System.currentTimeMillis());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setCollectionTime(clue.getCreateTime());
        clue.setUpdateUser(userId);
        clue.setCreateUser(userId);
        clue.setOrganizationId(orgId);
        clue.setId(IDGenerator.nextStr());
        clue.setStage(ClueStatus.NEW.name());
        clue.setInSharedPool(false);
        clue.setFrozen(false);
        clue.setApprovalStatus(ApprovalStatus.NONE.name());
        clue.setApproved(false);

        //保存自定义字段
        clueFieldService.saveModuleField(clue, orgId, userId, request.getModuleFields(), false);

        clueMapper.insert(clue);
        // 统计字段: 本条记录刚建好, 先按各统计字段的空值口径把值行落一次
        statisticFieldService.refreshDataStatisticFields(FormKey.CLUE.getKey(), clue.getId(), orgId);
        // 统计字段: 新数据可能关联到了别的表单记录, 被关联记录的统计值要跟着重算
        statisticFieldService.refreshByRelatedDataChange(FormKey.CLUE.getKey(), clue.getId(), orgId);
        baseService.handleAddLogWithResourceName(clue, request.getModuleFields());

        // 消息通知
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_ADD, clue.getName(), userId,
                orgId, List.of(clue.getOwner()), true);
        return clue;
    }

    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    @HitApproval(formKey = FormKey.CLUE, executeType = ExecuteTimingEnum.UPDATE, resourceId = "{#request.id}", updateType = "{#request.updateType}", operatorId = "{#userId}", comment = "{#request.comment}")
    public Clue update(ClueUpdateRequest request, String userId, String orgId) {
        productService.checkProductList(request.getProducts());
        Clue originClue = clueMapper.selectByPrimaryKey(request.getId());
        // 审批回退是把线索还给原负责人, 不是一次新的分配, 不该受负责人容量限制;
        // 若在这里抛异常, 会被 revertToSnapshot 的 catch 吞掉, 业务数据会原地不动
        if (!Strings.CS.equals(originClue.getOwner(), request.getOwner())
                && !isApprovalRevert(request.getUpdateType())) {
            poolClueService.validateCapacity(1, request.getOwner(), orgId);
        }

        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        // 保留审批状态, 编辑不改变审批状态
        clue.setApprovalStatus(originClue.getApprovalStatus());
        clue.setApproved(originClue.getApproved());

        if (StringUtils.isNotBlank(request.getOwner())) {
            if (!Strings.CS.equals(request.getOwner(), originClue.getOwner())) {
                // 如果责任人有修改，则添加责任人历史
                // 审批回退的负责人变更是回退动作本身, 不再记一条, 否则历史里会残留一次并未真正发生的负责人变更
                if (!isApprovalRevert(request.getUpdateType())) {
                    clueOwnerHistoryService.add(originClue, userId, false);
                }
                sendTransferNotice(List.of(originClue), request.getOwner(), userId, orgId);
            }
        }

        // 获取模块字段
        List<BaseModuleFieldValue> originCustomerFields = clueFieldService.getModuleFieldValuesByResourceId(request.getId());

        // 统计字段: 关联字段在下面会被覆盖, 改之前先把它当前指向的宿主捕下来 ——
        // 改成别的关联对象时, 变更前那条宿主的统计值会偏大, 而改完就再也查不出它了
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHosts(
                FormKey.CLUE.getKey(), List.of(request.getId()), orgId);

        if (BooleanUtils.isTrue(request.getAgentInvoke())) {
            clueFieldService.updateModuleFieldByAgent(clue, originCustomerFields, request.getModuleFields(), orgId, userId);
        } else {
            // 更新模块字段
            updateModuleField(clue, request.getModuleFields(), orgId, userId);
        }

        clueMapper.update(clue);
        clue = clueMapper.selectByPrimaryKey(request.getId());
        // 统计字段: 改前改后关联到的宿主记录都要重算(关联没动时这两批是同一批, 去重后只算一次)
        statisticFieldService.refreshAfterRelatedChange(statisticScope, List.of(request.getId()));
        baseService.handleUpdateLog(originClue, clue, originCustomerFields, request.getModuleFields(), originClue.getId(), originClue.getName());
        return clueMapper.selectByPrimaryKey(clue.getId());
    }

    private void sendTransferNotice(List<Clue> originClues, String toUser, String userId, String orgId) {
        originClues.forEach(clue -> commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.TRANSFER_CLUE, clue.getName(), userId,
                orgId, List.of(toUser), true));
    }

    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void updateStatus(ClueStatusUpdateRequest request, String userId, String orgId) {
        Clue originClue = clueMapper.selectByPrimaryKey(request.getId());
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        // 记录修改前的状态
        clue.setLastStage(originClue.getStage());
        clueMapper.update(clue);
        // 日志
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(originClue.getName())
                        .originalValue(originClue)
                        .modifiedValue(clueMapper.selectByPrimaryKey(request.getId()))
                        .build()
        );
    }

    private void updateModuleField(Clue clue, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        clueFieldService.deleteByResourceId(clue.getId());
        // 再保存
        clueFieldService.saveModuleField(clue, orgId, userId, moduleFields, true);
    }

    /**
     * 转移客户
     *
     * @param request 请求参数
     * @param userId  用户ID
     * @param orgId   组织ID
     */
    public void transitionCustomer(ClueTransitionCustomerRequest request, String userId, String orgId) {
        Customer customer = customerService.add(request, userId, orgId);
        Clue clue = clueMapper.selectByPrimaryKey(request.getClueId());
        clue.setTransitionId(customer.getId());
        clue.setTransitionType(FormKey.CUSTOMER.name());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        clueMapper.update(clue);

        // 同步添加联系人
        LambdaQueryWrapper<CustomerContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerContact::getPhone, clue.getPhone());
        List<CustomerContact> contacts = customerContactMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(contacts)) {
            CustomerContact contact = new CustomerContact();
            contact.setId(IDGenerator.nextStr());
            contact.setCustomerId(customer.getId());
            contact.setOwner(customer.getOwner());
            contact.setName(clue.getContact());
            contact.setPhone(clue.getPhone());
            contact.setEnable(true);
            contact.setOrganizationId(orgId);
            contact.setCreateUser(userId);
            contact.setCreateTime(System.currentTimeMillis());
            contact.setUpdateUser(userId);
            contact.setUpdateTime(System.currentTimeMillis());
            customerContactMapper.insert(contact);
        }

        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_CONVERT_CUSTOMER, clue.getName(), userId,
                orgId, List.of(clue.getOwner()), true);
    }


    /**
     * 删除线索（带审批校验）
     *
     * @param id     线索ID
     * @param userId 用户ID
     * @param orgId  组织ID
     */
    @HitApproval(formKey = FormKey.CLUE, executeType = ExecuteTimingEnum.DELETE, resourceId = "{#id}", operatorId = "{#userId}")
    public void deleteWithApprovalCheck(String id, String userId, String orgId) {
        delete(id, userId, orgId);
    }

    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        // 统计字段: 删除会一并带走关联字段的值, 宿主关系只能删前先捕; 重算要等下面全部删完才准。
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHosts(
                FormKey.CLUE.getKey(), List.of(id), orgId);
        // 删除客户
        clueMapper.deleteByPrimaryKey(id);
        // 删除客户模块字段
        clueFieldService.deleteByResourceId(id);
        // 删除责任人历史
        clueOwnerHistoryService.deleteByClueIds(List.of(id));
        // 删除跟进记录
        followUpRecordService.deleteByClueIds(List.of(id));
        // 删除跟进计划
        followUpPlanService.deleteByClueIds(List.of(id));
        statisticFieldService.refreshAfterRelatedDelete(statisticScope);

        // 设置操作对象
        OperationLogContext.setResourceName(clue.getName());

        // 消息通知
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_DELETED, clue.getName(), userId,
                orgId, List.of(clue.getOwner()), true);

    }

    public BatchAffectReasonResponse batchTransfer(ClueBatchTransferRequest request, String userId, String orgId) {
        List<Clue> clues = clueMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(clues)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0)
                    .errorMessages(Translator.get("clue.not.exist")).build();
        }
        long processCount = clues.stream().filter(clue -> !Strings.CS.equals(clue.getOwner(), request.getOwner())).count();
        poolClueService.validateCapacity((int) processCount, request.getOwner(), orgId);

        // 转移 SQL 只写负责人真正发生变化的线索, 审批同样只对这些线索触发, 避免给原本就是这个负责人的线索凭空建一条审批
        List<String> changedIds = clues.stream()
                .filter(clue -> !Strings.CS.equals(clue.getOwner(), request.getOwner()))
                .map(Clue::getId)
                .toList();
        // 快照须在转移前落库, 否则审批驳回/撤回时回退到的是转移后的负责人
        CommonBeanFactory.getBean(ApprovalResourceService.class).batchTransferTriggerApproval(
                changedIds, BusinessModuleField.CLUE_OWNER, FormKey.CLUE, orgId, userId, request.getOwner());

        // 添加责任人历史
        clueOwnerHistoryService.batchAdd(request, userId);
        extClueMapper.batchTransfer(request);

        // 记录日志
        List<LogDTO> logs = clues.stream()
                .map(clue -> {
                    Customer originCustomer = new Customer();
                    originCustomer.setOwner(clue.getOwner());
                    Customer modifieCustomer = new Customer();
                    modifieCustomer.setOwner(request.getOwner());
                    LogDTO logDTO = new LogDTO(orgId, clue.getId(), userId, LogType.UPDATE, LogModule.CLUE_INDEX, clue.getName());
                    logDTO.setOriginalValue(originCustomer);
                    logDTO.setModifiedValue(modifieCustomer);
                    return logDTO;
                }).toList();

        logService.batchAdd(logs);

        sendTransferNotice(clues, request.getOwner(), userId, orgId);

        // success: 真的换了负责人; skip: 负责人本来就是目标负责人; fail: 入参里有查不到的线索
        return BatchAffectReasonResponse.builder()
                .success((int) processCount)
                .fail(CollectionUtils.size(request.getIds()) - clues.size())
                .skip(clues.size() - (int) processCount)
                .errorMessages(Translator.get("batch.transfer.reason"))
                .build();
    }

    /**
     * 是否为审批驳回/撤回触发的回退更新
     * <p>
     * 回退复用的是编辑接口, 但语义是"把数据还原回去"而非一次新的编辑:
     * 不该占用负责人容量, 也不该再产生一条负责人变更记录。
     *
     * @param updateType 更新类型
     * @return true 表示这是回退
     */
    private boolean isApprovalRevert(String updateType) {
        return ApprovalResourceUpdateType.APPROVAL.getValue().equals(updateType);
    }

    public void batchDelete(List<String> ids, String userId, String orgId) {
        List<Clue> clues = clueMapper.selectByIds(ids);
        // 状态权限校验: 过滤出当前用户有权删除的线索
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.CLUE.getValue(),
                clues,
                PermissionConstants.CLUE_MANAGEMENT_DELETE,
                orgId,
                Clue::getId,
                Clue::getApprovalStatus
        );
        if (CollectionUtils.isEmpty(permittedIds)) {
            return;
        }

        List<Clue> permittedClues = clues.stream().filter(clue -> permittedIds.contains(clue.getId())).toList();
        List<String> toDoIds = permittedClues.stream().map(Clue::getId).toList();
        if (CollectionUtils.isEmpty(toDoIds)) {
            return;
        }

        // 命中删除审批流的线索不直接删除, 走审批
        Map<String, String> nameMap = permittedClues.stream()
                .collect(Collectors.toMap(Clue::getId, Clue::getName, (a, b) -> a));
        ApprovalResourceService approvalResourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
        List<String> approvalIds = approvalResourceService.batchDeleteTriggerApproval(
                toDoIds, FormKey.CLUE, orgId, userId, nameMap);
        List<String> deleteIds = toDoIds.stream().filter(id -> !approvalIds.contains(id)).toList();
        if (CollectionUtils.isEmpty(deleteIds)) {
            return;
        }

        // 统计字段: 删除会一并带走关联字段的值, 宿主关系只能删前先捕; 重算要等下面全部删完才准。
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHosts(
                FormKey.CLUE.getKey(), deleteIds, orgId);
        // 删除客户
        clueMapper.deleteByIds(deleteIds);
        // 删除客户模块字段
        clueFieldService.deleteByResourceIds(deleteIds);
        // 删除责任人历史
        clueOwnerHistoryService.deleteByClueIds(deleteIds);
        // 删除跟进记录
        followUpRecordService.deleteByClueIds(deleteIds);
        // 删除跟进计划
        followUpPlanService.deleteByClueIds(deleteIds);
        statisticFieldService.refreshAfterRelatedDelete(statisticScope);

        // 消息通知
        permittedClues.stream()
                .filter(clue -> deleteIds.contains(clue.getId()))
                .forEach(clue -> commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                        NotificationConstants.Event.CLUE_DELETED, clue.getName(), userId,
                        orgId, List.of(clue.getOwner()), true));
    }

    private List<String> getOwners(List<Clue> clues) {
        return clues.stream()
                .map(Clue::getOwner)
                .distinct()
                .toList();
    }

    /**
     * 批量移入线索池
     *
     * @param request     请求参数
     * @param orgId       组织ID
     * @param currentUser 当前用户
     */
    public BatchAffectResponse batchToPool(BatchPoolReasonRequest request, String currentUser, String orgId) {
        LambdaQueryWrapper<Clue> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Clue::getId, request.getIds());
        List<Clue> clues = clueMapper.selectListByLambda(wrapper);
        clues = clues.stream()
                .filter(clue -> !BooleanUtils.isTrue(clue.getInSharedPool()))
                .toList();
        if (CollectionUtils.isEmpty(clues)) {
            return BatchAffectResponse.builder().success(0).fail(request.getIds().size()).build();
        }

        CluePool targetPool = null;
        Map<String, CluePool> ownersDefaultPoolMap = new HashMap<>(4);
        if (StringUtils.isNotBlank(request.getPoolId())) {
            targetPool = getTargetCluePool(request.getPoolId(), orgId);
        } else {
            List<String> ownerIds = getOwners(clues);
            ownersDefaultPoolMap = cluePoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        }
        int success = 0;
        List<LogDTO> logs = new ArrayList<>();
        for (Clue clue : clues) {
            CluePool cluePool = targetPool != null ? targetPool : ownersDefaultPoolMap.get(clue.getOwner());
            if (cluePool == null) {
                // 未找到默认线索池，不移入
                continue;
            }
            // 日志
            LogDTO logDTO = new LogDTO(orgId, clue.getId(), currentUser, LogType.MOVE_TO_CUSTOMER_POOL, LogModule.CLUE_INDEX, clue.getName());
            String detail = Translator.getWithArgs("clue.to.pool", clue.getName(), cluePool.getName());
            logDTO.setDetail(detail);
            logs.add(logDTO);
            // 消息通知
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                    NotificationConstants.Event.CLUE_MOVED_POOL, clue.getName(), currentUser,
                    orgId, List.of(clue.getOwner()), true);
            // 插入责任人历史
            clue.setReasonId(request.getReasonId());
            clueOwnerHistoryService.add(clue, currentUser, true);
            clue.setPoolId(cluePool.getId());
            clue.setInSharedPool(true);
            clue.setOwner(null);
            clue.setCollectionTime(null);
            clue.setUpdateUser(currentUser);
            clue.setUpdateTime(System.currentTimeMillis());
            extClueMapper.moveToPool(clue);
            success++;
        }
        logService.batchAdd(logs);
        return BatchAffectResponse.builder().success(success).fail(request.getIds().size() - success).build();
    }

    /**
     * 移入线索池
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param orgId       组织ID
     */
    public BatchAffectResponse toPool(PoolReasonRequest request, String currentUser, String orgId) {
        BatchPoolReasonRequest batchRequest = new BatchPoolReasonRequest();
        batchRequest.setReasonId(request.getReasonId());
        batchRequest.setPoolId(request.getPoolId());
        batchRequest.setIds(List.of(request.getId()));
        return batchToPool(batchRequest, currentUser, orgId);
    }

    private CluePool getTargetCluePool(String poolId, String orgId) {
        CluePool cluePool = cluePoolMapper.selectByPrimaryKey(poolId);
        if (cluePool == null || !Strings.CS.equals(cluePool.getOrganizationId(), orgId) || !BooleanUtils.isTrue(cluePool.getEnable())) {
            throw new GenericException(Translator.get("clue_pool_not_exist"));
        }
        return cluePool;
    }

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String organizationId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, organizationId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CLUE_MANAGEMENT_READ, rolePermissions);
    }


    public String getClueName(String id) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(clue).map(Clue::getName).orElse(null);
    }

    public List<Clue> getClueListByNames(List<String> names) {
        LambdaQueryWrapper<Clue> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Clue::getName, names);
        return clueMapper.selectListByLambda(lambdaQueryWrapper);
    }

    @SuppressWarnings("unchecked")
    public String getClueNameByIds(List<String> ids) {
        List<Clue> clueList = clueMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(clueList)) {
            List<String> names = clueList.stream().map(Clue::getName).toList();
            return String.join(",", JSON.parseArray(JSON.toJSONString(names)));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 批量关联线索和客户
     *
     * @param request     关联参数
     * @param currentUser 当前用户
     * @param orgId       组织ID
     */
    public void batchTransition(BatchReTransitionCustomerRequest request, String currentUser, String orgId) {
        if (CollectionUtils.isEmpty(request.getClueIds())) {
            throw new GenericException(Translator.get("clue_ids_not_empty"));
        }
        Customer customer = customerMapper.selectByPrimaryKey(request.getCustomerId());
        // 操作人尝试领取公海客户, 领取失败则不继续
        if (customer.getInSharedPool()) {
            PoolCustomerPickRequest pickRequest = new PoolCustomerPickRequest();
            pickRequest.setCustomerId(customer.getId());
            pickRequest.setPoolId(customer.getPoolId());
            poolCustomerService.pick(pickRequest, currentUser, orgId);
        }
        request.getClueIds().forEach(clueId -> transitionCs(clueId, customer, currentUser, orgId));
    }

    /**
     * 线索关联已有客户
     *
     * @param clueId      线索ID
     * @param currentUser 当前用户ID
     * @param orgId       组织ID
     */
    public void transitionCs(String clueId, Customer transitionCs, String currentUser, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        // 负责人不存在, 跳过关联
        List<String> owners = extUserMapper.selectUserNameByIds(List.of(clue.getOwner()));
        if (CollectionUtils.isEmpty(owners)) {
            return;
        }

        // 关联
        TransformCsAssociateDTO transformCsAssociateDTO = transformCsAssociate(clue, transitionCs, currentUser, orgId);
        clue.setTransitionId(transitionCs.getId());
        clue.setTransitionType("CUSTOMER");
        clueMapper.update(clue);

        // 转移线索的计划&记录
        batchCopyCluePlanAndRecord(clue.getId(), transitionCs.getId(), null, transformCsAssociateDTO.getContactId());
        // 刷新转换过程中同名客户的最新跟进时间
        refreshCsFollowTime(clue, transitionCs);

        // 只通知线索负责人
        Map<String, Object> paramMap = new HashMap<>(8);
        paramMap.put("useTemplate", "true");
        paramMap.put("template", Translator.get("message.clue_relate_customer"));
        paramMap.put("customerName", transitionCs.getName());
        paramMap.put("name", clue.getName());
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE, NotificationConstants.Event.CLUE_CONVERT_CUSTOMER,
                paramMap, currentUser, orgId, List.of(clue.getOwner()), true);
    }

    /**
     * 线索转换
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param orgId       组织ID
     */
    public String transform(ClueTransformRequest request, String currentUser, String orgId) {
        checkTransformPermission(request.getOppCreated());
        Clue clue = clueMapper.selectByPrimaryKey(request.getClueId());
        if (clue == null) {
            throw new GenericException(Translator.get("clue_not_exist"));
        }
        List<String> owners = extUserMapper.selectUserNameByIds(List.of(clue.getOwner()));
        if (CollectionUtils.isEmpty(owners)) {
            throw new GenericException(Translator.get("clue_owner_not_exist"));
        }

        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getName, clue.getName());
        List<Customer> customers = customerMapper.selectListByLambda(wrapper);
        boolean uniqueCheck = moduleFormService.hasFieldUniqueCheck(FormKey.CUSTOMER.getKey(), orgId, BusinessModuleField.CUSTOMER_NAME.getKey());
        Customer transformCustomer;
        if (uniqueCheck && CollectionUtils.isNotEmpty(customers)) {
            // 表单存在唯一性校验, 且同名客户存在, 根据规则选取
            transformCustomer = selectorCs(customers, clue.getOwner());
        } else {
            // 根据表单联动来创建客户
            transformCustomer = generateCustomerByLinkForm(clue, currentUser, orgId);
        }
        // 刷新转换过程中同名客户的最新跟进时间
        refreshCsFollowTime(clue, transformCustomer);

        TransformCsAssociateDTO transformCsAssociateDTO = transformCsAssociate(clue, transformCustomer, currentUser, orgId);
        clue.setTransitionId(transformCustomer.getId());
        clue.setTransitionType("CUSTOMER");
        clueMapper.update(clue);

        // 客户转换通知
        Map<String, Object> paramMap = new HashMap<>(4);
        paramMap.put("useTemplate", "true");
        paramMap.put("template", Translator.get("message.clue_convert_customer_text"));
        paramMap.put("name", clue.getName());
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE, NotificationConstants.Event.CLUE_CONVERT_CUSTOMER,
                paramMap, currentUser, orgId, List.of(clue.getOwner()), true);

        // 是否转换商机
        if (request.getOppCreated()) {
            transformCustomer.setName(request.getOppName());
            Opportunity transformOpportunity = generateOpportunityByLinkForm(clue, transformCsAssociateDTO.getContactId(), transformCustomer, currentUser, orgId);
            // 转移线索的计划&记录
            batchCopyCluePlanAndRecord(clue.getId(), transformCustomer.getId(), transformOpportunity.getId(), transformCsAssociateDTO.getContactId());
            paramMap.put("template", Translator.get("message.clue_convert_business_text"));
            paramMap.put("name", clue.getName());
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE, NotificationConstants.Event.CLUE_CONVERT_BUSINESS,
                    paramMap, currentUser, orgId, List.of(clue.getOwner()), true);
            return transformOpportunity.getId();
        } else {
            // 转移线索的计划&记录
            batchCopyCluePlanAndRecord(clue.getId(), transformCustomer.getId(), null, transformCsAssociateDTO.getContactId());
            return transformCustomer.getId();
        }
    }

    /**
     * 批量复制线索跟进计划和记录
     *
     * @param clueId        线索ID
     * @param customerId    客户ID
     * @param opportunityId 商机ID
     */
    public void batchCopyCluePlanAndRecord(String clueId, String customerId, String opportunityId, String contactId) {
        // 记录
        LambdaQueryWrapper<FollowUpRecord> recordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        recordLambdaQueryWrapper.eq(FollowUpRecord::getClueId, clueId).eq(FollowUpRecord::getType, FollowUpPlanType.CLUE.name());
        List<FollowUpRecord> followUpRecords = followUpRecordMapper.selectListByLambda(recordLambdaQueryWrapper);
        if (CollectionUtils.isNotEmpty(followUpRecords)) {
            List<String> ids = followUpRecords.stream().map(FollowUpRecord::getId).toList();
            LambdaQueryWrapper<FollowUpRecordField> fieldLambdaQueryWrapper = new LambdaQueryWrapper<>();
            fieldLambdaQueryWrapper.in(FollowUpRecordField::getResourceId, ids);
            List<FollowUpRecordField> followUpRecordFields = followUpRecordFieldMapper.selectListByLambda(fieldLambdaQueryWrapper);
            LambdaQueryWrapper<FollowUpRecordFieldBlob> fieldBlobLambdaQueryWrapper = new LambdaQueryWrapper<>();
            fieldBlobLambdaQueryWrapper.in(FollowUpRecordFieldBlob::getResourceId, ids);
            List<FollowUpRecordFieldBlob> followUpRecordFieldBlobs = followUpRecordFieldBlobMapper.selectListByLambda(fieldBlobLambdaQueryWrapper);
            followUpRecords.forEach(record -> {
                String recordId = IDGenerator.nextStr();
                followUpRecordFields.stream().filter(recordField -> Strings.CS.equals(recordField.getResourceId(), record.getId())).forEach(field -> {
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(recordId);
                });
                followUpRecordFieldBlobs.stream().filter(recordFieldBlob -> Strings.CS.equals(recordFieldBlob.getResourceId(), record.getId())).forEach(fieldBlob -> {
                    fieldBlob.setId(IDGenerator.nextStr());
                    fieldBlob.setResourceId(recordId);
                });
                record.setId(recordId);
                record.setClueId(null);
                record.setType(FollowUpPlanType.CUSTOMER.name());
                record.setCustomerId(customerId);
                record.setOpportunityId(opportunityId);
                record.setContactId(contactId);
                record.setCommentCount(0L);
            });
            followUpRecordMapper.batchInsert(followUpRecords);
            followUpRecordFieldMapper.batchInsert(followUpRecordFields);
            followUpRecordFieldBlobMapper.batchInsert(followUpRecordFieldBlobs);
        }
        // 计划
        LambdaQueryWrapper<FollowUpPlan> planLambdaQueryWrapper = new LambdaQueryWrapper<>();
        planLambdaQueryWrapper.eq(FollowUpPlan::getClueId, clueId).eq(FollowUpPlan::getType, FollowUpPlanType.CLUE.name());
        List<FollowUpPlan> followUpPlans = followUpPlanMapper.selectListByLambda(planLambdaQueryWrapper);
        if (CollectionUtils.isNotEmpty(followUpPlans)) {
            List<String> ids = followUpPlans.stream().map(FollowUpPlan::getId).toList();
            LambdaQueryWrapper<FollowUpPlanField> fieldLambdaQueryWrapper = new LambdaQueryWrapper<>();
            fieldLambdaQueryWrapper.in(FollowUpPlanField::getResourceId, ids);
            List<FollowUpPlanField> followUpPlanFields = followUpPlanFieldMapper.selectListByLambda(fieldLambdaQueryWrapper);
            LambdaQueryWrapper<FollowUpPlanFieldBlob> fieldBlobLambdaQueryWrapper = new LambdaQueryWrapper<>();
            fieldBlobLambdaQueryWrapper.in(FollowUpPlanFieldBlob::getResourceId, ids);
            List<FollowUpPlanFieldBlob> followUpPlanFieldBlobs = followUpPlanFieldBlobMapper.selectListByLambda(fieldBlobLambdaQueryWrapper);
            followUpPlans.forEach(plan -> {
                String planId = IDGenerator.nextStr();
                followUpPlanFields.stream().filter(planField -> Strings.CS.equals(planField.getResourceId(), plan.getId())).forEach(field -> {
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(planId);
                });
                followUpPlanFieldBlobs.stream().filter(planFieldBlob -> Strings.CS.equals(planFieldBlob.getResourceId(), plan.getId())).forEach(fieldBlob -> {
                    fieldBlob.setId(IDGenerator.nextStr());
                    fieldBlob.setResourceId(planId);
                });
                plan.setId(planId);
                plan.setClueId(null);
                plan.setType(FollowUpPlanType.CUSTOMER.name());
                plan.setCustomerId(customerId);
                plan.setOpportunityId(opportunityId);
                plan.setContactId(contactId);
                plan.setCommentCount(0L);
            });
            followUpPlanMapper.batchInsert(followUpPlans);
            followUpPlanFieldMapper.batchInsert(followUpPlanFields);
            followUpPlanFieldBlobMapper.batchInsert(followUpPlanFieldBlobs);
        }
    }

    /**
     * 检查转换权限
     *
     * @param checkOpportunityPermission 是否检查商机权限
     */
    public void checkTransformPermission(boolean checkOpportunityPermission) {
        if (!PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_ADD)) {
            throw new GenericException(Translator.get("transform.miss.customer.permission"));
        }
        if (checkOpportunityPermission && !PermissionUtils.hasPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_ADD)) {
            throw new GenericException(Translator.get("transform.miss.opportunity.permission"));
        }
    }

    /**
     * 同名客户选择器
     *
     * @param customers 客户列表
     * @return 客户
     */
    public Customer selectorCs(List<Customer> customers, String clueOwner) {
        if (customers.size() == 1) {
            return customers.getFirst();
        }
        // 优先选择不在公海的且负责人一致的客户
        Optional<Customer> find = customers.stream().filter(customer -> !customer.getInSharedPool() && Strings.CS.equals(customer.getOwner(), clueOwner))
                .findFirst();
        return find.orElseGet(customers::getFirst);
    }

    /**
     * 通过表单联动来创建客户
     *
     * @param clue        线索
     * @param currentUser 当前用户
     * @param orgId       组织ID
     * @return 客户
     */
    public Customer generateCustomerByLinkForm(Clue clue, String currentUser, String orgId) {
        CustomerAddRequest addRequest = new CustomerAddRequest();
        ModuleFormConfigDTO customerFormConfig = moduleFormService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), orgId);
        FormLinkFill<Customer> customerLinkFillDTO;
        try {
            customerLinkFillDTO = moduleFormService.fillFormLinkValue(new Customer(), get(clue.getId()),
                    customerFormConfig, orgId, FormKey.CLUE.getKey(), LinkScenarioKey.CLUE_TO_CUSTOMER.name());
        } catch (Exception e) {
            log.error("Attempt to fill customer form error: {}", e.getMessage());
            throw new GenericException(Translator.get("transform.customer.error"));
        }
        // 部分内置字段未配置联动, 取线索值即可
        addRequest.setName(customerLinkFillDTO.getEntity() == null || StringUtils.isEmpty(customerLinkFillDTO.getEntity().getName()) ?
                clue.getName() : customerLinkFillDTO.getEntity().getName());
        addRequest.setOwner(customerLinkFillDTO.getEntity() == null || StringUtils.isEmpty(customerLinkFillDTO.getEntity().getOwner()) ?
                clue.getOwner() : customerLinkFillDTO.getEntity().getOwner());
        addRequest.setModuleFields(customerLinkFillDTO.getFields());
        addRequest.setFollower(clue.getFollower());
        addRequest.setFollowTime(clue.getFollowTime());
        return customerService.add(addRequest, currentUser, orgId);
    }

    /**
     * 通过表单联动来创建商机
     *
     * @param clue        线索
     * @param contactId   联系人ID
     * @param currentUser 当前用户
     * @param orgId       组织ID
     * @return 商机
     */
    public Opportunity generateOpportunityByLinkForm(Clue clue, String contactId, Customer transformCustomer, String currentUser, String orgId) {
        ModuleFormConfigDTO opportunityFormConfig = moduleFormService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), orgId);
        FormLinkFill<Opportunity> opportunityLinkFillDTO;
        try {
            opportunityLinkFillDTO = moduleFormService.fillFormLinkValue(new Opportunity(), get(clue.getId()),
                    opportunityFormConfig, orgId, FormKey.CLUE.getKey(), LinkScenarioKey.CLUE_TO_OPPORTUNITY.name());
        } catch (Exception e) {
            log.error("Attempt to fill opportunity form error: {}", e.getMessage());
            throw new GenericException(Translator.get("transform.opportunity.error"));
        }
        OpportunityAddRequest addRequest = new OpportunityAddRequest();
        if (opportunityLinkFillDTO.getEntity() != null) {
            BeanUtils.copyBean(addRequest, opportunityLinkFillDTO.getEntity());
        }
        // 部分内置字段需手动设置值
        addRequest.setName(transformCustomer.getName());
        addRequest.setCustomerId(transformCustomer.getId());
        if (CollectionUtils.isEmpty(addRequest.getProducts())) {
            addRequest.setProducts(new ArrayList<>());
        }
        if (StringUtils.isEmpty(addRequest.getOwner())) {
            addRequest.setOwner(clue.getOwner());
        }
        if (StringUtils.isEmpty(addRequest.getContactId())) {
            addRequest.setContactId(contactId);
        }
        addRequest.setModuleFields(opportunityLinkFillDTO.getFields());
        addRequest.setFollower(clue.getFollower());
        addRequest.setFollowTime(clue.getFollowTime());
        return opportunityService.add(addRequest, currentUser, orgId);
    }

    /**
     * 通过表单联动来构建客户联系人创建对象
     *
     * @param clue  线索
     * @param orgId 组织ID
     * @return 客户联系人创建对象
     */
    public CustomerContactAddRequest buildContactRequestByLinkForm(Clue clue, String orgId) {
        ModuleFormConfigDTO contactFormConfig = moduleFormService.getBusinessFormConfig(FormKey.CONTACT.getKey(), orgId);
        FormLinkFill<CustomerContactAddRequest> fillDTO = null;
        try {
            fillDTO = moduleFormService.fillFormLinkValue(new CustomerContactAddRequest(), get(clue.getId()),
                    contactFormConfig, orgId, FormKey.CLUE.getKey(), LinkScenarioKey.CLUE_TO_CONTACT.name());
        } catch (Exception e) {
            log.error("Attempt to fill contact form error: {}", e.getMessage());
        }
        if (fillDTO == null || fillDTO.getEntity() == null) {
            return new CustomerContactAddRequest();
        }
        CustomerContactAddRequest request = fillDTO.getEntity();
        request.setModuleFields(fillDTO.getFields());
        return request;
    }

    /**
     * 转换客户处理
     *
     * @param clue        线索
     * @param transformCs 转换客户
     * @param currentUser 当前用户
     * @param orgId       组织ID
     */
    public TransformCsAssociateDTO transformCsAssociate(Clue clue, Customer transformCs, String currentUser, String orgId) {
        TransformCsAssociateDTO transformDTO = new TransformCsAssociateDTO();
        // 如果当前线索负责人不是关联客户的负责人，且不是客户协作人, 则添加协作关系
        if (!Strings.CS.equals(transformCs.getOwner(), clue.getOwner()) && !customerCollaborationService.hasCollaboration(clue.getOwner(), transformCs.getId())) {
            CustomerCollaborationAddRequest collaborationAddRequest = new CustomerCollaborationAddRequest();
            collaborationAddRequest.setCustomerId(transformCs.getId());
            collaborationAddRequest.setCollaborationType("COLLABORATION");
            collaborationAddRequest.setUserId(clue.getOwner());
            customerCollaborationService.add(collaborationAddRequest, currentUser, orgId);
        }

        // 线索联系人 => 客户联系人
        CustomerContactAddRequest request = buildContactRequestByLinkForm(clue, orgId);
        if (StringUtils.isEmpty(request.getName())) {
            return transformDTO;
        }
        boolean unique = customerContactService.checkCustomerContactUnique(request.getName(), request.getPhone(), transformCs.getId(), orgId);
        if (unique) {
            request.setCustomerId(transformCs.getId());
            if (StringUtils.isEmpty(request.getOwner())) {
                request.setOwner(clue.getOwner());
            }
            CustomerContact contact = customerContactService.add(request, currentUser, orgId);
            transformDTO.setContactId(contact.getId());
        }

        return transformDTO;
    }

    /**
     * 下载导入的模板
     *
     * @param response 响应
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
        new EasyExcelExporter().exportMultiSheetTplWithSharedHandler(response,
                moduleFormService.getCustomImportHeadsNoRef(FormKey.CLUE.getKey(), currentOrg),
                Translator.get("clue.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
                new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(FormKey.CLUE.getKey(), currentOrg)),
                new CustomHeadColWidthStyleStrategy());
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
     * 线索导入
     *
     * @param file        导入文件
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     * @return 导入返回信息
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResponse realImport(MultipartFile file, ImportRequest request, String currentOrg, String currentUser) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CLUE.getKey(), currentOrg);
            CustomImportAfterDoConsumer<Clue, BaseResourceSubField> afterDo = (clues, clueFields, clueFieldBlobs) -> {
                List<LogDTO> logs = new ArrayList<>();
                ImportType importType = EnumUtils.valueOf(ImportType.class, request.getImportType());
                switch (importType) {
                    case ADD -> {
                        clues.forEach(clue -> {
                            clue.setCollectionTime(clue.getCreateTime());
                            clue.setStage(ClueStatus.NEW.name());
                            clue.setInSharedPool(false);
                            clue.setFrozen(false);
                            logs.add(new LogDTO(currentOrg, clue.getId(), currentUser, LogType.ADD, LogModule.CLUE_INDEX, clue.getName()));
                        });
                        clueMapper.batchInsert(clues);
                        clueFieldMapper.batchInsert(clueFields.stream().map(field -> BeanUtils.copyBean(new ClueField(), field)).toList());
                        clueFieldBlobMapper.batchInsert(clueFieldBlobs.stream().map(field -> BeanUtils.copyBean(new ClueFieldBlob(), field)).toList());
                        // 日志
                        logService.batchAdd(logs);
                    }
                    case UPDATE -> {
                        List<String> ids = clues.stream().map(Clue::getId).toList();
                        if (CollectionUtils.isEmpty(ids)) {
                            break;
                        }
                        //原数据
                        List<Clue> originClueList = clueMapper.selectByIds(ids);
                        if (CollectionUtils.isEmpty(originClueList)) {
                            break;
                        }
                        Map<String, Clue> originClueMaps = originClueList.stream().collect(Collectors.toMap(Clue::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> originFieldValueMap = clueFieldService.getResourceFieldMap(ids, true);

                        List<ClueField> insertField = new ArrayList<>();
                        List<ClueFieldBlob> insertFieldBlob = new ArrayList<>();
                        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
                        ExtClueMapper clueBatchMapper = sqlSession.getMapper(ExtClueMapper.class);
                        CommonMapper commonMapper = sqlSession.getMapper(CommonMapper.class);
                        //更新
                        if (CollectionUtils.isNotEmpty(clues)) {
                            clues.forEach(clue -> {
                                clue.setInSharedPool(false);
                                clueBatchMapper.updateClue(clue);
                            });
                        }

                        if (CollectionUtils.isNotEmpty(clueFields)) {
                            List<ClueField> fieldList = clueFieldMapper.selectByIds(clueFields.stream().map(BaseResourceSubField::getId).toList());
                            Map<String, ClueField> fieldMap = fieldList.stream().collect(Collectors.toMap(ClueField::getId, Function.identity()));
                            clueFields.forEach(clueField -> {
                                if (fieldMap.containsKey(clueField.getId())) {
                                    commonMapper.updateCustomerField("clue_field", clueField);
                                } else {
                                    insertField.add(BeanUtils.copyBean(new ClueField(), clueField));
                                }
                            });
                        }

                        if (CollectionUtils.isNotEmpty(clueFieldBlobs)) {
                            List<ClueFieldBlob> blobList = clueFieldBlobMapper.selectByIds(clueFieldBlobs.stream().map(BaseResourceSubField::getId).toList());
                            Map<String, ClueFieldBlob> blobMap = blobList.stream().collect(Collectors.toMap(ClueFieldBlob::getId, Function.identity()));
                            clueFieldBlobs.forEach(clueFieldBlob -> {
                                if (blobMap.containsKey(clueFieldBlob.getId())) {
                                    commonMapper.updateCustomerField("clue_field_blob", clueFieldBlob);
                                } else {
                                    insertFieldBlob.add(BeanUtils.copyBean(new ClueFieldBlob(), clueFieldBlob));
                                }
                            });
                        }

                        sqlSession.flushStatements();
                        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);

                        if (CollectionUtils.isNotEmpty(insertField)) {
                            clueFieldMapper.batchInsert(insertField);
                        }
                        if (CollectionUtils.isNotEmpty(insertFieldBlob)) {
                            clueFieldBlobMapper.batchInsert(insertFieldBlob);
                        }

                        SqlSession currentSession =
                                SqlSessionUtils.getSqlSession(sqlSessionFactory);
                        currentSession.clearCache();

                        Map<String, Clue> modifiedClueMaps = clueMapper.selectByIds(ids).stream().collect(Collectors.toMap(Clue::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> modifiedFieldValueMap = clueFieldService.getResourceFieldMap(ids, true);

                        //日志
                        ids.forEach(id -> {
                            Clue originDate = originClueMaps.get(id);
                            Clue modifiedDate = modifiedClueMaps.get(id);
                            baseService.handleUpdateLog(originDate, modifiedDate, originFieldValueMap.get(id), modifiedFieldValueMap.get(id), id, modifiedDate.getName());
                            LogContextInfo contextInfo = OperationLogContext.getContext();
                            if (contextInfo != null) {
                                LogDTO logDTO = new LogDTO(currentOrg, id, currentUser, LogType.UPDATE, LogModule.CLUE_INDEX, modifiedDate.getName());
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
            CustomFieldImportEventListener<Clue> eventListener = new CustomFieldImportEventListener<>(fields, Clue.class, currentOrg, currentUser,
                    "clue_field", "clue_field_blob", afterDo, 2000, null, null, request.getImportType());
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("clue import error: ", e);
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
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(FormKey.CLUE.getKey(), currentOrg);
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "clue", "clue_field", currentOrg, importType);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("clue import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public BatchAffectReasonResponse batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = clueFieldService.getAndCheckField(request.getFieldId(), organizationId);
        List<Clue> originClues = clueMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(originClues)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0)
                    .errorMessages(Translator.get("clue.not.exist")).build();
        }
        // 状态权限校验: 过滤出当前用户有权编辑的线索。放在各分支之前, 是因为下面还有一条走批量转移
        // 的路径, 它同样是一次编辑 —— 否则处在无权编辑状态(如审批中)的线索会从这条路径绕过去
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.CLUE.getValue(),
                originClues,
                PermissionConstants.CLUE_MANAGEMENT_UPDATE,
                organizationId,
                Clue::getId,
                Clue::getApprovalStatus
        );
        if (CollectionUtils.isEmpty(permittedIds)) {
            return BatchAffectReasonResponse.builder().success(0).fail(originClues.size()).skip(0)
                    .errorMessages(Translator.get("no.operation.permission")).build();
        }

        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.CLUE_OWNER.getBusinessKey())) {
            // 修改负责人，走批量转移接口
            ClueBatchTransferRequest batchTransferRequest = new ClueBatchTransferRequest();
            batchTransferRequest.setIds(permittedIds);
            batchTransferRequest.setOwner(request.getFieldValue().toString());
            return batchTransfer(batchTransferRequest, userId, organizationId);
        }

        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.CLUE_PRODUCTS.getBusinessKey())) {
            productService.checkProductList((List<String>) request.getFieldValue());
        }

        ApprovalResourceService approvalResourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
        approvalResourceService.batchEditTriggerApproval(permittedIds, request.getFieldId(), FormKey.CLUE, organizationId, userId, field.getName(), request.getFieldValue());
        List<Clue> permittedClues = originClues.stream()
                .filter(clue -> permittedIds.contains(clue.getId()))
                .toList();

        ResourceBatchEditRequest filteredRequest = new ResourceBatchEditRequest();
        filteredRequest.setIds(permittedIds);
        filteredRequest.setFieldId(request.getFieldId());
        filteredRequest.setFieldValue(request.getFieldValue());

        // 统计字段: 批量编辑只改一个字段, 改的若是关联字段, 下面这批线索的关联关系会整批换人 ——
        // 换之前它们指向的宿主得先捕下来, 否则那些宿主的统计值会一直偏大; 改的不是关联字段时
        // 这一步在服务内部直接短路, 只多一次反查。用 permittedIds: 没权限的那些根本没被写
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHostsForFieldChange(
                FormKey.CLUE.getKey(), request.getFieldId(), permittedIds, organizationId);
        clueFieldService.batchUpdate(filteredRequest, field, permittedClues, Clue.class, LogModule.CLUE_INDEX, extClueMapper::batchUpdate, userId, organizationId);
        // 统计字段: 改前改后关联到的宿主记录都要重算(关联没动时这两批是同一批, 去重后只算一次)
        statisticFieldService.refreshAfterRelatedChange(statisticScope, permittedIds);

        return BatchAffectReasonResponse.builder()
                .success(permittedIds.size())
                .fail(originClues.size() - permittedIds.size())
                .skip(0)
                .errorMessages(Translator.get("batch.update.reason"))
                .build();
    }

    public List<ChartResult> chart(ChartAnalysisRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        formConfig.getFields().addAll(BaseChartService.getChartBaseFields());
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request, formConfig);
        ClueChartAnalysisDbRequest clueChartAnalysisDbRequest = BeanUtils.copyBean(new ClueChartAnalysisDbRequest(), chartAnalysisDbRequest);
        List<ChartResult> chartResults = extClueMapper.chart(clueChartAnalysisDbRequest, userId, orgId, deptDataPermission);
        return baseChartService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
    }

    /**
     * 处理已转移线索的跟进计划和记录
     */
    public void processTransferredCluePlanAndRecord() {
        List<FollowUpRecord> records = new ArrayList<>();
        List<FollowUpPlan> plans = new ArrayList<>();
        LambdaQueryWrapper<Clue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Clue::getTransitionType, "CUSTOMER");
        List<Clue> clues = clueMapper.selectListByLambda(wrapper);
        List<Clue> transferredClues = clues.stream().filter(clue -> StringUtils.isNotBlank(clue.getTransitionId())).toList();
        Map<String, String> clueTransferMap = transferredClues.stream().collect(Collectors.toMap(Clue::getId, Clue::getTransitionId));
        List<String> clueIds = transferredClues.stream().map(Clue::getId).toList();
        // 记录
        LambdaQueryWrapper<FollowUpRecord> recordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        recordLambdaQueryWrapper.in(FollowUpRecord::getClueId, clueIds).eq(FollowUpRecord::getType, FollowUpPlanType.CLUE.name());
        List<FollowUpRecord> clueRecords = followUpRecordMapper.selectListByLambda(recordLambdaQueryWrapper);
        Map<String, ClueFollowDTO> clueFollowMap = new HashMap<>(8);
        clueRecords.forEach(clueRecord -> {
            String customerId = clueTransferMap.get(clueRecord.getClueId());
            clueRecord.setId(IDGenerator.nextStr());
            clueRecord.setCustomerId(customerId);
            clueRecord.setClueId(null);
            clueRecord.setType(FollowUpPlanType.CUSTOMER.name());
            clueRecord.setCommentCount(0L);
            if (StringUtils.isNotBlank(clueRecord.getCustomerId())) {
                records.add(clueRecord);
                ClueFollowDTO clueFollowDTO = clueFollowMap.get(customerId);
                if (clueFollowDTO == null) {
                    clueFollowMap.put(customerId, ClueFollowDTO.builder().follower(clueRecord.getOwner())
                            .followerTime(clueRecord.getFollowTime()).build());
                } else {
                    Long recordTime = clueRecord.getFollowTime();
                    if (recordTime == null) {
                        return;
                    }
                    Long followerTime = clueFollowDTO.getFollowerTime();
                    if (followerTime == null || recordTime > followerTime) {
                        clueFollowDTO.setFollower(clueRecord.getOwner());
                        clueFollowDTO.setFollowerTime(recordTime);
                        clueFollowMap.put(customerId, clueFollowDTO);
                    }
                }
            }
        });
        // 计划
        LambdaQueryWrapper<FollowUpPlan> planLambdaQueryWrapper = new LambdaQueryWrapper<>();
        planLambdaQueryWrapper.in(FollowUpPlan::getClueId, clueIds).eq(FollowUpPlan::getType, FollowUpPlanType.CLUE.name());
        List<FollowUpPlan> cluePlans = followUpPlanMapper.selectListByLambda(planLambdaQueryWrapper);
        cluePlans.forEach(cluePlan -> {
            cluePlan.setId(IDGenerator.nextStr());
            cluePlan.setCustomerId(clueTransferMap.get(cluePlan.getClueId()));
            cluePlan.setClueId(null);
            cluePlan.setType(FollowUpPlanType.CUSTOMER.name());
            cluePlan.setCommentCount(0L);
            if (StringUtils.isNotBlank(cluePlan.getCustomerId())) {
                plans.add(cluePlan);
            }
        });
        // 批量插入
        if (CollectionUtils.isNotEmpty(records)) {
            followUpRecordMapper.batchInsert(records);
        }
        if (CollectionUtils.isNotEmpty(plans)) {
            followUpPlanMapper.batchInsert(plans);
        }
        // 更新客户最新跟进人和时间
        List<String> customerIds = clueFollowMap.keySet().stream().toList();
        List<Customer> customers = customerMapper.selectByIds(customerIds);
        customers.forEach(customer -> {
            if (customer.getFollowTime() == null || customer.getFollowTime() < clueFollowMap.get(customer.getId()).getFollowerTime()) {
                customer.setFollower(clueFollowMap.get(customer.getId()).getFollower());
                customer.setFollowTime(clueFollowMap.get(customer.getId()).getFollowerTime());
            }
            customerMapper.updateById(customer);
        });
    }

    /**
     * 刷新客户的最新跟进时间
     *
     * @param clue         线索信息
     * @param transitionCs 转移的客户信息
     */
    private void refreshCsFollowTime(Clue clue, Customer transitionCs) {
        Long clueFollowTime = clue.getFollowTime();
        Long customerFollowTime = transitionCs.getFollowTime();
        if (clueFollowTime == null) {
            return;
        }
        if (customerFollowTime == null || customerFollowTime < clueFollowTime) {
            Customer updateCustomer = new Customer();
            updateCustomer.setId(transitionCs.getId());
            updateCustomer.setFollower(clue.getFollower());
            updateCustomer.setFollowTime(clueFollowTime);
            customerMapper.updateById(updateCustomer);
        }
    }

    @Override
    public FormKey getFormKey() {
        return FormKey.CLUE;
    }

    /**
     * 更新业务快照审批状态
     * <p>
     * 线索没有业务快照表，编辑回退统一走框架的 {@code approval_resource_snapshot}，此处无需处理。
     *
     * @param param 参数
     */
    @Override
    public void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param) {
        // 线索无业务快照
    }

    @Override
    public String getPreUpdateSnapshotData(String resourceId, String userId, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(resourceId);
        if (clue == null) {
            return null;
        }
        List<BaseModuleFieldValue> clueFields = clueFieldService.getModuleFieldValuesByResourceId(resourceId);
        ClueApprovalSnapshotRequest snapshotReq = BeanUtils.copyBean(new ClueApprovalSnapshotRequest(), clue);
        snapshotReq.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());
        ModuleFormConfigDTO clueFormConfig = getFormConfig(clue.getOrganizationId());
        // 获取模块字段
        moduleFormService.processBusinessFieldValues(snapshotReq, clueFields, clueFormConfig);
        return JSON.toJSONString(snapshotReq);
    }

    @Override
    public void revertToSnapshot(String resourceId, String userId, String orgId, String snapshotData) {
        try {
            ClueApprovalSnapshotRequest request = JSON.parseObject(snapshotData, ClueApprovalSnapshotRequest.class);
            if (request == null) {
                return;
            }
            CommonBeanFactory.getBean(ClueService.class).update(request, userId, orgId);
            // 阶段与领取时间不需要单独回退: 快照子类多出的这两个字段会被 update() 的 BeanUtils 拷贝带进实体,
            // 随 clueMapper.update() 一起写回。改动 update() 的字段拷贝方式时须留意这里。
            // 转移时 batchAdd 按转移前的负责人+领取时间写了一条变更记录, 回退要把它删掉,
            // 否则历史里会残留一次并未真正生效的负责人变更
            clueOwnerHistoryService.deleteTransferHistory(request.getId(), request.getOwner(), request.getCollectionTime());
        } catch (Exception e) {
            log.error("审批回退还原业务数据失败, resourceId:{}", resourceId, e);
        }
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
        Clue clue = clueMapper.selectByPrimaryKey(postFieldParam.getResourceId());
        if (clue == null) {
            return;
        }
        // 保存原始数据用于日志记录
        Clue originClue = BeanUtils.copyBean(new Clue(), clue);
        List<BaseModuleFieldValue> originFields = clueFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId());
        List<ClueField> clueFields = new ArrayList<>();
        List<ClueFieldBlob> clueFieldBlobs = new ArrayList<>();

        for (ResourceApprovalFieldUpdateParam fieldUpdateParam : postFieldParam.getFields()) {
            if (!fieldConfigMap.containsKey(fieldUpdateParam.getFieldId()) || fieldUpdateParam.getFieldValue() == null) {
                continue;
            }
            BaseField fieldConfig = fieldConfigMap.get(fieldUpdateParam.getFieldId());
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
            if (fieldConfig.hasBusinessKey()) {
                // 业务主表字段
                clueFieldService.setResourceFieldValue(clue, fieldConfig.getBusinessKey(), fieldUpdateParam.getFieldValue());
            } else {
                // 自定义字段
                if (fieldConfig.isBlob()) {
                    clueFieldService.getResourceFieldBlobMapper().deleteByLambda(new LambdaQueryWrapper<ClueFieldBlob>()
                            .eq(ClueFieldBlob::getFieldId, fieldUpdateParam.getFieldId())
                            .eq(ClueFieldBlob::getResourceId, postFieldParam.getResourceId()));
                    ClueFieldBlob field = new ClueFieldBlob();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
                    field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    clueFieldBlobs.add(field);
                } else {
                    clueFieldService.getResourceFieldMapper().deleteByLambda(new LambdaQueryWrapper<ClueField>()
                            .eq(ClueField::getFieldId, fieldUpdateParam.getFieldId())
                            .eq(ClueField::getResourceId, postFieldParam.getResourceId()));
                    ClueField field = new ClueField();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
                    field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    clueFields.add(field);
                }
            }
        }
        clueMapper.updateById(clue);
        if (CollectionUtils.isNotEmpty(clueFields)) {
            clueFieldService.getResourceFieldMapper().batchInsert(clueFields);
        }
        if (CollectionUtils.isNotEmpty(clueFieldBlobs)) {
            clueFieldService.getResourceFieldBlobMapper().batchInsert(clueFieldBlobs);
        }
        // 记录审批后置字段更新日志
        baseService.handleUpdateLog(originClue, clue, originFields,
                clueFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId()),
                postFieldParam.getResourceId(), clue.getName());
        // 从 OperationLogContext 中获取日志信息并手动记录
        LogContextInfo contextInfo = OperationLogContext.getContext();
        if (contextInfo != null) {
            String orgId = OrganizationContext.getOrganizationId();
            LogDTO logDTO = new LogDTO(orgId, postFieldParam.getResourceId(), postFieldParam.getOperator(), LogType.UPDATE, LogModule.CLUE_INDEX, clue.getName());
            logDTO.setOriginalValue(contextInfo.getOriginalValue());
            logDTO.setModifiedValue(contextInfo.getModifiedValue());
            logService.add(logDTO);
            OperationLogContext.clear();
        }
    }
}
