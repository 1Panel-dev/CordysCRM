package cn.cordys.crm.customer.controller;


import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.UserViewResourceType;
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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "联系人视图")
@RestController
@RequestMapping("/customer/contact/view")
public class CustomerContactUserViewController {

    @Resource
    private UserViewService userViewService;


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "添加联系人视图")
    public UserView add(@Validated @RequestBody UserViewAddRequest request) {
        return userViewService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), UserViewResourceType.CUSTOMER_CONTACT.name());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "编辑联系人视图")
    public UserView update(@Validated @RequestBody UserViewUpdateRequest request) {
        return userViewService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "删除联系人视图")
    public void delete(@PathVariable String id) {
        userViewService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/detail/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "联系人视图详情")
    public UserViewResponse viewDetail(@PathVariable String id) {
        return userViewService.getViewDetail(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), FormKey.CONTACT.getKey());
    }


    @GetMapping("/list")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "联系人视图列表")
    public List<UserViewListResponse> queryList() {
        return userViewService.list(UserViewResourceType.CUSTOMER_CONTACT.name(), SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/fixed/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "联系人视图固定/取消固定")
    public void fixed(@PathVariable String id) {
        userViewService.fixed(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/edit/pos")
    @Operation(summary = "联系人视图-拖拽排序")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    public void editPos(@Validated @RequestBody PosRequest request) {
        userViewService.editPos(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), UserViewResourceType.CUSTOMER_CONTACT.name());
    }


    @GetMapping("/enable/{id}")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
    @Operation(summary = "联系人视图-启用/禁用")
    public void enable(@PathVariable String id) {
        userViewService.enable(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

}
