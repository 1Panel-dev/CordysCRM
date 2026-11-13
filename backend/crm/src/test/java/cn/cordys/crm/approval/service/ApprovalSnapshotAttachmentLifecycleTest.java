package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalResourceSnapshot;
import cn.cordys.crm.approval.handler.ApprovalResourceHandler;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalResourceSnapshotMapper;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalSnapshotAttachmentLifecycleTest {

    private ApprovalResourceService resourceService;
    private ApprovalActionService actionService;
    private ApprovalResourceHandler handler;
    private ApprovalResourceHandler previousHandler;
    private ExtApprovalResourceSnapshotMapper snapshotQueries;
    private BaseMapper<ApprovalResourceSnapshot> snapshotMapper;
    private AttachmentService attachments;
    private ApprovalResourceSnapshot snapshot;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        resourceService = new ApprovalResourceService();
        actionService = new ApprovalActionService();
        handler = mock(ApprovalResourceHandler.class);
        previousHandler = ApprovalResourceService.FORM_SERVICE.put(FormKey.CONTRACT, handler);
        snapshotQueries = mock(ExtApprovalResourceSnapshotMapper.class);
        snapshotMapper = mock(BaseMapper.class);
        attachments = mock(AttachmentService.class);
        ReflectionTestUtils.setField(resourceService, "extApprovalResourceSnapshotMapper", snapshotQueries);
        ReflectionTestUtils.setField(resourceService, "approvalResourceSnapshotMapper", snapshotMapper);
        ReflectionTestUtils.setField(resourceService, "attachmentService", attachments);
        ReflectionTestUtils.setField(actionService, "extApprovalResourceSnapshotMapper", snapshotQueries);
        ReflectionTestUtils.setField(actionService, "approvalResourceSnapshotMapper", snapshotMapper);
        ReflectionTestUtils.setField(actionService, "attachmentService", attachments);
        snapshot = new ApprovalResourceSnapshot();
        snapshot.setId("snapshot-old");
        snapshot.setResourceId("contract-1");
        snapshot.setSnapshotData("{\"moduleFields\":[]}");
        when(snapshotQueries.selectByResourceId("contract-1")).thenReturn(snapshot);
    }

    @AfterEach
    void restoreHandler() {
        if (previousHandler == null) {
            ApprovalResourceService.FORM_SERVICE.remove(FormKey.CONTRACT);
        } else {
            ApprovalResourceService.FORM_SERVICE.put(FormKey.CONTRACT, previousHandler);
        }
    }

    @Test
    void replacementSavesNewSnapshotWithoutDeletingAttachmentsBeforeFieldSave() {
        String replacement = "{\"moduleFields\":[{\"fieldId\":\"files\",\"fieldValue\":[\"file-new\"]}]}";
        try (MockedStatic<IDGenerator> ids = mockStatic(IDGenerator.class)) {
            ids.when(IDGenerator::nextStr).thenReturn("snapshot-new");

            resourceService.savePreUpdateSnapshot(FormKey.CONTRACT, "contract-1", "user-1", replacement);
        }

        ArgumentCaptor<ApprovalResourceSnapshot> saved = ArgumentCaptor.forClass(ApprovalResourceSnapshot.class);
        InOrder order = inOrder(snapshotQueries, snapshotMapper);
        order.verify(snapshotQueries).deleteByResourceId("contract-1");
        order.verify(snapshotMapper).insert(saved.capture());
        verifyNoInteractions(attachments);
        assertEquals("snapshot-new", saved.getValue().getId());
        assertEquals(replacement, saved.getValue().getSnapshotData());
    }

    @Test
    void snapshotSaveFailurePropagatesWithoutReleasingAttachments() {
        IllegalStateException failure = new IllegalStateException("快照保存失败");
        doThrow(failure).when(snapshotMapper).insert(any(ApprovalResourceSnapshot.class));
        try (MockedStatic<IDGenerator> ids = mockStatic(IDGenerator.class)) {
            ids.when(IDGenerator::nextStr).thenReturn("snapshot-new");

            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> resourceService.savePreUpdateSnapshot(FormKey.CONTRACT, "contract-1", "user-1", "{}")));
        }
        verifyNoInteractions(attachments);
    }

    @ParameterizedTest
    @ValueSource(strings = { "APPROVING", "APPROVED" })
    void approvalCompletionReleasesSnapshotEvenWhenResourceWasAlreadyApproved(String previousStatus) {
        ExtApprovalInstanceMapper instances = mock(ExtApprovalInstanceMapper.class);
        ReflectionTestUtils.setField(resourceService, "extApprovalInstanceMapper", instances);
        when(instances.selectApprovalStatusForUpdate("contract", "contract-1")).thenReturn(previousStatus);
        when(handler.getPreUpdateSnapshotData("contract-1", "user-1", "org-1")).thenReturn("current-data");

        resourceService.updateResourceApprovalStatus(FormKey.CONTRACT, "contract-1", ApprovalStatus.APPROVED.name(),
                "user-1", "org-1");

        InOrder order = inOrder(handler, snapshotMapper, attachments);
        order.verify(handler).getPreUpdateSnapshotData("contract-1", "user-1", "org-1");
        order.verify(snapshotMapper).deleteByPrimaryKey("snapshot-old");
        order.verify(attachments).cleanupSnapshotAttachments(snapshot, "current-data", "org-1", "user-1");
    }

    @Test
    void approvingResourceKeepsRollbackSnapshotAndAttachments() {
        ExtApprovalInstanceMapper instances = mock(ExtApprovalInstanceMapper.class);
        ReflectionTestUtils.setField(resourceService, "extApprovalInstanceMapper", instances);
        when(instances.selectApprovalStatusForUpdate("contract", "contract-1"))
                .thenReturn(ApprovalStatus.APPROVING.name());

        resourceService.updateResourceApprovalStatus(FormKey.CONTRACT, "contract-1", ApprovalStatus.APPROVING.name(),
                "user-1", "org-1");

        verifyNoInteractions(snapshotQueries, snapshotMapper, attachments);
    }

    @Test
    void successfulRestoreReleasesSnapshotAfterReadingRestoredFields() {
        when(handler.getPreUpdateSnapshotData("contract-1", "user-1", "org-1")).thenReturn("restored-data");

        actionService.revertFromSnapshot(FormKey.CONTRACT, "UPDATE", "contract-1", "user-1", "org-1");

        InOrder order = inOrder(handler, snapshotMapper, attachments);
        order.verify(handler).revertToSnapshot("contract-1", "user-1", "org-1", snapshot.getSnapshotData());
        order.verify(handler).getPreUpdateSnapshotData("contract-1", "user-1", "org-1");
        order.verify(snapshotMapper).deleteByPrimaryKey("snapshot-old");
        order.verify(attachments).cleanupSnapshotAttachments(snapshot, "restored-data", "org-1", "user-1");
    }

    @Test
    void failedRestoreKeepsSnapshotAndAttachments() {
        IllegalStateException failure = new IllegalStateException("数据恢复失败");
        doThrow(failure).when(handler).revertToSnapshot("contract-1", "user-1", "org-1", snapshot.getSnapshotData());

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> actionService.revertFromSnapshot(FormKey.CONTRACT, "UPDATE", "contract-1", "user-1", "org-1")));

        verifyNoInteractions(snapshotMapper, attachments);
        verify(snapshotQueries).selectByResourceId("contract-1");
    }
}
