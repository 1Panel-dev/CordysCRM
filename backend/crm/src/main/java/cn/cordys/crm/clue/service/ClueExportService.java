package cn.cordys.crm.clue.service;

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
import cn.cordys.crm.clue.dto.request.ClueExportRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.utils.PoolClueFieldUtils;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ClueExportService extends BaseExportService {

  @Resource private ClueService clueService;
  @Resource private ExtClueMapper extClueMapper;
  @Resource private ExportTaskService exportTaskService;

  public String exportAll(
      ClueExportRequest request,
      String userId,
      String orgId,
      DeptDataPermissionDTO dataPermission,
      Locale locale) {
    checkFileName(request.getFileName());
    // 用户导出数量 限制
    exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

    String fileId = IDGenerator.nextStr();
    ExportTask exportTask =
        exportTaskService.saveTask(
            orgId,
            fileId,
            userId,
            ExportConstants.ExportType.CLUE.toString(),
            request.getFileName());

    runExport(
        orgId,
        userId,
        LogModule.CLUE_INDEX,
        locale,
        exportTask,
        request.getFileName(),
        () -> exportData(fileId, exportTask, userId, request, orgId, dataPermission));

    return exportTask.getId();
  }

  private void exportData(
      String fileId,
      ExportTask exportTask,
      String userId,
      ClueExportRequest request,
      String orgId,
      DeptDataPermissionDTO dataPermission)
      throws InterruptedException {
    // 表头信息
    List<List<String>> headList =
        request.getHeadList().stream()
            .map(head -> Collections.singletonList(head.getTitle()))
            .toList();

    // 分批查询数据并写入文件
    batchHandleData(
        fileId,
        headList,
        exportTask,
        request.getFileName(),
        request,
        t -> getExportData(request, userId, orgId, dataPermission, exportTask.getId()));
  }

  public String exportSelect(
      ExportSelectRequest request, String userId, String orgId, Locale locale) {
    checkFileName(request.getFileName());
    // 用户导出数量限制
    exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

    String fileId = IDGenerator.nextStr();
    ExportTask exportTask =
        exportTaskService.saveTask(
            orgId,
            fileId,
            userId,
            ExportConstants.ExportType.CLUE.toString(),
            request.getFileName());

    runExport(
        orgId,
        userId,
        LogModule.CLUE_INDEX,
        locale,
        exportTask,
        request.getFileName(),
        () -> exportData(exportTask, userId, request, orgId, fileId));

    return exportTask.getId();
  }

  private void exportData(
      ExportTask exportTask,
      String userId,
      ExportSelectRequest request,
      String orgId,
      String fileId) {
    // 表头信息
    List<List<String>> headList =
        request.getHeadList().stream()
            .map(head -> Collections.singletonList(head.getTitle()))
            .toList();

    // 准备导出文件
    File file = prepareExportFile(fileId, request.getFileName(), exportTask.getOrganizationId());
    try (ExcelWriter writer =
        EasyExcel.write(file).head(headList).excelType(ExcelTypeEnum.XLSX).build()) {
      WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

      SubListUtils.dealForSubList(
          request.getIds(),
          SubListUtils.DEFAULT_EXPORT_BATCH_SIZE,
          (subIds) -> {
            List<List<Object>> data = new ArrayList<>();
            try {
              data =
                  getExportDataBySelect(request.getHeadList(), subIds, orgId, exportTask.getId());
            } catch (InterruptedException e) {
              log.error("任务停止中断", e);
              exportTaskService.update(
                  exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
            }
            writer.write(data, sheet);
          });
    }
  }

  /**
   * 导出选中线索数据
   *
   * @param headList
   * @param ids
   * @param orgId
   * @param taskId
   * @return
   * @throws InterruptedException
   */
  public List<List<Object>> getExportDataBySelect(
      List<ExportHeadDTO> headList, List<String> ids, String orgId, String taskId)
      throws InterruptedException {

    // 获取原始数据
    List<ClueListResponse> rawList = extClueMapper.getListByIds(ids);

    return buildExportResult(headList, rawList, orgId, taskId);
  }

  /**
   * 导出全部线索数据
   *
   * @param request
   * @param userId
   * @param orgId
   * @param deptDataPermission
   * @param taskId
   * @return
   * @throws InterruptedException
   */
  public List<List<Object>> getExportData(
      ClueExportRequest request,
      String userId,
      String orgId,
      DeptDataPermissionDTO deptDataPermission,
      String taskId)
      throws InterruptedException {

    // 分页
    PageHelper.startPage(request.getCurrent(), request.getPageSize());

    // 获取原始数据
    List<ClueListResponse> rawList =
        extClueMapper.list(request, orgId, userId, deptDataPermission, false);

    return buildExportResult(request.getHeadList(), rawList, orgId, taskId);
  }

  private List<List<Object>> buildExportResult(
      List<ExportHeadDTO> headList, List<ClueListResponse> rawList, String orgId, String taskId)
      throws InterruptedException {

    // 构建业务数据
    List<ClueListResponse> dataList = clueService.buildListData(rawList, orgId);

    // 构建选项映射
    Map<String, List<OptionDTO>> optionMap = clueService.buildOptionMap(orgId, rawList, dataList);

    // 字段配置
    Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CLUE.getKey(), orgId);

    // 构建导出结果（预分配容量）
    List<List<Object>> result = new ArrayList<>(dataList.size());
    for (ClueListResponse response : dataList) {
      if (ExportThreadRegistry.isInterrupted(taskId)) {
        throw new InterruptedException("线程已被中断，主动退出");
      }
      result.add(buildData(headList, response, optionMap, fieldConfigMap));
    }

    return result;
  }

  private List<Object> buildData(
      List<ExportHeadDTO> headList,
      ClueListResponse data,
      Map<String, List<OptionDTO>> optionMap,
      Map<String, BaseField> fieldConfigMap) {
    List<Object> dataList = new ArrayList<>();
    // 固定字段map
    LinkedHashMap<String, Object> systemFiledMap =
        PoolClueFieldUtils.getSystemFieldMap(data, optionMap);
    // 自定义字段map
    AtomicReference<Map<String, Object>> moduleFieldMap =
        new AtomicReference<>(new LinkedHashMap<>());
    Optional.ofNullable(data.getModuleFields())
        .ifPresent(
            moduleFields ->
                moduleFieldMap.set(
                    moduleFields.stream()
                        .collect(
                            Collectors.toMap(
                                BaseModuleFieldValue::getFieldId,
                                BaseModuleFieldValue::getFieldValue))));
    // 处理数据转换
    transModuleFieldValue(headList, systemFiledMap, moduleFieldMap.get(), dataList, fieldConfigMap);
    return dataList;
  }
}
