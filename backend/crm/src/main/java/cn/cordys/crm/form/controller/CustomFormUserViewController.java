package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.dto.request.CustomFormPosRequest;
import cn.cordys.crm.form.dto.request.CustomFormUserViewAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormUserViewUpdateRequest;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.system.domain.UserView;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "自定义表单视图")
@RestController
@RequestMapping("/custom-form/view")
public class CustomFormUserViewController {

    @Resource
    private UserViewService userViewService;
    @Resource
    private CustomFormDataService customFormDataService;

    @PostMapping("/add")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "添加自定义表单视图")
    public UserView add(@Validated @RequestBody CustomFormUserViewAddRequest request) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        customFormDataService.getDataScope(request.getCustomFormId(), userId, orgId);
        return userViewService.add(request, userId, orgId, request.getCustomFormId());
    }

    @PostMapping("/update")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "编辑自定义表单视图")
    public UserView update(@Validated @RequestBody CustomFormUserViewUpdateRequest request) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        userViewService.checkView(userId, request.getId(), orgId, request.getCustomFormId());
        return userViewService.update(request, userId, orgId);
    }

    @GetMapping("/delete/{id}")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "删除自定义表单视图")
    public void delete(@PathVariable String id, @RequestParam String customFormId) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        userViewService.checkView(userId, id, orgId, customFormId);
        userViewService.delete(id, userId, orgId);
    }

    @GetMapping("/detail/{id}")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "自定义表单视图详情")
    public UserViewResponse viewDetail(@PathVariable String id, @RequestParam String customFormId) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        userViewService.checkView(userId, id, orgId, customFormId);
        return userViewService.getViewDetail(id, userId, orgId, customFormId);
    }

    @GetMapping("/list")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "自定义表单视图列表")
    public List<UserViewListResponse> queryList(@RequestParam String customFormId) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        customFormDataService.getDataScope(customFormId, userId, orgId);
        return userViewService.list(customFormId, userId, orgId);
    }

    @GetMapping("/fixed/{id}")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "自定义表单视图固定/取消固定")
    public void fixed(@PathVariable String id, @RequestParam String customFormId) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        userViewService.checkView(userId, id, orgId, customFormId);
        userViewService.fixed(id, userId, orgId);
    }

    @PostMapping("/edit/pos")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "自定义表单视图-拖拽排序")
    public void editPos(@Validated @RequestBody CustomFormPosRequest request) {
        String userId = SessionUtils.getUserId();
        userViewService.checkView(userId, request.getMoveId(), OrganizationContext.getOrganizationId(),
                request.getCustomFormId());
        userViewService.editPos(request, userId, request.getCustomFormId());
    }

    @GetMapping("/enable/{id}")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "自定义表单视图-启用/禁用")
    public void enable(@PathVariable String id, @RequestParam String customFormId) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        userViewService.checkView(userId, id, orgId, customFormId);
        userViewService.enable(id, userId, orgId);
    }
}
