package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.request.DictAddRequest;
import cn.cordys.crm.system.dto.request.DictSortRequest;
import cn.cordys.crm.system.dto.request.DictSwitchRequest;
import cn.cordys.crm.system.dto.request.DictUpdateRequest;
import cn.cordys.crm.system.service.DictService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dict")
@Tag(name = "模块配置-字典管理")
public class DictController {

  @Resource private DictService dictService;

  @GetMapping("/get/{module}")
  @Operation(summary = "获取字典列表")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_READ})
  public List<Dict> getDictListByType(
      @Schema(
              description = "类型",
              requiredMode = Schema.RequiredMode.REQUIRED,
              allowableValues = {"OPPORTUNITY_FAIL_RS", "CUSTOMER_POOL_RS", "CLUE_POOL_RS"})
          @PathVariable("module")
          String module) {
    return dictService.getDictListByType(module, OrganizationContext.getOrganizationId());
  }

  @PostMapping("/add")
  @Operation(summary = "添加字典值")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void addDict(@RequestBody DictAddRequest request) {
    dictService.addDict(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
  }

  @PostMapping("/update")
  @Operation(summary = "修改字典值")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void updateDict(@RequestBody DictUpdateRequest request) {
    dictService.updateDict(request, SessionUtils.getUserId());
  }

  @GetMapping("/delete/{id}")
  @Operation(summary = "删除字典值")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void deleteDict(@PathVariable("id") String id) {
    dictService.deleteDict(id);
  }

  @PostMapping("/switch")
  @Operation(summary = "字典开启关闭")
  @RequiresPermissions(value = {PermissionConstants.MODULE_SETTING_UPDATE})
  public void switchDict(@RequestBody DictSwitchRequest request) {
    dictService.switchDict(request, OrganizationContext.getOrganizationId());
  }

  @PostMapping("/sort")
  @Operation(summary = "字典排序")
  @RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
  public void sortModule(@Validated @RequestBody DictSortRequest request) {
    dictService.sort(request, SessionUtils.getUserId());
  }

  @GetMapping("/config/{module}")
  @Operation(summary = "获取字典配置")
  public DictConfigDTO getDictConf(
      @Schema(
              description = "字典类型",
              requiredMode = Schema.RequiredMode.REQUIRED,
              allowableValues = {"OPPORTUNITY_FAIL_RS", "CUSTOMER_POOL_RS", "CLUE_POOL_RS"})
          @PathVariable("module")
          String module) {
    return dictService.getDictConf(module, OrganizationContext.getOrganizationId());
  }
}
