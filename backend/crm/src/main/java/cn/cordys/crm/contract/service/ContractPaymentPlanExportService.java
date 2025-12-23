package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractPaymentPlanExportService extends BaseExportService {

    @Resource
    private ContractPaymentPlanService contractPaymentPlanService;
    @Resource
    private ExtContractPaymentPlanMapper extContractPaymentPlanMapper;
    @Resource
    private ExportTaskService exportTaskService;

    /**
     * 构建导出的数据
     *
     * @return 导出数据列表
     */
    @Override
    public List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {
        ContractPaymentPlanPageRequest pageRequest = (ContractPaymentPlanPageRequest) exportDTO.getPageRequest();
        String orgId = exportDTO.getOrgId();
        PageHelper.startPage(pageRequest.getCurrent(), pageRequest.getPageSize());
        //获取数据
        List<ContractPaymentPlanListResponse> allList = extContractPaymentPlanMapper.list(pageRequest, exportDTO.getUserId(), orgId, exportDTO.getDeptDataPermission());
        List<ContractPaymentPlanListResponse> dataList = contractPaymentPlanService.buildListData(allList, orgId);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (ContractPaymentPlanListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(exportDTO.getHeadList(), response, fieldConfigMap);
            data.add(value);
        }

        return data;
    }

    private List<Object> buildData(List<ExportHeadDTO> headList, ContractPaymentPlanListResponse data, Map<String, BaseField> fieldConfigMap) {
        List<Object> dataList = new ArrayList<>();
        //固定字段map
        LinkedHashMap<String, Object> systemFiledMap = getSystemFieldMap(data);
        //自定义字段map
        Map<String, Object> moduleFieldMap = getFieldIdValueMap(data.getModuleFields());
        //处理数据转换
        return transModuleFieldValue(headList, systemFiledMap, moduleFieldMap, dataList, fieldConfigMap);
    }

    public LinkedHashMap<String, Object> getSystemFieldMap(ContractPaymentPlanListResponse data) {
        LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
        systemFiledMap.put("contractId", data.getContractName());
        systemFiledMap.put("owner", data.getOwnerName());
        systemFiledMap.put("departmentId", data.getDepartmentName());
        systemFiledMap.put("planAmount", data.getPlanAmount());
        systemFiledMap.put("planEndTime", TimeUtils.getDataTimeStr(data.getPlanEndTime()));
        systemFiledMap.put("planStatus", Translator.get("contract.payment_plan.status." + data.getPlanStatus().toLowerCase()));

        systemFiledMap.put("createUser", data.getCreateUserName());
        systemFiledMap.put("createTime", TimeUtils.getDataTimeStr(data.getCreateTime()));
        systemFiledMap.put("updateUser", data.getUpdateUserName());
        systemFiledMap.put("updateTime", TimeUtils.getDataTimeStr(data.getUpdateTime()));
        return systemFiledMap;
    }


    /**
     * 选中回款计划数据
     *
     * @return 导出数据列表
     */
    @Override
    public List<List<Object>> getSelectExportData(List<String> ids, String taskId, ExportDTO exportDTO) throws InterruptedException {
        String orgId = exportDTO.getOrgId();
        String userId = exportDTO.getUserId();
        //获取数据
        List<ContractPaymentPlanListResponse> allList = extContractPaymentPlanMapper.getListByIds(ids, userId, orgId, exportDTO.getDeptDataPermission());
        List<ContractPaymentPlanListResponse> dataList = contractPaymentPlanService.buildListData(allList, orgId);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (ContractPaymentPlanListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(exportDTO.getHeadList(), response, fieldConfigMap);
            data.add(value);
        }
        return data;
    }

    /**
     * 导出选择数据
     *
     * @return 导出任务ID
     */
    public String exportSelect(ExportDTO exportDTO) {
        String fileName = exportDTO.getFileName();
        String userId = exportDTO.getUserId();
        String orgId = exportDTO.getOrgId();
        checkFileName(fileName);
        // 用户导出数量限制
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, exportDTO.getExportType(), fileName);

        runExport(orgId, userId, exportDTO.getLogModule(), exportDTO.getLocale(), exportTask, exportDTO.getFileName(),
                () -> exportSelectData(exportTask, exportDTO));

        return exportTask.getId();
    }

    public void exportSelectData(ExportTask exportTask, ExportDTO exportDTO) {
        LocaleContextHolder.setLocale(exportDTO.getLocale());
        ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
        //表头信息
        List<List<String>> headList = exportDTO.getHeadList().stream()
                .map(head -> Collections.singletonList(head.getTitle()))
                .toList();
        // 准备导出文件
        File file = prepareExportFile(exportTask.getFileId(), exportDTO.getFileName(), exportTask.getOrganizationId());
        try (ExcelWriter writer = EasyExcel.write(file)
                .head(headList)
                .excelType(ExcelTypeEnum.XLSX)
                .build()) {
            WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

            SubListUtils.dealForSubList(exportDTO.getSelectIds(), SubListUtils.DEFAULT_EXPORT_BATCH_SIZE, (subIds) -> {
                List<List<Object>> data = null;
                try {
                    data = getSelectExportData(subIds, exportTask.getId(), exportDTO);
                } catch (InterruptedException e) {
                    LogUtils.error("任务停止中断", e);
                    exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), exportDTO.getUserId());
                }
                writer.write(data, sheet);
            });
        }

        //更新导出任务状态
        exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), exportDTO.getUserId());
    }

    public String export(ExportDTO exportDTO) {
        String userId = exportDTO.getUserId();
        String orgId = exportDTO.getOrgId();
        String exportType = exportDTO.getExportType();
        String fileName = exportDTO.getFileName();
        checkFileName(exportDTO.getFileName());
        //用户导出数量 限制
        exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportStatus.PREPARED.toString());

        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, exportType, fileName);

        runExport(orgId, userId, exportDTO.getLogModule(), exportDTO.getLocale(), exportTask, exportDTO.getFileName(),
                () -> exportCustomerData(exportTask, exportDTO));

        return exportTask.getId();
    }

    public void exportCustomerData(ExportTask exportTask, ExportDTO exportDTO) throws Exception {
        LocaleContextHolder.setLocale(exportDTO.getLocale());
        ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
        //表头信息
        List<List<String>> headList = exportDTO.getHeadList().stream()
                .map(head -> Collections.singletonList(head.getTitle()))
                .toList();
        //分批查询数据并写入文件
        batchHandleData(exportTask.getFileId(),
                headList,
                exportTask,
                exportDTO.getFileName(),
                exportDTO.getPageRequest(),
                t -> getExportData(exportTask.getId(), exportDTO));
        //更新状态
        exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), exportDTO.getUserId());
    }

}
