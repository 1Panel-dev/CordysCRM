package io.cordys.crm.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统版本")
@RequestMapping("/system/version")
@RestController
public class SystemVersionController {

    @GetMapping("/current")
    @Operation(summary = "获取当前系统版本")
    public String getVersion() {
        return System.getenv("CRM_VERSION");
    }
}