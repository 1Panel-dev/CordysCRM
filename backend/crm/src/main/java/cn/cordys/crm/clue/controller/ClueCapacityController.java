package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.dto.ClueCapacityDTO;
import cn.cordys.crm.clue.service.ClueCapacityService;
import cn.cordys.crm.system.dto.request.CapacityAddRequest;
import cn.cordys.crm.system.dto.request.CapacityUpdateRequest;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lead-capacity")
@Tag(name = "线索库容设置")
public class ClueCapacityController {

  @Resource private ClueCapacityService clueCapacityService;

  @GetMapping("/get")
  @Operation(summary = "获取线索库容设置")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public List<ClueCapacityDTO> list() {
    return clueCapacityService.list(OrganizationContext.getOrganizationId());
  }

  @PostMapping("/add")
  @Operation(summary = "添加线索库容设置")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void add(@Validated @RequestBody CapacityAddRequest request) {
    clueCapacityService.add(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/update")
  @Operation(summary = "修改线索库容设置")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void update(@Validated @RequestBody CapacityUpdateRequest request) {
    clueCapacityService.update(
        request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @GetMapping("/delete/{id}")
  @Operation(summary = "删除线索库容设置")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void delete(@PathVariable("id") @Validated String id) {
    clueCapacityService.delete(id);
  }
}
