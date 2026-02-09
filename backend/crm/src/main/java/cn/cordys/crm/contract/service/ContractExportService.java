package cn.cordys.crm.contract.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportFieldParam;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.registry.ExportThreadRegistry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractExportService extends BaseExportService {

    @Resource
    private ContractService contractService;
    @Resource
    private ExtContractMapper extContractMapper;
    @Resource
    private ModuleFormService moduleFormService;

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) throws InterruptedException {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(new ArrayList<>()).mergeRegions(new ArrayList<>()).build();
        }
        var dataList = contractService.buildList(exportList, exportParam.getOrgId());
        moduleFormService.getBaseModuleFieldValues(dataList, ContractListResponse::getModuleFields);
        var exportFieldParam = exportParam.getExportFieldParam();
        return buildMergeResult(taskId, exportParam, dataList, exportFieldParam);
    }

    private List<ContractListResponse> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extContractMapper.getListByIds(exportParam.getSelectIds(), userId, orgId, deptDataPermission);
        }
        var request = (ContractPageRequest) exportParam.getPageRequest();
        return extContractMapper.list(request, orgId, userId, deptDataPermission, false);
    }

    private MergeResult buildMergeResult(String taskId,
                                         ExportDTO exportParam,
                                         List<ContractListResponse> dataList,
                                         ExportFieldParam exportFieldParam) throws InterruptedException {
        var data = new ArrayList<List<Object>>();
        var mergeRegions = new ArrayList<int[]>();
        int offset = 0;
        for (var detail : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("导出中断");
            }
            var buildData = buildData(detail, exportFieldParam, exportParam.getMergeHeads());
            if (buildData.size() > 1) {
                mergeRegions.add(new int[]{offset, offset + buildData.size() - 1});
            }
            offset += buildData.size();
            data.addAll(buildData);
        }
        return MergeResult.builder().mergeRegions(mergeRegions).dataList(data).build();
    }

    private List<List<Object>> buildData(ContractListResponse detail, ExportFieldParam exportFieldParam, List<String> heads) {
        return buildDataWithSub(detail.getModuleFields(), exportFieldParam, heads, getSystemFieldMap(detail));
    }

    public LinkedHashMap<String, Object> getSystemFieldMap(ContractListResponse data) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("departmentId", data.getDepartmentName());
        systemFieldMap.put("customerId", data.getCustomerName());
        systemFieldMap.put("amount", data.getAmount());
        systemFieldMap.put("alreadyPayAmount", data.getAlreadyPayAmount());
        systemFieldMap.put("number", data.getNumber());
        if (StringUtils.isNotBlank(data.getApprovalStatus())) {
            systemFieldMap.put("approvalStatus", Translator.get("contract.approval_status." + data.getApprovalStatus().toLowerCase()));
        }
        if (StringUtils.isNotBlank(data.getStage())) {
            systemFieldMap.put("stage", Translator.get("contract.stage." + data.getStage().toLowerCase()));
        }
        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        systemFieldMap.put("voidReason", data.getVoidReason());
        systemFieldMap.put("startTime", TimeUtils.getDateTimeStr(data.getStartTime()));
        systemFieldMap.put("endTime", TimeUtils.getDateTimeStr(data.getEndTime()));
        return systemFieldMap;
    }
}
