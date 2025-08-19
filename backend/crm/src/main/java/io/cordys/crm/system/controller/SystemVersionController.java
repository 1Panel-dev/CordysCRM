package io.cordys.crm.system.controller;

import io.cordys.crm.system.dto.VersionInfoDTO;
import io.cordys.crm.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统版本")
@RestController
public class SystemVersionController {

    @Resource
    private SystemService systemService;

    @GetMapping("/system/version")
    @Operation(summary = "获取当前系统版本")
    public VersionInfoDTO getVersion() {
        return systemService.getVersion();
    }
}