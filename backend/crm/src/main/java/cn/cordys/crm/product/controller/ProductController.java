package cn.cordys.crm.product.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.product.dto.request.ProductEditRequest;
import cn.cordys.crm.product.dto.request.ProductPageRequest;
import cn.cordys.crm.product.dto.response.ProductGetResponse;
import cn.cordys.crm.product.dto.response.ProductListResponse;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author guoyuqi
 */
@Tag(name = "产品")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @Resource
    private ProductService productService;


    @GetMapping("/module/form")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_READ)
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.PRODUCT.getKey(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_READ)
    @Operation(summary = "产品列表")
    public PagerWithOption<List<ProductListResponse>> list(@Validated @RequestBody ProductPageRequest request) {
        return productService.list(request, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_READ)
    @Operation(summary = "产品详情")
    public ProductGetResponse get(@PathVariable String id) {
        return productService.get(id);
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_ADD)
    @Operation(summary = "添加产品")
    public Product add(@Validated @RequestBody ProductEditRequest request) {
        return productService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE)
    @Operation(summary = "更新产品")
    public Product update(@Validated @RequestBody ProductEditRequest request) {
        return productService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/update")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE)
    @Operation(summary = "批量更新产品")
    public void batchUpdate(@Validated @RequestBody ResourceBatchEditRequest request) {
        productService.batchUpdate(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_DELETE)
    @Operation(summary = "删除产品")
    public void delete(@PathVariable String id) {
        productService.delete(id);
    }


    @PostMapping("/batch/delete")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_DELETE)
    @Operation(summary = "批量删除产品")
    public void batchDelete(@RequestBody @NotEmpty List<String> ids) {
        productService.batchDelete(ids, SessionUtils.getUserId());
    }

    @PostMapping("/edit/pos")
    @Operation(summary = "拖拽排序")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE)
    public void editPos(@Validated @RequestBody PosRequest request) {
        productService.editPos(request);
    }

    @GetMapping("/template/download")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_IMPORT)
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(HttpServletResponse response) {
        productService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import/pre-check")
    @Operation(summary = "导入检查")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_IMPORT)
    public ImportResponse preCheck(@RequestPart(value = "file") MultipartFile file) {
        return productService.importPreCheck(file, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_IMPORT)
    public ImportResponse realImport(@RequestPart(value = "file") MultipartFile file) {
        return productService.realImport(file, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @GetMapping("/list/option")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_READ)
    @Operation(summary = "获取当前组织下全部产品的id,和name集合")
    public List<OptionDTO> listOption() {
        return productService.listOption(OrganizationContext.getOrganizationId());
    }


}
