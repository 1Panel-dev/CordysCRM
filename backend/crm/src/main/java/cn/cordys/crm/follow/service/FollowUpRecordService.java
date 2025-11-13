package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.dto.RolePermissionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.follow.constants.FollowUpPlanType;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.CustomerDataDTO;
import cn.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import cn.cordys.crm.follow.dto.request.RecordHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpRecordDetailResponse;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Past;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpRecordService extends BaseFollowUpService {
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private FollowUpRecordFieldService followUpRecordFieldService;
    @Resource
    private ExtFollowUpRecordMapper extFollowUpRecordMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private DataScopeService dataScopeService;

    /**
     * 添加跟进记录
     *
     * @param request
     * @param userId
     * @param orgId
     *
     * @return
     */
    public FollowUpRecord add(FollowUpRecordAddRequest request, String userId, String orgId) {
        FollowUpRecord followUpRecord = BeanUtils.copyBean(new FollowUpRecord(), request);
        long time = System.currentTimeMillis();
        followUpRecord.setCreateTime(time);
        followUpRecord.setUpdateTime(time);
        followUpRecord.setUpdateUser(userId);
        followUpRecord.setCreateUser(userId);
        followUpRecord.setId(IDGenerator.nextStr());
        followUpRecord.setOrganizationId(orgId);
        if (StringUtils.isBlank(request.getOwner())) {
            followUpRecord.setOwner(userId);
        }

        //保存自定义字段
        followUpRecordFieldService.saveModuleField(followUpRecord, orgId, userId, request.getModuleFields(), false);

        followUpRecordMapper.insert(followUpRecord);

        handleFollowTimeAndFollower(request.getCustomerId(), request.getOpportunityId(), request.getClueId(), request.getFollowTime(), request.getOwner());
        return followUpRecord;
    }


    /**
     * 处理最新跟进使时间&最新跟进人
     *
     * @param customerId
     * @param opportunityId
     * @param clueId
     * @param followTime
     * @param owner
     */
    private void handleFollowTimeAndFollower(String customerId, String opportunityId, String clueId, Long followTime, String owner) {
        if (StringUtils.isNotBlank(customerId)) {
            Customer customer = new Customer();
            customer.setId(customerId);
            customer.setFollowTime(followTime);
            customer.setFollower(owner);
            customerMapper.update(customer);
        }

        if (StringUtils.isNotBlank(opportunityId)) {
            Opportunity opportunity = new Opportunity();
            opportunity.setId(opportunityId);
            opportunity.setFollowTime(followTime);
            opportunity.setFollower(owner);
            opportunityMapper.update(opportunity);
        }

        if (StringUtils.isNotBlank(clueId)) {
            Clue clue = new Clue();
            clue.setId(clueId);
            clue.setFollowTime(followTime);
            clue.setFollower(owner);
            clueMapper.update(clue);
        }
    }


    /**
     * 更新跟进记录
     *
     * @param request
     * @param userId
     *
     * @return
     */
    @OperationLog(module = LogModule.FOLLOW_UP_RECORD, type = LogType.UPDATE, resourceId = "{#request.id}")
    public FollowUpRecord update(FollowUpRecordUpdateRequest request, String userId, String orgId) {
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(followUpRecord).ifPresentOrElse(record -> {
            //更新跟进记录
            FollowUpRecord newRecord = BeanUtils.copyBean(new FollowUpRecord(), record);
            FollowUpRecord updateFollowUpRecord = newRecord(newRecord, request, userId);
            // 获取模块字段
            List<BaseModuleFieldValue> originCustomerFields = followUpRecordFieldService.getModuleFieldValuesByResourceId(request.getId());
            //更新模块字段
            updateModuleField(updateFollowUpRecord, request.getModuleFields(), orgId, userId);
            followUpRecordMapper.update(updateFollowUpRecord);
            handleFollowTimeAndFollower(updateFollowUpRecord.getCustomerId(), updateFollowUpRecord.getOpportunityId(), updateFollowUpRecord.getClueId(), updateFollowUpRecord.getFollowTime(), updateFollowUpRecord.getOwner());
            baseService.handleUpdateLog(followUpRecord, updateFollowUpRecord, originCustomerFields, request.getModuleFields(), followUpRecord.getId(), Translator.get("update_follow_up_record"));
        }, () -> {
            throw new GenericException("record_not_found");
        });

        return followUpRecord;
    }

    private void updateModuleField(FollowUpRecord updateFollowUpRecord, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        followUpRecordFieldService.deleteByResourceId(updateFollowUpRecord.getId());
        // 再保存
        followUpRecordFieldService.saveModuleField(updateFollowUpRecord, orgId, userId, moduleFields, true);
    }


    private FollowUpRecord newRecord(FollowUpRecord record, FollowUpRecordUpdateRequest request, String userId) {
        record.setCustomerId(request.getCustomerId());
        record.setOpportunityId(request.getOpportunityId());
        record.setType(request.getType());
        record.setClueId(request.getClueId());
        record.setOwner(request.getOwner());
        record.setContactId(request.getContactId());
        record.setFollowTime(request.getFollowTime());
        record.setFollowMethod(request.getFollowMethod());
        record.setContent(request.getContent());
        record.setUpdateTime(System.currentTimeMillis());
        record.setUpdateUser(userId);
        return record;
    }


    /**
     * 池/公海 跟进记录列表查询
     *
     * @param request
     * @param userId
     * @param orgId
     * @param resourceType
     * @param type
     *
     * @return
     */
    public PagerWithOption<List<FollowUpRecordListResponse>> poolList(FollowUpRecordPageRequest request, String userId, String orgId, String resourceType, String type) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<FollowUpRecordListResponse> list = extFollowUpRecordMapper.selectPoolList(request, userId, orgId, resourceType, type);
        buildListData(list, orgId);

        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_RECORD.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, FollowUpRecordListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(list,
                FollowUpRecordListResponse::getOwner, FollowUpRecordListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(list,
                FollowUpRecordListResponse::getContactId, FollowUpRecordListResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);

        return PageUtils.setPageInfoWithOption(page, list, optionMap);
    }

    private void buildListData(List<FollowUpRecordListResponse> list, String orgId) {
        List<String> ids = list.stream().map(FollowUpRecordListResponse::getId).toList();
        Map<String, List<BaseModuleFieldValue>> recordCustomFieldMap = followUpRecordFieldService.getResourceFieldMap(ids, true);

        List<String> createUserIds = list.stream().map(FollowUpRecordListResponse::getCreateUser).toList();
        List<String> updateUserIds = list.stream().map(FollowUpRecordListResponse::getUpdateUser).toList();
        List<String> ownerIds = list.stream().map(FollowUpRecordListResponse::getOwner).toList();
        List<String> userIds = Stream.of(createUserIds, updateUserIds, ownerIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        List<String> contactIds = list.stream().map(FollowUpRecordListResponse::getContactId).toList();
        Map<String, String> contactMap = baseService.getContactMap(contactIds);
        Map<String, String> contactPhoneMap = baseService.getContactPhone(contactIds);

        Map<String, UserResponse> userDeptMap = baseService.getUserDepAndPhoneByUserIds(ownerIds, orgId);

        List<String> customerIds = list.stream().map(FollowUpRecordListResponse::getCustomerId).toList();
        Map<String, String> customerMap = baseService.getCustomerMap(customerIds);

        List<String> opportunityIds = list.stream().map(FollowUpRecordListResponse::getOpportunityId).toList();
        Map<String, String> opportunityMap = baseService.getOpportunityMap(opportunityIds);

        List<String> clueIds = list.stream().map(FollowUpRecordListResponse::getClueId).toList();
        Map<String, String> clueMap = baseService.getClueMap(clueIds);

        list.forEach(recordListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = recordCustomFieldMap.get(recordListResponse.getId());
            recordListResponse.setModuleFields(customerFields);

            recordListResponse.setCreateUserName(userNameMap.get(recordListResponse.getCreateUser()));
            recordListResponse.setUpdateUserName(userNameMap.get(recordListResponse.getUpdateUser()));
            recordListResponse.setOwnerName(userNameMap.get(recordListResponse.getOwner()));
            recordListResponse.setContactName(contactMap.get(recordListResponse.getContactId()));
            recordListResponse.setCustomerName(customerMap.get(recordListResponse.getCustomerId()));
            recordListResponse.setOpportunityName(opportunityMap.get(recordListResponse.getOpportunityId()));
            recordListResponse.setClueName(clueMap.get(recordListResponse.getClueId()));
            recordListResponse.setPhone(contactPhoneMap.get(recordListResponse.getContactId()));
            recordListResponse.setResourceType(recordListResponse.getType());

            UserResponse userResponse = userDeptMap.get(recordListResponse.getOwner());
            if (userResponse != null) {
                recordListResponse.setDepartmentId(userResponse.getDepartmentId());
                recordListResponse.setDepartmentName(userResponse.getDepartmentName());
            }
        });
    }


    /**
     * 获取跟进记录详情
     *
     * @param id
     *
     * @return
     */
    public FollowUpRecordDetailResponse get(String id, String orgId) {
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(id);
        FollowUpRecordDetailResponse response = BeanUtils.copyBean(new FollowUpRecordDetailResponse(), followUpRecord);
        buildListData(List.of(response), orgId);

        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_RECORD.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(List.of(response), FollowUpRecordDetailResponse::getModuleFields);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(response,
                FollowUpRecordDetailResponse::getOwner, FollowUpRecordDetailResponse::getOwnerName);
        optionMap.put(BusinessModuleField.FOLLOW_RECORD_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(response,
                FollowUpRecordDetailResponse::getContactId, FollowUpRecordDetailResponse::getContactName);
        optionMap.put(BusinessModuleField.FOLLOW_RECORD_CONTACT.getBusinessKey(), contactFieldOption);

        //客户
        List<OptionDTO> customerOption = moduleFormService.getBusinessFieldOption(response,
                FollowUpRecordDetailResponse::getCustomerId, FollowUpRecordDetailResponse::getCustomerName);
        optionMap.put(BusinessModuleField.FOLLOW_RECORD_CUSTOMER.getBusinessKey(), customerOption);

        //商机
        List<OptionDTO> opportunityOption = moduleFormService.getBusinessFieldOption(response,
                FollowUpRecordDetailResponse::getOpportunityId, FollowUpRecordDetailResponse::getOpportunityName);
        optionMap.put(BusinessModuleField.FOLLOW_RECORD_OPPORTUNITY.getBusinessKey(), opportunityOption);

        //线索
        List<OptionDTO> clueOption = moduleFormService.getBusinessFieldOption(response,
                FollowUpRecordDetailResponse::getClueId, FollowUpRecordDetailResponse::getClueName);
        optionMap.put(BusinessModuleField.FOLLOW_RECORD_CLUE.getBusinessKey(), clueOption);

        response.setOptionMap(optionMap);

        // 附件信息
        response.setAttachmentMap(moduleFormService.getAttachmentMap(customerFormConfig, moduleFieldValues));
        return response;
    }


    /**
     * 跟进记录
     *
     * @param request
     * @param userId
     * @param orgId
     * @param resourceType
     * @param type
     *
     * @return
     */
    public PagerWithOption<List<FollowUpRecordListResponse>> list(FollowUpRecordPageRequest request, String userId, String orgId, String resourceType, String type, CustomerDataDTO customerData) {
        if (Strings.CS.equals(resourceType, FormKey.CUSTOMER.name())) {
            // 如果是客户类型，查询所有转移的线索来源ID的记录
            LambdaQueryWrapper<Clue> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Clue::getTransitionId, request.getSourceId());
            List<Clue> clues = clueMapper.selectListByLambda(queryWrapper);
            request.setTransitionIds(clues.stream().map(Clue::getId).toList());
        }
        if (Strings.CS.equals(resourceType, FormKey.OPPORTUNITY.name())) {
            // 商机跟进记录来源: 1. 线索转化 2. 商机手动创建
            Opportunity opportunity = opportunityMapper.selectByPrimaryKey(request.getSourceId());
            if (opportunity == null) {
                throw new GenericException(Translator.get("opportunity_not_found"));
            }
            request.setRefClueIds(List.of(opportunity.getClueId()));
        }
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<FollowUpRecordListResponse> list = extFollowUpRecordMapper.selectList(request, userId, orgId, resourceType, type, customerData);
        buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list);
        return PageUtils.setPageInfoWithOption(page, list, optionMap);
    }

    /**
     * 跟进记录的汇总列表
     *
     * @param request                请求参数
     * @param userId                 用户ID
     * @param orgId                  组织ID
     * @param clueDataPermission     线索业务数据权限
     * @param customerDataPermission 客户业务数据权限
     *
     * @return 记录的汇总列表
     */
    public PagerWithOption<List<FollowUpRecordListResponse>> totalList(RecordHomePageRequest request, String userId, String orgId,
                                                                       DeptDataPermissionDTO clueDataPermission, DeptDataPermissionDTO customerDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        // 解析当前用户数据权限
        List<FollowUpRecordListResponse> recordList = extFollowUpRecordMapper.selectTotalList(request, userId, orgId, clueDataPermission, customerDataPermission);
        buildListData(recordList, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, recordList);
        return PageUtils.setPageInfoWithOption(page, recordList, optionMap);
    }

    /**
     * 处理选项数据
     *
     * @param orgId      组织ID
     * @param recordList 记录列表
     *
     * @return 选项集合
     */
    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<FollowUpRecordListResponse> recordList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_RECORD.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(recordList, FollowUpRecordListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);
        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(recordList,
                FollowUpRecordListResponse::getOwner, FollowUpRecordListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey(), ownerFieldOption);
        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(recordList,
                FollowUpRecordListResponse::getContactId, FollowUpRecordListResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);
        return optionMap;
    }


    /**
     * 删除跟进记录
     *
     * @param id
     */
    @OperationLog(module = LogModule.FOLLOW_UP_RECORD, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(id);
        if (followUpRecord == null) {
            throw new GenericException("record_not_found");
        }
        deleteByIds(List.of(id));

        getFollowTimeAndFollower(followUpRecord);

        // 设置操作对象
        OperationLogContext.setResourceName(Translator.get("delete_follow_up_record"));
    }

    /**
     * 删除跟进记录，更新客户/商机/线索的最新跟进时间和跟进人
     *
     * @param followUpRecord
     */
    private void getFollowTimeAndFollower(FollowUpRecord followUpRecord) {
        switch (followUpRecord.getType()) {
            case "CUSTOMER" -> updateCustomerOrOpportunity(followUpRecord);
            case "CLUE" -> updateClue(followUpRecord);
            default -> {
            }
        }
    }

    /**
     * 跟新线索的最新跟进时间和跟进人
     *
     * @param followUpRecord
     */
    private void updateClue(FollowUpRecord followUpRecord) {
        if (StringUtils.isNotBlank(followUpRecord.getClueId())) {
            FollowUpRecord record = extFollowUpRecordMapper.selectRecord(null, null, followUpRecord.getClueId(), followUpRecord.getOrganizationId(), "CLUE");
            Optional.ofNullable(record).ifPresent(r -> {
                Clue clue = new Clue();
                clue.setId(r.getClueId());
                clue.setFollowTime(r.getFollowTime());
                clue.setFollower(r.getOwner());
                clueMapper.update(clue);
            });
        }
    }


    /**
     * 更新客户&商机 最近跟进时间&最近跟进人
     *
     * @param followUpRecord
     */
    private void updateCustomerOrOpportunity(FollowUpRecord followUpRecord) {

        if (StringUtils.isNotBlank(followUpRecord.getCustomerId())) {
            FollowUpRecord record = extFollowUpRecordMapper.selectRecord(followUpRecord.getCustomerId(), null, null, followUpRecord.getOrganizationId(), "CUSTOMER");
            Optional.ofNullable(record).ifPresent(r -> {
                Customer customer = new Customer();
                customer.setId(r.getCustomerId());
                customer.setFollowTime(r.getFollowTime());
                customer.setFollower(r.getOwner());
                customerMapper.update(customer);
            });
        }

        if (StringUtils.isNotBlank(followUpRecord.getOpportunityId())) {
            FollowUpRecord record = extFollowUpRecordMapper.selectRecord(null, followUpRecord.getOpportunityId(), null, followUpRecord.getOrganizationId(), "CUSTOMER");
            Optional.ofNullable(record).ifPresent(r -> {
                Opportunity opportunity = new Opportunity();
                opportunity.setId(r.getOpportunityId());
                opportunity.setFollowTime(r.getFollowTime());
                opportunity.setFollower(r.getOwner());
                opportunityMapper.update(opportunity);
            });
        }
    }

    public void deleteByCustomerIds(List<String> customerIds) {
        if (CollectionUtils.isEmpty(customerIds)) {
            return;
        }
        List<String> ids = getByCustomerIds(customerIds)
                .stream()
                .map(FollowUpRecord::getId)
                .toList();
        deleteByIds(ids);
    }

    private void deleteByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        followUpRecordFieldService.deleteByResourceIds(ids);
        followUpRecordMapper.deleteByIds(ids);
    }

    private List<FollowUpRecord> getByCustomerIds(List<String> customerIds) {
        LambdaQueryWrapper<FollowUpRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FollowUpRecord::getCustomerId, customerIds);
        queryWrapper.eq(FollowUpRecord::getType, FollowUpPlanType.CUSTOMER.name());
        return followUpRecordMapper.selectListByLambda(queryWrapper);
    }

    private List<FollowUpRecord> getByClueIds(List<String> clueIds) {
        LambdaQueryWrapper<FollowUpRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FollowUpRecord::getClueId, clueIds);
        queryWrapper.eq(FollowUpRecord::getType, FollowUpPlanType.CLUE.name());
        return followUpRecordMapper.selectListByLambda(queryWrapper);
    }

    public void deleteByClueIds(List<String> clueIds) {
        if (CollectionUtils.isEmpty(clueIds)) {
            return;
        }
        List<String> ids = getByClueIds(clueIds)
                .stream()
                .map(FollowUpRecord::getId)
                .toList();
        deleteByIds(ids);
    }

    /**
     * 获取跟进记录对应数据权限配置
     *
     * @param currentUser    当前用户ID
     * @param organizationId 组织ID
     *
     * @return 数据权限配置
     */
    public ResourceTabEnableDTO getTabEnableConfig(String currentUser, String organizationId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(currentUser, organizationId);
        // 由于统一的记录/计划页面会展示多个业务模块的数据，因此需要合并多个数据权限, 展示范围更大的数据权限配置
        ResourceTabEnableDTO clueTabConfig = PermissionUtils.getTabEnableConfig(currentUser, PermissionConstants.CLUE_MANAGEMENT_READ, rolePermissions);
        ResourceTabEnableDTO customerTabConfig = PermissionUtils.getTabEnableConfig(currentUser, PermissionConstants.CUSTOMER_MANAGEMENT_READ, rolePermissions);
        return clueTabConfig.or(customerTabConfig);
    }


    /**
     * 拦截跟进记录的操作权限
     *
     * @param id    记录ID
     * @param orgId 组织ID
     */
    public void checkRecordPermission(String id, String orgId) {
        FollowUpRecordDetailResponse recordDetail = get(id, orgId);
        boolean hasPermission;
        if (Strings.CS.equals(recordDetail.getType(), ModuleKey.CLUE.name())) {
            hasPermission = PermissionUtils.hasPermission(PermissionConstants.CLUE_MANAGEMENT_UPDATE);
        } else if (StringUtils.isNotEmpty(recordDetail.getOpportunityId())) {
            hasPermission = PermissionUtils.hasPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE);
        } else {
            hasPermission = PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE);
        }
        if (!hasPermission) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
    }
}
