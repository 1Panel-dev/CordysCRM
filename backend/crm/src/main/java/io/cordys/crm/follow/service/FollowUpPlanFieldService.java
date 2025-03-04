package io.cordys.crm.follow.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.service.BaseResourceFieldService;
import io.cordys.crm.follow.domain.FollowUpPlanField;
import io.cordys.crm.follow.domain.FollowUpPlanFieldBlob;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpPlanFieldService extends BaseResourceFieldService<FollowUpPlanField, FollowUpPlanFieldBlob> {

    @Resource
    private BaseMapper<FollowUpPlanField> followUpPlanFieldMapper;
    @Resource
    private BaseMapper<FollowUpPlanFieldBlob> followUpPlanFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.FOLLOW_PLAN.getKey();
    }

    @Override
    protected BaseMapper<FollowUpPlanField> getResourceFieldMapper() {
        return followUpPlanFieldMapper;
    }

    @Override
    protected BaseMapper<FollowUpPlanFieldBlob> getResourceFieldBlobMapper() {
        return followUpPlanFieldBlobMapper;
    }
}
