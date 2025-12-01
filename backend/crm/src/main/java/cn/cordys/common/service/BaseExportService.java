package cn.cordys.common.service;

import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.context.CustomFunction;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.*;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.registry.ExportThreadRegistry;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseExportService {

    //最大查询数量
    public static final int EXPORT_MAX_COUNT = 2000;
    @Resource
    private LogService logService;
    @Resource
    private ExportTaskService exportTaskService;


    public Map<String, BaseField> getFieldConfigMap(String formKey, String orgId) {
        return Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class))
                .getAllFields(formKey, orgId)
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));
    }


    public <T extends BasePageRequest> void batchHandleData(String fileId, List<List<String>> headList, ExportTask task, String fileName, T t, CustomFunction<T, List<?>> func) throws InterruptedException {
        // 准备导出文件
        File file = prepareExportFile(fileId, fileName, task.getOrganizationId());

        try (ExcelWriter writer = EasyExcel.write(file)
                .head(headList)
                .excelType(ExcelTypeEnum.XLSX)
                .build()) {

            WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

            int current = 1;
            t.setPageSize(EXPORT_MAX_COUNT);

            while (true) {
                t.setCurrent(current);
                List<?> data = func.apply(t);
                if (CollectionUtils.isEmpty(data)) {
                    break;
                }
                if (ExportThreadRegistry.isInterrupted(task.getId())) {
                    throw new InterruptedException("线程已被中断，主动退出");
                }
                writer.write(data, sheet);
                if (data.size() < EXPORT_MAX_COUNT) {
                    break;
                }
                current++;
            }
        }


    }


    /**
     * 准备导出文件
     *
     * @param fileId
     * @param fileName
     *
     * @return
     */
    public File prepareExportFile(String fileId, String fileName, String orgId) {
        if (fileId == null || fileName == null || orgId == null) {
            throw new IllegalArgumentException("文件ID、文件名和组织ID不能为空");
        }

        // 构建导出目录路径
        String exportDirPath = DefaultRepositoryDir.getDefaultDir()
                + File.separator
                + DefaultRepositoryDir.getExportDir(orgId)
                + File.separator + fileId;

        File dir = new File(exportDirPath);

        // 检查目录创建结果
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("无法创建导出目录: " + dir.getAbsolutePath());
        }

        // 返回完整的文件路径
        return new File(dir, fileName + ".xlsx");
    }

    /**
     * 根据数据value 转换对应值
     *
     * @param headList
     * @param systemFiledMap
     * @param moduleFieldMap
     * @param dataList
     * @param fieldConfigMap
     */
    public List<Object> transModuleFieldValue(List<ExportHeadDTO> headList, LinkedHashMap<String, Object> systemFiledMap, Map<String, Object> moduleFieldMap, List<Object> dataList, Map<String, BaseField> fieldConfigMap) {
        headList.forEach(head -> {
            if (systemFiledMap.containsKey(head.getKey())) {
                //固定字段
                dataList.add(systemFiledMap.get(head.getKey()));
            } else if (moduleFieldMap.containsKey(head.getKey())) {
                //自定义字段
                Map<String, Object> collect = moduleFieldMap.entrySet().stream()
                        .filter(entry -> entry.getKey().contains(head.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                getResourceFieldMap(collect, dataList, fieldConfigMap);
            } else {
                dataList.add(null);
            }

        });
        return dataList;
    }


    /**
     * 解析自定义字段
     *
     * @param moduleFieldMap 模块字段值
     * @param dataList       数据列表
     * @param fieldConfigMap 字段配置映射
     */
    public void getResourceFieldMap(Map<String, Object> moduleFieldMap, List<Object> dataList, Map<String, BaseField> fieldConfigMap) {
        moduleFieldMap.forEach((key, value) -> {
            BaseField fieldConfig = fieldConfigMap.get(key);
            if (fieldConfig == null) {
                return;
            }
            // 获取字段解析器
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
            // 将数据库中的字符串值,转换为对应的对象值
            Object objectValue = customFieldResolver.transformToValue(fieldConfig, value instanceof List ? JSON.toJSONString(value) : value.toString());
            dataList.add(objectValue);
        });
    }


    /**
     * 日志
     *
     * @param orgId
     * @param taskId
     * @param userId
     * @param logType
     * @param moduleType
     * @param fileName
     */
    public void exportLog(String orgId, String taskId, String userId, String logType, String moduleType, String fileName) {
        LogDTO logDTO = new LogDTO(orgId, taskId, userId, logType, moduleType, fileName);
        logService.add(logDTO);
    }


    public void checkFileName(String fileName) {
        if (fileName.contains("/")) {
            throw new GenericException(Translator.get("file_name_illegal"));
        }
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
        Thread.startVirtualThread(() -> {
            try {
                this.exportCustomerData(exportTask, exportDTO);
            } catch (InterruptedException e) {
                LogUtils.error("任务停止中断", e);
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
            } catch (Exception e) {
                //更新任务
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                //从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                //日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, exportDTO.getLogModule(), fileName);
            }
        });
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

    /**
     * 构建全部导出数据
     * @return 导出数据列表
     * @throws InterruptedException
     */
    protected List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {return null;}

    /**
     * 构建选择的导出数据
     *
     * @return 导出数据列表
     * @throws InterruptedException
     */
    protected List<List<Object>> getSelectExportData(List<String> ids, String taskId, ExportDTO exportDTO) throws InterruptedException {return null;}

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
        Thread.startVirtualThread(() -> {
            try {
                this.exportSelectData(exportTask, exportDTO);
            } catch (Exception e) {
                LogUtils.error("导出回款计划异常", e);
                //更新任务
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                //从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                //日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, exportDTO.getLogModule(), fileName);
            }
        });
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

    protected Map<String, Object> getFieldIdValueMap(List<BaseModuleFieldValue> fieldValues) {
        AtomicReference<Map<String, Object>> moduleFieldMap = new AtomicReference<>(new LinkedHashMap<>());
        Optional.ofNullable(fieldValues).ifPresent(moduleFields -> {
            moduleFieldMap.set(moduleFields.stream().collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue)));
        });
        return moduleFieldMap.get();
    }
}
