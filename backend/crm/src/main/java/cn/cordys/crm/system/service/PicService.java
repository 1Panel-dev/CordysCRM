package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.file.engine.FileRequest;
import cn.cordys.file.engine.StorageType;
import cn.cordys.mybatis.BaseMapper;
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
    @Resource
    private AttachmentService attachmentService;


    /**
     * 上传临时图片
     *
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
        return attachmentService.uploadTemp(pics);
    }


    /**
     * 获取图片流
     *
     * @param picId 图片ID
     *
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
                request = new FileRequest(DefaultRepositoryDir.getTempFileDir(picId), StorageType.LOCAL.name(), null);
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
                request = new FileRequest(DefaultRepositoryDir.getTransferFileDir(attachment.getOrganizationId(), attachment.getResourceId(), attachment.getId()), StorageType.LOCAL.name(), attachment.getName());
                picStream = fileCommonService.getFileInputStream(request);
                if (picStream == null) {
                    throw new GenericException("The picture does not exist or has been deleted");
                }
                responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getName() + "\"")
                        .contentLength(attachment.getSize())
                        .contentType(isSvg(attachment.getName()) ? MediaType.parseMediaType("image/svg+xml") : MediaType.parseMediaType("application/octet-stream"));
            }
            return responseBuilder
                    .body(new InputStreamResource(picStream));
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
}
