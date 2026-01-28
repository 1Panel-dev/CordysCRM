package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.dto.CustomerPoolDTO;
import cn.cordys.crm.customer.dto.request.CustomerPoolAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerPoolUpdateRequest;
import cn.cordys.crm.customer.service.CustomerPoolService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account-pool")
@Tag(name = "公海池")
public class CustomerPoolController {

  @Resource private CustomerPoolService customerPoolService;

  @PostMapping("/page")
  @Operation(summary = "分页获取公海池")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public Pager<List<CustomerPoolDTO>> page(@RequestBody BasePageRequest request) {
    Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
    return PageUtils.setPageInfo(
        page, customerPoolService.page(request, OrganizationContext.getOrganizationId()));
  }

  @PostMapping("/add")
  @Operation(summary = "保存公海池")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void add(@Validated @RequestBody CustomerPoolAddRequest request) {
    customerPoolService.add(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/update")
  @Operation(summary = "编辑公海池")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void update(@Validated @RequestBody CustomerPoolUpdateRequest request) {
    customerPoolService.update(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/quick-update")
  @Operation(summary = "快捷保存公海池")
  public void quickUpdate(@Validated @RequestBody CustomerPoolUpdateRequest request) {
    customerPoolService.update(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/no-pick/{id}")
  @Operation(summary = "公海池是否存在未领取线索")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public boolean checkNoPick(@PathVariable String id) {
    return customerPoolService.checkNoPick(id);
  }

  @GetMapping("/delete/{id}")
  @Operation(summary = "删除公海池")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void delete(@PathVariable String id) {
    customerPoolService.delete(id);
  }

  @GetMapping("/switch/{id}")
  @Operation(summary = "启用/禁用公海池")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void switchStatus(@PathVariable String id) {
    customerPoolService.switchStatus(id, SessionUtils.getUserId());
  }
}
