package cn.cordys.crm.system.service;

import cn.cordys.common.util.LogUtils;
import cn.cordys.file.engine.FileCenter;
import cn.cordys.file.engine.FileCopyRequest;
import cn.cordys.file.engine.FileRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @author song-cc-rock
 */
@Service
public class FileCommonService {

    /**
     * 清除临时文件资源
     *
     * @param request 文件请求参数
     */
    public void cleanTempResource(FileRequest request) {
        List<File> folderFiles = getFolderFiles(request);
        if (CollectionUtils.isEmpty(folderFiles)) {
            return;
        }
        folderFiles.forEach(File::deleteOnExit);
    }

    /**
     * 上传文件
     *
     * @param file    文件
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
     * 上传文件
     *
     * @param file
     * @param request
     */
    public void upload(File file, FileRequest request) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            FileCenter.getRepository(request.getStorage()).saveFile(inputStream, request);
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    /**
     * 获取目录下的所有文件
     *
     * @param request 文件请求参数
     *
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
     *
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
     *
     * @param request 文件请求参数
     *
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
     *
     * @param request 文件请求参数
     */
    public void deleteFolder(FileRequest request) {
        try {
            FileCenter.getRepository(request.getStorage()).deleteFolder(request);
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    /**
     * 下载文件
     *
     * @param request 文件请求信息，包含待下载的文件标识符或路径。
     *
     * @throws Exception 如果下载文件过程中发生错误，抛出异常。
     */
    public ResponseEntity<Resource> download(FileRequest request) throws Exception {
        return FileCenter.getRepository(request.getStorage()).downloadFile(request);
    }


}
