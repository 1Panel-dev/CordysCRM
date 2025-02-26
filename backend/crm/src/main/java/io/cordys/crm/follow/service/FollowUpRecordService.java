package io.cordys.crm.follow.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.service.BaseModuleFieldValueService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.follow.domain.FollowUpField;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpRecordService {
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private BaseMapper<FollowUpField> followUpFieldMapper;
    @Resource
    private BaseModuleFieldValueService baseModuleFieldValueService;


    /**
     * 添加跟进记录
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
}
