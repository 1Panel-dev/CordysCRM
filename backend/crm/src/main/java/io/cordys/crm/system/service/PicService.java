package io.cordys.crm.system.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.domain.Attachment;
import io.cordys.crm.system.dto.request.UploadTransferRequest;
import io.cordys.file.engine.DefaultRepositoryDir;
import io.cordys.file.engine.FileCopyRequest;
import io.cordys.file.engine.FileRequest;
import io.cordys.file.engine.StorageType;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @author song-cc-rock
 */
@Service
public class PicService {

	@Resource
	private BaseMapper<Attachment> attachmentMapper;

	@Resource
	private FileCommonService fileCommonService;

	/**
	 * 上传临时图片
	 * @param pics 上传图片集合
	 */
	public List<String> uploadTempPic(List<MultipartFile> pics) {
		pics.forEach(pic -> {
			String filename = pic.getOriginalFilename();
			if (StringUtils.isBlank(filename)) {
				throw new GenericException("图片类型不支持!");
			}
			if (!filename.endsWith(".jpg") && !filename.endsWith(".png") && !filename.endsWith(".jpeg") && !filename.endsWith(".gif")
					&& !filename.endsWith(".bmp") && !filename.endsWith(".svg") && !filename.endsWith(".webp")) {
				throw new GenericException("图片类型不支持!");
			}
		});
		List<String> tempPicIds = new ArrayList<>();
		FileRequest tempRequest = new FileRequest(null, StorageType.LOCAL.name(), null);
		pics.forEach(pic -> {
			String tempPicId = IDGenerator.nextStr();
			tempRequest.setFolder(getTempFileDir(tempPicId));
			tempRequest.setFileName(pic.getOriginalFilename());
			fileCommonService.upload(pic, tempRequest);
			tempPicIds.add(tempPicId);
		});
		return tempPicIds;
	}

	/**
	 * 处理图片: 转存新的临时图片, 移除删除的正式图片
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
				FileCopyRequest copyRequest = new FileCopyRequest(getTempFileDir(tempFileId),
						getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), tempFileId),
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
				FileRequest request = new FileRequest(getTransferFileDir(transferRequest.getOrganizationId(), transferRequest.getResourceId(), removeId), StorageType.LOCAL.name(), null);
				fileCommonService.deleteFolder(request);
			});
		}
	}

	/**
	 * 获取图片流
	 * @param picId 图片ID
	 * @return 图片流
	 */
	public ResponseEntity<org.springframework.core.io.Resource> getPicResource(String picId) {
		Attachment attachment = attachmentMapper.selectByPrimaryKey(picId);
		FileRequest request;
		ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
		try {
			InputStream picStream;
			if (attachment == null) {
				// get pic from temp dir
				request = new FileRequest(getTempFileDir(picId), StorageType.LOCAL.name(), null);
				List<File> folderFiles = fileCommonService.getFolderFiles(request);
				if (CollectionUtils.isEmpty(folderFiles)) {
					return null;
				}
				File picFile = folderFiles.getFirst();
				picStream = new FileInputStream(picFile);
				responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + picFile.getName() + "\"")
						.contentLength(picFile.length())
						.contentType(isSvg(picFile.getName()) ? MediaType.parseMediaType("image/svg+xml") : MediaType.parseMediaType("application/octet-stream"));
			} else {
				// get pic from transferred dir
				request = new FileRequest(getTransferFileDir(attachment.getOrganizationId(), attachment.getResourceId(), attachment.getId()), StorageType.LOCAL.name(), attachment.getName());
				picStream = fileCommonService.getFileInputStream(request);
				responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getName() + "\"")
						.contentLength(attachment.getSize())
						.contentType(isSvg(attachment.getName()) ? MediaType.parseMediaType("image/svg+xml") : MediaType.parseMediaType("application/octet-stream"));
			}
			return responseBuilder
					.body(new InputStreamResource(picStream));
		} catch (Exception e) {
			LogUtils.error(e);
			return null;
		}
	}

	/**
	 * 获取临时文件目录
	 * @param tempFileId 临时文件ID
	 * @return 临时文件目录
	 */
	private String getTempFileDir(String tempFileId) {
		return DefaultRepositoryDir.getTmpDir() + "/" + tempFileId;
	}

	/**
	 * 获取转存文件目录
	 * @param organizationId 组织ID
	 * @param resourceId 资源ID
	 * @param fileId 文件ID
	 * @return 转存文件目录
	 */
	private String getTransferFileDir(String organizationId, String resourceId, String fileId) {
		return "/" + organizationId + "/" + resourceId + "/" + fileId;
	}

	/**
	 * 判断是否svg文件
	 * @param fileName 文件名
	 * @return 是否svg
	 */
	private boolean isSvg(String fileName) {
		return fileName.endsWith(".svg") || fileName.endsWith(".SVG");
	}
}
