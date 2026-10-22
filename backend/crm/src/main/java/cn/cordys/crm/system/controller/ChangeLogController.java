package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.response.ChangeLogResponse;
import cn.cordys.crm.system.service.ChangeLogService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 详情页变更记录接口。
 * <p>
 * 按父表单分别声明入口，使 {@link CsPermission} 能在查询前完成资源级权限校验。
 * 自定义表单入口使用表单 ID 作为路径变量，与内置表单字面路径不冲突。
 * </p>
 */
@RestController
@RequestMapping("/change/log")
@Tag(name = "变更记录")
public class ChangeLogController {

    @Resource
    private ChangeLogService changeLogService;

    @PostMapping("/customer/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.CUSTOMER)
    @Operation(summary = "分页获取客户变更记录")
    public Pager<List<ChangeLogResponse>> customerPage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.CUSTOMER, resourceId, request);
    }

    @PostMapping("/clue/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CLUE_MANAGEMENT_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.CLUE)
    @Operation(summary = "分页获取线索变更记录")
    public Pager<List<ChangeLogResponse>> cluePage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.CLUE, resourceId, request);
    }

    @PostMapping("/opportunity/{resourceId}/page")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.OPPORTUNITY)
    @Operation(summary = "分页获取商机变更记录")
    public Pager<List<ChangeLogResponse>> opportunityPage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.OPPORTUNITY, resourceId, request);
    }

    @PostMapping("/quotation/{resourceId}/page")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_QUOTATION_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.QUOTATION)
    @Operation(summary = "分页获取报价单变更记录")
    public Pager<List<ChangeLogResponse>> quotationPage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.QUOTATION, resourceId, request);
    }

    @PostMapping("/contact/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.CONTACT)
    @Operation(summary = "分页获取联系人变更记录")
    public Pager<List<ChangeLogResponse>> contactPage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.CONTACT, resourceId, request);
    }

    @PostMapping("/contract/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CONTRACT_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.CONTRACT)
    @Operation(summary = "分页获取合同变更记录")
    public Pager<List<ChangeLogResponse>> contractPage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.CONTRACT, resourceId, request);
    }

    @PostMapping("/contractPaymentPlan/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CONTRACT_PAYMENT_PLAN_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.CONTRACT_PAYMENT_PLAN)
    @Operation(summary = "分页获取合同回款计划变更记录")
    public Pager<List<ChangeLogResponse>> contractPaymentPlanPage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.CONTRACT_PAYMENT_PLAN, resourceId, request);
    }

    @PostMapping("/contractPaymentRecord/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CONTRACT_PAYMENT_RECORD_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.CONTRACT_PAYMENT_RECORD)
    @Operation(summary = "分页获取回款记录变更记录")
    public Pager<List<ChangeLogResponse>> contractPaymentRecordPage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.CONTRACT_PAYMENT_RECORD, resourceId, request);
    }

    @PostMapping("/invoice/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CONTRACT_INVOICE_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.INVOICE)
    @Operation(summary = "分页获取发票变更记录")
    public Pager<List<ChangeLogResponse>> invoicePage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.INVOICE, resourceId, request);
    }

    @PostMapping("/order/{resourceId}/page")
    @CsPermission(value = PermissionConstants.ORDER_READ, resourceId = "{#resourceId}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "分页获取订单变更记录")
    public Pager<List<ChangeLogResponse>> orderPage(@PathVariable String resourceId,
            @Validated @RequestBody BasePageRequest request) {
        return page(FormKeyConstants.ORDER, resourceId, request);
    }

    @PostMapping("/{customFormId}/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "分页获取自定义表单数据变更记录")
    public Pager<List<ChangeLogResponse>> customFormPage(@PathVariable String customFormId,
            @PathVariable String resourceId, @Validated @RequestBody BasePageRequest request) {
        return page(customFormId, resourceId, request);
    }

    private Pager<List<ChangeLogResponse>> page(String formKey, String resourceId, BasePageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ChangeLogResponse> list = changeLogService.list(formKey, resourceId,
                OrganizationContext.getOrganizationId());
        return PageUtils.setPageInfo(page, list);
    }
}
