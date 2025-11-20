package cn.cordys.crm.product.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.product.dto.request.ProductPriceAddRequest;
import cn.cordys.crm.product.dto.request.ProductPriceEditRequest;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author song-cc-rock
 */
@RestController
@Tag(name = "价格表")
@RequestMapping("/price")
public class ProductPriceController {

	@Resource
	private ProductPriceService priceService;

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

}
