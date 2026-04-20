package cn.cordys.crm.contract.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportFieldParam;
import cn.cordys.common.dto.FieldExportMeta;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.registry.ExportThreadRegistry;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ContractExportService extends BaseExportService {

    @Resource
    private ContractService contractService;
    @Resource
    private ExtContractMapper extContractMapper;
    @Resource
    private ModuleFormService moduleFormService;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(new ArrayList<>()).mergeRegions(new ArrayList<>()).handleCount(0).build();
        }
        var dataList = contractService.buildList(exportList, exportParam.getOrgId());
        moduleFormService.getBaseModuleFieldValues(dataList, ContractListResponse::getModuleFields);
        var exportFieldParam = exportParam.getExportFieldParam();
        return parallelBuildMergeResult(taskId, exportParam, dataList, exportFieldParam);
    }

    private List<ContractListResponse> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extContractMapper.getListByIds(exportParam.getSelectIds(), userId, orgId, deptDataPermission);
        }
        var request = (ContractPageRequest) exportParam.getPageRequest();
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return extContractMapper.list(request, orgId, userId, deptDataPermission, false);
    }

    /**
     * 并行构建导出数据及合并区域
     *
     * @param taskId           导出任务ID
     * @param exportParam      导出参数
     * @param dataList         数据列表
     * @param exportFieldParam 导出字段参数
     * @return 合并结果
     */
    private MergeResult parallelBuildMergeResult(String taskId, ExportDTO exportParam, List<ContractListResponse> dataList,
                                                 ExportFieldParam exportFieldParam) {

        int size = dataList.size();
        List<List<Object>> mergeRowData = new ArrayList<>(size);
        List<int[]> mergeRegions = new ArrayList<>();

        // 任务列表 - 每个任务处理一行数据，构建该行的导出数据 Pair<位置索引, 行数据>
        List<Future<Pair<Integer, List<List<Object>>>>> futures = new ArrayList<>(3);
        for (int i = 0; i < size; i++) {
            final int idx = i;
            ContractListResponse detail = dataList.get(i);
            futures.add(executor.submit(() -> {
                if (ExportThreadRegistry.isInterrupted(taskId)) {
                    throw new InterruptedException("导出中断");
                }
                List<List<Object>> buildData = buildData(detail, exportFieldParam, exportParam.getExportMetas());
                return Pair.of(idx, buildData);
            }));
        }

        // 收集结果 (阻塞)
        List<Pair<Integer, List<List<Object>>>> results = new ArrayList<>(size);
        for (Future<Pair<Integer, List<List<Object>>>> f : futures) {
            try {
                Pair<Integer, List<List<Object>>> pairData = f.get();
                results.add(pairData);
            } catch (Exception e) {
                log.error("Parse row data error: {}", e.getMessage());
            }
        }

        // 按原始索引排序 (很重要, 否则合并区域错乱)
        results.sort(Comparator.comparingInt(Pair::getLeft));

        // 构建合并区域
        int offset = 0;
        for (Pair<Integer, List<List<Object>>> r : results) {
            List<List<Object>> buildData = r.getRight();
            if (buildData.size() > 1) {
                mergeRegions.add(new int[]{offset, offset + buildData.size() - 1});
            }
            offset += buildData.size();
            mergeRowData.addAll(buildData);
        }

        // 返回合并的结构
        return MergeResult.builder().mergeRegions(mergeRegions).dataList(mergeRowData).handleCount(size).build();
    }

    private List<List<Object>> buildData(ContractListResponse detail, ExportFieldParam exportFieldParam, List<FieldExportMeta> exportMetas) {
        return buildDataWithSub(detail.getModuleFields(), exportFieldParam, exportMetas, getSystemFieldMap(detail, exportMetas));
    }

    public LinkedHashMap<String, Object> getSystemFieldMap(ContractListResponse data, List<FieldExportMeta> exportMetas) {
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
            systemFieldMap.put("stage", Translator.get("contract.stage." + data.getStage().toLowerCase(), Locale.SIMPLIFIED_CHINESE));
        }
        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        systemFieldMap.put("voidReason", data.getVoidReason());

        FieldExportMeta startTime = exportMetas.stream().filter(field -> Strings.CI.equals(field.getBusinessKey(), "startTime")).findFirst().orElse(null);
        if (startTime != null) {
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(startTime.getField().getType());
            systemFieldMap.put("startTime", customFieldResolver.transformToValue(startTime.getField(), String.valueOf(data.getStartTime())));
        }
        FieldExportMeta endTime = exportMetas.stream().filter(field -> Strings.CI.equals(field.getBusinessKey(), "endTime")).findFirst().orElse(null);
        if (endTime != null) {
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(endTime.getField().getType());
            systemFieldMap.put("endTime", customFieldResolver.transformToValue(endTime.getField(), String.valueOf(data.getEndTime())));
        }
        return systemFieldMap;
    }
}
