package io.cordys.crm.system.service;

import io.cordys.common.util.LogUtils;
import io.cordys.file.engine.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @author song-cc-rock
 */
@Service
public class FileCommonService {

	/**
	 * 上传文件
	 * @param file 文件
	 * @param request 请求参数
	 */
	public void upload(MultipartFile file, FileRequest request) {
		try {
			FileCenter.getRepository(request.getStorage()).saveFile(file, request);
		} catch (Exception e) {
			LogUtils.error(e);
		}
	}

	/**
	 * 获取目录下的所有文件
	 * @param request 文件请求参数
	 * @return 文件列表
	 */
	public List<File> getFolderFiles(FileRequest request) {
		try {
			return FileCenter.getRepository(request.getStorage()).getFolderFiles(request);
		} catch (Exception e) {
			LogUtils.error(e);
			return null;
		}
	}

	/**
	 * 复制文件
	 * @param request 复制文件参数
	 * @param storage 存储类型
	 */
	public void copyFile(FileCopyRequest request, String storage) {
		try {
			FileCenter.getRepository(storage).copyFile(request);
		} catch (Exception e) {
			LogUtils.error(e);
		}
	}

	/**
	 * 获取文件输入流
	 * @param request 文件请求参数
	 * @return 文件输入流
	 */
	public InputStream getFileInputStream(FileRequest request) {
		try {
			return FileCenter.getRepository(request.getStorage()).getFileAsStream(request);
		} catch (Exception e) {
			LogUtils.error(e);
			return null;
		}
	}

	/**
	 * 删除目录
	 * @param request 文件请求参数
	 */
	public void deleteFolder(FileRequest request) {
		try {
			FileCenter.getRepository(request.getStorage()).deleteFolder(request);
		} catch (Exception e) {
			LogUtils.error(e);
		}
	}
}
