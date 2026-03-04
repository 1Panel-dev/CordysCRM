package cn.cordys.crm.order.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.opportunity.dto.request.StageRollBackRequest;
import cn.cordys.crm.order.dto.request.OrderStageAddRequest;
import cn.cordys.crm.order.dto.request.OrderStageUpdateRequest;
import cn.cordys.crm.order.dto.response.OrderStageConfigListResponse;
import cn.cordys.crm.order.service.OrderStageService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单状态流设置")
@RestController
@RequestMapping("/order_stage")
public class OrderStageController {

    @Resource
    private OrderStageService orderStageService;

    @GetMapping("/get")
    @Operation(summary = "订单状态配置列表")
    public OrderStageConfigListResponse getStageConfigList() {
        return orderStageService.getStageConfigList(OrganizationContext.getOrganizationId());
    }


    @PostMapping("/add")
    @Operation(summary = "添加订单状态流")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public String add(@RequestBody OrderStageAddRequest request) {
        return orderStageService.addStageConfig(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "删除订单状态流")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void delete(@PathVariable("id") @Validated String id) {
        orderStageService.delete(id, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update-rollback")
    @Operation(summary = "订单状态流回退设置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void update(@Validated @RequestBody StageRollBackRequest request) {
        orderStageService.updateRollBack(request, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @Operation(summary = "更新订单阶段配置")
    @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
    public void update(@Validated @RequestBody OrderStageUpdateRequest request) {
        orderStageService.update(request, SessionUtils.getUserId());
    }


    @PostMapping("/sort")
    @Operation(summary = "订单阶段排序")
    @RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
    public void sort(@RequestBody List<String> ids) {
        orderStageService.sort(ids, OrganizationContext.getOrganizationId());
    }
}
