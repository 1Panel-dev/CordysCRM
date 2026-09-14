package cn.cordys.crm.order.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.FieldExportMeta;
import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.order.dto.request.OrderPageRequest;
import cn.cordys.crm.order.dto.response.OrderListResponse;
import cn.cordys.crm.order.mapper.ExtOrderMapper;
import cn.cordys.crm.order.mapper.ExtOrderStageConfigMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class OrderExportService extends BaseExportService {
    private static final String STAGE_CONFIG_MAP_KEY = "stageConfigMap";
    @Resource
    private OrderService orderService;
    @Resource
    private ExtOrderMapper extOrderMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ExtOrderStageConfigMapper extOrderStageConfigMapper;

    /**
     * 构建导出的数据
     *
     * @return 导出数据列表
     */
    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var queryResult = collectExportList(exportParam);
        var filteredList = queryResult.getLeft();
        var queryCount = queryResult.getRight();
        if (CollectionUtils.isEmpty(filteredList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).queryCount(queryCount).build();
        }
        var dataList = orderService.buildList(filteredList, exportParam.getOrgId());
        // 从缓存获取阶段配置，避免重复查询
        Map<String, String> stageConfigMap = getOrLoadStageConfigMap(exportParam);
        var result = buildExportMergeResult(taskId, exportParam, dataList,
                OrderListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        getSystemFieldMap(detail, metas, stageConfigMap, exportParam.getLocale()), cache));
        result.setQueryCount(queryCount);
        return result;
    }


    private Pair<List<OrderListResponse>, Integer> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        List<OrderListResponse> exportList;
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            exportList = extOrderMapper.getListByIds(exportParam.getSelectIds(), userId, orgId, deptDataPermission);
            List<OrderListResponse> orderListResponses = filterExportPermission(exportList, orgId);
            return Pair.of(orderListResponses, orderListResponses.size());
        } else {
            var request = (OrderPageRequest) exportParam.getPageRequest();
            PageHelper.startPage(request.getCurrent(), request.getPageSize(), false);
            exportList = extOrderMapper.list(request, orgId, userId, deptDataPermission, false);
            int queryCount = exportList.size();
            var filtered = filterExportPermission(exportList, orgId);
            return Pair.of(filtered, queryCount);
        }
    }

    private List<OrderListResponse> filterExportPermission(List<OrderListResponse> exportList, String orgId) {
        return filterApprovalExportPermission(exportList, orgId, FormKey.ORDER.getKey(),
                OrderListResponse::getId, OrderListResponse::getApprovalStatus,
                approvalFlowService);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getOrLoadStageConfigMap(ExportDTO exportParam) {
        return (Map<String, String>) exportParam.getExtraParams()
                .computeIfAbsent(STAGE_CONFIG_MAP_KEY, key ->
                        extOrderStageConfigMapper.getStageConfigList(exportParam.getOrgId())
                                .stream().collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName)));
    }


    public LinkedHashMap<String, Object> getSystemFieldMap(OrderListResponse data, List<FieldExportMeta> exportMetas, Map<String, String> stageConfigMap, Locale locale) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("id", data.getId());
        systemFieldMap.put("number", data.getNumber());
        systemFieldMap.put("customerId", data.getCustomerName());
        systemFieldMap.put("contractId", data.getContractName());
        systemFieldMap.put("departmentId", data.getDepartmentName());
        systemFieldMap.put("owner", data.getOwnerName());
        if (StringUtils.isNotBlank(data.getStage())) {
            systemFieldMap.put("stage", stageConfigMap.get(data.getStage()));
        }
        if (StringUtils.isNotBlank(data.getApprovalStatus())) {
            systemFieldMap.put("approvalStatus", Translator.get("contract.approval_status." + data.getApprovalStatus().toLowerCase(), locale));
        }
        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));

        Map<String, FieldExportMeta> metaMap = exportMetas.stream()
                .collect(Collectors.toMap(FieldExportMeta::getBusinessKey, Function.identity(), (a, b) -> a));
        resolveAndPutTimeField(systemFieldMap, metaMap, "amount", data.getAmount());
        return systemFieldMap;
    }
}
