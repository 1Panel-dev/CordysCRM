package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.security.ApiKeyHandler;
import cn.cordys.crm.system.domain.UserKey;
import cn.cordys.crm.system.dto.request.UserKeyUpdateRequest;
import cn.cordys.crm.system.service.UserKeyService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletRequest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/api/key")
@Tag(name = "个人中心-APIKEY")
public class UserKeyController {

    @Resource
    private UserKeyService userKeyService;


    @GetMapping("/add")
    @RequiresPermissions(PermissionConstants.PERSONAL_API_KEY_ADD)
    @Operation(summary = "个人中心-APIKEY-新增")
    public void add() {
        userKeyService.addUserKey(SessionUtils.getUserId());
    }


    @GetMapping("/list")
    @RequiresPermissions(PermissionConstants.PERSONAL_API_KEY_READ)
    @Operation(summary = "个人中心-APIKEY-列表")
    public List<UserKey> getUserKeysInfo() {
        return userKeyService.getUserKeysInfo(SessionUtils.getUserId());
    }


    @PostMapping("/update")
    @Operation(summary = "个人中心-APIKEY-更新")
    @RequiresPermissions(PermissionConstants.PERSONAL_API_KEY_UPDATE)
    public void update(@Validated @RequestBody UserKeyUpdateRequest request) {
        userKeyService.updateUserKey(request);
    }


    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.PERSONAL_API_KEY_DELETE)
    @Operation(summary = "个人中心-APIKEY-删除")
    public void delete(@PathVariable String id) {
        userKeyService.deleteUserKey(id);
    }

    @GetMapping("/enable/{id}")
    @Operation(summary = "个人中心-APIKEY-启用")
    @RequiresPermissions(PermissionConstants.PERSONAL_API_KEY_UPDATE)
    public void enable(@PathVariable String id) {
        userKeyService.enableUserKey(id);
    }

    @GetMapping("/disable/{id}")
    @Operation(summary = "个人中心-APIKEY-启用")
    @RequiresPermissions(PermissionConstants.PERSONAL_API_KEY_UPDATE)
    public void disable(@PathVariable String id) {
        userKeyService.disableUserKey(id);
    }

    @GetMapping("/validate")
    @Operation(summary = "个人中心-验证Api Keys")
    public String validate(ServletRequest request) {
        return ApiKeyHandler.getUser(WebUtils.toHttp(request));
    }
}

