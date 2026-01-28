package cn.cordys.crm.opportunity.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.common.utils.OpportunityFieldUtils;
import cn.cordys.crm.opportunity.dto.request.OpportunityExportRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.dto.response.StageConfigResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.registry.ExportThreadRegistry;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class OpportunityExportService extends BaseExportService {

  @Resource private OpportunityService opportunityService;
  @Resource private ExportTaskService exportTaskService;
  @Resource private ExtOpportunityMapper extOpportunityMapper;
  @Resource private ExtOpportunityStageConfigMapper extOpportunityStageConfigMapper;

  /**
   * 商机导出（条件导出）
   *
   * @param userId 用户ID
   * @param request 导出请求参数
   * @param orgId 组织ID
   * @param deptDataPermission 部门数据权限
   * @param locale 语言环境
   * @return 导出任务ID
   */
  public String export(
      String userId,
      OpportunityExportRequest request,
      String orgId,
      DeptDataPermissionDTO deptDataPermission,
      Locale locale) {

    checkFileName(request.getFileName());
    Objects.requireNonNull(userId, "userId 不能为空");
    exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

    String fileId = IDGenerator.nextStr();
    ExportTask exportTask =
        exportTaskService.saveTask(
            orgId,
            fileId,
            userId,
            ExportConstants.ExportType.OPPORTUNITY.toString(),
            request.getFileName());

    runExport(
        orgId,
        userId,
        LogModule.OPPORTUNITY_INDEX,
        locale,
        exportTask,
        request.getFileName(),
        () -> exportData(fileId, exportTask, userId, request, orgId, deptDataPermission));

    return exportTask.getId();
  }

  /**
   * 执行商机条件导出数据写入
   *
   * @param fileId 文件ID
   * @param exportTask 导出任务
   * @param userId 用户ID
   * @param request 导出请求
   * @param orgId 组织ID
   * @param deptDataPermission 部门数据权限
   * @throws InterruptedException 线程中断异常
   */
  private void exportData(
      String fileId,
      ExportTask exportTask,
      String userId,
      OpportunityExportRequest request,
      String orgId,
      DeptDataPermissionDTO deptDataPermission)
      throws InterruptedException {

    List<List<String>> headList =
        request.getHeadList().stream().map(h -> Collections.singletonList(h.getTitle())).toList();

    batchHandleData(
        fileId,
        headList,
        exportTask,
        request.getFileName(),
        request,
        t ->
            getExportData(
                request.getHeadList(),
                request,
                userId,
                orgId,
                deptDataPermission,
                exportTask.getId()));
  }

  /**
   * 构建商机条件导出数据
   *
   * @param headList 表头列表
   * @param request 导出请求
   * @param userId 用户ID
   * @param orgId 组织ID
   * @param deptDataPermission 部门数据权限
   * @param taskId 任务ID
   * @return 导出数据列表
   * @throws InterruptedException 线程中断异常
   */
  private List<List<Object>> getExportData(
      List<ExportHeadDTO> headList,
      OpportunityExportRequest request,
      String userId,
      String orgId,
      DeptDataPermissionDTO deptDataPermission,
      String taskId)
      throws InterruptedException {

    PageHelper.startPage(request.getCurrent(), request.getPageSize());
    List<OpportunityListResponse> rawList =
        extOpportunityMapper.list(request, orgId, userId, deptDataPermission, false);

    return buildOpportunityExportData(headList, rawList, orgId, taskId);
  }

  /**
   * 商机导出（选中数据）
   *
   * @param userId 用户ID
   * @param request 选中导出请求
   * @param orgId 组织ID
   * @param locale 语言环境
   * @return 导出任务ID
   */
  public String exportSelect(
      String userId, ExportSelectRequest request, String orgId, Locale locale) {
    checkFileName(request.getFileName());
    Objects.requireNonNull(userId, "userId 不能为空");
    exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

    String fileId = IDGenerator.nextStr();
    ExportTask exportTask =
        exportTaskService.saveTask(
            orgId,
            fileId,
            userId,
            ExportConstants.ExportType.OPPORTUNITY.toString(),
            request.getFileName());

    runExport(
        orgId,
        userId,
        LogModule.OPPORTUNITY_INDEX,
        locale,
        exportTask,
        request.getFileName(),
        () -> exportData(fileId, exportTask, userId, request, orgId));

    return exportTask.getId();
  }

  /**
   * 执行商机选中数据导出
   *
   * @param fileId 文件ID
   * @param exportTask 导出任务
   * @param userId 用户ID
   * @param request 选中导出请求
   * @param orgId 组织ID
   */
  private void exportData(
      String fileId,
      ExportTask exportTask,
      String userId,
      ExportSelectRequest request,
      String orgId) {

    List<List<String>> headList =
        request.getHeadList().stream().map(h -> Collections.singletonList(h.getTitle())).toList();

    File file = prepareExportFile(fileId, request.getFileName(), exportTask.getOrganizationId());
    try (ExcelWriter writer =
        EasyExcel.write(file).head(headList).excelType(ExcelTypeEnum.XLSX).build()) {

      WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();
      SubListUtils.dealForSubList(
          request.getIds(),
          SubListUtils.DEFAULT_EXPORT_BATCH_SIZE,
          subIds -> {
            try {
              writer.write(
                  getExportDataBySelect(request.getHeadList(), subIds, orgId, exportTask.getId()),
                  sheet);
            } catch (InterruptedException e) {
              log.error("任务停止中断", e);
              exportTaskService.update(
                  exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
            }
          });
    }
  }

  /**
   * 构建选中商机导出数据
   *
   * @param headList 表头列表
   * @param ids 商机ID列表
   * @param orgId 组织ID
   * @param taskId 任务ID
   * @return 导出数据列表
   * @throws InterruptedException 线程中断异常
   */
  private List<List<Object>> getExportDataBySelect(
      List<ExportHeadDTO> headList, List<String> ids, String orgId, String taskId)
      throws InterruptedException {

    List<OpportunityListResponse> rawList = extOpportunityMapper.getListByIds(ids);
    return buildOpportunityExportData(headList, rawList, orgId, taskId);
  }

  /**
   * 构建商机导出核心数据
   *
   * @param headList 表头列表
   * @param rawList 原始商机数据
   * @param orgId 组织ID
   * @param taskId 任务ID
   * @return 导出数据列表
   * @throws InterruptedException 线程中断异常
   */
  private List<List<Object>> buildOpportunityExportData(
      List<ExportHeadDTO> headList,
      List<OpportunityListResponse> rawList,
      String orgId,
      String taskId)
      throws InterruptedException {

    List<OpportunityListResponse> dataList = opportunityService.buildListData(rawList, orgId);
    Map<String, List<OptionDTO>> optionMap =
        opportunityService.buildOptionMap(orgId, rawList, dataList);
    Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.OPPORTUNITY.getKey(), orgId);
    Map<String, String> stageConfigMap =
        extOpportunityStageConfigMapper.getStageConfigList(orgId).stream()
            .collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName));

    List<List<Object>> result = new ArrayList<>(dataList.size());
    for (OpportunityListResponse response : dataList) {
      if (ExportThreadRegistry.isInterrupted(taskId)) {
        throw new InterruptedException("线程已被中断，主动退出");
      }
      result.add(buildData(headList, response, optionMap, fieldConfigMap, stageConfigMap));
    }
    return result;
  }

  /**
   * 构建单行商机导出数据
   *
   * @param headList 表头列表
   * @param data 商机数据
   * @param optionMap 选项映射
   * @param fieldConfigMap 字段配置
   * @param stageConfigMap 阶段配置
   * @return 单行导出数据
   */
  private List<Object> buildData(
      List<ExportHeadDTO> headList,
      OpportunityListResponse data,
      Map<String, List<OptionDTO>> optionMap,
      Map<String, BaseField> fieldConfigMap,
      Map<String, String> stageConfigMap) {

    List<Object> dataList = new ArrayList<>();
    LinkedHashMap<String, Object> systemFiledMap =
        OpportunityFieldUtils.getSystemFieldMap(data, optionMap, stageConfigMap);

    Map<String, Object> moduleFieldMap =
        Optional.ofNullable(data.getModuleFields())
            .map(
                fields ->
                    fields.stream()
                        .collect(
                            Collectors.toMap(
                                BaseModuleFieldValue::getFieldId,
                                BaseModuleFieldValue::getFieldValue)))
            .orElseGet(LinkedHashMap::new);

    transModuleFieldValue(headList, systemFiledMap, moduleFieldMap, dataList, fieldConfigMap);
    return dataList;
  }
}
