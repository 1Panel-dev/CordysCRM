package cn.cordys.crm.clue.service;

import cn.cordys.common.dto.ChartAnalysisDbRequest;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.service.BaseChartService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.dto.request.ClueChartAnalysisDbRequest;
import cn.cordys.crm.customer.dto.request.PoolClueChartAnalysisRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CluePoolExportService extends ClueExportService {

    @Resource
    private ClueService clueService;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private BaseChartService baseChartService;

    public List<ChartResult> chart(PoolClueChartAnalysisRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = clueService.getFormConfig(orgId);
        formConfig.getFields().addAll(BaseChartService.getChartBaseFields());
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request, formConfig);
        ClueChartAnalysisDbRequest clueChartAnalysisDbRequest = BeanUtils.copyBean(new ClueChartAnalysisDbRequest(), chartAnalysisDbRequest);
        clueChartAnalysisDbRequest.setPoolId(request.getPoolId());
        List<ChartResult> chartResults = extClueMapper.chart(clueChartAnalysisDbRequest, userId, orgId, deptDataPermission);
        return baseChartService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
    }
}
