package cn.cordys.crm.product.controller;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.product.dto.request.ProductPriceAddRequest;
import cn.cordys.crm.product.dto.request.ProductPriceEditRequest;
import cn.cordys.crm.product.dto.request.ProductPriceExportRequest;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.dto.response.ProductPriceGetResponse;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;
import cn.cordys.crm.product.service.ProductPriceExportService;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author song-cc-rock
 */
@RestController
@Tag(name = "价格表")
@RequestMapping("/price")
public class ProductPriceController {

    @Resource
    private ProductPriceService priceService;
	@Resource
	private ProductPriceExportService priceExportService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.PRICE_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.PRICE_READ)
    @Operation(summary = "价格列表")
    public PagerWithOption<List<ProductPriceResponse>> list(@Validated @RequestBody ProductPricePageRequest request) {
        return priceService.list(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.PRICE_ADD)
    @Operation(summary = "添加价格表")
    public ProductPrice add(@Validated @RequestBody ProductPriceAddRequest request) {
        return priceService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.PRICE_UPDATE)
    @Operation(summary = "修改价格表")
    public ProductPrice update(@Validated @RequestBody ProductPriceEditRequest request) {
        return priceService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.PRICE_READ)
    @Operation(summary = "价格表详情")
    public ProductPriceGetResponse get(@PathVariable String id) {
        return priceService.get(id);
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.PRICE_DELETE)
    @Operation(summary = "删除价格表")
    public void delete(@PathVariable String id) {
        priceService.delete(id);
    }

	@PostMapping("/batch/update")
	@RequiresPermissions(PermissionConstants.PRICE_UPDATE)
	@Operation(summary = "批量更新价格表")
	public void batchUpdate(@Validated @RequestBody ResourceBatchEditRequest request) {
		priceService.batchUpdate(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

    @PostMapping("/edit/pos")
    @Operation(summary = "拖拽排序")
    @RequiresPermissions(PermissionConstants.PRICE_UPDATE)
    public void editPos(@Validated @RequestBody PosRequest request) {
        priceService.editPos(request);
    }

	@GetMapping("/template/download")
	@RequiresPermissions(PermissionConstants.PRICE_IMPORT)
	@Operation(summary = "下载导入模板")
	public void downloadImportTpl(HttpServletResponse response) {
		priceService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
	}

	@PostMapping("/import/pre-check")
	@Operation(summary = "导入检查")
	@RequiresPermissions(PermissionConstants.PRICE_IMPORT)
	public ImportResponse preCheck(@RequestPart(value = "file") @NotNull MultipartFile file) {
		return priceService.importPreCheck(file, OrganizationContext.getOrganizationId());
	}

	@PostMapping("/import")
	@Operation(summary = "导入")
	@RequiresPermissions(PermissionConstants.PRICE_IMPORT)
	public ImportResponse realImport(@RequestPart(value = "file") MultipartFile file) {
		return priceService.realImport(file, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
	}

	@PostMapping("/export")
	@RequiresPermissions(PermissionConstants.PRICE_EXPORT)
	@Operation(summary = "导出全部")
	public String exportAll(@Validated @RequestBody ProductPriceExportRequest request) {
		ConditionFilterUtils.parseCondition(request);
		ExportDTO exportParam = ExportDTO.builder().formKey(FormKey.PRICE.getKey()).fileName(request.getFileName()).headList(request.getHeadList())
				.orgId(OrganizationContext.getOrganizationId()).userId(SessionUtils.getUserId()).exportType(ExportConstants.ExportType.PRODUCT_PRICE.name())
				.locale(LocaleContextHolder.getLocale()).pageRequest(request).logModule(LogModule.PRODUCT_PRICE_MANAGEMENT).build();
		return priceExportService.exportAllWithMergeStrategy(exportParam);
	}

	@PostMapping("/export-select")
	@RequiresPermissions(PermissionConstants.PRICE_EXPORT)
	@Operation(summary = "导出选中")
	public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
		ExportDTO exportParam = ExportDTO.builder().formKey(FormKey.PRICE.getKey()).fileName(request.getFileName()).headList(request.getHeadList())
				.orgId(OrganizationContext.getOrganizationId()).userId(SessionUtils.getUserId()).exportType(ExportConstants.ExportType.PRODUCT_PRICE.name())
				.locale(LocaleContextHolder.getLocale()).selectIds(request.getIds()).logModule(LogModule.PRODUCT_PRICE_MANAGEMENT).build();
		return priceExportService.exportSelectWithMergeStrategy(exportParam);
	}
}
