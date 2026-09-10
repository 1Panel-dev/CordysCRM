package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.system.dto.request.DetailTabPageRequest;
import cn.cordys.crm.system.service.DetailTabDataService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 表单详情页标签数据接口。
 *
 * <p>当前按父表单分别声明入口，是为了让 {@link CsPermission} 在进入业务编排前完成
 * 准确的资源级权限校验。后续扩展其他父表单时应继续增加显式入口，不能降级为只校验权限位。</p>
 */
@RestController
@RequestMapping("/module/form/detail-tab")
@Tag(name = "表单详情页标签数据")
public class DetailTabDataController {

    @Resource
    private DetailTabDataService detailTabDataService;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private FollowUpPlanService followUpPlanService;

    @PostMapping("/customer/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.CUSTOMER)
    @Operation(summary = "分页获取客户详情页标签数据")
    public PagerWithOption<?> customerPage(@PathVariable String resourceId,
                                           @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.CUSTOMER, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/contact/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.CONTACT)
    @Operation(summary = "分页获取联系人详情页标签数据")
    public PagerWithOption<?> contactPage(@PathVariable String resourceId,
                                          @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.CONTACT, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/record/{resourceId}/page")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ,
            PermissionConstants.CUSTOMER_MANAGEMENT_READ,
            PermissionConstants.OPPORTUNITY_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "分页获取跟进记录详情页标签数据")
    public PagerWithOption<?> followRecordPage(@PathVariable String resourceId,
                                               @Validated @RequestBody DetailTabPageRequest request) {
        String userId = SessionUtils.getUserId();
        String organizationId = OrganizationContext.getOrganizationId();
        // 跟进记录可能属于线索、客户或商机，需按实际所属资源校验，不能固定使用某一个模块权限。
        followUpRecordService.checkRecordPermission(resourceId, organizationId, userId, true);
        return detailTabDataService.page(FormKeyConstants.FOLLOW_RECORD, resourceId, request, userId, organizationId);
    }

    @PostMapping("/plan/{resourceId}/page")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ,
            PermissionConstants.CUSTOMER_MANAGEMENT_READ,
            PermissionConstants.OPPORTUNITY_MANAGEMENT_READ}, logical = Logical.OR)
    @Operation(summary = "分页获取跟进计划详情页标签数据")
    public PagerWithOption<?> followPlanPage(@PathVariable String resourceId,
                                             @Validated @RequestBody DetailTabPageRequest request) {
        String userId = SessionUtils.getUserId();
        String organizationId = OrganizationContext.getOrganizationId();
        // 跟进计划与跟进记录使用相同的多父资源模型，因此复用其所属资源权限判断。
        followUpPlanService.checkPlanPermission(resourceId, organizationId, userId, true);
        return detailTabDataService.page(FormKeyConstants.FOLLOW_PLAN, resourceId, request, userId, organizationId);
    }

    @PostMapping("/clue/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CLUE_MANAGEMENT_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.CLUE)
    @Operation(summary = "分页获取线索详情页标签数据")
    public PagerWithOption<?> cluePage(@PathVariable String resourceId,
                                       @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.CLUE, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/opportunity/{resourceId}/page")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_MANAGEMENT_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.OPPORTUNITY)
    @Operation(summary = "分页获取商机详情页标签数据")
    public PagerWithOption<?> opportunityPage(@PathVariable String resourceId,
                                               @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.OPPORTUNITY, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/product/{resourceId}/page")
    @RequiresPermissions(PermissionConstants.PRODUCT_MANAGEMENT_READ)
    @Operation(summary = "分页获取产品详情页标签数据")
    public PagerWithOption<?> productPage(@PathVariable String resourceId,
                                          @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.PRODUCT, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/price/{resourceId}/page")
    @RequiresPermissions(PermissionConstants.PRICE_READ)
    @Operation(summary = "分页获取价格表详情页标签数据")
    public PagerWithOption<?> pricePage(@PathVariable String resourceId,
                                        @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.PRICE, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/quotation/{resourceId}/page")
    @CsPermission(value = PermissionConstants.OPPORTUNITY_QUOTATION_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.QUOTATION)
    @Operation(summary = "分页获取报价单详情页标签数据")
    public PagerWithOption<?> quotationPage(@PathVariable String resourceId,
                                            @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.QUOTATION, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/contract/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CONTRACT_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.CONTRACT)
    @Operation(summary = "分页获取合同详情页标签数据")
    public PagerWithOption<?> contractPage(@PathVariable String resourceId,
                                           @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.CONTRACT, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/invoice/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CONTRACT_INVOICE_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.INVOICE)
    @Operation(summary = "分页获取发票详情页标签数据")
    public PagerWithOption<?> invoicePage(@PathVariable String resourceId,
                                          @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.INVOICE, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/contractPaymentPlan/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CONTRACT_PAYMENT_PLAN_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.CONTRACT_PAYMENT_PLAN)
    @Operation(summary = "分页获取合同回款计划详情页标签数据")
    public PagerWithOption<?> contractPaymentPlanPage(@PathVariable String resourceId,
                                                      @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.CONTRACT_PAYMENT_PLAN, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/contractPaymentRecord/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CONTRACT_PAYMENT_RECORD_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.CONTRACT_PAYMENT_RECORD)
    @Operation(summary = "分页获取回款记录详情页标签数据")
    public PagerWithOption<?> contractPaymentRecordPage(@PathVariable String resourceId,
                                                        @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.CONTRACT_PAYMENT_RECORD, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/order/{resourceId}/page")
    @CsPermission(value = PermissionConstants.ORDER_READ,
            resourceId = "{#resourceId}", formType = FormKeyConstants.ORDER)
    @Operation(summary = "分页获取订单详情页标签数据")
    public PagerWithOption<?> orderPage(@PathVariable String resourceId,
                                        @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(FormKeyConstants.ORDER, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/{customFormId}/{resourceId}/page")
    @CsPermission(value = PermissionConstants.CUSTOM_FORM_READ)
    @Operation(summary = "分页获取自定义表单详情页标签数据")
    public PagerWithOption<?> orderPage(@PathVariable String customFormId, @PathVariable String resourceId,
                                        @Validated @RequestBody DetailTabPageRequest request) {
        return detailTabDataService.page(customFormId, resourceId, request,
                SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
