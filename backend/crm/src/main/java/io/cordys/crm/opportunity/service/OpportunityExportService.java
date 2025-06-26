package io.cordys.crm.opportunity.service;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.ExportHeadDTO;
import io.cordys.common.dto.ExportSelectRequest;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.service.BaseExportService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.SubListUtils;
import io.cordys.common.utils.OpportunityFieldUtils;
import io.cordys.crm.opportunity.dto.request.OpportunityExportRequest;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.constants.ExportConstants;
import io.cordys.crm.system.domain.ExportTask;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.service.ExportTaskService;
import io.cordys.registry.ExportThreadRegistry;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityExportService extends BaseExportService {

    @Resource
    private OpportunityService opportunityService;
    @Resource
    private ExportTaskService exportTaskService;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;


    public String export(String userId, OpportunityExportRequest request, String orgId, DeptDataPermissionDTO deptDataPermission) {
        // 参数校验
        Objects.requireNonNull(userId, "userId 不能为空");
        // 用户导出数量限制
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportType.OPPORTUNITY.toString(), ExportConstants.ExportStatus.PREPARED.toString());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.OPPORTUNITY.toString(), request.getFileName());

        Thread.startVirtualThread(() -> {
            try {
                ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());

                // 表头信息
                List<List<String>> headList = request.getHeadList().stream()
                        .map(head -> Collections.singletonList(head.getTitle()))
                        .toList();


                //分批查询数据并写入文件
                batchHandleData(fileId,
                        headList,
                        exportTask.getId(),
                        request.getFileName(),
                        request,
                        t -> getExportData(request.getHeadList(), request, userId, orgId, deptDataPermission, exportTask.getId()));

                // 更新状态
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), userId);

            } catch (InterruptedException e) {
                LogUtils.error("任务停止中断", e);
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
            } catch (Exception e) {
                LogUtils.error("导出数据异常", e);
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                // 从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                // 日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.OPPORTUNITY, request.getFileName());
            }
        });

        return exportTask.getId();
    }


    /**
     * 构建导出的数据
     *
     * @param headList
     * @param request
     * @param userId
     * @param orgId
     * @param deptDataPermission
     * @return
     */
    private List<List<Object>> getExportData(List<ExportHeadDTO> headList, OpportunityExportRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission, String taskId) throws InterruptedException {
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        //获取数据
        List<OpportunityListResponse> allList = extOpportunityMapper.list(request, orgId, userId, deptDataPermission);
        List<OpportunityListResponse> dataList = opportunityService.buildListData(allList, orgId);
        Map<String, List<OptionDTO>> optionMap = opportunityService.buildOptionMap(orgId, allList, dataList);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.OPPORTUNITY.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (OpportunityListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(headList, response, optionMap, fieldConfigMap);
            data.add(value);
        }

        return data;
    }

    private List<Object> buildData(List<ExportHeadDTO> headList, OpportunityListResponse data, Map<String, List<OptionDTO>> optionMap, Map<String, BaseField> fieldConfigMap) {
        List<Object> dataList = new ArrayList<>();
        //固定字段map
        LinkedHashMap<String, Object> systemFiledMap = OpportunityFieldUtils.getSystemFieldMap(data, optionMap);
        //自定义字段map
        AtomicReference<Map<String, Object>> moduleFieldMap = new AtomicReference<>(new LinkedHashMap<>());
        Optional.ofNullable(data.getModuleFields()).ifPresent(moduleFields -> {
            moduleFieldMap.set(moduleFields.stream().collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue)));
        });
        //处理数据转换
        transModuleFieldValue(headList, systemFiledMap, moduleFieldMap.get(), dataList, fieldConfigMap);
        return dataList;
    }


    /**
     * 导出选中的商机列表
     *
     * @param userId
     * @param request
     * @param orgId
     * @return
     */
    public String exportSelect(String userId, ExportSelectRequest request, String orgId) {
        // 参数校验
        Objects.requireNonNull(userId, "userId 不能为空");
        // 用户导出数量限制
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportType.OPPORTUNITY.toString(), ExportConstants.ExportStatus.PREPARED.toString());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.OPPORTUNITY.toString(), request.getFileName());
        Thread.startVirtualThread(() -> {
            try {
                ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
                //表头信息
                List<List<String>> headList = request.getHeadList().stream()
                        .map(head -> Arrays.asList(head.getTitle()))
                        .toList();

                // 准备导出文件
                File file = prepareExportFile(fileId, request.getFileName());
                try (ExcelWriter writer = EasyExcel.write(file)
                        .head(headList)
                        .excelType(ExcelTypeEnum.XLSX)
                        .build()) {
                    WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

                    SubListUtils.dealForSubList(request.getIds(), SubListUtils.DEFAULT_EXPORT_BATCH_SIZE, (subIds) -> {
                        List<List<Object>> data = null;
                        try {
                            data = getExportDataBySelect(request.getHeadList(), subIds, orgId, exportTask.getId());
                        } catch (InterruptedException e) {
                            LogUtils.error("任务停止中断", e);
                            exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
                        }
                        writer.write(data, sheet);
                    });
                }

                //更新导出任务状态
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), userId);
            } catch (Exception e) {
                //更新任务
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                //从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                //日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.OPPORTUNITY, request.getFileName());
            }
        });
        return exportTask.getId();
    }


    /**
     * 选中商机数据
     * @param headList
     * @param ids
     * @param orgId
     * @param taskId
     * @return
     * @throws InterruptedException
     */
    private List<List<Object>> getExportDataBySelect(List<ExportHeadDTO> headList, List<String> ids, String orgId, String taskId) throws InterruptedException {
        //获取数据
        List<OpportunityListResponse> allList = extOpportunityMapper.getListByIds(ids);
        List<OpportunityListResponse> dataList = opportunityService.buildListData(allList, orgId);
        Map<String, List<OptionDTO>> optionMap = opportunityService.buildOptionMap(orgId, allList, dataList);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.OPPORTUNITY.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (OpportunityListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(headList, response, optionMap, fieldConfigMap);
            data.add(value);
        }

        return data;
    }


}
