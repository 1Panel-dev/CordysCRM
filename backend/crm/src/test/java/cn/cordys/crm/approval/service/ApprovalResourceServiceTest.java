package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalResourceServiceTest {

    private ApprovalResourceService service;
    private BaseMapper<ApprovalTask> taskMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ApprovalResourceService();
        taskMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
    }

    @Test
    void completedAddSignApproverCanViewFinishedApproval() {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("instance-1");
        instance.setType("contract");
        instance.setSubmitterId("submitter");
        instance.setApprovalStatus(ApprovalStatus.APPROVED.name());

        ApprovalTask signTask = new ApprovalTask();
        signTask.setInstanceId(instance.getId());
        signTask.setApproverId("sign-approver");
        signTask.setType(ApprovalTaskType.SN.name());
        signTask.setStatus(ApprovalStatus.APPROVED.name());
        when(taskMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalTask> wrapper = invocation.getArgument(0);
            return wrapper.getParams().containsValue(instance.getId())
                    && wrapper.getParams().containsValue(signTask.getApproverId()) ? List.of(signTask) : List.of();
        });

        boolean permitted = ReflectionTestUtils.invokeMethod(service, "hasApprovalTaskPermission", instance, "sign-approver");

        assertTrue(permitted);
    }
}
