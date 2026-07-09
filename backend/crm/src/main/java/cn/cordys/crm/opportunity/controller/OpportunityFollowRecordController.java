package cn.cordys.crm.opportunity.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.domain.FollowUpRecord;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商机跟进记录")
@RestController
@RequestMapping("/opportunity/follow/record")
public class OpportunityFollowRecordController {

    @Resource
    private FollowUpRecordService followUpRecordService;

    @PostMapping("/add")
    @CsPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE)
    @Operation(summary = "添加商机跟进记录")
    public FollowUpRecord add(@Validated @RequestBody FollowUpRecordAddRequest request) {
        followUpRecordService.checkRecordPermission(BeanUtils.copyBean(new FollowUpRecord(), request),
                OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), false);
        return followUpRecordService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @Operation(summary = "更新商机跟进记录")
    public FollowUpRecord update(@Validated @RequestBody FollowUpRecordUpdateRequest request) {
        followUpRecordService.checkUpdatePermission(request.getId(), SessionUtils.getUserId());
        return followUpRecordService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @CsPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
    @Operation(summary = "商机跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> list(@Validated @RequestBody FollowUpRecordPageRequest request) {
        FollowUpRecord followUpRecord = new FollowUpRecord();
        followUpRecord.setOpportunityId(request.getSourceId());
        // 这里类型就是客户，没有商机类型
        followUpRecord.setType(ModuleKey.CUSTOMER.name());
        followUpRecordService.checkRecordPermission(followUpRecord, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_RECORD.getKey());
        return followUpRecordService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "OPPORTUNITY", "CUSTOMER");
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "商机跟进记录详情")
    public FollowUpRecordDetailResponse get(@PathVariable String id) {
        followUpRecordService.checkRecordPermission(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        return followUpRecordService.get(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "客户删除跟进记录")
    public void deleteRecord(@PathVariable String id) {
        followUpRecordService.checkUpdatePermission(id, SessionUtils.getUserId());
        followUpRecordService.delete(id);
    }
}
