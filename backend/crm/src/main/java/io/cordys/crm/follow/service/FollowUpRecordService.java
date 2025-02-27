package io.cordys.crm.follow.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseModuleFieldValueService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.follow.domain.FollowUpField;
import io.cordys.crm.follow.domain.FollowUpFieldBlob;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpRecordService {
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private BaseMapper<FollowUpField> followUpFieldMapper;
    @Resource
    private BaseMapper<FollowUpFieldBlob> followUpFieldBlobMapper;
    @Resource
    private BaseModuleFieldValueService baseModuleFieldValueService;


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
        saveModuleField(followUpRecord.getId(), request.getModuleFields());
        return followUpRecord;
    }

    /**
     * 保存自定义字段
     *
     * @param id
     * @param moduleFields
     */
    private void saveModuleField(String id, List<BaseModuleFieldValue> moduleFields) {
        List<FollowUpField> followUpFields = baseModuleFieldValueService.getCustomerFields(FormKey.FOLLOW_RECORD.getKey(), moduleFields, FollowUpField.class);
        followUpFields.forEach(followUpField -> {
            followUpField.setId(IDGenerator.nextStr());
            followUpField.setFollowUpId(id);
        });
        if (CollectionUtils.isNotEmpty(followUpFields)) {
            followUpFieldMapper.batchInsert(followUpFields);
        }
    }

    /**
     * 更新跟进记录
     * @param request
     * @param userId
     * @return
     */
    public FollowUpRecord update(FollowUpRecordUpdateRequest request, String userId) {
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(followUpRecord).ifPresentOrElse(record -> {
            //更新跟进记录
            updateRecord(record,request, userId);
            //更新模块字段
            updateModuleField(request.getId(), request.getModuleFields());
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
        deleteFollowUpFieldByFollowUpId(followUpId);
        // 再保存
        saveModuleField(followUpId, moduleFields);
    }

    private void deleteFollowUpFieldByFollowUpId(String followUpId) {
        FollowUpField followUpField = new FollowUpField();
        followUpField.setFollowUpId(followUpId);
        followUpFieldMapper.delete(followUpField);
        FollowUpFieldBlob followUpFieldBlob = new FollowUpFieldBlob();
        followUpFieldBlob.setFollowUpId(followUpId);
        followUpFieldBlobMapper.delete(followUpFieldBlob);
    }

    private void updateRecord(FollowUpRecord record, FollowUpRecordUpdateRequest request, String userId) {
        record.setCustomerId(request.getCustomerId());
        record.setOpportunityId(request.getOpportunityId());
        record.setType(request.getType());
        record.setLeadId(request.getLeadId());
        record.setOwner(request.getOwner());
        record.setContactId(request.getContactId());
        record.setUpdateTime(System.currentTimeMillis());
        record.setUpdateUser(userId);
        followUpRecordMapper.update(record);
    }
}
