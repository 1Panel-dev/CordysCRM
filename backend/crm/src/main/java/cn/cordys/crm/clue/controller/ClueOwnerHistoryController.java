package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.dto.response.ClueOwnerListResponse;
import cn.cordys.crm.clue.service.ClueOwnerHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
@Tag(name = "线索责任人历史")
@RestController
@RequestMapping("/lead/owner/history")
public class ClueOwnerHistoryController {
  @Resource private ClueOwnerHistoryService clueOwnerHistoryService;

  @GetMapping("/list/{clueId}")
  @RequiresPermissions(
      value = {
        PermissionConstants.CLUE_MANAGEMENT_READ,
        PermissionConstants.CLUE_MANAGEMENT_POOL_READ
      },
      logical = Logical.OR)
  @Operation(summary = "线索责任人历史列表")
  public List<ClueOwnerListResponse> list(@PathVariable String clueId) {
    return clueOwnerHistoryService.list(clueId, OrganizationContext.getOrganizationId());
  }
}
