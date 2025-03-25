package io.cordys.crm.system.controller;

import io.cordys.crm.system.service.PicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author song-cc-rock
 */
@RestController
@RequestMapping("/pic")
@Tag(name = "图片管理")
public class PicController {

	@Resource
	private PicService picService;

	@PostMapping("/upload/temp")
	@Operation(summary = "上传临时图片")
	public List<String> uploadTmpPic(@RequestParam("files") List<MultipartFile> files) {
		return picService.uploadTempPic(files);
	}

	@GetMapping("/preview/{id}")
	@Operation(summary = "预览图片")
	public ResponseEntity<org.springframework.core.io.Resource> preview(@PathVariable String id) {
		return picService.getPicBytes(id);
	}

	@GetMapping("/download/{id}")
	@Operation(summary = "下载图片")
	public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable String id) {
		return picService.getPicBytes(id);
	}

	@GetMapping("/delete/{id}")
	@Operation(summary = "删除图片")
	public void delete(@PathVariable String id) {
		picService.deletePic(id);
	}
}
