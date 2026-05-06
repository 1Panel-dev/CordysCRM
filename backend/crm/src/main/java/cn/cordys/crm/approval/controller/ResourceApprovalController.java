package cn.cordys.crm.approval.controller;

import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.approval.service.ResourceApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源审核相关
 * <p>
 * 提供资源目前审核状态以及审核信息的功能
 * </p>
 */
@RestController
@RequestMapping("/resource-approval")
@Tag(name = "资源审核状态")
public class ResourceApprovalController {

    @Resource
    private ResourceApprovalService resourceApprovalService;

    @GetMapping("/detail/{resourceId}")
    @Operation(summary = "资源审核状态-查找资源当前审核状态")
    public ResourceApprovalResponse resourceDetail(@PathVariable String resourceId) {
        return resourceApprovalService.resourceDetail(resourceId);
    }

}
