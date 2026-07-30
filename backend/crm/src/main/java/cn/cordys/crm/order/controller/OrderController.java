package cn.cordys.crm.order.controller;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.stage.StageSortRequest;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsBatchPermission;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.order.dto.request.*;
import cn.cordys.crm.order.dto.response.OrderGetResponse;
import cn.cordys.crm.order.dto.response.OrderListResponse;
import cn.cordys.crm.order.dto.response.OrderStatisticResponse;
import cn.cordys.crm.order.service.OrderExportService;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.dto.request.ImportRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @Resource
    private OrderExportService orderExportService;

    @GetMapping("/module/form")
    @CsPermission(PermissionConstants.ORDER_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.ORDER.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @CsPermission(PermissionConstants.ORDER_ADD)
    @Operation(summary = "创建")
    public Order add(@Validated @RequestBody OrderAddRequest request) {
        return orderService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @CsPermission(value = PermissionConstants.ORDER_UPDATE, resourceId = "{#request.id}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "更新")
    public Order update(@Validated @RequestBody OrderUpdateRequest request) {
        return orderService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update/stage")
    @CsPermission(value = PermissionConstants.ORDER_UPDATE, resourceId = "{#request.id}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "更新订单阶段")
    public void updateStage(@Validated @RequestBody OrderStageRequest request) {
        orderService.updateStage(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/update")
    @CsBatchPermission(value = PermissionConstants.ORDER_UPDATE, resourceId = "{#request.ids}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "批量更新订单")
    public BatchAffectReasonResponse batchUpdate(@Validated @RequestBody ResourceBatchEditRequest request) {
        return orderService.batchUpdate(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @CsPermission(value = PermissionConstants.ORDER_DELETE, resourceId = "{#id}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "删除")
    public void delete(@PathVariable("id") String id) {
        orderService.deleteWithApprovalCheck(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/snapshot/{id}")
    @CsPermission(value = PermissionConstants.ORDER_READ, resourceId = "{#id}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "获取详情快照")
    public OrderGetResponse getSnapshot(@PathVariable("id") String id) {
        return orderService.getSnapshot(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @CsPermission(value = PermissionConstants.ORDER_READ, resourceId = "{#id}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "详情")
    public OrderGetResponse get(@PathVariable("id") String id) {
        return orderService.get(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/module/form/snapshot/{id}")
    @CsPermission(value = PermissionConstants.ORDER_READ, resourceId = "{#id}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "获取表单快照配置")
    public ModuleFormConfigDTO getFormSnapshot(@PathVariable("id") String id) {
        return orderService.getFormSnapshot(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @CsPermission(PermissionConstants.ORDER_READ)
    @Operation(summary = "列表")
    public PagerWithOption<List<OrderListResponse>> list(@Validated @RequestBody OrderPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.ORDER.getKey());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.ORDER_READ);
        return orderService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission, false);
    }

    @GetMapping("/tab")
    @CsPermission(PermissionConstants.ORDER_READ)
    @Operation(summary = "tab是否显示")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return orderService.getTabEnableConfig(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/download/{id}")
    @CsPermission(value = PermissionConstants.ORDER_DOWNLOAD, resourceId = "{#id}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "下载订单日志记录")
    public void download(@PathVariable("id") String id) {
        orderService.download(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/statistic")
    @CsPermission(PermissionConstants.ORDER_READ)
    @Operation(summary = "订单统计")
    public OrderStatisticResponse searchStatistic(@Validated @RequestBody BaseCondition request) {
        ConditionFilterUtils.parseCondition(request, FormKey.ORDER.getKey());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.ORDER_READ);
        return orderService.searchStatistic(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
    }


    @PostMapping("/sort")
    @Operation(summary = "订单看板拖拽排序")
    public void sortModule(@Validated @RequestBody StageSortRequest request) {
        orderService.sort(request, SessionUtils.getUserId());
    }


    @PostMapping("/export-select")
    @Operation(summary = "导出选中订单")
    @CsBatchPermission(value = PermissionConstants.ORDER_EXPORT, resourceId = "{#request.ids}", formType = FormKeyConstants.ORDER)
    public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), PermissionConstants.ORDER_READ);
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.ORDER.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.ORDER_INDEX)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .deptDataPermission(deptDataPermission)
                .selectIds(request.getIds())
                .selectRequest(request)
                .formKey(FormKey.ORDER.getKey())
                .build();
        return orderExportService.exportSelectWithMergeStrategy(exportDTO);
    }

    @PostMapping("/export-all")
    @Operation(summary = "导出全部订单")
    @CsPermission(PermissionConstants.ORDER_EXPORT)
    public String exportAll(@Validated @RequestBody OrderExportRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.ORDER.getKey());
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.ORDER_READ);
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.ORDER.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.ORDER_INDEX)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .deptDataPermission(deptDataPermission)
                .pageRequest(request)
                .formKey(FormKey.ORDER.getKey())
                .build();
        return orderExportService.exportAllWithMergeStrategy(exportDTO);
    }


    @GetMapping("/template/download")
    @CsPermission(PermissionConstants.ORDER_IMPORT)
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(HttpServletResponse response) {
        orderService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import/pre-check")
    @Operation(summary = "导入检查")
    @CsPermission(PermissionConstants.ORDER_IMPORT)
    public ImportResponse preCheck(@Validated @RequestPart("request") ImportRequest request, @RequestPart(value = "file") MultipartFile file) {
        return orderService.importPreCheck(file, request.getImportType(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入")
    @CsPermission(PermissionConstants.ORDER_IMPORT)
    public ImportResponse realImport(@Validated @RequestPart("request") ImportRequest request, @RequestPart(value = "file") MultipartFile file) {
        return orderService.realImport(file, request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
