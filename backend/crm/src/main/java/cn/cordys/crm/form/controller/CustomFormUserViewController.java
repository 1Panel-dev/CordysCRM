package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.system.domain.UserView;
import cn.cordys.crm.system.dto.request.UserViewAddRequest;
import cn.cordys.crm.system.dto.request.UserViewUpdateRequest;
import cn.cordys.crm.system.dto.response.UserViewListResponse;
import cn.cordys.crm.system.dto.response.UserViewResponse;
import cn.cordys.crm.system.service.UserViewService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 自定义表单的用户视图；resource_type 直接存放自定义表单 ID。 */
@Tag(name = "自定义表单视图")
@RestController
@RequestMapping("/custom-form/view/{customFormId}")
public class CustomFormUserViewController {

    @Resource
    private UserViewService userViewService;
    @Resource
    private CustomFormDataService customFormDataService;

    @PostMapping("/add")
    @Operation(summary = "添加自定义表单视图")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public UserView add(@PathVariable String customFormId, @Validated @RequestBody UserViewAddRequest request) {
        customFormDataService.checkViewPermission(customFormId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        return userViewService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                customFormId);
    }

    @PostMapping("/update")
    @Operation(summary = "编辑自定义表单视图")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public UserView update(@PathVariable String customFormId, @Validated @RequestBody UserViewUpdateRequest request) {
        checkView(customFormId, request.getId());
        return userViewService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除自定义表单视图")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void delete(@PathVariable String customFormId, @PathVariable String id) {
        checkView(customFormId, id);
        userViewService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "自定义表单视图详情")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public UserViewResponse detail(@PathVariable String customFormId, @PathVariable String id) {
        checkView(customFormId, id);
        return userViewService.getViewDetail(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                customFormId);
    }

    @GetMapping("/list")
    @Operation(summary = "自定义表单视图列表")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public List<UserViewListResponse> list(@PathVariable String customFormId) {
        customFormDataService.checkViewPermission(customFormId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        return userViewService.list(customFormId, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/fixed/{id}")
    @Operation(summary = "自定义表单视图固定/取消固定")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void fixed(@PathVariable String customFormId, @PathVariable String id) {
        checkView(customFormId, id);
        userViewService.fixed(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/edit/pos")
    @Operation(summary = "自定义表单视图拖拽排序")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void editPos(@PathVariable String customFormId, @Validated @RequestBody PosRequest request) {
        checkView(customFormId, request.getMoveId());
        checkView(customFormId, request.getTargetId());
        userViewService.editPos(request, SessionUtils.getUserId(), customFormId);
    }

    @GetMapping("/enable/{id}")
    @Operation(summary = "自定义表单视图启用/禁用")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void enable(@PathVariable String customFormId, @PathVariable String id) {
        checkView(customFormId, id);
        userViewService.enable(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    private void checkView(String customFormId, String viewId) {
        userViewService.checkViewResource(viewId, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                customFormId);
    }
}
