package cn.cordys.crm.follow.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.FieldExportMeta;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.follow.dto.request.RecordHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Service
public class FollowUpRecordExportService extends BaseExportService {

    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private ExtFollowUpRecordMapper extFollowUpRecordMapper;

    @Override
    public MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        List<FollowUpRecordListResponse> exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        List<FollowUpRecordListResponse> dataList = followUpRecordService.buildListData(exportList, exportParam.getOrgId());
        return buildExportMergeResult(taskId, exportParam, dataList, FollowUpRecordListResponse::getModuleFields,
                (data, fieldParam, metas, cache) -> buildDataWithSub(data.getModuleFields(), fieldParam, metas,
                        getSystemFieldMap(data), cache));
    }

    private List<FollowUpRecordListResponse> collectExportList(ExportDTO exportParam) {
        RecordHomePageRequest request = (RecordHomePageRequest) exportParam.getPageRequest();
        if (request == null) {
            request = new RecordHomePageRequest();
        }
        if (CollectionUtils.isEmpty(exportParam.getSelectIds())) {
            PageHelper.startPage(request.getCurrent(), request.getPageSize(), false);
            return extFollowUpRecordMapper.selectTotalList(request, exportParam.getUserId(), exportParam.getOrgId(),
                    exportParam.getDeptDataPermission());
        }
        Set<String> ids = Set.copyOf(exportParam.getSelectIds());
        return extFollowUpRecordMapper.selectTotalList(request, exportParam.getUserId(), exportParam.getOrgId(),
                exportParam.getDeptDataPermission()).stream().filter(data -> ids.contains(data.getId())).toList();
    }

    private LinkedHashMap<String, Object> getSystemFieldMap(FollowUpRecordListResponse data) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("id", data.getId());
        result.put("customerId", data.getCustomerName());
        result.put("opportunityId", data.getOpportunityName());
        result.put("clueId", data.getClueName());
        result.put("type", data.getType());
        result.put("content", data.getContent());
        result.put("followTime", TimeUtils.getDateTimeStr(data.getFollowTime()));
        result.put("followMethod", data.getFollowMethod());
        result.put("owner", data.getOwnerName());
        result.put("departmentId", data.getDepartmentName());
        result.put("contactId", data.getContactName());
        result.put("createUser", data.getCreateUserName());
        result.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        result.put("updateUser", data.getUpdateUserName());
        result.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        return result;
    }
}
