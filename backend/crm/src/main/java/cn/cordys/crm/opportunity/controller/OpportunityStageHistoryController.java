package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.opportunity.dto.response.OpportunityStageHistoryResponse;
import cn.cordys.crm.opportunity.service.OpportunityStageHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "商机阶段变更历史")
@RestController
@RequestMapping("/opportunity/stage/history")
public class OpportunityStageHistoryController {
    @Resource
    private OpportunityStageHistoryService opportunityStageHistoryService;

    @GetMapping("/list/{opportunityId}")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机阶段变更历史列表")
    public List<OpportunityStageHistoryResponse> list(@PathVariable String opportunityId) {
        return opportunityStageHistoryService.list(opportunityId, OrganizationContext.getOrganizationId());
    }
}
