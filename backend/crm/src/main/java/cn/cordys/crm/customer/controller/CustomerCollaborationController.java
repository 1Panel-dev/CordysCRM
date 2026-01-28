package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.dto.request.CustomerCollaborationAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerCollaborationUpdateRequest;
import cn.cordys.crm.customer.dto.response.CustomerCollaborationListResponse;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
@Tag(name = "客户协作人")
@RestController
@RequestMapping("/account/collaboration")
public class CustomerCollaborationController {
  @Resource private CustomerCollaborationService customerCollaborationService;

  @GetMapping("/list/{customerId}")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
  @Operation(summary = "客户协作人列表")
  public List<CustomerCollaborationListResponse> list(@PathVariable String customerId) {
    return customerCollaborationService.list(customerId, OrganizationContext.getOrganizationId());
  }

  @PostMapping("/add")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
  @Operation(summary = "添加客户协作人")
  public CustomerCollaboration add(
      @Validated @RequestBody CustomerCollaborationAddRequest request) {
    return customerCollaborationService.add(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/update")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
  @Operation(summary = "更新客户协作人")
  public CustomerCollaboration update(
      @Validated @RequestBody CustomerCollaborationUpdateRequest request) {
    return customerCollaborationService.update(request, SessionUtils.getUserId());
  }

  @GetMapping("/delete/{id}")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
  @Operation(summary = "删除客户协作人")
  public void delete(@PathVariable String id) {
    customerCollaborationService.delete(id);
  }

  @PostMapping("/batch/delete")
  @Operation(summary = "批量删除客户协作人")
  @RequiresPermissions(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
  public void batchDelete(@RequestBody List<String> ids) {
    customerCollaborationService.batchDelete(ids);
  }
}
