package io.cordys.crm.system.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.dto.request.ProductBatchEditRequest;
import io.cordys.crm.system.dto.request.ProductEditRequest;
import io.cordys.crm.system.dto.request.ProductPageRequest;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.dto.response.product.ProductGetResponse;
import io.cordys.crm.system.dto.response.product.ProductListResponse;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ProductService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ModuleFormConfigDTO getModuleFormConfig(){
        return moduleFormCacheService.getBusinessFormConfig(FormKey.PRODUCT.getKey(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_READ)
    @Operation(summary = "产品列表")
    public Pager<List<ProductListResponse>> list(@Validated @RequestBody ProductPageRequest request){
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, productService.list(request, OrganizationContext.getOrganizationId()));
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_READ)
    @Operation(summary = "产品详情")
    public ProductGetResponse get(@PathVariable String id){
        return productService.get(id);
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_ADD)
    @Operation(summary = "添加产品")
    public Product add(@Validated @RequestBody ProductEditRequest request){
        return productService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE)
    @Operation(summary = "更新产品")
    public Product update(@Validated @RequestBody ProductEditRequest request){
        return productService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/update")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE)
    @Operation(summary = "批量更新产品")
    public void batchUpdate(@Validated @RequestBody ProductBatchEditRequest request){
         productService.batchUpdate(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_DELETE)
    @Operation(summary = "删除产品")
    public void delete(@PathVariable String id){
        productService.delete(id);
    }


    @PostMapping("/batch/delete")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_DELETE)
    @Operation(summary = "批量删除产品")
    public void batchDelete(@RequestBody @NotEmpty List<String> ids) {
        productService.batchDelete(ids, SessionUtils.getUserId());
    }
}
