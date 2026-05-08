package cn.cordys.crm.approval.controller;


import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.request.ApprovalReturnBackRequest;
import cn.cordys.crm.approval.service.ApprovalReturnBackService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "审批流回退操作")
@RestController
@RequestMapping("/approval-return")
public class ApprovalReturnBackController {

    @Resource
    private ApprovalReturnBackService approvalReturnBackService;

    @PostMapping("/back")
    @Operation(summary = "回退")
    public void back(@Validated @RequestBody ApprovalReturnBackRequest request) {
        approvalReturnBackService.back(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
