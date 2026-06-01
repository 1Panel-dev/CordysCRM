package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.CustomerDataDTO;
import cn.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import cn.cordys.crm.follow.dto.response.FollowUpRecordDetailResponse;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "客户跟进记录")
@RestController
@RequestMapping("/account/follow/record")
public class CustomerFollowRecordController {

    @Resource
    private FollowUpRecordService followUpRecordService;

    @PostMapping("/add")
    @CsPermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "添加客户跟进记录")
    public FollowUpRecord add(@Validated @RequestBody FollowUpRecordAddRequest request) {
        return followUpRecordService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, resourceId = "{#request.id}", formType = FormKeyConstants.FOLLOW_RECORD)
    @Operation(summary = "更新客户跟进记录")
    public FollowUpRecord update(@Validated @RequestBody FollowUpRecordUpdateRequest request) {
        return followUpRecordService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/pool/page")
    @CsPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ)
    @Operation(summary = "客户公海池跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> poolList(@Validated @RequestBody FollowUpRecordPageRequest request) {
        return followUpRecordService.poolList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CUSTOMER", "CUSTOMER");
    }

    @PostMapping("/page")
    @CsPermission(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
    @Operation(summary = "客户跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> list(@Validated @RequestBody FollowUpRecordPageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_RECORD.getKey());
        CustomerDataDTO customerData = followUpRecordService.getCustomerPermission(SessionUtils.getUserId(),
                request.getSourceId(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return followUpRecordService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CUSTOMER", "CUSTOMER", customerData);
    }


    @GetMapping("/get/{id}")
    @RequiresPermissions(value = {PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ}, logical = Logical.OR)
    @Operation(summary = "客户跟进记录详情")
    public FollowUpRecordDetailResponse get(@PathVariable String id) {
        return followUpRecordService.get(id, OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "客户删除跟进记录")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, resourceId = "{#id}", formType = FormKeyConstants.FOLLOW_RECORD)
    public void deleteRecord(@PathVariable String id) {
        followUpRecordService.delete(id);
    }
}
