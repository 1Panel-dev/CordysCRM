package cn.cordys.crm.clue.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.domain.*;
import cn.cordys.crm.clue.dto.CluePoolDTO;
import cn.cordys.crm.clue.dto.CluePoolPickRuleDTO;
import cn.cordys.crm.clue.dto.CluePoolRecycleRuleDTO;
import cn.cordys.crm.clue.dto.request.CluePoolImportRequest;
import cn.cordys.crm.clue.dto.request.PoolCluePickRequest;
import cn.cordys.crm.clue.mapper.ExtClueCapacityMapper;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.RuleConditionDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import cn.cordys.crm.system.dto.request.PoolBatchPickRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class PoolClueService {

    public static final long DAY_MILLIS = 24 * 60 * 60 * 1000;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private BaseMapper<ClueOwner> ownerMapper;
    @Resource
    private BaseMapper<CluePool> poolMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<CluePoolPickRule> pickRuleMapper;
    @Resource
    private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;
    @Resource
    private ExtClueCapacityMapper extClueCapacityMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private LogService logService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<ClueField> clueFieldMapper;
    @Resource
    private BaseMapper<ClueFieldBlob> clueFieldBlobMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private BaseService baseService;

    /**
     * 获取当前用户线索池选项
     *
     * @param currentUser  当前用户ID
     * @param currentOrgId 当前组织ID
     * @return 线索池选项
     */
    public List<CluePoolDTO> getPoolOptions(String currentUser, String currentOrgId) {
        List<CluePoolDTO> options = new ArrayList<>();
        LambdaQueryWrapper<CluePool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(CluePool::getEnable, true).eq(CluePool::getOrganizationId, currentOrgId);
        poolWrapper.orderByDesc(CluePool::getUpdateTime);
        List<CluePool> pools = poolMapper.selectListByLambda(poolWrapper);
        if (CollectionUtils.isEmpty(pools)) {
            return new ArrayList<>();
        }

        List<String> userIds = pools.stream().flatMap(pool -> Stream.of(pool.getCreateUser(), pool.getUpdateUser())).toList();
        List<User> createOrUpdateUsers = userMapper.selectByIds(userIds.toArray(new String[0]));
        Map<String, String> userMap = createOrUpdateUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<String> poolIds = pools.stream()
                .map(CluePool::getId)
                .toList();

        LambdaQueryWrapper<CluePoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
        pickRuleWrapper.in(CluePoolPickRule::getPoolId, poolIds);

        List<CluePoolPickRule> pickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
        Map<String, CluePoolPickRule> pickRuleMap = pickRules.stream()
                .collect(Collectors.toMap(CluePoolPickRule::getPoolId, pickRule -> pickRule));

        LambdaQueryWrapper<CluePoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
        recycleRuleWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);

        List<CluePoolRecycleRule> recycleRules = recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
        Map<String, CluePoolRecycleRule> recycleRuleMap = recycleRules.stream()
                .collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, recycleRule -> recycleRule));

        Map<String, List<CluePoolHiddenField>> hiddenFieldMap = cluePoolService.getCluePoolHiddenFieldsByPoolIds(poolIds)
                .stream()
                .collect(Collectors.groupingBy(CluePoolHiddenField::getPoolId));

        List<BaseField> fields = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), currentOrgId).getFields();

        pools.forEach(pool -> {
            List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), currentOrgId);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), currentOrgId);
            if (scopeIds.contains(currentUser) || ownerIds.contains(currentUser) || Strings.CS.equals(currentUser, InternalUser.ADMIN.getValue())) {
                CluePoolDTO poolDTO = new CluePoolDTO();
                BeanUtils.copyBean(poolDTO, pool);
                poolDTO.setMembers(userExtendService.getScope(JSON.parseArray(pool.getScopeId(), String.class)));
                poolDTO.setOwners(userExtendService.getScope(JSON.parseArray(pool.getOwnerId(), String.class)));
                poolDTO.setCreateUserName(userMap.get(pool.getCreateUser()));
                poolDTO.setUpdateUserName(userMap.get(pool.getUpdateUser()));

                CluePoolPickRuleDTO pickRule = new CluePoolPickRuleDTO();
                if (pickRuleMap.get(pool.getId()) != null) {
                    BeanUtils.copyBean(pickRule, pickRuleMap.get(pool.getId()));
                }

                CluePoolRecycleRuleDTO recycleRule = new CluePoolRecycleRuleDTO();
                CluePoolRecycleRule cluePoolRecycleRule = recycleRuleMap.get(pool.getId());
                if (cluePoolRecycleRule != null) {
                    BeanUtils.copyBean(recycleRule, cluePoolRecycleRule);
                    recycleRule.setConditions(JSON.parseArray(cluePoolRecycleRule.getCondition(), RuleConditionDTO.class));
                }

                poolDTO.setPickRule(pickRule);
                poolDTO.setRecycleRule(recycleRule);
                poolDTO.setEditable(ownerIds.contains(currentUser));

                Set<String> hiddenFieldIds;
                if (hiddenFieldMap.get(pool.getId()) != null) {
                    hiddenFieldIds = hiddenFieldMap.get(pool.getId()).stream()
                            .map(CluePoolHiddenField::getFieldId)
                            .collect(Collectors.toSet());
                } else {
                    hiddenFieldIds = Set.of();
                }

                poolDTO.setFieldConfigs(cluePoolService.getCluePoolFieldConfigs(fields, hiddenFieldIds));
                options.add(poolDTO);
            }
        });
        return options;
    }

    /**
     * 领取线索
     *
     * @param request      请求参数
     * @param currentUser  当前用户ID
     * @param currentOrgId 当前组织ID
     */
    public void pick(PoolCluePickRequest request, String currentUser, String currentOrgId) {
        CluePool pool = poolMapper.selectByPrimaryKey(request.getPoolId());
        validateCapacity(1, currentUser, currentOrgId);
        LambdaQueryWrapper<CluePoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
        pickRuleWrapper.eq(CluePoolPickRule::getPoolId, request.getPoolId());
        List<CluePoolPickRule> cluePoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
        CluePoolPickRule pickRule = cluePoolPickRules.getFirst();
        boolean poolAdmin = userExtendService.isPoolAdmin(JSON.parseArray(pool.getOwnerId(), String.class), currentUser, currentOrgId);
        if (!poolAdmin) {
            validateDailyPickNum(1, currentUser, pickRule);
        }
        ownClue(request.getClueId(), currentUser, pickRule, currentUser, LogType.PICK, currentOrgId, poolAdmin);
    }

    /**
     * 分配线索
     *
     * @param id           客户ID
     * @param assignUserId 分配用户ID
     */
    @OperationLog(module = LogModule.CLUE_POOL_INDEX, type = LogType.ASSIGN, resourceId = "{#request.clueId}")
    public void assign(String id, String assignUserId, String currentOrgId, String currentUser) {
        validateCapacity(1, assignUserId, currentOrgId);
        ownClue(id, assignUserId, null, currentUser, LogType.ASSIGN, currentOrgId, false);
    }

    /**
     * 删除线索
     *
     * @param id 客户ID
     */
    @OperationLog(module = LogModule.CLUE_POOL_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
        clueWrapper.eq(Clue::getId, id);
        clueMapper.deleteByLambda(clueWrapper);

        // 设置操作对象
        OperationLogContext.setResourceName(clue.getName());
    }

    /**
     * 批量领取线索
     *
     * @param request      请求参数
     * @param currentUser  当前用户ID
     * @param currentOrgId 当前组织ID
     */
    public void batchPick(PoolBatchPickRequest request, String currentUser, String currentOrgId) {
        CluePool pool = poolMapper.selectByPrimaryKey(request.getPoolId());
        validateCapacity(request.getBatchIds().size(), currentUser, currentOrgId);
        LambdaQueryWrapper<CluePoolPickRule> pickRuleWrapper = new LambdaQueryWrapper<>();
        pickRuleWrapper.eq(CluePoolPickRule::getPoolId, request.getPoolId());
        List<CluePoolPickRule> cluePoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
        CluePoolPickRule pickRule = cluePoolPickRules.getFirst();
        boolean poolAdmin = userExtendService.isPoolAdmin(JSON.parseArray(pool.getOwnerId(), String.class), currentUser, currentOrgId);
        if (!poolAdmin) {
            validateDailyPickNum(request.getBatchIds().size(), currentUser, pickRule);
        }
        request.getBatchIds().forEach(id -> ownClue(id, currentUser, pickRule, currentUser, LogType.PICK, currentOrgId, poolAdmin));
    }

    /**
     * 批量分配线索
     *
     * @param request      请求参数
     * @param assignUserId 分配用户ID
     * @param currentOrgId 当前组织ID
     */
    public void batchAssign(PoolBatchAssignRequest request, String assignUserId, String currentOrgId, String currentUser) {
        validateCapacity(request.getBatchIds().size(), assignUserId, currentOrgId);
        request.getBatchIds().forEach(id -> ownClue(id, assignUserId, null, currentUser, LogType.ASSIGN, currentOrgId, false));
    }

    /**
     * 批量删除客户
     *
     * @param ids 客户ID集合
     */
    public void batchDelete(List<String> ids, String userId, String orgId) {
        List<Clue> clues = clueMapper.selectByIds(ids);
        LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
        clueWrapper.in(Clue::getId, ids);
        clueMapper.deleteByLambda(clueWrapper);

        List<LogDTO> logs = clues.stream()
                .map(clue ->
                        new LogDTO(orgId, clue.getId(), userId, LogType.DELETE, LogModule.CLUE_POOL_INDEX, clue.getName())
                )
                .toList();
        logService.batchAdd(logs);
    }

    /**
     * 校验库容
     *
     * @param processCount 处理数量
     * @param ownUserId    负责人用户ID
     * @param currentOrgId 当前组织ID
     */
    public void validateCapacity(int processCount, String ownUserId, String currentOrgId) {
        // 实际可处理条数 = 负责人库容容量 - 所领取的数量 < 处理数量, 提示库容不足.
        Integer capacity = getUserCapacity(ownUserId, currentOrgId);
        int ownCount = (int) extClueMapper.getOwnerCount(ownUserId);
        if (capacity != null && capacity - ownCount < processCount) {
            throw new GenericException(Translator.getWithArgs("customer.capacity.over", Math.max(capacity - ownCount, 0)));
        }
    }

    /**
     * 校验每日领取数量
     *
     * @param pickingCount 领取数量
     * @param ownUserId    负责人用户ID
     * @param pickRule     领取规则
     */
    public void validateDailyPickNum(int pickingCount, String ownUserId, CluePoolPickRule pickRule) {
        if (pickRule.getLimitOnNumber()) {
            LambdaQueryWrapper<Clue> clueWrapper = new LambdaQueryWrapper<>();
            clueWrapper
                    .eq(Clue::getOwner, ownUserId)
                    .eq(Clue::getInSharedPool, false)
                    .between(Clue::getCollectionTime, TimeUtils.getTodayStart(), TimeUtils.getTodayStart() + DAY_MILLIS);
            List<Clue> clues = clueMapper.selectListByLambda(clueWrapper);
            int pickedCount = clues.size();
            if (pickingCount + pickedCount > pickRule.getPickNumber()) {
                throw new GenericException(Translator.get("customer.daily.pick.over"));
            }
        }
    }

    /**
     * 获取用户库容
     *
     * @param userId         用户ID
     * @param organizationId 组织ID
     * @return 库容
     */
    public Integer getUserCapacity(String userId, String organizationId) {
        List<String> scopeIds = userExtendService.getUserScopeIds(userId, organizationId);
        return extClueCapacityMapper.getCapacityByScopeIds(scopeIds, organizationId);
    }

    /**
     * 拥有客户
     *
     * @param clueId  线索ID
     * @param ownerId 拥有人ID
     */
    private void ownClue(String clueId, String ownerId, CluePoolPickRule pickRule, String operateUserId, String logType, String currentOrgId, boolean isPoolAdmin) {
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue == null) {
            throw new IllegalArgumentException(Translator.get("clue.not.exist"));
        }
        if (!clue.getInSharedPool()) {
            throw new GenericException(Translator.getWithArgs("clue.pool.occupied", clue.getName()));
        }
        if (!isPoolAdmin && pickRule != null && pickRule.getLimitNew()) {
            LocalDateTime joinPoolTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(clue.getUpdateTime()), ZoneId.systemDefault());
            LocalDateTime releaseDate = joinPoolTime.plusDays(pickRule.getNewPickInterval());
            if (releaseDate.isAfter(LocalDateTime.now())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                throw new GenericException(Translator.getWithArgs("pool.data.release.date", releaseDate.format(formatter)));
            }
        }
        if (pickRule != null && pickRule.getLimitPreOwner()) {
            LambdaQueryWrapper<ClueOwner> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ClueOwner::getClueId, clueId);
            List<ClueOwner> clueOwners = ownerMapper.selectListByLambda(queryWrapper);
            if (CollectionUtils.isNotEmpty(clueOwners)) {
                clueOwners.sort(Comparator.comparingLong(ClueOwner::getCollectionTime).reversed());
                ClueOwner lastOwner = clueOwners.getFirst();
                if (Strings.CS.equals(lastOwner.getOwner(), ownerId) &&
                        System.currentTimeMillis() < pickRule.getPickIntervalDays() * DAY_MILLIS + lastOwner.getEndTime()) {
                    LocalDateTime nextPickTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(pickRule.getPickIntervalDays() * DAY_MILLIS + lastOwner.getEndTime()),
                            ZoneId.systemDefault());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    throw new GenericException(Translator.getWithArgs("customer.pre_owner.pick.limit", nextPickTime.format(formatter)));
                }
            }
        }
        clue.setPoolId(null);
        clue.setInSharedPool(false);
        clue.setOwner(ownerId);
        clue.setCollectionTime(System.currentTimeMillis());
        clue.setStage(ClueStatus.FOLLOWING.name());
        clue.setUpdateUser(ownerId);
        clue.setUpdateTime(System.currentTimeMillis());
        extClueMapper.updateIncludeNullById(clue);

        // 日志
        LogDTO logDTO = new LogDTO(currentOrgId, clue.getId(), operateUserId, logType, LogModule.CLUE_POOL_INDEX, clue.getName());
        logService.add(logDTO);

        // 分配通知
        if (Strings.CS.equals(logType, LogType.ASSIGN)) {
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE, NotificationConstants.Event.CLUE_DISTRIBUTED,
                    clue.getName(), operateUserId, currentOrgId, List.of(clue.getOwner()), true);
        }
    }

    /**
     * 校验当前用户是否为线索池成员（成员或管理员均可访问）
     *
     * @param poolId   线索池ID
     * @param userId   当前用户ID
     * @param orgId    组织ID
     */
    public void checkPoolMember(String poolId, String userId, String orgId) {
        CluePool pool = poolMapper.selectByPrimaryKey(poolId);
        if (pool == null) {
            throw new GenericException(Translator.get("clue_pool_not_exist"));
        }
        List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), orgId);
        List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), orgId);
        if (!scopeIds.contains(userId) && !ownerIds.contains(userId) && !Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
            throw new GenericException(Translator.get("clue_pool_member_access_fail"));
        }
    }

    /**
     * 根据线索ID获取所属线索池ID
     *
     * @param clueId 线索ID
     * @return 线索池ID
     */
    public String getPoolIdByClueId(String clueId) {
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue == null) {
            throw new GenericException(Translator.get("clue.not.exist"));
        }
        return clue.getPoolId();
    }

    public void batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = clueFieldService.getAndCheckField(request.getFieldId(), organizationId);

        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.CLUE_OWNER.getBusinessKey())) {
            // 修改负责人，走批量分配的接口
            PoolBatchAssignRequest batchAssignRequest = new PoolBatchAssignRequest();
            batchAssignRequest.setBatchIds(request.getIds());
            batchAssignRequest.setAssignUserId(request.getFieldValue().toString());
            batchAssign(batchAssignRequest, batchAssignRequest.getAssignUserId(), organizationId, userId);
            return;
        }

        List<Clue> originCustomers = clueMapper.selectByIds(request.getIds());

        clueFieldService.batchUpdate(request, field, originCustomers, Clue.class, LogModule.CLUE_POOL_INDEX, extClueMapper::batchUpdate, userId, organizationId);
    }


    /**
     * 下载导入模板
     *
     * @param response
     * @param orgId
     */
    public void downloadImportTpl(HttpServletResponse response, String orgId) {
        List<List<String>> headList = moduleFormService.getCustomImportHeadsNoRefAndOwner(FormKey.CLUE.getKey(), orgId);
        new EasyExcelExporter().exportMultiSheetTplWithSharedHandler(response,
                headList,
                Translator.get("clue_pool.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
                new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFieldsNoOwner(FormKey.CLUE.getKey(), orgId)),
                new CustomHeadColWidthStyleStrategy());
    }

    /**
     * 导入校验
     *
     * @param file
     * @param request
     * @param orgId
     * @return
     */
    public ImportResponse importPreCheck(MultipartFile file, CluePoolImportRequest request, String orgId) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        CluePool pool = poolMapper.selectByPrimaryKey(request.getPoolId());
        if (pool == null) {
            throw new GenericException(Translator.get("clue_pool_not_exist"));
        }
        return checkImportExcel(file, orgId);
    }

    private ImportResponse checkImportExcel(MultipartFile file, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(FormKey.CLUE.getKey(), currentOrg);
            fields.removeIf(baseField -> Strings.CI.equals(baseField.getBusinessKey(), BusinessModuleField.CLUE_OWNER.getBusinessKey()));
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "clue", "clue_field", currentOrg);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("clue import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 导入
     *
     * @param file
     * @param request
     * @param orgId
     * @param userId
     * @return
     */
    public ImportResponse realImport(MultipartFile file, CluePoolImportRequest request, String orgId, String userId) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CLUE.getKey(), orgId);
            fields.removeIf(baseField -> Strings.CI.equals(baseField.getBusinessKey(), BusinessModuleField.CLUE_OWNER.getBusinessKey()));
            CustomImportAfterDoConsumer<Clue, BaseResourceSubField> afterDo = (clues, clueFields, clueFieldBlobs) -> {
                List<LogDTO> logs = new ArrayList<>();
                ImportType importType = EnumUtils.valueOf(ImportType.class, request.getImportType());
                switch (importType) {
                    case ADD -> {
                        clues.forEach(clue -> {
                            clue.setStage(ClueStatus.NEW.name());
                            clue.setInSharedPool(true);
                            clue.setPoolId(request.getPoolId());
                            logs.add(new LogDTO(orgId, clue.getId(), userId, LogType.ADD, LogModule.CLUE_POOL_INDEX, clue.getName()));
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
                                clue.setInSharedPool(true);
                                clue.setPoolId(request.getPoolId());
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


                        Map<String, Clue> modifiedClueMaps = clueMapper.selectByIds(ids).stream().collect(Collectors.toMap(Clue::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> modifiedFieldValueMap = clueFieldService.getResourceFieldMap(ids, true);

                        //日志
                        ids.forEach(id -> {
                            Clue originDate = originClueMaps.get(id);
                            Clue modifiedDate = modifiedClueMaps.get(id);
                            baseService.handleUpdateLog(originDate, modifiedDate, originFieldValueMap.get(id), modifiedFieldValueMap.get(id), id, modifiedDate.getName());
                            LogContextInfo contextInfo = OperationLogContext.getContext();
                            if (contextInfo != null) {
                                LogDTO logDTO = new LogDTO(orgId, id, userId, LogType.UPDATE, LogModule.CLUE_POOL_INDEX, modifiedDate.getName());
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
            CustomFieldImportEventListener<Clue> eventListener = new CustomFieldImportEventListener<>(fields, Clue.class, orgId, userId,
                    "clue_field", afterDo, 2000, null, null);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("clue import error: ", e);
            throw new GenericException(e.getMessage());
        }
    }

}
