package cn.cordys.common.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtAttachmentMapper;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 组装资源操作日志快照并写入当前操作日志上下文。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ResourceLogService {
    @Resource
    private ExtModuleFieldMapper extModuleFieldMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private ExtAttachmentMapper extAttachmentMapper;

    public <T> void handleAddLog(T resource, List<BaseModuleFieldValue> moduleFields) {
        Map<String, Object> resourceLog = JSON.parseToMap(JSON.toJSONString(resource));

        if (moduleFields != null) {
            moduleFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> resourceLog.put(field.getFieldId(), field.getFieldValue()));
        }

        writeAddLogContext(resource, resourceLog);
    }

    /**
     * 新增日志, 替换资源名称
     *
     * @param <T>          资源类型
     * @param resource     资源信息
     * @param moduleFields 自定义字段
     */
    public <T> void handleAddLogWithResourceName(T resource, List<BaseModuleFieldValue> moduleFields) {
        Map<String, Object> resourceLog = JSON.parseToMap(JSON.toJSONString(resource));

        if (moduleFields != null) {
            moduleFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> resourceLog.put(field.getFieldId(), field.getFieldValue()));
        }

        writeAddLogContextWithResourceName(resource, resourceLog);
    }

    public <T> void handleAddLogWithSubTable(
            T resource,
            List<BaseModuleFieldValue> moduleFields,
            String subTableKeyName,
            ModuleFormConfigDTO moduleFormConfigDTO) {
        Map<String, Object> resourceLog = JSON.parseToMap(JSON.toJSONString(resource));
        Set<String> subRefKey = getSubTableRefIds(moduleFormConfigDTO);
        if (moduleFields != null) {
            Map<String, String> fieldNameMap = getFieldNameMap(moduleFields, moduleFormConfigDTO);
            List<BaseModuleFieldValue> validFields = moduleFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .toList();
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(resourceLog, validFields, fieldNameMap, subTableIdKeyMap, subTableKeyName, subRefKey,
                    moduleFormConfigDTO);
        }

        writeAddLogContextWithResourceName(resource, resourceLog);
    }

    /**
     * 写入操作日志上下文
     */
    private <T> void writeAddLogContext(T resource, Map<String, Object> resourceLog) {
        try {
            Method idGetter = resource.getClass().getMethod("getId");
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId((String) idGetter.invoke(resource))
                            .modifiedValue(resourceLog)
                            .build());
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }

    /**
     * 写入操作日志上下文，替换资源名称 (参数资源名存在占位符)
     *
     * @param <T>         资源类型
     * @param resource    资源
     * @param resourceLog 日志信息
     */
    private <T> void writeAddLogContextWithResourceName(T resource, Map<String, Object> resourceLog) {
        try {
            Method idGetter = resource.getClass().getMethod("getId");
            Method nameGetter = resource.getClass().getMethod("getName");
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId((String) idGetter.invoke(resource))
                            .resourceName((String) nameGetter.invoke(resource))
                            .modifiedValue(resourceLog)
                            .build());
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }

    public <T> void handleUpdateLog(
            T originResource,
            T modifiedResource,
            List<BaseModuleFieldValue> originResourceFields,
            List<BaseModuleFieldValue> modifiedResourceFields,
            String id,
            String name) {
        Map<String, Object> originResourceLog = JSON.parseToMap(JSON.toJSONString(originResource));
        Map<String, Object> modifiedResourceLog = JSON.parseToMap(JSON.toJSONString(modifiedResource));

        // 收集所有字段ID，检测附件类型字段
        Set<String> allFieldIds = new HashSet<>();
        if (originResourceFields != null) {
            originResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> allFieldIds.add(field.getFieldId()));
        }
        if (modifiedResourceFields != null) {
            modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> allFieldIds.add(field.getFieldId()));
        }
        Set<String> attachmentFieldIds = filterAttachmentFieldIds(allFieldIds);

        // 添加原始字段值（附件类型字段将ID替换为名称）
        if (originResourceFields != null) {
            originResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> {
                        Object value = field.getFieldValue();
                        if (attachmentFieldIds.contains(field.getFieldId())) {
                            value = replaceAttachmentIdsWithNames(value);
                        }
                        originResourceLog.put(field.getFieldId(), value);
                    });
        }

        // 添加修改后的字段值（过滤无效字段，附件类型字段将ID替换为名称）
        if (modifiedResourceFields != null) {
            modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .forEach(field -> {
                        Object value = field.getFieldValue();
                        if (attachmentFieldIds.contains(field.getFieldId())) {
                            value = replaceAttachmentIdsWithNames(value);
                        }
                        modifiedResourceLog.put(field.getFieldId(), value);
                    });
        }

        try {
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId(id)
                            .resourceName(name)
                            .originalValue(originResourceLog)
                            .modifiedValue(modifiedResourceLog)
                            .build());
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }

    public <T> void handleUpdateLogWithSubTable(
            T originResource,
            T modifiedResource,
            List<BaseModuleFieldValue> originResourceFields,
            List<BaseModuleFieldValue> modifiedResourceFields,
            String id,
            String name,
            String subTableKeyName,
            ModuleFormConfigDTO moduleFormConfigDTO) {
        Map<String, Object> originResourceLog = JSON.parseToMap(JSON.toJSONString(originResource));
        Map<String, Object> modifiedResourceLog = JSON.parseToMap(JSON.toJSONString(modifiedResource));
        Set<String> subRefKey = getSubTableRefIds(moduleFormConfigDTO);

        // 从表单配置中获取附件类型字段ID集合
        Set<String> attachmentFieldIds = getAttachmentFieldIdsFromFormConfig(moduleFormConfigDTO);

        if (CollectionUtils.isNotEmpty(originResourceFields)) {
            Map<String, String> oldFieldNameMap = getFieldNameMap(originResourceFields, moduleFormConfigDTO);
            List<BaseModuleFieldValue> validFields = originResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .toList();
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(originResourceLog, validFields, oldFieldNameMap, subTableIdKeyMap, subTableKeyName,
                    subRefKey, moduleFormConfigDTO);
        }

        if (CollectionUtils.isNotEmpty(modifiedResourceFields)) {
            Map<String, String> newFieldNameMap = getFieldNameMap(modifiedResourceFields, moduleFormConfigDTO);
            List<BaseModuleFieldValue> validFields = modifiedResourceFields.stream()
                    .filter(BaseModuleFieldValue::valid)
                    .toList();
            Map<String, String> subTableIdKeyMap = getSubTableIdKeyMap(moduleFormConfigDTO);
            fillResourceLog(modifiedResourceLog, validFields, newFieldNameMap, subTableIdKeyMap, subTableKeyName,
                    subRefKey, moduleFormConfigDTO);
        }

        // 将附件字段中的附件ID替换为附件名称
        replaceAttachmentValuesInLog(originResourceLog, attachmentFieldIds);
        replaceAttachmentValuesInLog(modifiedResourceLog, attachmentFieldIds);

        try {
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceId(id)
                            .resourceName(name)
                            .originalValue(originResourceLog)
                            .modifiedValue(modifiedResourceLog)
                            .build());
        } catch (Exception e) {
            throw new GenericException(e);
        }
    }

    /**
     * 处理普通字段 + 子表字段
     */
    private void fillResourceLog(
            Map<String, Object> resourceLog,
            List<BaseModuleFieldValue> fields,
            Map<String, String> fieldNameMap,
            Map<String, String> subTableKeyMap,
            String subTableKeyName,
            Set<String> subRefKey,
            ModuleFormConfigDTO moduleFormConfigDTO) {
        fields.forEach(field -> {
            String fieldId = field.getFieldId();
            // 普通字段
            if (!subTableKeyMap.containsKey(fieldId) && !subTableKeyMap.containsValue(fieldId)) {
                resourceLog.put(fieldId, field.getFieldValue());
                return;
            }

            // 子表字段
            List<Map<String, Object>> subTableList = JSON.parseArray(JSON.toJSONString(field.getFieldValue()),
                    new TypeReference<>() {
                    });
            if (CollectionUtils.isEmpty(subTableList)) {
                return;
            }
            AtomicReference<String> subTableName = new AtomicReference<>(moduleFormConfigDTO.getFields().stream()
                    .filter(BaseField::isSubField).filter(subField -> subField.getId().equals(fieldId)).findFirst()
                    .map(BaseField::getName).orElse(subTableKeyName));
            if (Strings.CI.equals(fieldId, "products")) {
                subTableKeyMap.forEach((key, value) -> {
                    if (StringUtils.isNotBlank(value) && Strings.CI.equals(value, fieldId)) {
                        subTableName.set(moduleFormConfigDTO.getFields().stream().filter(BaseField::isSubField)
                                .filter(subField -> subField.getId().equals(key)).findFirst().map(BaseField::getName)
                                .orElse(subTableKeyName));
                    }
                });
            }

            int size = subTableList.size();
            for (int i = 0; i < size; i++) {
                Map<String, Object> row = subTableList.get(i);
                if (size > 1) {
                    int finalI = i;
                    String finalSubTableName1 = subTableName.get();
                    row.forEach((key, value) -> {
                        if (!fieldNameMap.containsKey(key) || subRefKey.contains(key)) {
                            return;
                        }
                        resourceLog.put(finalSubTableName1 + "-" + fieldNameMap.get(key) + "-" + Translator.get("row")
                                + (finalI + 1) + "-" + key, value);
                    });
                } else {
                    String finalSubTableName = subTableName.get();
                    row.forEach((key, value) -> {
                        if (!fieldNameMap.containsKey(key) || subRefKey.contains(key)) {
                            return;
                        }
                        resourceLog.put(finalSubTableName + "-" + fieldNameMap.get(key) + "-" + key, value);
                    });
                }
            }
        });
    }

    /**
     * 字段 ID → 字段名称 映射表
     */
    private Map<String, String> getFieldNameMap(List<BaseModuleFieldValue> fields,
            ModuleFormConfigDTO moduleFormConfigDTO) {
        List<String> fieldIds = new ArrayList<>(fields.stream()
                .map(BaseModuleFieldValue::getFieldId)
                .distinct()
                .toList());
        if (CollectionUtils.isEmpty(fields)) {
            return new HashMap<>();
        }
        List<OptionDTO> fieldOptions = extModuleFieldMapper.getSourceOptionsByIds("sys_module_field", fieldIds);
        Map<String, String> nameMap = fieldOptions.stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));

        if (CollectionUtils.isNotEmpty(moduleFormConfigDTO.getFields())) {
            for (BaseField field : moduleFormConfigDTO.getFields()) {
                if (field.isSubField()) {
                    if (field instanceof SubField objectSubField) {
                        objectSubField.getSubFields().forEach(subField -> {
                            nameMap.put(StringUtils.isNotBlank(subField.getBusinessKey()) ? subField.getBusinessKey()
                                    : subField.getId(), subField.getName());
                        });
                    }
                }
            }
        }
        return nameMap;
    }

    public Map<String, String> getSubTableIdKeyMap(ModuleFormConfigDTO moduleFormConfigDTO) {
        Map<String, String> subTableIdKeyMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(moduleFormConfigDTO.getFields())) {
            for (BaseField field : moduleFormConfigDTO.getFields()) {
                if (field.isSubField()) {
                    subTableIdKeyMap.put(field.getId(), field.getBusinessKey());
                }
            }
        }
        return subTableIdKeyMap;
    }

    /**
     * 获取子表引用字段ID集合
     *
     * @param formConfig 表单陪配置
     * @return 子表引用字段ID集合
     */
    private Set<String> getSubTableRefIds(ModuleFormConfigDTO formConfig) {
        if (CollectionUtils.isNotEmpty(formConfig.getFields())) {
            List<BaseField> subList = formConfig.getFields().stream().filter(BaseField::isSubField).toList();
            if (CollectionUtils.isNotEmpty(subList)) {
                return subList.stream()
                        .flatMap(subField -> ((SubField) subField).getSubFields().stream())
                        .filter(f -> StringUtils.isNotBlank(f.getResourceFieldId()))
                        .map(BaseField::getId)
                        .collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }

    /**
     * 从字段ID集合中筛选出附件类型的字段ID
     *
     * @param fieldIds 字段ID集合
     * @return 附件类型字段ID集合
     */
    private Set<String> filterAttachmentFieldIds(Set<String> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ModuleField::getId, new ArrayList<>(fieldIds))
                .eq(ModuleField::getType, FieldType.ATTACHMENT.name());
        List<ModuleField> attachmentFields = moduleFieldMapper.selectListByLambda(queryWrapper);
        return attachmentFields.stream().map(ModuleField::getId).collect(Collectors.toSet());
    }

    /**
     * 将附件字段值中的附件ID替换为附件名称
     * 在记录操作日志时调用，确保即使附件被删除，日志中仍保留附件名称
     *
     * @param fieldValue 附件字段值（附件ID列表）
     * @return 替换为附件名称后的值
     */
    private Object replaceAttachmentIdsWithNames(Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        List<String> ids;
        if (fieldValue instanceof List<?> values) {
            ids = values.stream().map(String::valueOf).toList();
        } else {
            ids = JSON.parseArray(fieldValue.toString(), String.class);
        }
        if (CollectionUtils.isEmpty(ids)) {
            return fieldValue;
        }
        List<String> names = extAttachmentMapper.selectNameByIds(ids);
        if (CollectionUtils.isNotEmpty(names)) {
            return names;
        }
        return fieldValue;
    }

    /**
     * 从表单配置中获取附件类型字段ID集合
     *
     * @param formConfig 表单配置
     * @return 附件类型字段ID集合
     */
    private Set<String> getAttachmentFieldIdsFromFormConfig(ModuleFormConfigDTO formConfig) {
        if (formConfig == null || CollectionUtils.isEmpty(formConfig.getFields())) {
            return Collections.emptySet();
        }
        return formConfig.getFields().stream()
                .filter(field -> Strings.CS.equals(field.getType(), FieldType.ATTACHMENT.name()))
                .map(BaseField::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 替换日志中附件字段的值（将附件ID替换为附件名称）
     *
     * @param resourceLog        日志数据
     * @param attachmentFieldIds 附件类型字段ID集合
     */
    private void replaceAttachmentValuesInLog(Map<String, Object> resourceLog, Set<String> attachmentFieldIds) {
        if (CollectionUtils.isEmpty(attachmentFieldIds) || resourceLog == null) {
            return;
        }
        for (String fieldId : attachmentFieldIds) {
            if (resourceLog.containsKey(fieldId)) {
                resourceLog.put(fieldId, replaceAttachmentIdsWithNames(resourceLog.get(fieldId)));
            }
        }
    }
}
