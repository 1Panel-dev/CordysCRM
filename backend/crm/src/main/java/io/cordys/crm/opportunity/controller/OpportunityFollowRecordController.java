package io.cordys.crm.opportunity.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import io.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import io.cordys.crm.follow.service.FollowUpRecordService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商机跟进记录")
@RestController
@RequestMapping("/opportunity/follow/record")
public class OpportunityFollowRecordController {

    @Resource
    private FollowUpRecordService followUpRecordService;

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_ADD)
    @Operation(summary = "添加商机跟进记录")
    public FollowUpRecord add(@Validated @RequestBody FollowUpRecordAddRequest request) {
        return followUpRecordService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)
    @Operation(summary = "更新商机跟进记录")
    public FollowUpRecord update(@Validated @RequestBody FollowUpRecordUpdateRequest request) {
        return followUpRecordService.update(request, SessionUtils.getUserId());
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.OPPORTUNITY_MANAGEMENT_ADD)
    @Operation(summary = "商机跟进记录列表")
    public Pager<List<FollowUpRecordListResponse>> list(@Validated @RequestBody FollowUpRecordPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, followUpRecordService.list(request, OrganizationContext.getOrganizationId(),"OPPORTUNITY","CUSTOMER"));
    }
}
