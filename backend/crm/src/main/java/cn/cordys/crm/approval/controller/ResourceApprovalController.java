package cn.cordys.crm.approval.controller;

import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.approval.service.ApprovalInstanceService;
import cn.cordys.crm.approval.service.ResourceApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源审批相关
 */
@RestController
@RequestMapping("/resource-approval")
@Tag(name = "资源审批")
public class ResourceApprovalController {

    @Resource
    private ResourceApprovalService resourceApprovalService;
	@Resource
	private ApprovalInstanceService approvalInstanceService;

    @GetMapping("/detail/{resourceId}")
    @Operation(summary = "当前资源审批详情")
    public ResourceApprovalResponse resourceDetail(@PathVariable String resourceId) {
        return resourceApprovalService.resourceDetail(resourceId);
    }

	@GetMapping("/record-detail/{resourceId}")
	@Operation(summary = "审批记录详情")
	public ApprovalInstanceDetail getDetail(@PathVariable String resourceId) {
		return approvalInstanceService.getLatestApprovalInstanceDetail(resourceId);
	}
}
