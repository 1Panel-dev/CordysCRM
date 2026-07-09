package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.pager.PagerWithOption;
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

@Tag(name = "线索跟进记录")
@RestController
@RequestMapping("/lead/follow/record")
public class ClueFollowRecordController {

    @Resource
    private FollowUpRecordService followUpRecordService;

    @PostMapping("/add")
    @Operation(summary = "添加线索跟进记录")
    public FollowUpRecord add(@Validated @RequestBody FollowUpRecordAddRequest request) {
        followUpRecordService.checkRecordPermission(BeanUtils.copyBean(new FollowUpRecord(), request),
                OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), false);
        return followUpRecordService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @Operation(summary = "更新线索跟进记录")
    public FollowUpRecord update(@Validated @RequestBody FollowUpRecordUpdateRequest request) {
        followUpRecordService.checkUpdatePermission(request.getId(), SessionUtils.getUserId());
        return followUpRecordService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/pool/page")
    @Operation(summary = "线索池跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> poolList(@Validated @RequestBody FollowUpRecordPageRequest request) {
        FollowUpRecord followUpRecord = new FollowUpRecord();
        followUpRecord.setClueId(request.getSourceId());
        followUpRecord.setType(ModuleKey.CLUE.name());
        followUpRecordService.checkRecordPermission(followUpRecord, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        return followUpRecordService.poolList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CLUE", "CLUE");
    }

    @PostMapping("/page")
    @Operation(summary = "线索跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> list(@Validated @RequestBody FollowUpRecordPageRequest request) {
        FollowUpRecord followUpRecord = new FollowUpRecord();
        followUpRecord.setClueId(request.getSourceId());
        followUpRecord.setType(ModuleKey.CLUE.name());
        followUpRecordService.checkRecordPermission(followUpRecord, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_RECORD.getKey());
        return followUpRecordService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), "CLUE", "CLUE");
    }


    @GetMapping("/get/{id}")
    @Operation(summary = "线索跟进记录详情")
    public FollowUpRecordDetailResponse get(@PathVariable String id) {
        followUpRecordService.checkRecordPermission(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        return followUpRecordService.get(id, OrganizationContext.getOrganizationId());
    }


    @GetMapping("/delete/{id}")
    @Operation(summary = "线索删除跟进记录")
    public void deleteRecord(@PathVariable String id) {
        followUpRecordService.checkUpdatePermission(id, SessionUtils.getUserId());
        followUpRecordService.delete(id);
    }
}
