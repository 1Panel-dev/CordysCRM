package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.FieldExportMeta;
import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractStageConfigMapper;
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
public class ContractExportService extends BaseExportService {

    private static final String STAGE_CONFIG_MAP_KEY = "stageConfigMap";

    @Resource
    private ContractService contractService;
    @Resource
    private ExtContractMapper extContractMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ExtContractStageConfigMapper extContractStageConfigMapper;

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var queryResult = collectExportList(exportParam);
        var filteredList = queryResult.getLeft();
        var queryCount = queryResult.getRight();
        if (CollectionUtils.isEmpty(filteredList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).queryCount(queryCount).build();
        }
        var dataList = contractService.buildList(filteredList, exportParam.getOrgId());
        // 从缓存获取阶段配置，避免重复查询
        Map<String, String> stageConfigMap = getOrLoadStageConfigMap(exportParam);
        var result = buildExportMergeResult(taskId, exportParam, dataList,
                ContractListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        getSystemFieldMap(detail, metas, stageConfigMap), cache));
        result.setQueryCount(queryCount);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getOrLoadStageConfigMap(ExportDTO exportParam) {
        return (Map<String, String>) exportParam.getExtraParams()
                .computeIfAbsent(STAGE_CONFIG_MAP_KEY, key ->
                        extContractStageConfigMapper.getStageConfigList(exportParam.getOrgId())
                                .stream().collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName)));
    }

    private Pair<List<ContractListResponse>, Integer> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        List<ContractListResponse> exportList;
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            exportList = extContractMapper.getListByIds(exportParam.getSelectIds(), userId, orgId, deptDataPermission);
            return Pair.of(exportList, exportList.size());
        } else {
            var request = (ContractPageRequest) exportParam.getPageRequest();
            int offset = (request.getCurrent() - 1) * request.getPageSize();
            PageHelper.startPage(offset, request.getPageSize());
            exportList = extContractMapper.list(request, orgId, userId, deptDataPermission, false);
            int queryCount = exportList.size();
            var filtered = filterExportPermission(exportList, orgId);
            return Pair.of(filtered, queryCount);
        }
    }

    private List<ContractListResponse> filterExportPermission(List<ContractListResponse> exportList, String orgId) {
        return filterApprovalExportPermission(exportList, orgId, FormKey.CONTRACT.getKey(),
                ContractListResponse::getId, ContractListResponse::getApprovalStatus,
                approvalFlowService);
    }

    public LinkedHashMap<String, Object> getSystemFieldMap(ContractListResponse data, List<FieldExportMeta> exportMetas, Map<String, String> stageConfigMap) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("departmentId", data.getDepartmentName());
        systemFieldMap.put("customerId", data.getCustomerName());
        systemFieldMap.put("amount", data.getAmount());
        systemFieldMap.put("alreadyPayAmount", data.getAlreadyPayAmount());
        systemFieldMap.put("number", data.getNumber());

        if (StringUtils.isNotBlank(data.getApprovalStatus())) {
            systemFieldMap.put("approvalStatus", Translator.get("contract.approval_status." + data.getApprovalStatus().toLowerCase(), Locale.SIMPLIFIED_CHINESE));
        }
        if (StringUtils.isNotBlank(data.getStage())) {
            systemFieldMap.put("stage", stageConfigMap.get(data.getStage()));
        }

        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        systemFieldMap.put("voidReason", data.getVoidReason());

        Map<String, FieldExportMeta> metaMap = exportMetas.stream()
                .collect(Collectors.toMap(FieldExportMeta::getBusinessKey, Function.identity(), (a, b) -> a));

        resolveAndPutTimeField(systemFieldMap, metaMap, "startTime", String.valueOf(data.getStartTime()));
        resolveAndPutTimeField(systemFieldMap, metaMap, "endTime", String.valueOf(data.getEndTime()));

        return systemFieldMap;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void resolveAndPutTimeField(LinkedHashMap<String, Object> map,
                                        Map<String, FieldExportMeta> metaMap,
                                        String businessKey,
                                        String rawValue) {
        FieldExportMeta meta = metaMap.get(businessKey);
        if (meta != null) {
            AbstractModuleFieldResolver resolver = ModuleFieldResolverFactory.getResolver(meta.getField().getType());
            map.put(businessKey, resolver.transformToValue(meta.getField(), rawValue));
        }
    }
}
