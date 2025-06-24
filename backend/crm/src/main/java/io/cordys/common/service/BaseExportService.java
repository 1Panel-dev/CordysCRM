package io.cordys.common.service;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.resolver.field.AbstractModuleFieldResolver;
import io.cordys.common.resolver.field.ModuleFieldResolverFactory;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.dto.ExportHeadDTO;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.service.LogService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.file.engine.DefaultRepositoryDir;
import io.cordys.registry.ExportThreadRegistry;
import jakarta.annotation.Resource;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseExportService {

    @Resource
    private LogService logService;


    public Map<String, BaseField> getFieldConfigMap(String formKey, String orgId) {
        return CommonBeanFactory.getBean(ModuleFormService.class)
                .getAllFields(formKey, orgId)
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));
    }


    /**
     * 导出数据
     *
     * @param headList
     * @param fileId
     * @param fileName
     * @param data
     */
    public void exportData(List<List<String>> headList, String fileId, String fileName, List<List<Object>> data, String taskId) throws InterruptedException {
        String tempFileDir = getTempFileDir(fileId);
        File dir = new File(DefaultRepositoryDir.getDefaultDir() + tempFileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, fileName + ".xlsx");

        if (ExportThreadRegistry.isStop(taskId)) {
            throw new InterruptedException("线程已被中断，主动退出");
        }

        EasyExcel.write(file)
                .head(headList)
                .excelType(ExcelTypeEnum.XLSX).sheet("导出数据").doWrite(data);

    }


    /**
     * 获取导出临时文件目录
     *
     * @param tempFileId
     * @return
     */
    private String getTempFileDir(String tempFileId) {
        return DefaultRepositoryDir.getExportDir() + "/" + tempFileId;
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
    public void transModuleFieldValue(List<ExportHeadDTO> headList, LinkedHashMap<String, Object> systemFiledMap, Map<String, Object> moduleFieldMap, List<Object> dataList, Map<String, BaseField> fieldConfigMap) {
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
            Object objectValue = customFieldResolver.trans2Value(fieldConfig, value.toString());
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

}
