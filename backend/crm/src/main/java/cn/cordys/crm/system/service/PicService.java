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
     * 获取图片文件流
     * @param picId 图片ID
     * @return 文件流
     */
    public ResponseEntity<org.springframework.core.io.Resource> getResource(String picId) {
        return attachmentService.getResource(picId);
    }
}
