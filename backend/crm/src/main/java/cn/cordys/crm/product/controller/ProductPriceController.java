package cn.cordys.crm.product.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.product.dto.request.ProductPriceAddRequest;
import cn.cordys.crm.product.dto.request.ProductPriceEditRequest;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.dto.response.ProductPriceGetResponse;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
}
