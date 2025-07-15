package io.cordys.crm.home.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.home.dto.request.HomeStatisticSearchRequest;
import io.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import io.cordys.crm.home.dto.response.HomeCustomerStatistic;
import io.cordys.crm.home.service.HomeStatisticService;
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
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
@Tag(name = "首页统计")
@RestController
@RequestMapping("/home/statistic")
public class HomeStatisticController {

    @Resource
    private HomeStatisticService homeStatisticService;

    @PostMapping("/customer")
    @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户统计")
    public HomeCustomerStatistic getCustomerStatistic(@RequestBody @Validated HomeStatisticSearchRequest request) {
        DeptDataPermissionDTO deptDataPermission = homeStatisticService.getDeptDataPermissionDTO(request, PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        HomeStatisticSearchWrapperRequest wrapperRequest = new HomeStatisticSearchWrapperRequest(request, deptDataPermission);
        wrapperRequest.setOrgId(OrganizationContext.getOrganizationId());
        return homeStatisticService.getCustomerStatistic(wrapperRequest);
    }

}
