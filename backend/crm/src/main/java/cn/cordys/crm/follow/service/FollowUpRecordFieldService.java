package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.follow.domain.FollowUpRecordField;
import cn.cordys.crm.follow.domain.FollowUpRecordFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpRecordFieldService extends BaseResourceFieldService<FollowUpRecordField, FollowUpRecordFieldBlob> {
    @Resource
    private BaseMapper<FollowUpRecordField> followUpRecordFieldMapper;
    @Resource
    private BaseMapper<FollowUpRecordFieldBlob> followUpRecordFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.FOLLOW_RECORD.getKey();
    }

    @Override
    protected BaseMapper<FollowUpRecordField> getResourceFieldMapper() {
        return followUpRecordFieldMapper;
    }

    @Override
    protected BaseMapper<FollowUpRecordFieldBlob> getResourceFieldBlobMapper() {
        return followUpRecordFieldBlobMapper;
    }
}