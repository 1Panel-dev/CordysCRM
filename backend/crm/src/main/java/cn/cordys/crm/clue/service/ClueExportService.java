package cn.cordys.crm.clue.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportFieldParam;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.utils.PoolClueFieldUtils;
import cn.cordys.crm.system.excel.domain.MergeResult;
import cn.cordys.crm.system.service.ModuleFormService;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ClueExportService extends BaseExportService {

    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ClueService clueService;
    @Resource
    private ModuleFormService moduleFormService;

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        // 构建自定义字段数据
        var dataList = clueService.buildListData(exportList, exportParam.getOrgId());
        // 构建选项数据
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(dataList, exportParam.getExportFieldParam());
        return buildExportMergeResult(taskId, exportParam, dataList, ClueListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        PoolClueFieldUtils.getSystemFieldMap(detail, optionMap), cache));
    }

    private List<ClueListResponse> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extClueMapper.getListByIds(exportParam.getSelectIds());
        }
        var request = (CluePageRequest) exportParam.getPageRequest();
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return extClueMapper.list(request, orgId, userId, deptDataPermission, false);
    }

    private Map<String, List<OptionDTO>> buildOptionMap(List<ClueListResponse> dataList,
                                                        ExportFieldParam exportFieldParam) {
        List<BaseModuleFieldValue> moduleFieldValues =
                moduleFormService.getBaseModuleFieldValues(dataList, ClueListResponse::getModuleFields);
        return moduleFormService.getOptionMap(exportFieldParam.getFormConfig(), moduleFieldValues);
    }
}
