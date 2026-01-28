package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.UserViewResourceType;
import cn.cordys.crm.system.domain.UserView;
import cn.cordys.crm.system.dto.request.UserViewAddRequest;
import cn.cordys.crm.system.dto.request.UserViewUpdateRequest;
import cn.cordys.crm.system.dto.response.UserViewListResponse;
import cn.cordys.crm.system.dto.response.UserViewResponse;
import cn.cordys.crm.system.service.UserViewService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author song-cc-rock
 */
@Tag(name = "合同回款记录视图")
@RestController
@RequestMapping("/contract/payment-record/view")
public class ContractPaymentRecordUserViewController {

  @Resource private UserViewService userViewService;

  @PostMapping("/add")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
  @Operation(summary = "添加回款记录视图")
  public UserView add(@Validated @RequestBody UserViewAddRequest request) {
    return userViewService.add(
        request,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        UserViewResourceType.CONTRACT_PAYMENT_RECORD.name());
  }

  @PostMapping("/update")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
  @Operation(summary = "编辑回款记录视图")
  public UserView update(@Validated @RequestBody UserViewUpdateRequest request) {
    return userViewService.update(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/delete/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
  @Operation(summary = "删除回款记录视图")
  public void delete(@PathVariable String id) {
    userViewService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/detail/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
  @Operation(summary = "回款记录视图详情")
  public UserViewResponse viewDetail(@PathVariable String id) {
    return userViewService.getViewDetail(
        id,
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId(),
        FormKey.CONTRACT_PAYMENT_RECORD.getKey());
  }

  @GetMapping("/list")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
  @Operation(summary = "回款记录视图列表")
  public List<UserViewListResponse> queryList() {
    return userViewService.list(
        UserViewResourceType.CONTRACT_PAYMENT_RECORD.name(),
        SessionUtils.getUserId(),
        OrganizationContext.getOrganizationId());
  }

  @GetMapping("/fixed/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
  @Operation(summary = "回款记录视图固定/取消固定")
  public void fixed(@PathVariable String id) {
    userViewService.fixed(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/edit/pos")
  @Operation(summary = "回款记录视图-拖拽排序")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
  public void editPos(@Validated @RequestBody PosRequest request) {
    userViewService.editPos(
        request, SessionUtils.getUserId(), UserViewResourceType.CONTRACT_PAYMENT_RECORD.name());
  }

  @GetMapping("/enable/{id}")
  @RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
  @Operation(summary = "回款记录视图-启用/禁用")
  public void enable(@PathVariable String id) {
    userViewService.enable(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }
}
