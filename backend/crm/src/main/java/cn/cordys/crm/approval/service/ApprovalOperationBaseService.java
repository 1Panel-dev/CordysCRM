package cn.cordys.crm.approval.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalInstanceAttachment;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalOperationBaseRequest;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ApprovalOperationBaseService {

    @Resource
    private BaseMapper<ApprovalFlow> approvalFlowMapper;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;
    @Resource
    private FileCommonService fileCommonService;


    /**
     * 保存加签附件
     *
     * @param attachmentIds
     * @param instanceId
     * @param userId
     * @param orgId
     * @param approvalElementId
     */
    public void addAttachment(List<String> attachmentIds, String instanceId, String userId, String orgId, String approvalElementId) {
        UploadTransferRequest transferRequest = new UploadTransferRequest(orgId, approvalElementId, userId, attachmentIds);
        List<ApprovalInstanceAttachment> attachments = new ArrayList<>();
        LambdaQueryWrapper<ApprovalInstanceAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApprovalInstanceAttachment::getApprovalElementId, transferRequest.getResourceId());
        List<ApprovalInstanceAttachment> transferredAttachments = approvalInstanceAttachmentMapper.selectListByLambda(queryWrapper);
        List<String> transferredIds = transferredAttachments.stream().map(ApprovalInstanceAttachment::getAttachmentId).toList();

        attachmentIds.stream().filter(tempFileId -> !transferredIds.contains(tempFileId)).forEach(tempFileId -> {
            FileRequest fileRequest = new FileRequest(DefaultRepositoryDir.getTmpDir() + "/" + tempFileId, StorageType.LOCAL.name(), null);
            List<File> folderTempFiles = fileCommonService.getFolderFiles(fileRequest);
            if (!CollectionUtils.isEmpty(folderTempFiles)) {
                File tempFile = folderTempFiles.getFirst();
                FileCopyRequest copyRequest = new FileCopyRequest(DefaultRepositoryDir.getTempFileDir(tempFileId),
                        DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), tempFileId),
                        tempFile.getName());
                fileCommonService.copyFile(copyRequest, StorageType.LOCAL.name());
                ApprovalInstanceAttachment attachment = new ApprovalInstanceAttachment();
                attachment.setId(IDGenerator.nextStr());
                attachment.setInstanceId(instanceId);
                attachment.setApprovalElementId(approvalElementId);
                attachment.setAttachmentId(tempFileId);
                attachments.add(attachment);
            }
        });
        approvalInstanceAttachmentMapper.batchInsert(attachments);

        List<String> removedIds = transferredIds.stream().filter(aId -> !transferRequest.getTempFileIds().contains(aId)).toList();
        if (!CollectionUtils.isEmpty(removedIds)) {
            LambdaQueryWrapper<ApprovalInstanceAttachment> duplicateQueryWrapper = new LambdaQueryWrapper<>();
            duplicateQueryWrapper.in(ApprovalInstanceAttachment::getAttachmentId, removedIds);
            approvalInstanceAttachmentMapper.deleteByLambda(duplicateQueryWrapper);
            removedIds.forEach(removeId -> {
                FileRequest fileRequest = new FileRequest(DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), removeId), StorageType.LOCAL.name(), null);
                fileCommonService.deleteFolder(fileRequest, true);
            });
        }

    }


    /**
     * 获取流程配置相关权限
     *
     * @param id
     * @return
     */
    public ApprovalFlow getFlowPermission(String id) {
        ApprovalInstance approvalInstance = approvalInstanceMapper.selectByPrimaryKey(id);
        ApprovalFlow approvalFlow = approvalFlowMapper.selectByPrimaryKey(approvalInstance.getFlowId());
        return approvalFlow;
    }


    /**
     * 按ACTION 新增task任務
     *
     * @param request
     * @param userId
     * @return
     */
    public ApprovalTask addApprovalTask(ApprovalOperationBaseRequest request, String userId, String action) {
        ApprovalTask approvalTask = new ApprovalTask();
        BeanUtils.copyBean(approvalTask, request);
        approvalTask.setId(IDGenerator.nextStr());
        approvalTask.setCreateTime(System.currentTimeMillis());
        approvalTask.setUpdateTime(System.currentTimeMillis());
        approvalTask.setCreateUser(userId);
        approvalTask.setUpdateUser(userId);
        approvalTask.setStatus(ApprovalStatus.APPROVING.name());
        approvalTask.setType(action);
        approvalTaskMapper.insert(approvalTask);
        return approvalTask;
    }

}
