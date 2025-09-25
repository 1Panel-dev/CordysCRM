package cn.cordys.crm.system.controller;

import cn.cordys.crm.system.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/preview/{id}")
    @Operation(summary = "预览附件")
    public ResponseEntity<org.springframework.core.io.Resource> preview(@PathVariable String id) {
        return attachmentService.getResource(id);
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "下载附件")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable String id) {
        return attachmentService.getResource(id);
    }
}
