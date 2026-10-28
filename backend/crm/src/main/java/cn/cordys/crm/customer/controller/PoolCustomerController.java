package cn.cordys.crm.customer.controller;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.dto.CustomerPoolDTO;
import cn.cordys.crm.customer.dto.request.CustomerExportRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerPoolImportRequest;
import cn.cordys.crm.customer.dto.request.PoolCustomerAssignRequest;
import cn.cordys.crm.customer.dto.request.PoolCustomerChartAnalysisRequest;
import cn.cordys.crm.customer.dto.request.PoolCustomerPickRequest;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerPoolExportService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import cn.cordys.crm.system.dto.request.PoolBatchPickRequest;
import cn.cordys.crm.system.dto.request.PoolBatchRequest;
import cn.cordys.crm.system.dto.request.PoolFreezeRequest;
import cn.cordys.crm.system.dto.request.PoolUnfreezeRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "公海客户")
@RestController
@RequestMapping("/pool/account")
public class PoolCustomerController {

    @Resource
    private PoolCustomerService poolCustomerService;
    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private CustomerService customerService;
    @Resource
    private CustomerPoolExportService customerPoolExportService;

    @GetMapping("/options")
    @Operation(summary = "获取当前用户公海选项")
    @RequiresPermissions(value = { PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ })
    public List<CustomerPoolDTO> getPoolOptions() {
        return poolCustomerService.getPoolOptions(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @Operation(summary = "客户列表")
    @RequiresPermissions(value = { PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ })
    public PagerWithOption<List<CustomerListResponse>> list(@Validated @RequestBody CustomerPageRequest request) {
        if (StringUtils.isEmpty(request.getPoolId())) {
            throw new GenericException(Translator.get("miss.customer_pool_id"));
        }
        poolCustomerService.checkPoolMember(request.getPoolId(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        ConditionFilterUtils.parseCondition(request, FormKey.CUSTOMER.getKey());
        return customerService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), null);
    }

    @PostMapping("/pick")
    @Operation(summary = "领取客户")
    @RequiresPermissions(value = { PermissionConstants.CUSTOMER_MANAGEMENT_POOL_PICK })
    public void pick(@Validated @RequestBody PoolCustomerPickRequest request) {
        poolCustomerService.checkPoolMember(request.getPoolId(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        poolCustomerService.pick(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/assign")
    @Operation(summary = "分配客户")
    @RequiresPermissions(value = { PermissionConstants.CUSTOMER_MANAGEMENT_POOL_ASSIGN })
    public void assign(@Validated @RequestBody PoolCustomerAssignRequest request) {
        String poolId = poolCustomerService.getPoolIdByCustomerId(request.getCustomerId());
        poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        poolCustomerService.assign(request.getCustomerId(), request.getAssignUserId(),
                OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/freeze")
    @Operation(summary = "冻结客户")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_FREEZE)
    public void freeze(@Validated @RequestBody PoolFreezeRequest request) {
        String poolId = poolCustomerService.getPoolIdByCustomerId(request.getId());
        poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        poolCustomerService.freeze(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/unfreeze")
    @Operation(summary = "解冻客户")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_FREEZE)
    public void unfreeze(@Validated @RequestBody PoolUnfreezeRequest request) {
        String poolId = poolCustomerService.getPoolIdByCustomerId(request.getId());
        poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        poolCustomerService.unfreeze(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除客户")
    @RequiresPermissions(value = { PermissionConstants.CUSTOMER_MANAGEMENT_POOL_DELETE })
    public void delete(@PathVariable String id) {
        String poolId = poolCustomerService.getPoolIdByCustomerId(id);
        poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        poolCustomerService.deleteWithApprovalCheck(id, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "客户详情")
    public CustomerGetResponse get(@PathVariable String id) {
        // 待办审批人和有效评论@用户可查看对应资源详情。
        if (!resourcePermissionService.hasApprovalViewPermission(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), FormKey.CUSTOMER.getKey())) {
            if (!PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ)) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            String poolId = poolCustomerService.getPoolIdByCustomerId(id);
            poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(),
                    OrganizationContext.getOrganizationId());
        }
        return customerService.get(id);
    }

    @PostMapping("/batch-pick")
    @Operation(summary = "批量领取客户")
    @RequiresPermissions(value = { PermissionConstants.CUSTOMER_MANAGEMENT_POOL_PICK })
    public void batchPick(@Validated @RequestBody PoolBatchPickRequest request) {
        poolCustomerService.checkPoolMember(request.getPoolId(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        poolCustomerService.batchPick(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch-assign")
    @Operation(summary = "批量分配客户")
    @RequiresPermissions(value = { PermissionConstants.CUSTOMER_MANAGEMENT_POOL_ASSIGN })
    public void batchAssign(@Validated @RequestBody PoolBatchAssignRequest request) {
        // 先校验全部记录，避免首条合法记录授权整批操作。
        for (String id : request.getBatchIds()) {
            String poolId = poolCustomerService.getPoolIdByCustomerId(id);
            poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(),
                    OrganizationContext.getOrganizationId());
        }
        poolCustomerService.batchAssign(request, request.getAssignUserId(), OrganizationContext.getOrganizationId(),
                SessionUtils.getUserId());
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除客户")
    @RequiresPermissions(value = { PermissionConstants.CUSTOMER_MANAGEMENT_POOL_DELETE })
    public BatchAffectReasonResponse batchDelete(@Validated @RequestBody PoolBatchRequest request) {
        for (String id : request.getBatchIds()) {
            String poolId = poolCustomerService.getPoolIdByCustomerId(id);
            poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(),
                    OrganizationContext.getOrganizationId());
        }
        return poolCustomerService.batchDelete(request.getBatchIds(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch-update")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_UPDATE)
    @Operation(summary = "批量更新客户")
    public BatchAffectReasonResponse batchUpdate(@Validated @RequestBody ResourceBatchEditRequest request) {
        for (String id : request.getIds()) {
            String poolId = poolCustomerService.getPoolIdByCustomerId(id);
            poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(),
                    OrganizationContext.getOrganizationId());
        }
        return poolCustomerService.batchUpdate(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/export-all")
    @Operation(summary = "客户导出全部")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_EXPORT)
    public String customerPoolExportAll(@Validated @RequestBody CustomerExportRequest request) {
        poolCustomerService.checkPoolMember(request.getPoolId(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        ConditionFilterUtils.parseCondition(request, FormKey.CUSTOMER.getKey());
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.CUSTOMER_POOL.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CUSTOMER_POOL)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .pageRequest(request)
                .formKey(FormKey.CUSTOMER.getKey())
                .build();
        return customerPoolExportService.exportAllWithMergeStrategy(exportDTO);
    }

    @PostMapping("/export-select")
    @Operation(summary = "导出选中客户")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_EXPORT)
    public String customerPoolExportSelect(@Validated @RequestBody ExportSelectRequest request) {
        String poolId = poolCustomerService.getPoolIdByCustomerId(request.getIds().getFirst());
        poolCustomerService.checkPoolMember(poolId, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.CUSTOMER_POOL.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CUSTOMER_POOL)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .selectIds(request.getIds())
                .selectRequest(request)
                .formKey(FormKey.CUSTOMER.getKey())
                .build();
        return customerPoolExportService.exportSelectWithMergeStrategy(exportDTO);
    }

    @PostMapping("/chart")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ)
    @Operation(summary = "客户图表生成")
    public List<ChartResult> chart(@Validated @RequestBody PoolCustomerChartAnalysisRequest request) {
        poolCustomerService.checkPoolMember(request.getPoolId(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        return poolCustomerService.chart(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                null);
    }

    @GetMapping("/template/download")
    @CsPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_IMPORT)
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(HttpServletResponse response) {
        poolCustomerService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import/pre-check")
    @Operation(summary = "导入检查")
    @CsPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_IMPORT)
    public ImportResponse preCheck(@Validated @RequestPart("request") CustomerPoolImportRequest request,
            @RequestPart(value = "file") MultipartFile file) {
        poolCustomerService.checkPoolMember(request.getPoolId(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        return poolCustomerService.importPreCheck(file, request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入")
    @CsPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_IMPORT)
    public ImportResponse realImport(@Validated @RequestPart("request") CustomerPoolImportRequest request,
            @RequestPart(value = "file") MultipartFile file) {
        poolCustomerService.checkPoolMember(request.getPoolId(), SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
        return poolCustomerService.realImport(file, request, OrganizationContext.getOrganizationId(),
                SessionUtils.getUserId());
    }
}
