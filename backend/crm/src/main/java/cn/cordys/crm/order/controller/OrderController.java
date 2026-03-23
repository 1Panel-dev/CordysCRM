package cn.cordys.crm.order.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.opportunity.dto.request.OpportunitySearchStatisticRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunitySearchStatisticResponse;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.order.dto.request.OrderAddRequest;
import cn.cordys.crm.order.dto.request.OrderPageRequest;
import cn.cordys.crm.order.dto.request.OrderStageRequest;
import cn.cordys.crm.order.dto.request.OrderUpdateRequest;
import cn.cordys.crm.order.dto.response.OrderGetResponse;
import cn.cordys.crm.order.dto.response.OrderListResponse;
import cn.cordys.crm.order.dto.response.OrderStatisticResponse;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "订单")
@RestController
@RequestMapping("/order")
public class OrderController {
    @Resource
    private OrderService orderService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.ORDER_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.ORDER.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.ORDER_ADD)
    @Operation(summary = "创建")
    public Order add(@Validated @RequestBody OrderAddRequest request) {
        return orderService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.ORDER_UPDATE)
    @Operation(summary = "更新")
    public Order update(@Validated @RequestBody OrderUpdateRequest request) {
        return orderService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update/stage")
    @RequiresPermissions(PermissionConstants.ORDER_UPDATE)
    @Operation(summary = "更新订单阶段")
    public void updateStage(@Validated @RequestBody OrderStageRequest request) {
        orderService.updateStage(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.ORDER_DELETE)
    @Operation(summary = "删除")
    public void delete(@PathVariable("id") String id) {
        orderService.delete(id);
    }

    @GetMapping("/get/snapshot/{id}")
    @RequiresPermissions(PermissionConstants.ORDER_READ)
    @Operation(summary = "获取详情快照")
    public OrderGetResponse getSnapshot(@PathVariable("id") String id) {
        return orderService.getSnapshotWithDataPermissionCheck(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.ORDER_READ)
    @Operation(summary = "详情")
    public OrderGetResponse get(@PathVariable("id") String id) {
        return orderService.getWithDataPermissionCheck(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/module/form/snapshot/{id}")
    @RequiresPermissions(PermissionConstants.ORDER_READ)
    @Operation(summary = "获取表单快照配置")
    public ModuleFormConfigDTO getFormSnapshot(@PathVariable("id") String id) {
        return orderService.getFormSnapshot(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.ORDER_READ)
    @Operation(summary = "列表")
    public PagerWithOption<List<OrderListResponse>> list(@Validated @RequestBody OrderPageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.ORDER_READ);
        return orderService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, false);
    }

    @GetMapping("/tab")
    @RequiresPermissions(PermissionConstants.ORDER_READ)
    @Operation(summary = "tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return orderService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/download/{id}")
    @RequiresPermissions(PermissionConstants.ORDER_DOWNLOAD)
    @Operation(summary = "下载订单日志记录")
    public void download(@PathVariable("id") String id) {
        orderService.download(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/statistic")
    @RequiresPermissions(PermissionConstants.ORDER_READ)
    @Operation(summary = "订单统计")
    public OrderStatisticResponse searchStatistic(@Validated @RequestBody BaseCondition request) {
        ConditionFilterUtils.parseCondition(request);
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.ORDER_READ);
        return orderService.searchStatistic(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }

}
