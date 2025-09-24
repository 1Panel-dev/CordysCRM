package cn.cordys.crm.system.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class AttachmentService {

    @Resource
    private BaseMapper<Attachment> attachmentMapper;

    @Resource
    private FileCommonService fileCommonService;

    /**
     * 上传临时附件
     *
     * @param files 上传附件集合
     */
    public List<String> uploadTemp(List<MultipartFile> files) {
        List<String> tempPicIds = new ArrayList<>();
        FileRequest tempRequest = new FileRequest(null, StorageType.LOCAL.name(), null);
        files.forEach(file -> {
            String tempPicId = IDGenerator.nextStr();
            tempRequest.setFolder(DefaultRepositoryDir.getTempFileDir(tempPicId));
            tempRequest.setFileName(file.getOriginalFilename());
            fileCommonService.upload(file, tempRequest);
            tempPicIds.add(tempPicId);
        });
        return tempPicIds;
    }

    /**
     * 处理临时附件: 转存新的临时附件, 移除删除的正式附件
     *
     * @param transferRequest 转存参数
     */
    public void processTemp(UploadTransferRequest transferRequest) {
        if (CollectionUtils.isEmpty(transferRequest.getTempFileIds())) {
            return;
        }
        List<Attachment> attachments = new ArrayList<>();
        LambdaQueryWrapper<Attachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Attachment::getId, transferRequest.getTempFileIds());
        List<Attachment> transferredPics = attachmentMapper.selectListByLambda(queryWrapper);
        List<String> transferredPicIds = transferredPics.stream().map(Attachment::getId).toList();
        transferRequest.getTempFileIds().stream().filter(tempFileId -> !transferredPicIds.contains(tempFileId)).forEach(tempFileId -> {
            // transfer new pic
            FileRequest request = new FileRequest(DefaultRepositoryDir.getTmpDir() + "/" + tempFileId, StorageType.LOCAL.name(), null);
            List<File> folderTempFiles = fileCommonService.getFolderFiles(request);
            if (!CollectionUtils.isEmpty(folderTempFiles)) {
                File tempFile = folderTempFiles.getFirst();
                FileCopyRequest copyRequest = new FileCopyRequest(DefaultRepositoryDir.getTempFileDir(tempFileId),
                        DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), tempFileId),
                        tempFile.getName());
                fileCommonService.copyFile(copyRequest, StorageType.LOCAL.name());
                Attachment attachment = new Attachment();
                attachment.setId(tempFileId);
                attachment.setName(tempFile.getName());
                attachment.setType(tempFile.getName().split("\\.")[1]);
                attachment.setSize(tempFile.length());
                attachment.setStorage(StorageType.LOCAL.name());
                attachment.setOrganizationId(transferRequest.getOrganizationId());
                attachment.setResourceId(transferRequest.getResourceId());
                attachment.setCreateTime(System.currentTimeMillis());
                attachment.setCreateUser(transferRequest.getOperatorUserId());
                attachment.setUpdateTime(System.currentTimeMillis());
                attachment.setUpdateUser(transferRequest.getOperatorUserId());
                attachments.add(attachment);
            }
        });
        // insert pic info
        attachmentMapper.batchInsert(attachments);
        // remove deleted pic
        List<String> removePicIds = transferredPicIds.stream().filter(picId -> !transferRequest.getTempFileIds().contains(picId)).toList();
        if (!CollectionUtils.isEmpty(removePicIds)) {
            LambdaQueryWrapper<Attachment> duplicateQueryWrapper = new LambdaQueryWrapper<>();
            duplicateQueryWrapper.in(Attachment::getId, removePicIds);
            attachmentMapper.deleteByLambda(duplicateQueryWrapper);
            removePicIds.forEach(removeId -> {
                FileRequest request = new FileRequest(DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), removeId), StorageType.LOCAL.name(), null);
                fileCommonService.deleteFolder(request);
            });
        }
    }
}
