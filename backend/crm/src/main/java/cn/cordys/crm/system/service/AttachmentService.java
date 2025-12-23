package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */
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
     * 删除附件
     *
     * @param attachmentId 附件ID
     */
    public void delete(String attachmentId) {
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        if (attachment == null) {
            // 删除临时文件目录
            fileCommonService.deleteFolder(new FileRequest(DefaultRepositoryDir.getTempFileDir(attachmentId), StorageType.LOCAL.name(), null), true);
        } else {
            // 删除正式文件
            fileCommonService.deleteFolder(new FileRequest(DefaultRepositoryDir.getTransferFileDir(attachment.getOrganizationId(), attachment.getResourceId(), attachment.getId()),
                    StorageType.LOCAL.name(), attachment.getName()), false);
        }
    }

    /**
     * 获取文件流
     *
     * @param attachmentId 附件ID
     *
     * @return 文件流
     */
    public ResponseEntity<org.springframework.core.io.Resource> getResource(String attachmentId) {
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        FileRequest request;
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        try {
            InputStream fileStream;
            if (attachment == null) {
                // get pic from temp dir
                request = new FileRequest(DefaultRepositoryDir.getTempFileDir(attachmentId), StorageType.LOCAL.name(), null);
                List<File> folderFiles = fileCommonService.getFolderFiles(request);
                if (CollectionUtils.isEmpty(folderFiles)) {
                    return null;
                }
                File file = folderFiles.getFirst();
                fileStream = new FileInputStream(file);
                responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodeName(file.getName()))
                        .contentLength(file.length())
                        .contentType(isSvg(file.getName()) ? MediaType.parseMediaType("image/svg+xml") : MediaType.parseMediaType("application/octet-stream"));
            } else {
                // get attachment from transferred dir
                request = new FileRequest(DefaultRepositoryDir.getTransferFileDir(attachment.getOrganizationId(), attachment.getResourceId(), attachment.getId()), StorageType.LOCAL.name(), attachment.getName());
                fileStream = fileCommonService.getFileInputStream(request);
                if (fileStream == null) {
                    throw new GenericException("The file does not exist or has been deleted");
                }
                responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodeName(attachment.getName()))
                        .contentLength(attachment.getSize())
                        .contentType(isSvg(attachment.getName()) ? MediaType.parseMediaType("image/svg+xml") : MediaType.parseMediaType("application/octet-stream"));
            }
            return responseBuilder
                    .body(new InputStreamResource(fileStream));
        } catch (Exception e) {
            LogUtils.error(e.getMessage());
            return null;
        }
    }


    /**
     * 判断是否svg文件
     *
     * @param fileName 文件名
     *
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
        List<Attachment> attachments = new ArrayList<>();
        LambdaQueryWrapper<Attachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attachment::getResourceId, transferRequest.getResourceId());
        List<Attachment> transferredAttachments = attachmentMapper.selectListByLambda(queryWrapper);
        List<String> transferredIds = transferredAttachments.stream().map(Attachment::getId).toList();
        // insert new attachment
        transferRequest.getTempFileIds().stream().filter(tempFileId -> !transferredIds.contains(tempFileId)).forEach(tempFileId -> {
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
            }
        });
        attachmentMapper.batchInsert(attachments);
        // remove deleted
        List<String> removedIds = transferredIds.stream().filter(aId -> !transferRequest.getTempFileIds().contains(aId)).toList();
        if (!CollectionUtils.isEmpty(removedIds)) {
            LambdaQueryWrapper<Attachment> duplicateQueryWrapper = new LambdaQueryWrapper<>();
            duplicateQueryWrapper.in(Attachment::getId, removedIds);
            attachmentMapper.deleteByLambda(duplicateQueryWrapper);
            removedIds.forEach(removeId -> {
                FileRequest request = new FileRequest(DefaultRepositoryDir.getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), removeId), StorageType.LOCAL.name(), null);
                fileCommonService.deleteFolder(request, true);
            });
        }
    }

	/**
	 * 通过ID映射批量复制附件
	 * @param oldOfNewIdMap {旧的附件ID: 新的附件ID} 集合
	 * @param targetId 目标资源ID
	 * @param currentUser 当前操作用户
	 */
	public void batchCopyOfIdMap(Map<String, String> oldOfNewIdMap, String targetId, String currentUser) {
		LambdaQueryWrapper<Attachment> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.in(Attachment::getId, oldOfNewIdMap.keySet().stream().toList());
		List<Attachment> attachments = attachmentMapper.selectListByLambda(queryWrapper);
		Map<String, Attachment> attachmentMap = attachments.stream().collect(Collectors.toMap(Attachment::getId, Function.identity()));
		List<Attachment> newAttachments = new ArrayList<>();
		oldOfNewIdMap.forEach((oId, nId) -> {
			if (!attachmentMap.containsKey(oId)) {
				return;
			}
			Attachment oldAttachment = attachmentMap.get(oId);
			// 复制文件
			FileCopyRequest copyRequest = new FileCopyRequest(
					DefaultRepositoryDir.getTransferFileDir(oldAttachment.getOrganizationId(), oldAttachment.getResourceId(), oId),
					DefaultRepositoryDir.getTransferFileDir(oldAttachment.getOrganizationId(), targetId, nId),
					oldAttachment.getName());
			Thread.startVirtualThread(() -> {
				fileCommonService.copyFile(copyRequest, StorageType.LOCAL.name());
			});
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
	 * @param fileName 文件名
	 * @return 编码后文件名
	 */
	private String encodeName(String fileName) {
		return URLEncoder.encode(fileName, StandardCharsets.UTF_8)
				.replaceAll("\\+", "%20");
	}
}
