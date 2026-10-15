package cn.cordys.crm.system.service;

import cn.cordys.crm.approval.domain.ApprovalInstanceAttachment;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AttachmentServiceTest {
    private static final String ORGANIZATION_ID = "test-org";
    private static final String RESOURCE_ID = "test-resource";
    private static final String USER_ID = "test-user";

    private final AttachmentService service = new AttachmentService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Attachment> attachmentMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalInstanceAttachment> approvalMapper = mock(BaseMapper.class);
    private final FileCommonService files = mock(FileCommonService.class);

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "attachmentMapper", attachmentMapper);
        ReflectionTestUtils.setField(service, "approvalInstanceAttachmentMapper", approvalMapper);
        ReflectionTestUtils.setField(service, "fileCommonService", files);
    }

    @Test
    void emptyFormDoesNotDeleteAttachmentsFromEitherApproval() {
        when(attachmentMapper.selectListByLambda(any())).thenReturn(List.of(attachment("approval-a"), attachment("approval-b")));
        when(approvalMapper.selectListByLambda(any())).thenReturn(List.of(approval("instance-a", "approval-a"), approval("instance-a", "approval-b")));

        service.processTemp(request(List.of()));

        assertAll(
                () -> verify(attachmentMapper, never()).deleteByLambda(any()),
                () -> verifyNoInteractions(files));
        verifyResourceScope();
        verifyApprovalCandidates(List.of("approval-a", "approval-b"));
    }

    @Test
    void onlyRemovedFormAttachmentIsDeletedWhenApprovalAndFormAttachmentsCoexist() {
        when(attachmentMapper.selectListByLambda(any())).thenReturn(List.of(
                attachment("approval-a"), attachment("approval-b"), attachment("form-removed"), attachment("form-kept")));
        when(approvalMapper.selectListByLambda(any())).thenReturn(List.of(approval("instance-a", "approval-a"), approval("instance-b", "approval-b")));

        service.processTemp(request(List.of("form-kept")));

        verifyDeletedAttachments(List.of("form-removed"));
        verifyResourceScope();
        verifyApprovalCandidates(List.of("approval-a", "approval-b", "form-removed"));
    }

    @Test
    void emptyFormStillDeletesAttachmentsWithoutApprovalReferences() {
        when(attachmentMapper.selectListByLambda(any())).thenReturn(List.of(attachment("form-removed")));
        when(approvalMapper.selectListByLambda(any())).thenReturn(List.of());

        service.processTemp(request(List.of()));

        verifyResourceScope();
        verifyApprovalCandidates(List.of("form-removed"));
        verifyDeletedAttachments(List.of("form-removed"));
    }

    @Test
    void newTemporaryFileIsCopiedAndInsertedWithoutDeletingKeptAttachments() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("new-file.txt"), "测试附件内容");
        when(attachmentMapper.selectListByLambda(any())).thenReturn(List.of(attachment("form-kept")));
        when(files.getFolderFiles(any())).thenReturn(List.of(source.toFile()));

        service.processTemp(request(List.of("form-kept", "new-file")));

        verifyResourceScope();
        verifyNoInteractions(approvalMapper);
        verify(attachmentMapper, never()).deleteByLambda(any());
        verify(files, never()).deleteFolder(any(), anyBoolean());
        ArgumentCaptor<FileRequest> folderRequest = ArgumentCaptor.forClass(FileRequest.class);
        verify(files).getFolderFiles(folderRequest.capture());
        assertEquals(DefaultRepositoryDir.getTempFileDir("new-file"), folderRequest.getValue().getFolder());
        assertEquals(StorageType.LOCAL.name(), folderRequest.getValue().getStorage());
        ArgumentCaptor<FileCopyRequest> copyRequest = ArgumentCaptor.forClass(FileCopyRequest.class);
        verify(files).copyFile(copyRequest.capture(), eq(StorageType.LOCAL.name()));
        assertEquals(DefaultRepositoryDir.getTempFileDir("new-file"), copyRequest.getValue().getSourceFolder());
        assertEquals(DefaultRepositoryDir.getTransferFileDir(ORGANIZATION_ID, RESOURCE_ID, "new-file"), copyRequest.getValue().getDestFolder());
        assertEquals(source.getFileName().toString(), copyRequest.getValue().getFileName());
        verifyNoMoreInteractions(files);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Attachment>> inserted = ArgumentCaptor.forClass(List.class);
        verify(attachmentMapper).batchInsert(inserted.capture());
        assertEquals(1, inserted.getValue().size());
        Attachment attachment = inserted.getValue().getFirst();
        assertEquals("new-file", attachment.getId());
        assertEquals("new-file.txt", attachment.getName());
        assertEquals("txt", attachment.getType());
        assertEquals(Files.size(source), attachment.getSize());
        assertEquals(StorageType.LOCAL.name(), attachment.getStorage());
        assertEquals(ORGANIZATION_ID, attachment.getOrganizationId());
        assertEquals(RESOURCE_ID, attachment.getResourceId());
        assertEquals(USER_ID, attachment.getCreateUser());
        assertEquals(USER_ID, attachment.getUpdateUser());
    }

    private void verifyResourceScope() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Attachment>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(attachmentMapper).selectListByLambda(query.capture());
        assertEquals(Map.of("resource_id_eq", RESOURCE_ID, "organization_id_eq", ORGANIZATION_ID), query.getValue().getParams());
        assertTrue(query.getValue().getWhereClause().contains("resource_id = #{resource_id_eq}"));
        assertTrue(query.getValue().getWhereClause().contains("organization_id = #{organization_id_eq}"));
    }

    private void verifyApprovalCandidates(List<String> attachmentIds) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ApprovalInstanceAttachment>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(approvalMapper).selectListByLambda(query.capture());
        assertEquals(Map.of("array_attachment_id", attachmentIds), query.getValue().getParams());
        assertTrue(query.getValue().getWhereClause().contains("attachment_id IN"));
    }

    private void verifyDeletedAttachments(List<String> attachmentIds) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Attachment>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(attachmentMapper).deleteByLambda(query.capture());
        ArgumentCaptor<FileRequest> deletedFolders = ArgumentCaptor.forClass(FileRequest.class);
        verify(files, atLeastOnce()).deleteFolder(deletedFolders.capture(), eq(true));
        assertAll(
                () -> assertEquals(Map.of("array_id", attachmentIds), query.getValue().getParams()),
                () -> assertTrue(query.getValue().getWhereClause().contains("id IN")),
                () -> assertEquals(attachmentIds.stream().map(id -> DefaultRepositoryDir.getTransferFileDir(ORGANIZATION_ID, RESOURCE_ID, id)).toList(),
                        deletedFolders.getAllValues().stream().map(FileRequest::getFolder).toList()),
                () -> assertTrue(deletedFolders.getAllValues().stream().allMatch(request -> StorageType.LOCAL.name().equals(request.getStorage()))));
        verifyNoMoreInteractions(files);
    }

    private UploadTransferRequest request(List<String> attachmentIds) {
        return new UploadTransferRequest(ORGANIZATION_ID, RESOURCE_ID, USER_ID, attachmentIds);
    }

    private Attachment attachment(String id) {
        Attachment attachment = new Attachment();
        attachment.setId(id);
        attachment.setOrganizationId(ORGANIZATION_ID);
        attachment.setResourceId(RESOURCE_ID);
        return attachment;
    }

    private ApprovalInstanceAttachment approval(String instanceId, String attachmentId) {
        ApprovalInstanceAttachment attachment = new ApprovalInstanceAttachment();
        attachment.setInstanceId(instanceId);
        attachment.setElementId("element-" + attachmentId);
        attachment.setAttachmentId(attachmentId);
        return attachment;
    }
}
