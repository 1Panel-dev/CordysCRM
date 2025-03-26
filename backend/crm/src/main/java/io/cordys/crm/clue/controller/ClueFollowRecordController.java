package io.cordys.crm.clue.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import io.cordys.crm.follow.dto.response.FollowUpRecordDetailResponse;
import io.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import io.cordys.crm.follow.service.FollowUpRecordService;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "线索跟进记录")
@RestController
@RequestMapping("/clue/follow/record")
public class ClueFollowRecordController {

    @Resource
    private FollowUpRecordService followUpRecordService;

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_ADD)
    @Operation(summary = "添加线索跟进记录")
    public FollowUpRecord add(@Validated @RequestBody FollowUpRecordAddRequest request) {
        return followUpRecordService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_UPDATE)
    @Operation(summary = "更新线索跟进记录")
    public FollowUpRecord update(@Validated @RequestBody FollowUpRecordUpdateRequest request) {
        return followUpRecordService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/pool/page")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_POOL_READ)
    @Operation(summary = "线索池跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> poolList(@Validated @RequestBody FollowUpRecordPageRequest request) {
        return followUpRecordService.poolList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CLUE", "CLUE");
    }

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_READ)
    @Operation(summary = "线索跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> list(@Validated @RequestBody FollowUpRecordPageRequest request) {
        return followUpRecordService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CLUE", "CLUE", null);
    }


    @GetMapping("/get/{id}")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_POOL_READ}, logical = Logical.OR)
    @Operation(summary = "线索跟进记录详情")
    public FollowUpRecordDetailResponse get(@PathVariable String id) {
        return followUpRecordService.get(id, OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "线索删除跟进记录")
    @RequiresPermissions(PermissionConstants.CLUE_MANAGEMENT_DELETE)
    public void deleteRecord(@PathVariable String id) {
        followUpRecordService.delete(id);
    }
}
