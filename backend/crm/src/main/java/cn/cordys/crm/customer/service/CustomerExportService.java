package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.crm.customer.dto.request.CustomerExportRequest;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.utils.PoolCustomerFieldUtils;
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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CustomerExportService extends BaseExportService {

  @Resource private CustomerService customerService;
  @Resource private ExportTaskService exportTaskService;
  @Resource private ExtCustomerMapper extCustomerMapper;

  public String export(
      String userId,
      CustomerExportRequest request,
      String orgId,
      DeptDataPermissionDTO deptDataPermission,
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
            ExportConstants.ExportType.CUSTOMER.toString(),
            request.getFileName());

    // 启动虚拟线程执行导出任务
    runExport(
        orgId,
        userId,
        LogModule.CUSTOMER_INDEX,
        locale,
        exportTask,
        request.getFileName(),
        () -> exportCustomerData(exportTask, userId, request, orgId, deptDataPermission));

    return exportTask.getId();
  }

  public void exportCustomerData(
      ExportTask exportTask,
      String userId,
      CustomerExportRequest request,
      String orgId,
      DeptDataPermissionDTO deptDataPermission)
      throws Exception {
    // 表头信息
    List<List<String>> headList =
        request.getHeadList().stream()
            .map(head -> Collections.singletonList(head.getTitle()))
            .toList();
    // 分批查询数据并写入文件
    batchHandleData(
        exportTask.getFileId(),
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

  private List<Object> buildData(
      List<ExportHeadDTO> headList,
      CustomerListResponse data,
      Map<String, BaseField> fieldConfigMap) {
    List<Object> dataList = new ArrayList<>();
    // 固定字段map
    LinkedHashMap<String, Object> systemFiledMap = PoolCustomerFieldUtils.getSystemFieldMap(data);
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

  /**
   * 导出选择数据
   *
   * @param userId 用户ID
   * @param request 导出选择请求
   * @param orgId 组织ID
   * @return 导出任务ID
   */
  public String exportSelect(
      String userId, ExportSelectRequest request, String orgId, Locale locale) {
    checkFileName(request.getFileName());
    // 用户导出数量限制
    exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

    String fileId = IDGenerator.nextStr();
    ExportTask exportTask =
        exportTaskService.saveTask(
            orgId,
            fileId,
            userId,
            ExportConstants.ExportType.CUSTOMER.toString(),
            request.getFileName());

    runExport(
        orgId,
        userId,
        LogModule.CUSTOMER_INDEX,
        locale,
        exportTask,
        request.getFileName(),
        () -> exportSelectData(exportTask, userId, request, orgId, locale));

    return exportTask.getId();
  }

  public void exportSelectData(
      ExportTask exportTask,
      String userId,
      ExportSelectRequest request,
      String orgId,
      Locale locale) {
    LocaleContextHolder.setLocale(locale);
    ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
    // 表头信息
    List<List<String>> headList =
        request.getHeadList().stream()
            .map(head -> Collections.singletonList(head.getTitle()))
            .toList();
    // 准备导出文件
    File file =
        prepareExportFile(
            exportTask.getFileId(), request.getFileName(), exportTask.getOrganizationId());
    try (ExcelWriter writer =
        EasyExcel.write(file).head(headList).excelType(ExcelTypeEnum.XLSX).build()) {
      WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

      SubListUtils.dealForSubList(
          request.getIds(),
          SubListUtils.DEFAULT_EXPORT_BATCH_SIZE,
          (subIds) -> {
            List<List<Object>> data = null;
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

    // 更新导出任务状态
    exportTaskService.update(
        exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), userId);
  }

  /** 选中客户数据 */
  private List<List<Object>> getExportDataBySelect(
      List<ExportHeadDTO> headList, List<String> ids, String orgId, String taskId)
      throws InterruptedException {

    List<CustomerListResponse> rawList = extCustomerMapper.getListByIds(ids);
    return buildCustomerExportData(headList, rawList, orgId, taskId);
  }

  /** 条件分页导出客户数据 */
  private List<List<Object>> getExportData(
      List<ExportHeadDTO> headList,
      CustomerExportRequest request,
      String userId,
      String orgId,
      DeptDataPermissionDTO deptDataPermission,
      String taskId)
      throws InterruptedException {

    PageHelper.startPage(request.getCurrent(), request.getPageSize());
    List<CustomerListResponse> rawList =
        extCustomerMapper.list(request, orgId, userId, deptDataPermission);
    return buildCustomerExportData(headList, rawList, orgId, taskId);
  }

  /** 构建客户导出数据（公共逻辑） */
  private List<List<Object>> buildCustomerExportData(
      List<ExportHeadDTO> headList, List<CustomerListResponse> rawList, String orgId, String taskId)
      throws InterruptedException {

    List<CustomerListResponse> dataList = customerService.buildListData(rawList, orgId);
    Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CUSTOMER.getKey(), orgId);

    List<List<Object>> result = new ArrayList<>(dataList.size());
    for (CustomerListResponse response : dataList) {
      if (ExportThreadRegistry.isInterrupted(taskId)) {
        throw new InterruptedException("线程已被中断，主动退出");
      }
      result.add(buildData(headList, response, fieldConfigMap));
    }
    return result;
  }
}
