package cn.cordys.crm.approval.controller;


import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.request.ApprovalAddSignRequest;
import cn.cordys.crm.approval.service.ApprovalAddSignService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "审批流加签")
@RestController
@RequestMapping("/approval-sign")
public class ApprovalAddSignController {

    @Resource
    private ApprovalAddSignService approvalAddSignService;

    @PostMapping("/add")
    @Operation(summary = "新建加签")
    public void add(@Validated @RequestBody ApprovalAddSignRequest request) {
        approvalAddSignService.addSign(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
