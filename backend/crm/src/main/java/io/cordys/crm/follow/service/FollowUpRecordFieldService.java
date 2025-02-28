package io.cordys.crm.follow.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.service.BaseResourceFieldService;
import io.cordys.crm.follow.domain.FollowUpField;
import io.cordys.crm.follow.domain.FollowUpFieldBlob;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpRecordFieldService extends BaseResourceFieldService<FollowUpField, FollowUpFieldBlob> {
    @Resource
    private BaseMapper<FollowUpField> followUpFieldMapper;
    @Resource
    private BaseMapper<FollowUpFieldBlob> followUpFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.FOLLOW_RECORD.getKey();
    }

    @Override
    protected BaseMapper<FollowUpField> getResourceFieldMapper() {
        return followUpFieldMapper;
    }

    @Override
    protected BaseMapper<FollowUpFieldBlob> getResourceFieldBlobMapper() {
        return followUpFieldBlobMapper;
    }
}