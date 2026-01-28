package cn.cordys.crm.contract.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.DeptDataPermissionDTO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractExportService extends BaseExportService {

  @Resource private ContractService contractService;
  @Resource private ExtContractMapper extContractMapper;
  @Resource private ModuleFormService moduleFormService;

  @Override
  protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam)
      throws InterruptedException {
    String orgId = exportParam.getOrgId();
    String userId = exportParam.getUserId();
    DeptDataPermissionDTO deptDataPermission = exportParam.getDeptDataPermission();
    List<ContractListResponse> exportList;
    if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
      exportList =
          extContractMapper.getListByIds(
              exportParam.getSelectIds(), userId, orgId, deptDataPermission);
    } else {
      ContractPageRequest request = (ContractPageRequest) exportParam.getPageRequest();
      exportList = extContractMapper.list(request, orgId, userId, deptDataPermission, false);
    }
    if (CollectionUtils.isEmpty(exportList)) {
      return MergeResult.builder()
          .dataList(new ArrayList<>())
          .mergeRegions(new ArrayList<>())
          .build();
    }
    List<ContractListResponse> dataList = contractService.buildList(exportList, orgId);
    List<BaseModuleFieldValue> moduleFieldValues =
        moduleFormService.getBaseModuleFieldValues(dataList, ContractListResponse::getModuleFields);
    ExportFieldParam exportFieldParam = exportParam.getExportFieldParam();
    // 构建导出数据
    List<List<Object>> data = new ArrayList<>();
    List<int[]> mergeRegions = new ArrayList<>();
    int offset = 0;
    for (ContractListResponse response : dataList) {
      if (ExportThreadRegistry.isInterrupted(taskId)) {
        throw new InterruptedException("导出中断");
      }
      List<List<Object>> buildData =
          buildData(response, exportFieldParam, exportParam.getMergeHeads());
      if (buildData.size() > 1) {
        // 多行需要合并
        mergeRegions.add(new int[] {offset, offset + buildData.size() - 1});
      }
      offset += buildData.size();
      data.addAll(buildData);
    }
    return MergeResult.builder().mergeRegions(mergeRegions).dataList(data).build();
  }

  private List<List<Object>> buildData(
      ContractListResponse detail, ExportFieldParam exportFieldParam, List<String> heads) {
    return buildDataWithSub(
        detail.getModuleFields(), exportFieldParam, heads, getSystemFieldMap(detail));
  }

  public LinkedHashMap<String, Object> getSystemFieldMap(ContractListResponse data) {
    LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
    systemFiledMap.put("name", data.getName());
    systemFiledMap.put("owner", data.getOwnerName());
    systemFiledMap.put("departmentId", data.getDepartmentName());
    systemFiledMap.put("customerId", data.getCustomerName());
    systemFiledMap.put("amount", data.getAmount());
    systemFiledMap.put("alreadyPayAmount", data.getAlreadyPayAmount());
    systemFiledMap.put("number", data.getNumber());
    if (StringUtils.isNotBlank(data.getApprovalStatus())) {
      systemFiledMap.put(
          "approvalStatus",
          Translator.get("contract.approval_status." + data.getApprovalStatus().toLowerCase()));
    }
    if (StringUtils.isNotBlank(data.getStage())) {
      systemFiledMap.put(
          "stage", Translator.get("contract.stage." + data.getStage().toLowerCase()));
    }
    systemFiledMap.put("createUser", data.getCreateUserName());
    systemFiledMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
    systemFiledMap.put("updateUser", data.getUpdateUserName());
    systemFiledMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
    systemFiledMap.put("voidReason", data.getVoidReason());
    return systemFiledMap;
  }
}
