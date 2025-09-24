package cn.cordys.crm.system.controller;

import cn.cordys.crm.system.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/attachment")
@Tag(name = "附件管理")
public class AttachmentController {


    @Resource
    private AttachmentService attachmentService;

    @PostMapping("/upload/temp")
    @Operation(summary = "上传临时附件")
    public List<String> uploadTmp(@RequestParam("files") List<MultipartFile> files) {
        return attachmentService.uploadTemp(files);
    }
}
