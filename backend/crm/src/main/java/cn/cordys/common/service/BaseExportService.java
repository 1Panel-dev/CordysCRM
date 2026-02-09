package cn.cordys.common.service;

import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.context.CustomFunction;
import cn.cordys.common.context.ExportTaskFunction;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportFieldParam;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.domain.MergeResult;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.SummaryMergeHandler;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.file.engine.DefaultRepositoryDir;
import cn.cordys.registry.ExportThreadRegistry;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseExportService {

    /**
     * 最大查询数量
     */
    public static final int EXPORT_MAX_COUNT = 2000;
    private static final String SUM_PREFIX = "sum_";
    public static final String SLASH = "/";
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
                if (ExportThreadRegistry.isInterrupted(task.getId())) {
                    throw new InterruptedException("线程已被中断，主动退出");
                }

                t.setCurrent(current);
                List<?> data = func.apply(t);
                if (CollectionUtils.isEmpty(data)) {
                    break;
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
     * 分页写入数据并合并
     *
     * @param headList     头信息
     * @param task         任务
     * @param fileName     文件名
     * @param t            请求参数
     * @param func         获取数据方法
     * @param mergeColumns 合并列
     * @param <T>          参数类型
     *
     * @throws InterruptedException 异常信息
     */
    public <T extends BasePageRequest> void batchHandleDataWithMergeStrategy(List<List<String>> headList, ExportTask task, String fileName,
                                                                             List<Integer> mergeColumns, T t,
                                                                             CustomFunction<T, MergeResult> func) throws InterruptedException {

        File file = prepareExportFile(task.getFileId(), fileName, task.getOrganizationId());

        try (ExcelWriter writer = EasyExcel.write(file)
                .head(headList)
                .excelType(ExcelTypeEnum.XLSX)
                .registerWriteHandler(new CustomHeadColWidthStyleStrategy())
                .build()) {

            WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

            int offset = 2, current = 1;
            t.setPageSize(EXPORT_MAX_COUNT);

            while (true) {
                if (ExportThreadRegistry.isInterrupted(task.getId())) {
                    throw new InterruptedException("线程已被中断，主动退出");
                }

                t.setCurrent(current);
                MergeResult mergeResult = func.apply(t);
                if (CollectionUtils.isEmpty(mergeResult.getDataList())) {
                    break;
                }
                // 写入数据
                writer.write(mergeResult.getDataList(), sheet);
                // 执行合并策略
                Sheet mergeSheet = writer.writeContext().writeWorkbookHolder().getWorkbook().getSheetAt(0);
                SummaryMergeHandler strategy = new SummaryMergeHandler(mergeResult.getMergeRegions(), mergeColumns, offset);
                strategy.merge(mergeSheet);
                if (mergeResult.getDataList().size() < EXPORT_MAX_COUNT) {
                    break;
                }
                // 下一页&&记录偏移量
                current++;
                offset += mergeResult.getDataList().size();
            }
        }
    }


    /**
     * 准备导出文件
     *
     * @param fileId   文件ID
     * @param fileName 文件名
     *
     * @return 导出文件
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
     * @param headList       头集合信息
     * @param systemFieldMap 系统字段值
     * @param moduleFieldMap 模块字段值
     * @param dataList       数据列表
     * @param fieldConfigMap 字段配置集合
     */
    public List<Object> transModuleFieldValue(List<ExportHeadDTO> headList, LinkedHashMap<String, Object> systemFieldMap, Map<String, Object> moduleFieldMap, List<Object> dataList, Map<String, BaseField> fieldConfigMap) {
        headList.forEach(head -> {
            if (systemFieldMap.containsKey(head.getKey())) {
                //固定字段
                dataList.add(systemFieldMap.get(head.getKey()));
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
     * 导出全部(合并策略)
     *
     * @param exportParam 导出参数
     *
     * @return 导出任务ID
     */
    public String exportAllWithMergeStrategy(ExportDTO exportParam) {
        List<List<String>> exportHeads = getExportMergeHeadList(exportParam.getHeadList(), exportParam.getOrgId(), exportParam.getFormKey());
        List<Integer> mergeColumns = getMergeColumns(exportHeads);
        exportParam.setMergeHeads(getMergeHeads(exportParam.getHeadList(), exportParam.getFormKey(), exportParam.getOrgId()));
        return exportWithMergeStrategy(exportParam, (task) -> batchHandleDataWithMergeStrategy(exportHeads, task, exportParam.getFileName(),
                mergeColumns, exportParam.getPageRequest(),
                t -> getExportMergeData(task.getId(), exportParam)));
    }

    /**
     * 导出选中(合并策略)
     *
     * @param exportParam 导出参数
     *
     * @return 导出任务ID
     */
    public String exportSelectWithMergeStrategy(ExportDTO exportParam) {
        List<List<String>> exportHeads = getExportMergeHeadList(exportParam.getHeadList(), exportParam.getOrgId(), exportParam.getFormKey());
        List<Integer> mergeColumns = getMergeColumns(exportHeads);
        exportParam.setMergeHeads(getMergeHeads(exportParam.getHeadList(), exportParam.getFormKey(), exportParam.getOrgId()));
        return exportWithMergeStrategy(exportParam, (task) -> {
            File file = prepareExportFile(task.getFileId(), exportParam.getFileName(), task.getOrganizationId());
            try (ExcelWriter writer = EasyExcel.write(file).head(exportHeads).excelType(ExcelTypeEnum.XLSX)
                    .registerWriteHandler(new CustomHeadColWidthStyleStrategy()).build()) {
                WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();
                AtomicInteger offset = new AtomicInteger(2);
                SubListUtils.dealForSubList(exportParam.getSelectIds(), SubListUtils.DEFAULT_EXPORT_BATCH_SIZE, (ids) -> {
                    MergeResult mergeResult = new MergeResult();
                    try {
                        mergeResult = getExportMergeData(task.getId(), exportParam);
                    } catch (InterruptedException e) {
                        log.error("任务停止中断", e);
                        exportTaskService.update(task.getId(), ExportConstants.ExportStatus.STOP.toString(), exportParam.getUserId());
                    }
                    // 写入数据
                    writer.write(mergeResult.getDataList(), sheet);
                    // 执行合并策略
                    Sheet mergeSheet = writer.writeContext().writeWorkbookHolder().getWorkbook().getSheetAt(0);
                    SummaryMergeHandler strategy = new SummaryMergeHandler(mergeResult.getMergeRegions(), mergeColumns, offset.get());
                    strategy.merge(mergeSheet);
                    offset.addAndGet(mergeResult.getDataList().size());
                });
            }
        });
    }

    /**
     * 通用导出(合并策略)
     *
     * @param exportParam 导出参数
     * @param executor    导出执行器
     *
     * @return 导出任务ID
     */
    private String exportWithMergeStrategy(ExportDTO exportParam, ExportExecutor executor) {
        exportParam.setExportFieldParam(getExportFieldParam(exportParam));
        return asyncExport(exportParam.getFileName(), exportParam.getOrgId(), exportParam.getUserId(), exportParam.getLocale(),
                exportParam.getLogModule(), exportParam.getExportType(), executor);
    }

    /**
     * 获取导出的表头信息
     *
     * @param headList   表头列表
     * @param currentOrg 当前组织
     *
     * @return 表头信息
     */
    private List<List<String>> getExportMergeHeadList(List<ExportHeadDTO> headList, String currentOrg, String formKey) {
        return Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class)).getAllExportHeads(headList, formKey, currentOrg);
    }

    /**
     * 解析字段值 (子表格)
     *
     * @param heads          表头信息
     * @param sysFieldValMap 系统字段值
     * @param fieldConfigMap 字段配置映射
     *
     * @return 单行记录值
     */
    public List<Object> transFieldValueWithSub(List<String> heads, LinkedHashMap<String, Object> sysFieldValMap, Map<String, Object> moduleFieldMap,
                                               Map<String, BaseField> fieldConfigMap) {
        List<Object> dataList = new ArrayList<>();
        heads.forEach(head -> {
            if (head.contains(SUM_PREFIX)) {
                head = head.split(SUM_PREFIX)[1];
            }
            BaseField field = fieldConfigMap.get(head);
            if (field == null) {
                if (sysFieldValMap.containsKey(head)) {
                    // 系统固定字段
                    dataList.add(sysFieldValMap.get(head));
                }
                return;
            }
            if (sysFieldValMap.containsKey(field.getBusinessKey()) && StringUtils.isEmpty(field.getResourceFieldId())) {
                //固定字段
                dataList.add(sysFieldValMap.get(field.getBusinessKey()));
            } else if (moduleFieldMap.containsKey(field.getBusinessKey())) {
                // 子表业务字段
                Object value = moduleFieldMap.get(field.getBusinessKey());
                dataList.add(transformFieldValue(field, value));
            } else if (moduleFieldMap.containsKey(field.getId())) {
                Object value = moduleFieldMap.get(field.getId());
                dataList.add(transformFieldValue(field, value));
            } else {
                dataList.add(null);
            }
        });
        return dataList;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object transformFieldValue(BaseField field, Object value) {
        if (value == null) {
            return null;
        }
        AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
        // 将数据库中的字符串值,转换为对应的对象值
        return customFieldResolver.transformToValue(field, value instanceof List ? JSON.toJSONString(value) : value.toString());
    }

	/**
	 * 构建含有子表格的导出数据
	 * @param moduleFieldValues 自定义字段值
	 * @param exportFieldParam 导出参数
	 * @param heads 导出表头信息
	 * @param systemFieldMap 系统字段值
	 * @return 导出数据列表
	 */
    protected List<List<Object>> buildDataWithSub(List<BaseModuleFieldValue> moduleFieldValues, ExportFieldParam exportFieldParam, List<String> heads, LinkedHashMap<String, Object> systemFieldMap) {
        List<List<Object>> dataList = new ArrayList<>();
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(moduleFieldValues)) {
            // 无自定义字段, 导出系统字段值, 单行导出
            List<Object> data = transFieldValueWithSub(heads, systemFieldMap, new LinkedHashMap<>(), exportFieldParam.getFieldConfigMap());
            dataList.add(data);
            return dataList;
        }

        List<BaseModuleFieldValue> subFvs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(exportFieldParam.getSubIds())) {
            subFvs = moduleFieldValues.stream().filter(fv -> exportFieldParam.getSubIds().contains(fv.getFieldId())).toList();
        }
        if (isNullSubValue(subFvs)) {
            // 子表格缺失, 无需合并, 单行导出.
            Map<String, Object> normalFvs = moduleFieldValues
                    .stream()
                    .filter(moduleFieldValue -> moduleFieldValue.getFieldValue() != null)
                    .collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue));
            List<Object> data = transFieldValueWithSub(heads, systemFieldMap, normalFvs, exportFieldParam.getFieldConfigMap());
            dataList.add(data);
            return dataList;
        }
        moduleFieldValues.removeIf(fv -> exportFieldParam.getSubIds().contains(fv.getFieldId()));

		List<Map<String, Object>> alignSubFvs = alignSubFvs(subFvs);
		alignSubFvs.forEach(subFvMap -> {
            // 包含子表格行数据, 需多行合并导出
            Map<String, Object> normalFvs = moduleFieldValues.stream()
                    .filter(moduleFieldValue -> moduleFieldValue.getFieldValue() != null)
                    .collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue, (p, n) -> p));
            subFvMap.putAll(normalFvs);
            List<Object> data = transFieldValueWithSub(heads, systemFieldMap, subFvMap, exportFieldParam.getFieldConfigMap());
            dataList.add(data);
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
            dataList.add(transformFieldValue(fieldConfig, value));
        });
    }


    /**
     * 日志
     *
     * @param orgId      组织ID
     * @param taskId     任务ID
     * @param userId     用户ID
     * @param logType    日志类型
     * @param moduleType 模块类型
     * @param fileName   文件名
     */
    public void exportLog(String orgId, String taskId, String userId, String logType, String moduleType, String fileName) {
        LogDTO logDTO = new LogDTO(orgId, taskId, userId, logType, moduleType, fileName);
        logService.add(logDTO);
    }


    public void checkFileName(String fileName) {
        if (fileName.contains(SLASH)) {
            throw new GenericException(Translator.get("file_name_illegal"));
        }
    }

    /**
     * 构建全部导出数据
     *
     * @return 导出数据列表
     *
     * @throws InterruptedException 异常信息
     */
    protected List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {
        return null;
    }

    /**
     * 构建导出合并数据
     *
     * @param taskId      任务ID
     * @param exportParam 导出参数
     *
     * @return 导出数据列表 && 合并区域
     *
     * @throws InterruptedException 异常信息
     */
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) throws InterruptedException {
        return null;
    }

    /**
     * 构建选择的导出数据
     *
     * @return 导出数据列表
     *
     * @throws InterruptedException 异常信息
     */
    protected List<List<Object>> getSelectExportData(List<String> ids, String taskId, ExportDTO exportDTO) throws InterruptedException {
        return null;
    }


    protected Map<String, Object> getFieldIdValueMap(List<BaseModuleFieldValue> fieldValues) {
        AtomicReference<Map<String, Object>> moduleFieldMap = new AtomicReference<>(new LinkedHashMap<>());
        Optional.ofNullable(fieldValues).ifPresent(moduleFields -> moduleFieldMap.set(
                moduleFields.stream()
                        .filter(fv -> fv.getFieldValue() != null)
                        .collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue)))
        );
        return moduleFieldMap.get();
    }

    /**
     * 异步导出通用方法
     *
     * @param exportFileName 导出文件名
     * @param currentOrg     当前组织
     * @param currentUser    当前用户
     * @param locale         语言环境
     * @param logModule      日志模块
     * @param executor       导出执行器
     */
    public String asyncExport(String exportFileName, String currentOrg, String currentUser, Locale locale, String logModule,
                              String exportType, ExportExecutor executor) {
        checkFileName(exportFileName);
        exportTaskService.checkUserTaskLimit(currentUser, ExportConstants.ExportStatus.PREPARED.toString());
        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(currentOrg, fileId, currentUser, exportType, exportFileName);

        runExport(currentOrg, currentUser, logModule, locale, exportTask, exportFileName,
                () -> executor.execute(exportTask));

        return exportTask.getId();
    }

    /**
     * 获取一些公共的导出字段参数
     *
     * @param exportParam 导出参数
     *
     * @return 导出字段参数
     */
    protected ExportFieldParam getExportFieldParam(ExportDTO exportParam) {
        ModuleFormConfigDTO formConfig = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormCacheService.class))
                .getBusinessFormConfig(exportParam.getFormKey(), exportParam.getOrgId());
        List<String> exportTitles = exportParam.getHeadList().stream().map(ExportHeadDTO::getTitle).toList();
        List<BaseField> flattenFields = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class)).flattenFormAllFields(formConfig);
		List<String> subTableIds = flattenFields.stream().filter(f -> f instanceof SubField && exportTitles.contains(f.getName())).map(BaseField::getId).toList();
        Map<String, BaseField> fieldConfigMap = flattenFields.stream().collect(Collectors.toMap(BaseField::getId, f -> f, (f1, f2) -> f1));
        return ExportFieldParam.builder().subIds(subTableIds).fieldConfigMap(fieldConfigMap)
                .formConfig(formConfig)
                .build();
    }

    /**
     * 获取表头ID集合
     *
     * @param exportHeads 表头信息
     * @param currentOrg  当前组织
     * @param formKey     表单Key
     *
     * @return 表头ID集合
     */
    private List<String> getMergeHeads(List<ExportHeadDTO> exportHeads, String formKey, String currentOrg) {
        return Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class)).getExportMergeHeads(formKey, currentOrg, exportHeads);
    }

    /**
     * 获取需要合并的列索引
     *
     * @param heads 导出的表头信息
     *
     * @return 需要合并的列索引集合
     */
    private List<Integer> getMergeColumns(List<List<String>> heads) {
        List<Integer> mergeColumns = new ArrayList<>();
        for (int i = 0; i < heads.size(); i++) {
            List<String> headCols = heads.get(i);
            if (headCols.size() == 1) {
                mergeColumns.add(i);
            }
        }
        if (mergeColumns.size() == heads.size()) {
            // 因为子表格缺失, 全部列都需要合并. 跳过即可
            mergeColumns.clear();
        }
        return mergeColumns;
    }

    public void runExport(String orgId, String userId, String module, Locale locale, ExportTask exportTask, String fileName, ExportTaskFunction func) {
        Thread.startVirtualThread(() -> {
            try {
                LocaleContextHolder.setLocale(locale);
                ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());

                func.apply();

                exportTaskService.update(
                        exportTask.getId(),
                        ExportConstants.ExportStatus.SUCCESS.name(),
                        userId
                );
            } catch (InterruptedException e) {
                log.error("任务停止中断", e);
                exportTaskService.update(
                        exportTask.getId(),
                        ExportConstants.ExportStatus.STOP.name(),
                        userId
                );
            } catch (Exception e) {
                log.error("导出任务异常", e);
                exportTaskService.update(
                        exportTask.getId(),
                        ExportConstants.ExportStatus.ERROR.name(),
                        userId
                );
            } finally {
                ExportThreadRegistry.remove(exportTask.getId());
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, module, fileName);
            }
        });
    }

    protected void exportSelectData(ExportTask exportTask, ExportDTO exportDTO) {
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
                    log.error("任务停止中断", e);
                    exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), exportDTO.getUserId());
                }
                writer.write(data, sheet);
            });
        }

        //更新导出任务状态
        exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), exportDTO.getUserId());
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

    protected String getOptionLabel(String value, List<OptionProp> options) {
        for (OptionProp option : options) {
            if (Strings.CS.equals(option.getValue(), value)) {
                return option.getLabel();
            }
        }
        return null;
    }

	/**
	 * 是否为空子表格
	 * @param subFvs 子表格字段值
	 * @return 是否为空子表格
	 */
	private boolean isNullSubValue(List<BaseModuleFieldValue> subFvs) {
		if (CollectionUtils.isEmpty(subFvs)) {
			return true;
		}
		boolean allNull = true;
		for (BaseModuleFieldValue subFv : subFvs) {
			if (CollectionUtils.isNotEmpty((List<?>) subFv.getFieldValue())) {
				allNull = false;
				break;
			}
		}
		return allNull;
	}

	/**
	 * 合并对齐多个子表格的值
	 * @param subFvs 多子表格字段值
	 * @return 合并后的子表格值列表
	 */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> alignSubFvs(List<BaseModuleFieldValue> subFvs) {
		BaseModuleFieldValue longSubFv = subFvs.stream()
				.max(Comparator.comparingInt(fv -> {
					List<?> v = (List<?>) fv.getFieldValue();
					return v != null ? v.size() : 0;
				}))
				.orElse(null);
		if (longSubFv == null) {
			return new ArrayList<>();
		}
		List<?> longFvs = (List<?>) longSubFv.getFieldValue();
		List<Map<String, Object>> alignedList = new ArrayList<>(longFvs.size());
		for (int i = 0; i < longFvs.size(); i++) {
			Map<String, Object> longFvMap = (Map<String, Object>) longFvs.get(i);
			for (BaseModuleFieldValue subFv : subFvs) {
				if (Strings.CS.equals(subFv.getFieldId(), longSubFv.getFieldId())) {
					continue;
				}
				List<?> shortFvs = (List<?>) subFv.getFieldValue();
				if (CollectionUtils.isNotEmpty(shortFvs) && shortFvs.size() > i) {
					// 合并跨区子表格
					Map<String, Object> shortFvMap = (Map<String, Object>) shortFvs.get(i);
					longFvMap.putAll(shortFvMap);
				}
			}
			alignedList.add(longFvMap);
		}
		return alignedList;
	}

}
