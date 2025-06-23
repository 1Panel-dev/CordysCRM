package io.cordys.crm.system.controller;

import com.fasterxml.jackson.databind.node.TextNode;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.system.dto.LicenseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/license")
@Tag(name = "系统设置-授权管理")
public class LicenseController {

    @GetMapping("/validate")
    @Operation(summary = "License 校验")
    public LicenseDTO validate() {
        // TODO 后续: 实现 License 校验逻辑
        boolean isValid = CommonBeanFactory.packageExists("io.cordys.xpack");
        return LicenseDTO.builder()
                .status(isValid ? "valid" : "invalid")
                .build();
    }

    @PostMapping("/add")
    @Operation(summary = "添加License")
    public LicenseDTO addLicense(@RequestBody TextNode licenseCode) {

        return LicenseDTO.builder().build();
    }
}
