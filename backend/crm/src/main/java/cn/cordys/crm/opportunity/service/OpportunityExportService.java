package cn.cordys.crm.opportunity.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.utils.OpportunityFieldUtils;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.dto.response.StageConfigResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class OpportunityExportService extends BaseExportService {

    private static final String STAGE_CONFIG_MAP_KEY = "stageConfigMap";

    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtOpportunityStageConfigMapper extOpportunityStageConfigMapper;

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        // 从缓存获取阶段配置，避免重复查询
        Map<String, String> stageConfigMap = getOrLoadStageConfigMap(exportParam);
        return buildExportMergeResult(taskId, exportParam, exportList,
                OpportunityListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        OpportunityFieldUtils.getSystemFieldMap(detail, null, stageConfigMap, null), cache));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getOrLoadStageConfigMap(ExportDTO exportParam) {
        return (Map<String, String>) exportParam.getExtraParams()
                .computeIfAbsent(STAGE_CONFIG_MAP_KEY, key ->
                        extOpportunityStageConfigMapper.getStageConfigList(exportParam.getOrgId())
                                .stream().collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName)));
    }

    private List<OpportunityListResponse> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extOpportunityMapper.getListByIds(exportParam.getSelectIds());
        }
        var request = (OpportunityPageRequest) exportParam.getPageRequest();
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return extOpportunityMapper.list(request, orgId, userId, deptDataPermission, false);
    }
}
