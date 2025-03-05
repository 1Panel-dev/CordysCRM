package io.cordys.crm.follow.service;

import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import io.cordys.crm.follow.dto.response.FollowUpRecordDetailResponse;
import io.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import io.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import io.cordys.crm.system.service.LogService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpRecordService {
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private FollowUpRecordFieldService followUpRecordFieldService;
    @Resource
    private ExtFollowUpRecordMapper extFollowUpRecordMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private LogService logService;

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
        followUpRecord.setCreateTime(System.currentTimeMillis());
        followUpRecord.setUpdateTime(System.currentTimeMillis());
        followUpRecord.setUpdateUser(userId);
        followUpRecord.setCreateUser(userId);
        followUpRecord.setId(IDGenerator.nextStr());
        followUpRecord.setOrganizationId(orgId);
        followUpRecordMapper.insert(followUpRecord);

        //保存自定义字段
        followUpRecordFieldService.saveModuleField(followUpRecord.getId(), request.getModuleFields());
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
        record.setLeadId(request.getLeadId());
        record.setOwner(request.getOwner());
        record.setContactId(request.getContactId());
        record.setContent(request.getContent());
        record.setUpdateTime(System.currentTimeMillis());
        record.setUpdateUser(userId);
        followUpRecordMapper.update(record);
    }


    /**
     * 跟进记录列表查询
     *
     * @param request
     * @param userId
     * @param orgId
     * @param resourceType
     * @param type
     * @return
     */
    public List<FollowUpRecordListResponse> list(FollowUpRecordPageRequest request, String userId, String orgId, String resourceType, String type) {
        List<FollowUpRecordListResponse> list = extFollowUpRecordMapper.selectList(request, userId, orgId, resourceType, type);
         buildListData(list);
        return list;
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
     * @param id
     * @return
     */
    public FollowUpRecordDetailResponse get(String id) {
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(id);
        FollowUpRecordDetailResponse response = BeanUtils.copyBean(new FollowUpRecordDetailResponse(), followUpRecord);
        List<BaseModuleFieldValue> fieldValueList = followUpRecordFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fieldValueList);
        buildListData(List.of(response));
        return response;
    }
}
