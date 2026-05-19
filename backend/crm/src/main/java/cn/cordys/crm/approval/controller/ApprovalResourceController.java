package cn.cordys.crm.approval.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalResourceBaseParam;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.approval.service.ApprovalInstanceService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 资源审批相关
 */
@RestController
@RequestMapping("/approval-resource")
@Tag(name = "审批资源")
public class ApprovalResourceController {

    @Resource
    private ApprovalResourceService approvalResourceService;
	@Resource
	private ApprovalInstanceService approvalInstanceService;

	@PostMapping("/push")
	@Operation(summary = "提审")
	public void push(@RequestBody ApprovalResourceBaseParam param) {
		approvalResourceService.push(param, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
	}

	@PostMapping("/revoke")
	@Operation(summary = "撤销")
	public void revoke(@RequestBody ApprovalResourceBaseParam param) {
		approvalResourceService.revoke(param, SessionUtils.getUserId());
	}

    @GetMapping("/simple-detail/{resourceId}")
    @Operation(summary = "列表详情")
    public ResourceApprovalResponse resourceDetail(@PathVariable String resourceId) {
        return approvalResourceService.resourceDetail(resourceId);
    }

	@GetMapping("/detail/{resourceId}")
	@Operation(summary = "记录详情")
	public ApprovalInstanceDetail getRecordDetail(@PathVariable String resourceId) {
		return approvalInstanceService.getLatestApprovalInstanceDetail(resourceId, OrganizationContext.getOrganizationId());
	}
}
