package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.domain.ApprovalInstanceAttachment;
import cn.cordys.crm.approval.domain.ApprovalResourceSnapshot;
import cn.cordys.crm.approval.mapper.ExtApprovalResourceSnapshotMapper;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileCenter;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */
@Service
@Slf4j
public class AttachmentService {

    private static final String TEMP_OWNER_PREFIX = "attachment:temp:owner:";

    @Resource
    private BaseMapper<Attachment> attachmentMapper;

    @Resource
    private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;

    @Resource
    private ExtApprovalResourceSnapshotMapper approvalResourceSnapshotMapper;

    @Resource
    private FileCommonService fileCommonService;

    @Lazy
    @Resource
    private AttachmentPermissionService attachmentPermissionService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 上传临时附件
     *
     * @param files 上传附件集合
     */
    public List<String> uploadTemp(List<MultipartFile> files) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        if (StringUtils.isAnyBlank(userId, orgId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        List<String> tempPicIds = new ArrayList<>();
        FileRequest tempRequest = new FileRequest(null, StorageType.LOCAL.name(), null);
        files.forEach(file -> {
            String tempPicId = IDGenerator.nextStr();
            tempRequest.setFolder(DefaultRepositoryDir.getTempFileDir(tempPicId));
            tempRequest.setFileName(file.getOriginalFilename());
            fileCommonService.upload(file, tempRequest);
            stringRedisTemplate.opsForValue().set(TEMP_OWNER_PREFIX + tempPicId, orgId + ":" + userId,
                    Duration.ofDays(1));
            tempPicIds.add(tempPicId);
        });
        return tempPicIds;
    }

    /**
     * 删除附件
     *
     * @param attachmentId 附件ID
     */
    public void delete(String attachmentId) {
        checkAttachmentId(attachmentId);
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        checkPermission(attachmentId, attachment);
        deleteFile(attachmentId, attachment);
    }

    /**
     * 业务数据删除后清理附件，权限由调用方的业务删除校验保证。
     */
    public void deleteForResourceCleanup(String attachmentId) {
        deleteFile(attachmentId, attachmentMapper.selectByPrimaryKey(attachmentId));
    }

    private void deleteFile(String attachmentId, Attachment attachment) {
        if (attachment == null) {
            // 删除临时文件目录
            fileCommonService.deleteFolder(
                    new FileRequest(DefaultRepositoryDir.getTempFileDir(attachmentId), StorageType.LOCAL.name(), null),
                    true);
        } else {
            // 删除正式文件
            fileCommonService.deleteFolder(new FileRequest(
                    DefaultRepositoryDir.getTransferFileDir(attachment.getOrganizationId(), attachment.getResourceId(),
                            attachment.getId()),
                    StorageType.LOCAL.name(), attachment.getName()), false);
        }
    }

    /**
     * 获取文件流
     *
     * @param attachmentId 附件ID
     * @return 文件流
     */
    public ResponseEntity<org.springframework.core.io.Resource> getResource(String attachmentId) {
        checkAttachmentId(attachmentId);
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        // 在文件读取的异常捕获之外校验，确保无权限时返回禁止访问。
        if (attachment == null) {
            checkPermission(attachmentId, null);
        } else {
            attachmentPermissionService.checkReadPermission(attachment);
        }
        FileRequest request;
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        try {
            InputStream fileStream;
            if (attachment == null) {
                // get pic from temp dir
                request = new FileRequest(DefaultRepositoryDir.getTempFileDir(attachmentId), StorageType.LOCAL.name(),
                        null);
                List<File> folderFiles = fileCommonService.getFolderFiles(request);
                if (CollectionUtils.isEmpty(folderFiles)) {
                    return null;
                }
                File file = folderFiles.getFirst();
                fileStream = new FileInputStream(file);
                responseBuilder
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename*=UTF-8''" + encodeName(file.getName()))
                        .contentLength(file.length())
                        .contentType(isSvg(file.getName()) ? MediaType.parseMediaType("image/svg+xml")
                                : MediaType.parseMediaType("application/octet-stream"));
            } else {
                // get attachment from transferred dir
                request = new FileRequest(
                        DefaultRepositoryDir.getTransferFileDir(attachment.getOrganizationId(),
                                attachment.getResourceId(), attachment.getId()),
                        StorageType.LOCAL.name(), attachment.getName());
                fileStream = fileCommonService.getFileInputStream(request);
                if (fileStream == null) {
                    throw new GenericException("The file does not exist or has been deleted");
                }
                responseBuilder
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename*=UTF-8''" + encodeName(attachment.getName()))
                        .contentLength(attachment.getSize())
                        .contentType(isSvg(attachment.getName()) ? MediaType.parseMediaType("image/svg+xml")
                                : MediaType.parseMediaType("application/octet-stream"));
            }
            return responseBuilder
                    .body(new InputStreamResource(fileStream));
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public void checkPermission(Attachment attachment) {
        attachmentPermissionService.checkPermission(attachment);
    }

    private void checkPermission(String attachmentId, Attachment attachment) {
        if (attachment == null) {
            checkTemporaryPermission(attachmentId, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
        } else {
            checkPermission(attachment);
        }
    }

    private void checkTemporaryPermission(String attachmentId, String orgId, String userId) {
        checkAttachmentId(attachmentId);
        if (StringUtils.isAnyBlank(orgId, userId)
                || !(orgId + ":" + userId).equals(stringRedisTemplate.opsForValue().get(TEMP_OWNER_PREFIX + attachmentId))) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    private void checkAttachmentId(String attachmentId) {
        if (attachmentId == null || !attachmentId.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    /**
     * 判断是否svg文件
     *
     * @param fileName 文件名
     * @return 是否svg
     */
    private boolean isSvg(String fileName) {
        return fileName.endsWith(".svg") || fileName.endsWith(".SVG");
    }

    /**
     * 处理临时附件: 转存新的临时附件, 移除删除的正式附件
     *
     * @param transferRequest 转存参数
     */
    public void processTemp(UploadTransferRequest transferRequest) {
        List<String> requestedIds = transferRequest.getTempFileIds().stream().distinct().toList();
        requestedIds.forEach(this::checkAttachmentId);
        List<Attachment> attachments = new ArrayList<>();
        LambdaQueryWrapper<Attachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attachment::getResourceId, transferRequest.getResourceId());
        queryWrapper.eq(Attachment::getOrganizationId, transferRequest.getOrganizationId());
        List<Attachment> transferredAttachments = attachmentMapper.selectListByLambda(queryWrapper);
        List<String> transferredIds = transferredAttachments.stream().map(Attachment::getId).toList();
        attachmentMapper.selectByIds(requestedIds).forEach(attachment -> {
            if (!Objects.equals(transferRequest.getOrganizationId(), attachment.getOrganizationId())
                    || !Objects.equals(transferRequest.getResourceId(), attachment.getResourceId())) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
        });
        requestedIds.stream().filter(id -> !transferredIds.contains(id))
                .forEach(id -> checkTemporaryPermission(id, transferRequest.getOrganizationId(),
                        transferRequest.getOperatorUserId()));
        // insert new attachment
        requestedIds.stream().filter(tempFileId -> !transferredIds.contains(tempFileId))
                .forEach(tempFileId -> {
                    // transfer new pic
                    FileRequest request = new FileRequest(DefaultRepositoryDir.getTmpDir() + "/" + tempFileId,
                            StorageType.LOCAL.name(), null);
                    List<File> folderTempFiles = fileCommonService.getFolderFiles(request);
                    File tempFile = folderTempFiles == null ? null
                            : folderTempFiles.stream().filter(File::isFile).findFirst().orElse(null);
                    if (tempFile == null) {
                        throw new GenericException(Translator.get("approval.attachment.save_failed"));
                    }
                    FileCopyRequest copyRequest = new FileCopyRequest(
                            DefaultRepositoryDir.getTempFileDir(tempFileId),
                            DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(),
                                    transferRequest.getResourceId(), tempFileId),
                            tempFile.getName());
                    try {
                        FileCenter.getRepository(StorageType.LOCAL.name()).copyFile(copyRequest);
                    } catch (Exception e) {
                        throw new GenericException(Translator.get("approval.attachment.save_failed"), e);
                    }
                    Attachment attachment = new Attachment();
                    attachment.setId(tempFileId);
                    attachment.setName(tempFile.getName());
                    String type = "";
                    int index = tempFile.getName().lastIndexOf('.');
                    if (index != -1 && index < tempFile.getName().length() - 1) {
                        type = tempFile.getName().substring(index + 1);
                    }
                    attachment.setType(type);
                    attachment.setSize(tempFile.length());
                    attachment.setStorage(StorageType.LOCAL.name());
                    attachment.setOrganizationId(transferRequest.getOrganizationId());
                    attachment.setResourceId(transferRequest.getResourceId());
                    attachment.setCreateTime(System.currentTimeMillis());
                    attachment.setCreateUser(transferRequest.getOperatorUserId());
                    attachment.setUpdateTime(System.currentTimeMillis());
                    attachment.setUpdateUser(transferRequest.getOperatorUserId());
                    attachments.add(attachment);
                });
        attachments.forEach(attachmentMapper::insert);
        // remove deleted
        List<String> removedIds = transferredIds.stream().filter(aId -> !transferRequest.getTempFileIds().contains(aId))
                .toList();
        ApprovalResourceSnapshot snapshot = approvalResourceSnapshotMapper
                .selectByResourceId(transferRequest.getResourceId());
        Set<String> snapshotIds = snapshot == null ? Set.of() : snapshotAttachmentIds(snapshot.getSnapshotData());
        removedIds = removedIds.stream().filter(id -> !snapshotIds.contains(id)).toList();
        if (!CollectionUtils.isEmpty(removedIds)) {
            // 审批附件与表单附件共用资源ID，表单同步不能删除仍被审批记录引用的附件。
            LambdaQueryWrapper<ApprovalInstanceAttachment> approvalQueryWrapper = new LambdaQueryWrapper<>();
            approvalQueryWrapper.in(ApprovalInstanceAttachment::getAttachmentId, removedIds);
            Set<String> approvalAttachmentIds = approvalInstanceAttachmentMapper
                    .selectListByLambda(approvalQueryWrapper).stream()
                    .map(ApprovalInstanceAttachment::getAttachmentId)
                    .collect(Collectors.toSet());
            removedIds = removedIds.stream().filter(id -> !approvalAttachmentIds.contains(id)).toList();
        }
        if (!CollectionUtils.isEmpty(removedIds)) {
            Set<String> deletedIds = Set.copyOf(removedIds);
            transferredAttachments.stream().filter(attachment -> deletedIds.contains(attachment.getId()))
                    .forEach(this::checkPermission);
            deleteRemovedAttachments(transferRequest, removedIds);
        }
    }

    /**
     * 仅用于服务端已存字段或快照中的可信旧引用，正式记录缺失即失效，不依赖残留临时所有权。
     */
    public Set<String> findStaleAttachmentReferences(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Set.of();
        }
        Set<String> existingIds = attachmentMapper.selectByIds(ids).stream().map(Attachment::getId)
                .collect(Collectors.toSet());
        return ids.stream().filter(id -> !existingIds.contains(id))
                .collect(Collectors.toSet());
    }

    /**
     * 审批前快照解除引用后，仅清理它曾使用且已不再被当前字段或审批记录引用的附件。
     */
    public void cleanupSnapshotAttachments(ApprovalResourceSnapshot snapshot, String currentSnapshotData,
            String orgId, String userId) {
        // ponytail: 其他模块仍会容错读取字段，改用严格读取后再启用快照附件回收。
        if (!FormKey.CONTRACT.getKey().equals(snapshot.getFormKey())) {
            return;
        }
        Set<String> candidates = snapshotAttachmentIds(snapshot.getSnapshotData());
        candidates.removeAll(snapshotAttachmentIds(currentSnapshotData));
        ApprovalResourceSnapshot remaining = approvalResourceSnapshotMapper.selectByResourceId(snapshot.getResourceId());
        if (remaining != null) {
            candidates.removeAll(snapshotAttachmentIds(remaining.getSnapshotData()));
        }
        if (candidates.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<ApprovalInstanceAttachment> approvalQuery = new LambdaQueryWrapper<>();
        approvalQuery.in(ApprovalInstanceAttachment::getAttachmentId, new ArrayList<>(candidates));
        approvalInstanceAttachmentMapper.selectListByLambda(approvalQuery)
                .forEach(attachment -> candidates.remove(attachment.getAttachmentId()));
        List<String> removedIds = attachmentMapper.selectByIds(new ArrayList<>(candidates)).stream()
                .filter(attachment -> Objects.equals(orgId, attachment.getOrganizationId())
                        && Objects.equals(snapshot.getResourceId(), attachment.getResourceId()))
                .map(Attachment::getId).toList();
        if (!removedIds.isEmpty()) {
            deleteRemovedAttachments(new UploadTransferRequest(orgId, snapshot.getResourceId(), userId, List.of()),
                    removedIds);
        }
    }

    private Set<String> snapshotAttachmentIds(String snapshotData) {
        Set<String> ids = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(snapshotData)) {
            Map<?, ?> snapshot = JSON.parseObject(snapshotData, Map.class);
            collectSnapshotIds(snapshot.get("moduleFields"), ids);
        }
        return ids;
    }

    private void collectSnapshotIds(Object value, Set<String> ids) {
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(item -> collectSnapshotIds(item, ids));
        } else if (value instanceof List<?> list) {
            list.forEach(item -> collectSnapshotIds(item, ids));
        } else if (value instanceof String id) {
            ids.add(id);
        }
    }

    /**
     * 删除已移除的附件。
     * <p>
     * 事务中把附件记录删除延迟到提交前、物理文件删除延迟到提交后: 更新接口会先保存字段值(触发本方法)
     * 再在同一事务内记录操作日志并解析附件名称, 若立即删除附件记录, 日志将查不到名称。
     * 无事务上下文时直接删除。
     * </p>
     */
    private void deleteRemovedAttachments(UploadTransferRequest transferRequest, List<String> removedIds) {
        Runnable deleteRecords = () -> {
            LambdaQueryWrapper<Attachment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Attachment::getId, removedIds);
            attachmentMapper.deleteByLambda(queryWrapper);
        };
        Runnable deleteFiles = () -> removedIds.forEach(removeId -> {
            FileRequest request = new FileRequest(
                    DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(),
                            transferRequest.getResourceId(), removeId),
                    StorageType.LOCAL.name(), null);
            fileCommonService.deleteFolder(request, true);
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    deleteRecords.run();
                }

                @Override
                public void afterCommit() {
                    deleteFiles.run();
                }
            });
        } else {
            deleteRecords.run();
            deleteFiles.run();
        }
    }

    /**
     * 通过ID映射批量复制附件
     *
     * @param oldOfNewIdMap {旧的附件ID: 新的附件ID} 集合
     * @param targetId      目标资源ID
     * @param currentUser   当前操作用户
     */
    public void batchCopyOfIdMap(Map<String, String> oldOfNewIdMap, String targetId, String currentUser) {
        LambdaQueryWrapper<Attachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Attachment::getId, oldOfNewIdMap.keySet().stream().toList());
        List<Attachment> attachments = attachmentMapper.selectListByLambda(queryWrapper);
        Map<String, Attachment> attachmentMap = attachments.stream()
                .collect(Collectors.toMap(Attachment::getId, Function.identity()));
        List<Attachment> newAttachments = new ArrayList<>();
        oldOfNewIdMap.forEach((oId, nId) -> {
            if (!attachmentMap.containsKey(oId)) {
                return;
            }
            Attachment oldAttachment = attachmentMap.get(oId);
            // 复制文件
            FileCopyRequest copyRequest = new FileCopyRequest(
                    DefaultRepositoryDir.getTransferFileDir(oldAttachment.getOrganizationId(),
                            oldAttachment.getResourceId(), oId),
                    DefaultRepositoryDir.getTransferFileDir(oldAttachment.getOrganizationId(), targetId, nId),
                    oldAttachment.getName());
            Thread.startVirtualThread(() -> fileCommonService.copyFile(copyRequest, StorageType.LOCAL.name()));
            // 复制附件记录
            oldAttachment.setId(nId);
            oldAttachment.setResourceId(targetId);
            oldAttachment.setCreateTime(System.currentTimeMillis());
            oldAttachment.setCreateUser(currentUser);
            oldAttachment.setUpdateTime(System.currentTimeMillis());
            oldAttachment.setUpdateUser(currentUser);
            newAttachments.add(oldAttachment);
        });
        attachmentMapper.batchInsert(newAttachments);
    }

    /**
     * 编码文件名
     *
     * @param fileName 文件名
     * @return 编码后文件名
     */
    private String encodeName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
    }

    /**
     * 同步追加临时文件，附件保存失败时回滚调用方事务。
     *
     * @param transferRequest 参数
     * @return 转存后的正式附件ID
     */
    public List<String> appendTemp(UploadTransferRequest transferRequest) {
        List<String> tempFileIds = transferRequest.getTempFileIds().stream().distinct().toList();
        Set<String> existingIds = attachmentMapper.selectByIds(tempFileIds).stream()
                .peek(attachment -> {
                    if (!Objects.equals(transferRequest.getOrganizationId(), attachment.getOrganizationId())
                            || !Objects.equals(transferRequest.getResourceId(), attachment.getResourceId())) {
                        throw new GenericException(Translator.get("approval.attachment.save_failed"));
                    }
                    checkPermission(attachment);
                }).map(Attachment::getId).collect(Collectors.toSet());
        tempFileIds.stream().filter(id -> !existingIds.contains(id))
                .forEach(id -> checkTemporaryPermission(id, transferRequest.getOrganizationId(),
                        transferRequest.getOperatorUserId()));
        List<Attachment> attachments = new ArrayList<>();
        List<String> savedAttachmentIds = new ArrayList<>();
        tempFileIds.forEach(tempFileId -> {
            if (existingIds.contains(tempFileId)) {
                savedAttachmentIds.add(tempFileId);
                return;
            }
            FileRequest request = new FileRequest(DefaultRepositoryDir.getTmpDir() + "/" + tempFileId,
                    StorageType.LOCAL.name(), null);
            List<File> folderTempFiles = fileCommonService.getFolderFiles(request);
            File tempFile = folderTempFiles == null ? null
                    : folderTempFiles.stream().filter(File::isFile).findFirst().orElse(null);
            if (tempFile == null) {
                throw new GenericException(Translator.get("approval.attachment.save_failed"));
            }
            // 每次转存生成独立正式ID，避免批量审批共用附件文件。
            String attachmentId = IDGenerator.nextStr();
            FileCopyRequest copyRequest = new FileCopyRequest(DefaultRepositoryDir.getTempFileDir(tempFileId),
                    DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(),
                            transferRequest.getResourceId(), attachmentId),
                    tempFile.getName());
            try {
                FileCenter.getRepository(StorageType.LOCAL.name()).copyFile(copyRequest);
            } catch (Exception e) {
                throw new GenericException(Translator.get("approval.attachment.save_failed"), e);
            }
            Attachment attachment = new Attachment();
            attachment.setId(attachmentId);
            attachment.setName(tempFile.getName());
            String type = "";
            int index = tempFile.getName().lastIndexOf('.');
            if (index != -1 && index < tempFile.getName().length() - 1) {
                type = tempFile.getName().substring(index + 1);
            }
            attachment.setType(type);
            attachment.setSize(tempFile.length());
            attachment.setStorage(StorageType.LOCAL.name());
            attachment.setOrganizationId(transferRequest.getOrganizationId());
            attachment.setResourceId(transferRequest.getResourceId());
            attachment.setCreateTime(System.currentTimeMillis());
            attachment.setCreateUser(transferRequest.getOperatorUserId());
            attachment.setUpdateTime(System.currentTimeMillis());
            attachment.setUpdateUser(transferRequest.getOperatorUserId());
            attachments.add(attachment);
            savedAttachmentIds.add(attachmentId);
        });
        // 使用当前会话插入，使附件记录与审批关联共享事务。
        attachments.forEach(attachmentMapper::insert);
        return savedAttachmentIds;
    }
}
