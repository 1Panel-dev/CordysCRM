package cn.cordys.crm.follow.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.follow.dto.request.PlanHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Service
public class FollowUpPlanExportService extends BaseExportService {

    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private ExtFollowUpPlanMapper extFollowUpPlanMapper;

    @Override
    public MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        List<FollowUpPlanListResponse> exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        List<FollowUpPlanListResponse> dataList = followUpPlanService.buildListData(exportList, exportParam.getOrgId());
        return buildExportMergeResult(taskId, exportParam, dataList, FollowUpPlanListResponse::getModuleFields,
                (data, fieldParam, metas, cache) -> buildDataWithSub(data.getModuleFields(), fieldParam, metas,
                        getSystemFieldMap(data), cache));
    }

    private List<FollowUpPlanListResponse> collectExportList(ExportDTO exportParam) {
        PlanHomePageRequest request = (PlanHomePageRequest) exportParam.getPageRequest();
        if (request == null) {
            request = new PlanHomePageRequest();
        }
        if (CollectionUtils.isEmpty(exportParam.getSelectIds())) {
            PageHelper.startPage(request.getCurrent(), request.getPageSize(), false);
            return extFollowUpPlanMapper.selectTotalList(request, exportParam.getUserId(), exportParam.getOrgId(),
                    exportParam.getDeptDataPermission());
        }
        Set<String> ids = Set.copyOf(exportParam.getSelectIds());
        return extFollowUpPlanMapper.selectTotalList(request, exportParam.getUserId(), exportParam.getOrgId(),
                exportParam.getDeptDataPermission()).stream().filter(data -> ids.contains(data.getId())).toList();
    }

    private LinkedHashMap<String, Object> getSystemFieldMap(FollowUpPlanListResponse data) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("id", data.getId());
        result.put("customerId", data.getCustomerName());
        result.put("opportunityId", data.getOpportunityName());
        result.put("clueId", data.getClueName());
        result.put("type", data.getType());
        result.put("content", data.getContent());
        result.put("estimatedTime", TimeUtils.getDateTimeStr(data.getEstimatedTime()));
        result.put("method", data.getMethod());
        result.put("status", data.getStatus());
        result.put("owner", data.getOwnerName());
        result.put("contactId", data.getContactName());
        result.put("createUser", data.getCreateUserName());
        result.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        result.put("updateUser", data.getUpdateUserName());
        result.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        return result;
    }
}
