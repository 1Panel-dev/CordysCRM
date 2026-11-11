package cn.cordys.crm.follow.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import cn.cordys.crm.follow.dto.request.RecordHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpRecordDetailResponse;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "跟进记录统一页面")
@RestController
@RequestMapping("/follow/record")
public class FollowUpRecordController {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private DataScopeService dataScopeService;

    @GetMapping("/module/form")
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_RECORD.getKey(),
                OrganizationContext.getOrganizationId());
    }

    @GetMapping("/tab")
    @CsPermission(PermissionConstants.FOLLOW_RECORD_READ)
    @Operation(summary = "数据权限TAB")
    public ResourceTabEnableDTO getTabEnableConfig() {
        return followUpRecordService.getTabEnableConfig(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/page")
    @CsPermission(PermissionConstants.FOLLOW_RECORD_READ)
    @Operation(summary = "跟进记录列表")
    public PagerWithOption<List<FollowUpRecordListResponse>> list(
            @Validated @RequestBody RecordHomePageRequest request) {
        ConditionFilterUtils.parseCondition(request, FormKey.FOLLOW_RECORD.getKey());
        DeptDataPermissionDTO dataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.FOLLOW_RECORD_READ);
        return followUpRecordService.totalList(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), dataPermission);
    }

    @GetMapping("/delete/{id}")
    @CsPermission(value = PermissionConstants.FOLLOW_RECORD_DELETE, resourceId = "{#id}", formType = FormKeyConstants.FOLLOW_RECORD)
    @Operation(summary = "删除跟进记录")
    public void delete(@PathVariable String id) {
        followUpRecordService.delete(id);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "跟进记录详情")
    @CsPermission(PermissionConstants.FOLLOW_RECORD_READ)
    public FollowUpRecordDetailResponse get(@PathVariable String id) {
        followUpRecordService.checkReadPermission(id, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
        return followUpRecordService.get(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @CsPermission(PermissionConstants.FOLLOW_RECORD_ADD)
    @Operation(summary = "添加跟进记录")
    public FollowUpRecord add(@Validated @RequestBody FollowUpRecordAddRequest request) {
        return followUpRecordService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @CsPermission(value = PermissionConstants.FOLLOW_RECORD_UPDATE, resourceId = "{#request.id}", formType = FormKeyConstants.FOLLOW_RECORD)
    @Operation(summary = "更新跟进记录")
    public FollowUpRecord update(@Validated @RequestBody FollowUpRecordUpdateRequest request) {
        return followUpRecordService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

}
