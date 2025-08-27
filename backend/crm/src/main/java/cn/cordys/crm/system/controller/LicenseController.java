package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.system.dto.LicenseDTO;
import cn.cordys.crm.system.service.LicenseService;
import cn.cordys.security.SessionUtils;
import com.fasterxml.jackson.databind.node.TextNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/license")
@Tag(name = "系统设置-授权管理")
public class LicenseController {

    @Resource
    private LicenseService licenseService;

    @GetMapping("/validate")
    @Operation(summary = "License 校验")
    public LicenseDTO validate() {
        return licenseService.validate();
    }

    @PostMapping("/add")
    @Operation(summary = "添加License")
    @RequiresPermissions(PermissionConstants.LICENSE_EDIT)
    public LicenseDTO addLicense(@RequestBody TextNode licenseCode) {
        return licenseService.add(licenseCode.asText(), SessionUtils.getUserId());
    }
}
