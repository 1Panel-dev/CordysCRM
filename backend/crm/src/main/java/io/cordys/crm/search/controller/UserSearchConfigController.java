package io.cordys.crm.search.controller;


import io.cordys.context.OrganizationContext;
import io.cordys.crm.search.request.UserSearchConfigAddRequest;
import io.cordys.crm.search.service.UserSearchConfigService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/search/config")
@Tag(name = "用户全局搜索字段配置")
public class UserSearchConfigController {

    @Resource
    private UserSearchConfigService userSearchConfigService;


    @PostMapping("/save")
    @Operation(summary = "保存配置")
    public void save(@Validated @RequestBody UserSearchConfigAddRequest request) {
        userSearchConfigService.save(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/reset")
    @Operation(summary = "重置")
    public void reset() {
        userSearchConfigService.reset(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

}
