package io.cordys.crm.integration.auth.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.request.AuthSourceRequest;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.security.SessionUtils;
import io.cordys.crm.integration.auth.request.AuthSourceEditRequest;
import io.cordys.crm.integration.auth.response.AuthSourceDTO;
import io.cordys.crm.integration.auth.service.AuthSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "企业设置")
@RestController
@RequestMapping("/system/authsource")
public class AuthSourceController {
    @Resource
    private AuthSourceService authSourceService;

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    @PostMapping("/list")
    @Operation(summary = "企业设置-认证设置-列表查询")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public Pager<List<OrganizationConfigDetail>> list(@Validated @RequestBody AuthSourceRequest request) {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(OrganizationContext.getOrganizationId(), OrganizationConfigConstants.ConfigType.AUTH.name());
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        if (organizationConfig == null) {
            return PageUtils.setPageInfo(page, new ArrayList<>());
        } else {
            request.setConfigId(organizationConfig.getId());
        }
        return PageUtils.setPageInfo(page, authSourceService.list(request));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "企业设置-认证设置-详细信息")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ)
    public AuthSourceDTO get(@PathVariable(value = "id") String id) {
        return authSourceService.getAuthSource(id);
    }

    @GetMapping("/get/enable/detail/by/type/{type}")
    @Operation(summary = "系统设置-系统-系统参数-认证设置-详细信息")
    public AuthSourceDTO getByType(@PathVariable(value = "type") String type) {
        return authSourceService.getAuthSourceByType(OrganizationContext.getOrganizationId(), type);
    }

    @PostMapping("/add")
    @Operation(summary = "企业设置-认证设置-新增")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_ADD)
    public void add(@Validated @RequestBody AuthSourceEditRequest authSource) {
        authSourceService.addAuthSource(authSource, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @Operation(summary = "企业设置-认证设置-更新")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void update(@Validated @RequestBody AuthSourceEditRequest authSource) {
        authSourceService.updateAuthSource(authSource, SessionUtils.getUserId());
    }

    @GetMapping("/update/status/{id}")
    @Operation(summary = "企业设置-认证设置-更新状态")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void updateStatus(@Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
                             @PathVariable String id,
                             @Schema(description = "更新状态", requiredMode = Schema.RequiredMode.REQUIRED)
                             @RequestParam(value = "enable", required = false) Boolean enable) {
        authSourceService.updateStatus(id, enable);
    }


    @GetMapping("/update/name/{id}")
    @Operation(summary = "企业设置-认证设置-更新名称")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_UPDATE)
    public void updateName(@Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
                           @PathVariable String id,
                           @Schema(description = "更新状态", requiredMode = Schema.RequiredMode.REQUIRED)
                           @RequestParam(value = "name", required = false) String name) {
        authSourceService.updateName(id, name, SessionUtils.getUserId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "企业设置-认证设置-删除")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_DELETE)
    public void delete(@PathVariable(value = "id") String id) {
        authSourceService.deleteAuthSource(id);
    }


    @GetMapping("/enable/type/list")
    @Operation(summary = "获取开启的认证类型列表(登录类型展示)")
    public List<String> getEnableAuthSourceTypeList() {
        return authSourceService.getEnableAuthSourceTypeList(OrganizationContext.getOrganizationId());
    }


}
