package io.cordys.crm.search.controller;


import io.cordys.context.OrganizationContext;
import io.cordys.crm.search.request.UserSearchConfigAddRequest;
import io.cordys.crm.search.service.UserSearchConfigService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/search/config")
@Tag(name = "用户全局搜索字段配置")
public class UserSearchConfigController {

    @Resource
    private UserSearchConfigService userSearchConfigService;


    @PostMapping("/add")
    @Operation(summary = "添加配置")
    public void add(@Validated @RequestBody UserSearchConfigAddRequest request) {
        userSearchConfigService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


}
