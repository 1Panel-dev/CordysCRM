package cn.cordys.crm.approval.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalAction;
import cn.cordys.crm.approval.constants.ApprovalAddSignType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.request.ApprovalAddSignRequest;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.FileCommonService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApprovalAddSignService {

    @Resource
    private BaseMapper<ApprovalFlow> approvalFlowMapper;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalAddSignTask> approvalAddSignTasMapper;
    @Resource
    private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;
    @Resource
    private FileCommonService fileCommonService;

    /**
     * 加签流程
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    public void addSign(ApprovalAddSignRequest request, String userId, String orgId) {
        //校验流程是否允许加签
        checkAddSignPermission(request.getInstanceId());
        //新增加签task
        addApprovalTask(request, userId);
        //新增加签task拓展信息
        ApprovalAddSignTask approvalAddSignTask = addAddSignTask(request);
        //保存加签附件
        addAttachment(request, userId, orgId, approvalAddSignTask);
        //更新原task
        updateOrgTask(request);

    }


    /**
     * 保存加签附件
     *
     * @param request
     * @param userId
     * @param orgId
     * @param approvalAddSignTask
     */
    private void addAttachment(ApprovalAddSignRequest request, String userId, String orgId, ApprovalAddSignTask approvalAddSignTask) {
        UploadTransferRequest transferRequest = new UploadTransferRequest(orgId, approvalAddSignTask.getId(), userId, request.getAttachmentIds());
        List<ApprovalInstanceAttachment> attachments = new ArrayList<>();
        LambdaQueryWrapper<ApprovalInstanceAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApprovalInstanceAttachment::getApprovalElementId, transferRequest.getResourceId());
        List<ApprovalInstanceAttachment> transferredAttachments = approvalInstanceAttachmentMapper.selectListByLambda(queryWrapper);
        List<String> transferredIds = transferredAttachments.stream().map(ApprovalInstanceAttachment::getAttachmentId).toList();

        request.getAttachmentIds().stream().filter(tempFileId -> !transferredIds.contains(tempFileId)).forEach(tempFileId -> {
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
                attachment.setInstanceId(request.getInstanceId());
                attachment.setApprovalElementId(approvalAddSignTask.getId());
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
     * 新增加签task拓展信息
     *
     * @param request
     */
    private ApprovalAddSignTask addAddSignTask(ApprovalAddSignRequest request) {
        ApprovalAddSignTask approvalAddSignTask = new ApprovalAddSignTask();
        approvalAddSignTask.setId(IDGenerator.nextStr());
        approvalAddSignTask.setSignTaskId(request.getId());

        //获取taskId
        ApprovalAddSignTask addSignTask = approvalAddSignTasMapper.selectListByLambda(
                new LambdaQueryWrapper<ApprovalAddSignTask>()
                        .eq(ApprovalAddSignTask::getSignTaskId, request.getId())
        ).stream().findFirst().orElse(null);
        if (addSignTask == null) {
            approvalAddSignTask.setTaskId(request.getId());
        } else {
            approvalAddSignTask.setTaskId(addSignTask.getTaskId());
        }

        approvalAddSignTask.setType(request.getType());
        approvalAddSignTask.setComment(request.getComment());
        approvalAddSignTasMapper.insert(approvalAddSignTask);
        return approvalAddSignTask;
    }

    /**
     * 更新原task
     *
     * @param request
     */
    private void updateOrgTask(ApprovalAddSignRequest request) {
        //更新原task action
        ApprovalTask approvalTask = approvalTaskMapper.selectByPrimaryKey(request.getId());
        if (Strings.CI.equals(request.getType(), ApprovalAddSignType.BEFORE.name())) {
            //之前：加签
            approvalTask.setAction(ApprovalAction.SIGN.name());
        }
        if (Strings.CI.equals(request.getType(), ApprovalAddSignType.AFTER.name())) {
            //之后：同意
            approvalTask.setAction(ApprovalAction.APPROVE.name());
        }
        approvalTaskMapper.update(approvalTask);
    }


    /**
     * 新增加签task 任务
     *
     * @param request
     * @param userId
     */
    private ApprovalTask addApprovalTask(ApprovalAddSignRequest request, String userId) {
        ApprovalTask approvalTask = new ApprovalTask();
        BeanUtils.copyBean(approvalTask, request);
        approvalTask.setId(IDGenerator.nextStr());
        approvalTask.setCreateTime(System.currentTimeMillis());
        approvalTask.setUpdateTime(System.currentTimeMillis());
        approvalTask.setCreateUser(userId);
        approvalTask.setUpdateUser(userId);
        approvalTask.setStatus(ApprovalStatus.APPROVING.name());
        approvalTask.setType(ApprovalTaskType.SN.name());
        approvalTaskMapper.insert(approvalTask);
        return approvalTask;
    }


    /**
     * 流程是允许加签
     *
     * @param id
     */
    private void checkAddSignPermission(String id) {
        ApprovalInstance approvalInstance = approvalInstanceMapper.selectByPrimaryKey(id);
        ApprovalFlow approvalFlow = approvalFlowMapper.selectByPrimaryKey(approvalInstance.getFlowId());
        if (approvalFlow == null || !approvalFlow.getAllowAddSign()) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
    }
}
