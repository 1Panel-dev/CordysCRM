package io.cordys.crm.follow.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.CustomerDataDTO;
import io.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import io.cordys.crm.follow.dto.response.FollowUpRecordDetailResponse;
import io.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import io.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.LogService;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
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
    private LogService logService;
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

    /**
     * 添加跟进记录
     *
     * @param request
     * @param userId
     * @param orgId
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
        followUpRecordMapper.insert(followUpRecord);

        //保存自定义字段
        followUpRecordFieldService.saveModuleField(followUpRecord.getId(), request.getModuleFields());

        if (StringUtils.isNotBlank(request.getCustomerId())) {
            Customer customer = new Customer();
            customer.setId(request.getCustomerId());
            customer.setFollowTime(time);
            customer.setFollower(request.getOwner());
            customerMapper.update(customer);
        }

        if (StringUtils.isNotBlank(request.getOpportunityId())) {
            Opportunity opportunity = new Opportunity();
            opportunity.setId(request.getCustomerId());
            opportunity.setFollowTime(time);
            opportunity.setFollower(request.getOwner());
            opportunityMapper.update(opportunity);
        }

        if (StringUtils.isNotBlank(request.getClueId())) {
            Clue clue = new Clue();
            clue.setId(request.getCustomerId());
            clue.setFollowTime(time);
            clue.setFollower(request.getOwner());
            clueMapper.update(clue);
        }

        return followUpRecord;
    }

    /**
     * 更新跟进记录
     *
     * @param request
     * @param userId
     * @return
     */
    public FollowUpRecord update(FollowUpRecordUpdateRequest request, String userId, String orgId) {
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(followUpRecord).ifPresentOrElse(record -> {
            LogDTO logDTO = new LogDTO(orgId, followUpRecord.getId(), userId, LogType.UPDATE, LogModule.FOLLOW_UP_RECORD, Translator.get("update_follow_up_record"));
            logDTO.setOriginalValue(record);
            //更新跟进记录
            updateRecord(record, request, userId);
            //更新模块字段
            updateModuleField(request.getId(), request.getModuleFields());
            logDTO.setModifiedValue(record);
            logService.add(logDTO);
        }, () -> {
            throw new GenericException("record_not_found");
        });

        return followUpRecord;
    }

    private void updateModuleField(String followUpId, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        followUpRecordFieldService.deleteByResourceId(followUpId);
        // 再保存
        followUpRecordFieldService.saveModuleField(followUpId, moduleFields);
    }


    private void updateRecord(FollowUpRecord record, FollowUpRecordUpdateRequest request, String userId) {
        record.setCustomerId(request.getCustomerId());
        record.setOpportunityId(request.getOpportunityId());
        record.setType(request.getType());
        record.setClueId(request.getClueId());
        record.setOwner(request.getOwner());
        record.setContactId(request.getContactId());
        record.setContent(request.getContent());
        record.setUpdateTime(System.currentTimeMillis());
        record.setUpdateUser(userId);
        followUpRecordMapper.update(record);
    }


    /**
     * 池/公海 跟进记录列表查询
     *
     * @param request
     * @param userId
     * @param orgId
     * @param resourceType
     * @param type
     * @return
     */
    public PagerWithOption<List<FollowUpRecordListResponse>> poolList(FollowUpRecordPageRequest request, String userId, String orgId, String resourceType, String type) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<FollowUpRecordListResponse> list = extFollowUpRecordMapper.selectPoolList(request, userId, orgId, resourceType, type);
        buildListData(list);

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

    private void buildListData(List<FollowUpRecordListResponse> list) {
        List<String> ids = list.stream().map(FollowUpRecordListResponse::getId).toList();
        Map<String, List<BaseModuleFieldValue>> recordCustomFieldMap = followUpRecordFieldService.getResourceFieldMap(ids);

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

        list.forEach(recordListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = recordCustomFieldMap.get(recordListResponse.getId());
            recordListResponse.setModuleFields(customerFields);

            recordListResponse.setCreateUserName(userNameMap.get(recordListResponse.getCreateUser()));
            recordListResponse.setUpdateUserName(userNameMap.get(recordListResponse.getUpdateUser()));
            recordListResponse.setOwnerName(userNameMap.get(recordListResponse.getOwner()));
            recordListResponse.setContactName(contactMap.get(recordListResponse.getContactId()));
        });
    }


    /**
     * 获取跟进记录详情
     *
     * @param id
     * @return
     */
    public FollowUpRecordDetailResponse get(String id, String orgId) {
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(id);
        FollowUpRecordDetailResponse response = BeanUtils.copyBean(new FollowUpRecordDetailResponse(), followUpRecord);
        List<BaseModuleFieldValue> fieldValueList = followUpRecordFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fieldValueList);
        buildListData(List.of(response));

        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_RECORD.getKey(), orgId);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, fieldValueList);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(response,
                FollowUpRecordDetailResponse::getOwner, FollowUpRecordDetailResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(response,
                FollowUpRecordDetailResponse::getContactId, FollowUpRecordDetailResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);

        response.setOptionMap(optionMap);

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
     * @return
     */
    public PagerWithOption<List<FollowUpRecordListResponse>> list(FollowUpRecordPageRequest request, String userId, String orgId, String resourceType, String type, CustomerDataDTO customerData) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<FollowUpRecordListResponse> list = extFollowUpRecordMapper.selectList(request, userId, orgId, resourceType, type, customerData);
        buildListData(list);

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


    /**
     * 删除跟进记录
     * @param id
     */
    public void delete(String id) {
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(id);
        if(followUpRecord == null){
            throw new GenericException("record_not_found");
        }
        followUpRecordMapper.deleteByPrimaryKey(id);
    }
}
