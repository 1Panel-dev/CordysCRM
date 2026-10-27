package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.domain.ApprovalInstanceAttachment;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.LocalRepository;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AttachmentServiceTest {
    private static final String ORGANIZATION_ID = "test-org";
    private static final String RESOURCE_ID = "test-resource";
    private static final String USER_ID = "test-user";
    private static final String SAVE_FAILED_MESSAGE = "审批附件保存失败";

    private ApplicationContext previousApplicationContext;
    private MessageSource previousMessageSource;
    private final AttachmentService service = new AttachmentService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Attachment> attachmentMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<ApprovalInstanceAttachment> approvalMapper = mock(BaseMapper.class);
    private final FileCommonService files = mock(FileCommonService.class);
    private final LocalRepository repository = mock(LocalRepository.class);

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "attachmentMapper", attachmentMapper);
        ReflectionTestUtils.setField(service, "approvalInstanceAttachmentMapper", approvalMapper);
        ReflectionTestUtils.setField(service, "fileCommonService", files);
        previousApplicationContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class,
                "context");
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(LocalRepository.class)).thenReturn(repository);
        DefaultUidGenerator generator = new DefaultUidGenerator();
        ReflectionTestUtils.setField(generator, "workerIdAssigner", (WorkerIdAssigner) () -> 1L);
        generator.init();
        when(context.getBean(DefaultUidGenerator.class)).thenReturn(generator);
        new CommonBeanFactory().setApplicationContext(context);
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("approval.attachment.save_failed", LocaleContextHolder.getLocale(), SAVE_FAILED_MESSAGE);
        new Translator().setMessageSource(messages);
    }

    @AfterEach
    void restoreContext() {
        new CommonBeanFactory().setApplicationContext(previousApplicationContext);
        new Translator().setMessageSource(previousMessageSource);
    }

    @Test
    void emptyFormDoesNotDeleteAttachmentsFromEitherApproval() {
        when(attachmentMapper.selectListByLambda(any()))
                .thenReturn(List.of(attachment("approval-a"), attachment("approval-b")));
        when(approvalMapper.selectListByLambda(any()))
                .thenReturn(List.of(approval("instance-a", "approval-a"), approval("instance-a", "approval-b")));

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
                attachment("approval-a"), attachment("approval-b"), attachment("form-removed"),
                attachment("form-kept")));
        when(approvalMapper.selectListByLambda(any()))
                .thenReturn(List.of(approval("instance-a", "approval-a"), approval("instance-b", "approval-b")));

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
        assertEquals(DefaultRepositoryDir.getTransferFileDir(ORGANIZATION_ID, RESOURCE_ID, "new-file"),
                copyRequest.getValue().getDestFolder());
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

    @ParameterizedTest
    @ValueSource(strings = { "gif", "GIF" })
    void approvalGifIsCopiedAndInsertedBeforeReturning(String extension) throws Exception {
        Path source = Files.write(temporaryDirectory.resolve("审批动图." + extension),
                Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"));
        when(files.getFolderFiles(any())).thenReturn(List.of(source.toFile()));
        List<Runnable> asyncTasks = new ArrayList<>();
        AsyncAnnotationBeanPostProcessor asyncProcessor = new AsyncAnnotationBeanPostProcessor();
        asyncProcessor.setExecutor(asyncTasks::add);
        asyncProcessor.setBeanFactory(new DefaultListableBeanFactory());
        AttachmentService proxy = (AttachmentService) asyncProcessor.postProcessAfterInitialization(service,
                "attachmentService");

        List<String> savedIds = proxy.appendTemp(request(List.of("approval-gif", "approval-gif")));

        assertTrue(asyncTasks.isEmpty());
        verify(attachmentMapper).selectByIds(List.of("approval-gif"));
        ArgumentCaptor<FileCopyRequest> copyRequest = ArgumentCaptor.forClass(FileCopyRequest.class);
        verify(repository).copyFile(copyRequest.capture());
        assertEquals(DefaultRepositoryDir.getTempFileDir("approval-gif"), copyRequest.getValue().getSourceFolder());
        assertEquals(source.getFileName().toString(), copyRequest.getValue().getFileName());
        ArgumentCaptor<Attachment> inserted = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentMapper).insert(inserted.capture());
        Attachment attachment = inserted.getValue();
        assertEquals(List.of(attachment.getId()), savedIds);
        assertNotEquals("approval-gif", attachment.getId());
        assertEquals(DefaultRepositoryDir.getTransferFileDir(ORGANIZATION_ID, RESOURCE_ID, attachment.getId()),
                copyRequest.getValue().getDestFolder());
        assertEquals(source.getFileName().toString(), attachment.getName());
        assertEquals(extension, attachment.getType());
        assertEquals(Files.size(source), attachment.getSize());
        assertEquals(StorageType.LOCAL.name(), attachment.getStorage());
        assertEquals(ORGANIZATION_ID, attachment.getOrganizationId());
        assertEquals(RESOURCE_ID, attachment.getResourceId());
        assertEquals(USER_ID, attachment.getCreateUser());
        verify(attachmentMapper, never()).batchInsert(any());
    }

    @Test
    void missingApprovalAttachmentFailsWithoutInsertingMetadata() {
        when(files.getFolderFiles(any())).thenReturn(List.of());

        GenericException error = assertThrows(GenericException.class,
                () -> service.appendTemp(request(List.of("missing-file"))));

        assertEquals(SAVE_FAILED_MESSAGE, error.getMessage());
        verify(attachmentMapper, never()).insert(any());
        verify(attachmentMapper, never()).batchInsert(any());
        verifyNoInteractions(repository);
    }

    @Test
    void approvalAttachmentCopyFailurePreventsMetadataInsert() throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("审批动图.gif"), "GIF89a");
        when(files.getFolderFiles(any())).thenReturn(List.of(source.toFile()));
        IOException copyFailure = new IOException("复制失败");
        doThrow(copyFailure).when(repository).copyFile(any());

        GenericException error = assertThrows(GenericException.class,
                () -> service.appendTemp(request(List.of("approval-gif"))));

        assertEquals(SAVE_FAILED_MESSAGE, error.getMessage());
        assertSame(copyFailure, error.getCause());
        verify(attachmentMapper, never()).insert(any());
        verify(attachmentMapper, never()).batchInsert(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { RESOURCE_ID, "another-resource" })
    void temporaryApprovalAttachmentCreatesIndependentCopiesForEachAction(String secondResource) throws Exception {
        Path source = Files.writeString(temporaryDirectory.resolve("审批动图.gif"), "GIF89a");
        when(files.getFolderFiles(any())).thenReturn(List.of(source.toFile()));
        Map<String, Attachment> persisted = new LinkedHashMap<>();
        when(attachmentMapper.selectByIds(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            return ids.stream().map(persisted::get).filter(Objects::nonNull).toList();
        });
        doAnswer(invocation -> {
            Attachment attachment = invocation.getArgument(0);
            assertNull(persisted.put(attachment.getId(), attachment));
            return 1;
        }).when(attachmentMapper).insert(any());
        List<String> temporaryIds = List.of("approval-gif");

        List<String> firstIds = service.appendTemp(request(temporaryIds));
        List<String> secondIds = service.appendTemp(new UploadTransferRequest(ORGANIZATION_ID, secondResource,
                USER_ID, temporaryIds));

        assertEquals(1, firstIds.size());
        assertEquals(1, secondIds.size());
        assertNotEquals(firstIds, secondIds);
        assertNotEquals(temporaryIds, firstIds);
        assertNotEquals(temporaryIds, secondIds);
        assertEquals(List.of("approval-gif"), temporaryIds);
        assertEquals(List.of(firstIds.getFirst(), secondIds.getFirst()), new ArrayList<>(persisted.keySet()));
        assertEquals(RESOURCE_ID, persisted.get(firstIds.getFirst()).getResourceId());
        assertEquals(secondResource, persisted.get(secondIds.getFirst()).getResourceId());
        persisted.values().forEach(attachment -> {
            assertEquals(ORGANIZATION_ID, attachment.getOrganizationId());
            assertEquals(USER_ID, attachment.getCreateUser());
            assertEquals(source.getFileName().toString(), attachment.getName());
            assertEquals(source.toFile().length(), attachment.getSize());
        });
        ArgumentCaptor<FileCopyRequest> copies = ArgumentCaptor.forClass(FileCopyRequest.class);
        verify(repository, times(2)).copyFile(copies.capture());
        assertEquals(List.of(
                DefaultRepositoryDir.getTransferFileDir(ORGANIZATION_ID, RESOURCE_ID, firstIds.getFirst()),
                DefaultRepositoryDir.getTransferFileDir(ORGANIZATION_ID, secondResource, secondIds.getFirst())),
                copies.getAllValues().stream().map(FileCopyRequest::getDestFolder).toList());
        copies.getAllValues().forEach(copy -> {
            assertEquals(DefaultRepositoryDir.getTempFileDir("approval-gif"), copy.getSourceFolder());
            assertEquals(source.getFileName().toString(), copy.getFileName());
        });
        verify(attachmentMapper, times(2)).insert(any());
        verify(files, never()).deleteFolder(any(), anyBoolean());
        verify(attachmentMapper, never()).batchInsert(any());
    }

    @Test
    void existingApprovalAttachmentInCurrentScopeIsReusedWithoutCopying() {
        List<String> savedIds = List.of("approval-saved");
        when(attachmentMapper.selectByIds(savedIds)).thenReturn(List.of(attachment("approval-saved")));

        assertEquals(savedIds, service.appendTemp(request(savedIds)));
        assertEquals(savedIds, service.appendTemp(request(savedIds)));

        verifyNoInteractions(files, repository);
        verify(attachmentMapper, never()).insert(any());
        verify(attachmentMapper, never()).batchInsert(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "organization", "resource" })
    void existingApprovalAttachmentOutsideCurrentScopeIsRejected(String mismatchedScope) {
        Attachment existing = attachment("approval-gif");
        if ("organization".equals(mismatchedScope)) {
            existing.setOrganizationId("another-org");
        } else {
            existing.setResourceId("another-resource");
        }
        when(attachmentMapper.selectByIds(List.of("approval-gif"))).thenReturn(List.of(existing));

        GenericException error = assertThrows(GenericException.class,
                () -> service.appendTemp(request(List.of("approval-gif"))));

        assertEquals(SAVE_FAILED_MESSAGE, error.getMessage());
        verifyNoInteractions(files, repository);
        verify(attachmentMapper, never()).insert(any());
        verify(attachmentMapper, never()).batchInsert(any());
    }

    private void verifyResourceScope() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Attachment>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(attachmentMapper).selectListByLambda(query.capture());
        assertEquals(Map.of("resource_id_eq", RESOURCE_ID, "organization_id_eq", ORGANIZATION_ID),
                query.getValue().getParams());
        assertTrue(query.getValue().getWhereClause().contains("resource_id = #{resource_id_eq}"));
        assertTrue(query.getValue().getWhereClause().contains("organization_id = #{organization_id_eq}"));
    }

    private void verifyApprovalCandidates(List<String> attachmentIds) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ApprovalInstanceAttachment>> query = ArgumentCaptor
                .forClass(LambdaQueryWrapper.class);
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
                () -> assertEquals(attachmentIds.stream()
                        .map(id -> DefaultRepositoryDir.getTransferFileDir(ORGANIZATION_ID, RESOURCE_ID, id)).toList(),
                        deletedFolders.getAllValues().stream().map(FileRequest::getFolder).toList()),
                () -> assertTrue(deletedFolders.getAllValues().stream()
                        .allMatch(request -> StorageType.LOCAL.name().equals(request.getStorage()))));
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
