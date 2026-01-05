package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.BusinessTitle;
import cn.cordys.crm.contract.dto.request.BusinessTitleAddRequest;
import cn.cordys.crm.contract.dto.request.BusinessTitlePageRequest;
import cn.cordys.crm.contract.dto.request.BusinessTitleUpdateRequest;
import cn.cordys.crm.contract.dto.response.BusinessTitleListResponse;
import cn.cordys.crm.contract.service.BusinessTitleService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "工商抬头")
@RestController
@RequestMapping("/business-title")
public class BusinessTitleController {

    @Resource
    private BusinessTitleService businessTitleService;


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.BUSINESS_TITLE_READ)
    @Operation(summary = "创建")
    public BusinessTitle add(@Validated @RequestBody BusinessTitleAddRequest request) {
        return businessTitleService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.BUSINESS_TITLE_UPDATE)
    @Operation(summary = "更新")
    public BusinessTitle update(@Validated @RequestBody BusinessTitleUpdateRequest request) {
        return businessTitleService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.BUSINESS_TITLE_DELETE)
    @Operation(summary = "删除")
    public void delete(@PathVariable("id") String id) {
        businessTitleService.delete(id);
    }


    @GetMapping("/invoice/check/{id}")
    @RequiresPermissions(PermissionConstants.BUSINESS_TITLE_DELETE)
    @Operation(summary = "检查工商抬头是否开过票")
    public boolean checkInvoice(@PathVariable String id) {
        return businessTitleService.checkHasInvoice(id);
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.BUSINESS_TITLE_READ)
    @Operation(summary = "列表")
    public Pager<List<BusinessTitleListResponse>> list(@Validated @RequestBody BusinessTitlePageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        return businessTitleService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.BUSINESS_TITLE_READ)
    @Operation(summary = "详情")
    public BusinessTitle get(@PathVariable("id") String id) {
        return businessTitleService.get(id);
    }


}
