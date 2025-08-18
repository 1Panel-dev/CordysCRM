package io.cordys.crm.system.controller;

import io.cordys.crm.system.dto.VersionInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "系统版本")
@RestController
public class SystemVersionController {

    @GetMapping("/system/version")
    @Operation(summary = "获取当前系统版本")
    public VersionInfoDTO getVersion() {
        return VersionInfoDTO.builder()
                .currentVersion(System.getenv("CRM_VERSION"))
                .latestVersion(System.getenv("CRM_VERSION"))
                .releaseDate(String.valueOf(LocalDateTime.now()))
                .copyright("Copyright © 2014-2025 飞致云")
                .architecture("amd64").build();
    }
}