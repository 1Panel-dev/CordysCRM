package io.cordys.crm.opportunity.controller;


import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.request.PosRequest;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.constants.UserViewResourceType;
import io.cordys.crm.system.domain.UserView;
import io.cordys.crm.system.dto.request.UserViewAddRequest;
import io.cordys.crm.system.dto.request.UserViewUpdateRequest;
import io.cordys.crm.system.dto.response.UserViewListResponse;
import io.cordys.crm.system.dto.response.UserViewResponse;
import io.cordys.crm.system.service.UserViewService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商机视图")
@RestController
@RequestMapping("/opportunity/view")
public class OpportunityUserViewController {

    @Resource
    private UserViewService userViewService;


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "添加商机视图")
    public UserView add(@Validated @RequestBody UserViewAddRequest request) {
        return userViewService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), UserViewResourceType.OPPORTUNITY.name());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "编辑商机视图")
    public UserView update(@Validated @RequestBody UserViewUpdateRequest request) {
        return userViewService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "删除商机视图")
    public void delete(@PathVariable String id) {
        userViewService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/detail/{id}")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机视图详情")
    public UserViewResponse viewDetail(@PathVariable String id) {
        return userViewService.getViewDetail(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/list/{isOption}")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机视图列表")
    public List<UserViewListResponse> queryList(@PathVariable boolean isOption) {
        return userViewService.list(UserViewResourceType.OPPORTUNITY.name(), SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), isOption);
    }


    @GetMapping("/fixed/{id}")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机视图固定/取消固定")
    public void fixed(@PathVariable String id) {
        userViewService.fixed(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/edit/pos")
    @Operation(summary = "商机视图-拖拽排序")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    public void editPos(@Validated @RequestBody PosRequest request) {
        userViewService.editPos(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


}
