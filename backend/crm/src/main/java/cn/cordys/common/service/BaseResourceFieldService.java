package cn.cordys.common.service;

import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.dto.BatchUpdateDbParam;
import cn.cordys.common.dto.ChartAnalysisDbRequest;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.chart.ChartCategoryAxisDbParam;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.*;
import cn.cordys.common.utils.RegionUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.field.MemberField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资源与模块字段值的公共处理类
 *
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
public abstract class BaseResourceFieldService<T extends BaseResourceField, V extends BaseResourceField> {

    @Resource
    private SerialNumGenerator serialNumGenerator;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private LogService logService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;

    private static List<ChartResult> mergeResult(List<ChartResult> chartResults, Function<ChartResult, String> getKeyFunc) {
        Map<String, ChartResult> mergedResults = new HashMap<>();
        for (ChartResult chartResult : chartResults) {
            String categoryAxis = getKeyFunc.apply(chartResult);
            if (mergedResults.containsKey(categoryAxis)) {
                ChartResult existingResult = mergedResults.get(categoryAxis);
                if (existingResult.getValueAxis() instanceof Double && chartResult.getValueAxis() instanceof Double) {
                    existingResult.setValueAxis((Double) existingResult.getValueAxis() + (Double) chartResult.getValueAxis());
                } else if (existingResult.getValueAxis() instanceof Long && chartResult.getValueAxis() instanceof Long) {
                    existingResult.setValueAxis((Long) existingResult.getValueAxis() + (Long) chartResult.getValueAxis());
                } else if (existingResult.getValueAxis() instanceof Integer && chartResult.getValueAxis() instanceof Integer) {
                    existingResult.setValueAxis((Integer) existingResult.getValueAxis() + (Integer) chartResult.getValueAxis());
                }
            } else {
                mergedResults.put(categoryAxis, chartResult);
            }
        }
        return new ArrayList<>(mergedResults.values());
    }

    private static BaseModuleFieldValue getBaseModuleFieldValue(String fieldKey, BaseField baseField) {
        BaseModuleFieldValue categoryFieldValue = new BaseModuleFieldValue();
        if (baseField != null) {
            // 业务字段key翻译成字段ID
            categoryFieldValue.setFieldId(baseField.getId());
        } else {
            categoryFieldValue.setFieldId(fieldKey);
        }
        return categoryFieldValue;
    }

    private static BaseField getBaseField(List<BaseField> fields, String fieldKey) {
        if (StringUtils.isBlank(fieldKey)) {
            return null;
        }
        return fields.stream()
                .filter(field -> Strings.CI.equals(field.getId(), fieldKey)
                        || Strings.CI.equals(field.getBusinessKey(), fieldKey))
                .findFirst().orElse(null);
    }

    public static List<BaseField> getChartBaseFields() {
        BaseField createUserField = new MemberField();
        createUserField.setType(FieldType.MEMBER.name());
        createUserField.setId("createUser");
        createUserField.setBusinessKey("createUser");

        BaseField updateUserField = new MemberField();
        updateUserField.setType(FieldType.MEMBER.name());
        updateUserField.setId("updateUser");
        updateUserField.setBusinessKey("updateUser");

        return List.of(createUserField, updateUserField);
    }

    protected abstract String getFormKey();

    protected abstract BaseMapper<T> getResourceFieldMapper();

    protected abstract BaseMapper<V> getResourceFieldBlobMapper();

    protected void checkUnique(BaseModuleFieldValue fieldValue, BaseField field) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseResourceField::getFieldId, fieldValue.getFieldId());
        wrapper.eq(BaseResourceField::getFieldValue, fieldValue.getFieldValue());
        if (!getResourceFieldMapper().selectListByLambda(wrapper).isEmpty()) {
            throw new GenericException(Translator.getWithArgs("common.field_value.repeat", field.getName()));
        }
    }

    private Class<T> getResourceFieldClass() {
        return (Class<T>) getGenericType(0);
    }

    private Class<V> getResourceFieldBlobClass() {
        return (Class<V>) getGenericType(1);
    }

    protected Type getGenericType(int index) {
        // 获取当前类的泛型父类
        Type superclass = getClass().getGenericSuperclass();

        // 检查是否是ParameterizedType
        if (superclass instanceof ParameterizedType parameterizedType) {

            // 获取泛型类型的实际类型参数
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            // 返回第一个泛型类型参数
            return typeArguments[index];
        }

        throw new IllegalStateException("No generic type found");
    }

    /**
     * 获取资源和模块字段的 map
     *
     * @param resourceId
     *
     * @return
     */
    public List<BaseModuleFieldValue> getModuleFieldValuesByResourceId(String resourceId) {
        List<BaseModuleFieldValue> fieldValues = getResourceFieldMap(List.of(resourceId), true).get(resourceId);
        return fieldValues == null ? List.of() : fieldValues;
    }

    /**
     * @param resource          资源
     * @param orgId             组织ID
     * @param userId            用户ID
     * @param moduleFieldValues 自定义字段值
     * @param update            是否更新
     */
    public <K> void saveModuleField(K resource, String orgId, String userId, List<BaseModuleFieldValue> moduleFieldValues, boolean update) {
        List<BaseField> allFields = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class))
                .getAllFields(getFormKey(), OrganizationContext.getOrganizationId());
        if (CollectionUtils.isEmpty(allFields)) {
            return;
        }

        String resourceId = (String) getResourceFieldValue(resource, "id");

        Map<String, BaseModuleFieldValue> fieldValueMap;
        if (CollectionUtils.isNotEmpty(moduleFieldValues)) {
            fieldValueMap = moduleFieldValues.stream().collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, v -> v));
        } else {
            fieldValueMap = new HashMap<>();
        }

        // 校验业务字段，字段值是否重复
        businessFieldRepeatCheck(orgId, resource, update ? List.of(resourceId) : List.of(), allFields);

        List<T> customerFields = new ArrayList<>();
        List<V> customerFieldBlobs = new ArrayList<>();
        allFields.stream()
                .filter(field -> {
                    BaseModuleFieldValue fieldValue = fieldValueMap.get(field.getId());
                    return (fieldValue != null && fieldValue.valid()) || field.isSerialNumber();
                })
                .forEach(field -> {
                    BaseModuleFieldValue fieldValue;
                    if (field.isSerialNumber() && !update) {
                        fieldValue = new BaseModuleFieldValue();
                        fieldValue.setFieldId(field.getId());
                        String serialNo = serialNumGenerator.generateByRules(((SerialNumberField) field).getSerialNumberRules(), orgId, getFormKey());
                        fieldValue.setFieldValue(serialNo);
                    } else {
                        fieldValue = fieldValueMap.get(field.getId());
                    }
                    if (fieldValue == null) {
                        return;
                    }
					
					// 处理子表格的字段值

                    if (field.needRepeatCheck()) {
                        checkUnique(fieldValue, field);
                    }

                    // 获取字段解析器
                    AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
                    // 校验参数值
                    customFieldResolver.validate(field, fieldValue.getFieldValue());
                    // 将参数值转换成字符串入库
                    String strValue = customFieldResolver.convertToString(field, fieldValue.getFieldValue());

                    if (field.isBlob()) {
                        V resourceField = newResourceFieldBlob();
                        resourceField.setId(IDGenerator.nextStr());
                        resourceField.setResourceId(resourceId);
                        resourceField.setFieldId(fieldValue.getFieldId());
                        resourceField.setFieldValue(strValue);
                        customerFieldBlobs.add(resourceField);
                    } else {
                        T resourceField = newResourceField();
                        resourceField.setId(IDGenerator.nextStr());
                        resourceField.setResourceId(resourceId);
                        resourceField.setFieldId(fieldValue.getFieldId());
                        resourceField.setFieldValue(strValue);
                        customerFields.add(resourceField);
                    }

                });

        // Process all attachment field
        List<BaseModuleFieldValue> attachmentFieldVals = allFields.stream()
                .filter(field -> {
                    BaseModuleFieldValue fieldValue = fieldValueMap.get(field.getId());
                    return (fieldValue != null && fieldValue.valid()) && field.isAttachment();
                }).map(field -> fieldValueMap.get(field.getId())).toList();
        List processVal = attachmentFieldVals.stream().map(val -> (List) val.getFieldValue()).flatMap(List::stream).toList();
        preProcessTempAttachment(orgId, resourceId, userId, processVal);

        if (CollectionUtils.isNotEmpty(customerFields)) {
            getResourceFieldMapper().batchInsert(customerFields);
        }

        if (CollectionUtils.isNotEmpty(customerFieldBlobs)) {
            getResourceFieldBlobMapper().batchInsert(customerFieldBlobs);
        }
    }



    /**
     * 校验业务字段，字段值是否重复
     *
     * @param orgId
     * @param resource
     * @param updateIds
     * @param allFields
     * @param <K>
     */
    private <K> void businessFieldRepeatCheck(String orgId, K resource, List<String> updateIds, List<BaseField> allFields) {
        Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
                collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));

        allFields.forEach(field -> {
            if (businessModuleFieldMap.containsKey(field.getInternalKey())) {
                BusinessModuleField businessModuleField = businessModuleFieldMap.get(field.getInternalKey());
                businessFieldRepeatCheck(orgId, resource, updateIds, field, businessModuleField.getBusinessKey());
            }
        });
    }

    private <K> void businessFieldRepeatCheck(String orgId, K resource, List<String> updateIds, BaseField field, String fieldName) {
        if (!field.needRepeatCheck()) {
            return;
        }
        Class<?> clazz = resource.getClass();
        String tableName = CaseFormatUtils.camelToUnderscore(clazz.getSimpleName());

        Object fieldValue = getResourceFieldValue(resource, fieldName);

        if (!isBlankValue(fieldValue)) {
            boolean repeat;
            if (CollectionUtils.isNotEmpty(updateIds)) {
                repeat = commonMapper.checkUpdateExist(tableName, fieldName, fieldValue.toString(), orgId, updateIds);
            } else {
                repeat = commonMapper.checkAddExist(tableName, fieldName, fieldValue.toString(), orgId);
            }
            if (repeat) {
                throw new GenericException(Translator.getWithArgs("common.field_value.repeat", field.getName()));
            }
        }
    }

    public <K> void batchUpdate(ResourceBatchEditRequest request,
                                BaseField field,
                                List<K> originResourceList,
                                Class<K> clazz,
                                String logModule,
                                Consumer<BatchUpdateDbParam> batchInsertFunc,
                                String userId,
                                String orgId) {

        if (field.needRepeatCheck() && request.getIds().size() > 1 && !isBlankValue(request.getFieldValue())) {
            // 如果字段唯一，则校验不能同时修改多条
            throw new GenericException(Translator.getWithArgs("common.field_value.repeat", field.getName()));
        }

        BatchUpdateDbParam updateParam = new BatchUpdateDbParam();
        updateParam.setIds(request.getIds());
        // 修改更新时间和用户
        updateParam.setUpdateTime(System.currentTimeMillis());
        updateParam.setUpdateUser(userId);

        if (StringUtils.isNotBlank(field.getBusinessKey())) {
            K resource = newInstance(clazz);
            // 设置值用于唯一校验
            setResourceFieldValue(resource, field.getBusinessKey(), request.getFieldValue());
            // 字段唯一性校验
            businessFieldRepeatCheck(orgId, resource, request.getIds(), field, field.getBusinessKey());

            updateParam.setFieldName(field.getBusinessKey());
            updateParam.setFieldValue(request.getFieldValue());

            // 添加日志
            addBusinessFieldBatchUpdateLog(originResourceList, field, request, logModule, userId, orgId);
        } else {
            ModuleField moduleField = moduleFieldMapper.selectByPrimaryKey(request.getFieldId());

            // 查询修改前的字段，记录日志
            List<? extends BaseResourceField> originFields;
            if (field.isBlob()) {
                originFields = getResourceFieldBlob(request.getIds(), request.getFieldId());
            } else {
                originFields = getResourceField(request.getIds(), request.getFieldId());
            }

            // 先删除
            batchDeleteFieldValues(request, moduleField);

            if (field.needRepeatCheck() && !isBlankValue(request.getFieldValue())) {
                // 字段唯一性校验
                checkUnique(BeanUtils.copyBean(new BaseModuleFieldValue(), request), field);
            }

            if (!isBlankValue(request.getFieldValue())) {
                // 再插入
                batchUpdateFieldValues(request, field, moduleField);
            }

            // 添加日志
            addCustomFieldBatchUpdateLog(originResourceList, originFields, request, field, logModule, userId, orgId);
        }

        // 批量修改业务字段和更新时间等
        batchInsertFunc.accept(updateParam);
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String str && StringUtils.isBlank(str)) {
            return true;
        }
        return value instanceof List list && list.isEmpty();
    }

    public BaseField getAndCheckField(String fieldId, String organizationId) {
        return moduleFormCacheService.getConfig(getFormKey(), organizationId)
                .getFields()
                .stream()
                .filter(f -> fieldId.equals(f.getId()))
                .findFirst()
                .orElseThrow(() -> new GenericException(Translator.get("module.field.not_exist")));
    }

    private <K> K newInstance(Class<K> clazz) {
        K resource;
        try {
            resource = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            LogUtils.error(e);
            throw new RuntimeException(e);
        }
        return resource;
    }

    /**
     * 记录业务字段批量更新日志
     *
     * @param originResourceList
     * @param request
     * @param userId
     * @param orgId
     * @param <K>
     */
    private <K> void addBusinessFieldBatchUpdateLog(List<K> originResourceList,
                                                    BaseField field,
                                                    ResourceBatchEditRequest request,
                                                    String logModule,
                                                    String userId,
                                                    String orgId) {
        // 记录日志
        List<LogDTO> logs = originResourceList.stream()
                .map(resource -> {
                    Map originResource = new HashMap();
                    if (!isBlankValue(getResourceFieldValue(resource, field.getBusinessKey()))) {
                        originResource.put(field.getBusinessKey(), getResourceFieldValue(resource, field.getBusinessKey()));
                    }

                    Map modifiedResource = new HashMap();
                    if (!isBlankValue(request.getFieldValue())) {
                        modifiedResource.put(field.getBusinessKey(), request.getFieldValue());
                    }

                    Object id = getResourceFieldValue(resource, "id");
                    Object name = getResourceFieldValue(resource, "name");

                    LogDTO logDTO = new LogDTO(orgId, id.toString(), userId, LogType.UPDATE, logModule, name.toString());
                    logDTO.setOriginalValue(originResource);
                    logDTO.setModifiedValue(modifiedResource);
                    return logDTO;
                }).toList();

        logService.batchAdd(logs);
    }

    /**
     * 记录自定义字段批量更新日志
     *
     * @param originResourceList
     * @param request
     * @param userId
     * @param orgId
     * @param <K>
     */
    private <K> void addCustomFieldBatchUpdateLog(List<K> originResourceList,
                                                  List<? extends BaseResourceField> originFields,
                                                  ResourceBatchEditRequest request,
                                                  BaseField field,
                                                  String logModule,
                                                  String userId,
                                                  String orgId) {
        Map<String, ? extends BaseResourceField> fieldMap = originFields.stream()
                .collect(Collectors.toMap(BaseResourceField::getResourceId, Function.identity()));
        // 记录日志
        List<LogDTO> logs = originResourceList.stream()
                .map(resource -> {
                    Object id = getResourceFieldValue(resource, "id");
                    Object name = getResourceFieldValue(resource, "name");

                    BaseResourceField baseResourceField = fieldMap.get(id);
                    Map originResource = new HashMap();
                    if (baseResourceField != null && !isBlankValue(baseResourceField.getFieldValue())) {
                        // 获取字段解析器
                        AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
                        // 将数据库中的字符串值,转换为对应的对象值
                        Object objectValue = customFieldResolver.convertToValue(field, baseResourceField.getFieldValue().toString());
                        baseResourceField.setFieldValue(objectValue);
                        originResource.put(request.getFieldId(), baseResourceField.getFieldValue());
                    }

                    Map modifiedResource = new HashMap();
                    if (!isBlankValue(request.getFieldValue())) {
                        modifiedResource.put(request.getFieldId(), request.getFieldValue());
                    }

                    LogDTO logDTO = new LogDTO(orgId, id.toString(), userId, LogType.UPDATE, logModule, name.toString());
                    logDTO.setOriginalValue(originResource);
                    logDTO.setModifiedValue(modifiedResource);
                    return logDTO;
                }).toList();

        logService.batchAdd(logs);
    }

    private <K> Object getResourceFieldValue(K resource, String fieldName) {
        Class<?> clazz = resource.getClass();
        // 获取字段值
        Object fieldValue = null;
        try {
            fieldValue = clazz.getMethod("get" + CaseFormatUtils.capitalizeFirstLetter(fieldName))
                    .invoke(resource);
        } catch (Exception e) {
            LogUtils.error(e);
        }
        return fieldValue;
    }

    private <K> void setResourceFieldValue(K resource, String fieldName, Object value) {
        Class<?> clazz = resource.getClass();
        // 设置字段值
        try {
            if (value != null) {
                Class<?> valueClass = value.getClass();
                switch (value) {
                    case List<?> ignored -> valueClass = List.class;
                    case Map<?, ?> ignored -> valueClass = Map.class;
                    case Integer i -> {
                        Class<?> type = clazz.getDeclaredField(fieldName).getType();
                        if (type.equals(BigDecimal.class)) {
                            value = BigDecimal.valueOf(i);
                        } else if (type.equals(Long.class)) {
                            value = Long.valueOf(i);
                        }
                        valueClass = value.getClass();
                    }
                    default -> {
                    }
                }
                clazz.getMethod("set" + CaseFormatUtils.capitalizeFirstLetter(fieldName), valueClass)
                        .invoke(resource, value);
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    private T newResourceField() {
        try {
            return getResourceFieldClass().getConstructor().newInstance();
        } catch (Exception e) {
            LogUtils.error(e);
            throw new GenericException(e);
        }
    }

    private V newResourceFieldBlob() {
        try {
            return getResourceFieldBlobClass().getConstructor().newInstance();
        } catch (Exception e) {
            LogUtils.error(e);
            throw new GenericException(e);
        }
    }

    /**
     * 查询指定资源的模块字段值
     *
     * @param resourceIds
     *
     * @return
     */
    public Map<String, List<BaseModuleFieldValue>> getResourceFieldMap(List<String> resourceIds, boolean withBlob) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            return Map.of();
        }
        Map<String, BaseField> fieldConfigMap = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class)).
                getAllFields(getFormKey(), OrganizationContext.getOrganizationId())
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));

        Map<String, List<BaseModuleFieldValue>> resourceMap = new HashMap<>();

        List<T> resourceFields = getResourceField(resourceIds);
        resourceFields.forEach(resourceField -> {
            if (resourceField.getFieldValue() != null) {
                BaseField fieldConfig = fieldConfigMap.get(resourceField.getFieldId());
                if (fieldConfig == null) {
                    return;
                }
                // 获取字段解析器
                AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
                // 将数据库中的字符串值,转换为对应的对象值
                Object objectValue = customFieldResolver.convertToValue(fieldConfig, resourceField.getFieldValue().toString());
                resourceField.setFieldValue(objectValue);

                String resourceId = resourceField.getResourceId();
                resourceMap.putIfAbsent(resourceId, new ArrayList<>());
                resourceMap.get(resourceId).add(new BaseModuleFieldValue(resourceField.getFieldId(), objectValue));
            }
        });

        if (!withBlob) {
            return resourceMap;
        }

        List<V> resourceFieldBlobs = getResourceFieldBlob(resourceIds);
        resourceFieldBlobs.forEach(resourceFieldBlob -> {
            // 处理大文本
            if (resourceFieldBlob != null && resourceFieldBlob.getFieldValue() != null) {
                BaseField fieldConfig = fieldConfigMap.get(resourceFieldBlob.getFieldId());
                if (fieldConfig == null) {
                    return;
                }
                AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
                Object objectValue = customFieldResolver.convertToValue(fieldConfig, resourceFieldBlob.getFieldValue().toString());

                String resourceId = resourceFieldBlob.getResourceId();
                resourceMap.putIfAbsent(resourceId, new ArrayList<>());
                resourceMap.get(resourceId).add(new BaseModuleFieldValue(resourceFieldBlob.getFieldId(), objectValue));
            }
        });

        return resourceMap;
    }

    private List<T> getResourceField(List<String> resourceIds) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        return getResourceFieldMapper().selectListByLambda(wrapper);
    }

    private List<T> getResourceField(List<String> resourceIds, String fieldId) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        wrapper.eq(BaseResourceField::getFieldId, fieldId);
        return getResourceFieldMapper().selectListByLambda(wrapper);
    }

    private List<V> getResourceFieldBlob(List<String> resourceIds) {
        LambdaQueryWrapper<V> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        return getResourceFieldBlobMapper().selectListByLambda(wrapper);
    }

    private List<V> getResourceFieldBlob(List<String> resourceIds, String fieldId) {
        LambdaQueryWrapper<V> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        wrapper.eq(BaseResourceField::getFieldId, fieldId);
        return getResourceFieldBlobMapper().selectListByLambda(wrapper);
    }

    /**
     * 删除指定资源的模块字段值
     *
     * @param resourceId
     */
    public void deleteByResourceId(String resourceId) {
        T example = newResourceField();
        example.setResourceId(resourceId);
        getResourceFieldMapper().delete(example);

        V blobExample = newResourceFieldBlob();
        blobExample.setResourceId(resourceId);
        getResourceFieldBlobMapper().delete(blobExample);
    }

    /**
     * 删除指定资源的模块字段值
     *
     * @param resourceIds
     */
    public void deleteByResourceIds(List<String> resourceIds) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        getResourceFieldMapper().deleteByLambda(wrapper);

        LambdaQueryWrapper<V> blobWrapper = new LambdaQueryWrapper<>();
        blobWrapper.in(BaseResourceField::getResourceId, resourceIds);
        getResourceFieldBlobMapper().deleteByLambda(blobWrapper);
    }

    /**
     * 保存指定资源的模块字段值
     *
     * @param moduleFieldValues
     */
    public void saveModuleFieldByResourceIds(List<String> resourceIds, List<BaseModuleFieldValue> moduleFieldValues) {
        if (CollectionUtils.isEmpty(moduleFieldValues)) {
            return;
        }

        Map<String, BaseField> fieldConfigMap = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class))
                .getAllFields(getFormKey(), OrganizationContext.getOrganizationId())
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));

        List<T> customerFields = new ArrayList<>();
        List<V> customerFieldBlobs = new ArrayList<>();
        moduleFieldValues.stream()
                .filter(BaseModuleFieldValue::valid)
                .forEach(fieldValue -> {
                    BaseField fieldConfig = fieldConfigMap.get(fieldValue.getFieldId());
                    if (fieldConfig == null) {
                        return;
                    }

                    // 获取字段解析器
                    AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
                    // 校验参数值
                    customFieldResolver.validate(fieldConfig, fieldValue.getFieldValue());
                    // 将参数值转换成字符串入库
                    String strValue = customFieldResolver.convertToString(fieldConfig, fieldValue.getFieldValue());
                    for (String resourceId : resourceIds) {
                        if (fieldConfig.isBlob()) {
                            V resourceField = newResourceFieldBlob();
                            resourceField.setId(IDGenerator.nextStr());
                            resourceField.setResourceId(resourceId);
                            resourceField.setFieldId(fieldValue.getFieldId());
                            resourceField.setFieldValue(strValue);
                            customerFieldBlobs.add(resourceField);
                        } else {
                            T resourceField = newResourceField();
                            resourceField.setId(IDGenerator.nextStr());
                            resourceField.setResourceId(resourceId);
                            resourceField.setFieldId(fieldValue.getFieldId());
                            resourceField.setFieldValue(strValue);
                            customerFields.add(resourceField);
                        }
                    }
                });

        if (CollectionUtils.isNotEmpty(customerFields)) {
            getResourceFieldMapper().batchInsert(customerFields);
        }

        if (CollectionUtils.isNotEmpty(customerFieldBlobs)) {
            getResourceFieldBlobMapper().batchInsert(customerFieldBlobs);
        }
    }

    /**
     * 处理临时附件
     */
    @SuppressWarnings("unchecked")
    private void preProcessTempAttachment(String orgId, String resourceId, String userId, Object processValue) {
        try {
            List<String> tmpPicIds = new ArrayList<>();
            if (processValue instanceof String) {
                tmpPicIds.add(processValue.toString());
            } else if (processValue instanceof List) {
                tmpPicIds.addAll((List<String>) processValue);
            }
            AttachmentService attachmentService = CommonBeanFactory.getBean(AttachmentService.class);
            UploadTransferRequest transferRequest = new UploadTransferRequest(orgId, resourceId, userId, tmpPicIds);
            if (attachmentService != null) {
                attachmentService.processTemp(transferRequest);
            }
        } catch (Exception e) {
            LogUtils.error("临时附件处理失败: {}", e.getMessage());
        }
    }

    /**
     * 批量更新自定义字段值
     *
     * @param request
     */
    public void batchUpdateFieldValues(ResourceBatchEditRequest request, BaseField field, ModuleField moduleField) {
        // 获取字段解析器
        AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
        // 校验参数值
        customFieldResolver.validate(field, request.getFieldValue());
        // 将参数值转换成字符串入库
        String strValue = customFieldResolver.convertToString(field, request.getFieldValue());
        if (moduleField == null) {
            throw new GenericException(Translator.get("module.field.not_exist"));
        }
        if (BaseField.isBlob(moduleField.getType())) {
            List<V> resourceFields = request.getIds().stream()
                    .map(id -> {
                        V resourceField = newResourceFieldBlob();
                        resourceField.setId(IDGenerator.nextStr());
                        resourceField.setResourceId(id);
                        resourceField.setFieldId(request.getFieldId());
                        resourceField.setFieldValue(strValue);
                        return resourceField;
                    }).collect(Collectors.toList());
            getResourceFieldBlobMapper().batchInsert(resourceFields);
        } else {
            List<T> resourceFields = request.getIds().stream()
                    .map(id -> {
                        T resourceField = newResourceField();
                        resourceField.setId(IDGenerator.nextStr());
                        resourceField.setResourceId(id);
                        resourceField.setFieldId(request.getFieldId());
                        resourceField.setFieldValue(strValue);
                        return resourceField;
                    }).collect(Collectors.toList());
            getResourceFieldMapper().batchInsert(resourceFields);
        }
    }

    /**
     * 批量删除自定义字段值
     *
     * @param request
     */
    public void batchDeleteFieldValues(ResourceBatchEditRequest request, ModuleField moduleField) {
        if (moduleField == null) {
            throw new GenericException(Translator.get("module.field.not_exist"));
        }
        if (BaseField.isBlob(moduleField.getType())) {
            // 先删除
            LambdaQueryWrapper<V> example = new LambdaQueryWrapper<>();
            example.eq(BaseResourceField::getFieldId, request.getFieldId());
            example.in(BaseResourceField::getResourceId, request.getIds());
            getResourceFieldBlobMapper().deleteByLambda(example);
        } else {
            // 先删除
            LambdaQueryWrapper<T> example = new LambdaQueryWrapper<>();
            example.eq(BaseResourceField::getFieldId, request.getFieldId());
            example.in(BaseResourceField::getResourceId, request.getIds());
            getResourceFieldMapper().deleteByLambda(example);
        }
    }

    public <K> void updateModuleFieldByAgent(K customer, List<BaseModuleFieldValue> originCustomerFields, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }

        if (CollectionUtils.isEmpty(originCustomerFields)) {
            saveModuleField(customer, orgId, userId, moduleFields, false);
        } else {
            updateModuleField(customer, orgId, userId, moduleFields, true);
        }
    }

    public <K> void updateModuleField(K resource, String orgId, String userId, List<BaseModuleFieldValue> moduleFieldValues, boolean update) {
        List<BaseField> allFields = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class))
                .getAllFields(getFormKey(), OrganizationContext.getOrganizationId());
        if (CollectionUtils.isEmpty(allFields)) {
            return;
        }
        String resourceId = (String) getResourceFieldValue(resource, "id");
        List<T> resourceFields = getResourceField(List.of(resourceId));

        Map<String, BaseModuleFieldValue> moduleFieldValueMap = moduleFieldValues.stream().collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, t -> t));

        // 校验业务字段，字段值是否重复
        businessFieldRepeatCheck(orgId, resource, List.of(resourceId), allFields);
        List<T> updateFields = new ArrayList<>();
        List<V> updateBlobFields = new ArrayList<>();

        resourceFields.forEach(resourceField -> {
            if (moduleFieldValueMap.containsKey(resourceField.getFieldId())) {
                BaseModuleFieldValue fieldValue = moduleFieldValueMap.get(resourceField.getFieldId());

                BaseField base = allFields.stream().filter(baseField -> baseField.getId().equals(resourceField.getFieldId())).findFirst().orElse(null);
                if (base != null) {
                    if (base.needRepeatCheck()) {
                        checkUnique(fieldValue, base);
                    }
                    // 获取字段解析器
                    AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(base.getType());
                    // 校验参数值
                    customFieldResolver.validate(base, fieldValue.getFieldValue());
                    // 将参数值转换成字符串入库
                    String strValue = customFieldResolver.convertToString(base, fieldValue.getFieldValue());
                    if (base.isBlob()) {
                        V fieldBlob = newResourceFieldBlob();
                        fieldBlob.setId(resourceField.getId());
                        fieldBlob.setResourceId(resourceField.getResourceId());
                        fieldBlob.setFieldId(resourceField.getFieldId());
                        fieldBlob.setFieldValue(strValue);
                        updateBlobFields.add(fieldBlob);
                    } else {
                        T field = newResourceField();
                        field.setId(resourceField.getId());
                        field.setResourceId(resourceField.getResourceId());
                        field.setFieldId(resourceField.getFieldId());
                        field.setFieldValue(strValue);
                        updateFields.add(field);
                    }

                }
            }
        });

        Map<String, T> resourceMap = resourceFields.stream().collect(Collectors.toMap(BaseResourceField::getFieldId, Function.identity()));
        Map<String, BaseField> allbaseFieldMap = allFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity()));
        List<BaseModuleFieldValue> addlist = moduleFieldValues.stream().filter(moduleField ->
                allbaseFieldMap.containsKey(moduleField.getFieldId()) && !resourceMap.containsKey(moduleField.getFieldId())
        ).toList();


        saveModuleField(resource, orgId, userId, addlist, update);

        updateData(updateFields, updateBlobFields);
    }

    private void updateData(List<T> updateFields, List<V> updateBlobFields) {
        for (T resourceField : updateFields) {
            getResourceFieldMapper().update(resourceField);
        }

        for (V updateBlobField : updateBlobFields) {
            getResourceFieldBlobMapper().update(updateBlobField);
        }
    }

    public List<ChartResult> translateAxisName(ModuleFormConfigDTO formConfig, ChartAnalysisDbRequest chartAnalysisDbRequest, List<ChartResult> chartResults) {
        ChartCategoryAxisDbParam categoryAxisParam = chartAnalysisDbRequest.getCategoryAxisParam();
        ChartCategoryAxisDbParam subCategoryAxisParam = chartAnalysisDbRequest.getSubCategoryAxisParam();

        chartResults = chartResults.stream()
                .filter(Objects::nonNull)
                .toList();

        BaseField categoryAxisField = getBaseField(formConfig.getFields(), categoryAxisParam.getFieldId());
        BaseField subCategoryAxisField = null;
        if (subCategoryAxisParam != null) {
            subCategoryAxisField = getBaseField(formConfig.getFields(), subCategoryAxisParam.getFieldId());
        }

        Map<String, List<OptionDTO>> optionMap = getChartOptionMap(formConfig, chartResults, categoryAxisParam,
                subCategoryAxisParam);

        Map<String, String> categoryOptionMap = Optional.ofNullable(optionMap.get(categoryAxisField.getId()))
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

        Map<String, String> subCategoryOptionMap = null;
        if (subCategoryAxisParam != null) {
            subCategoryOptionMap = Optional.ofNullable(optionMap.get(subCategoryAxisField.getId()))
                    .orElse(List.of())
                    .stream()
                    .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
        }

        for (ChartResult chartResult : chartResults) {
            if (categoryOptionMap.get(chartResult.getCategoryAxis()) != null) {
                chartResult.setCategoryAxisName(categoryOptionMap.get(chartResult.getCategoryAxis()));
            } else {
                chartResult.setCategoryAxisName(chartResult.getCategoryAxis());
            }

            if (subCategoryAxisParam != null && subCategoryOptionMap.get(chartResult.getSubCategoryAxis()) != null) {
                chartResult.setSubCategoryAxisName(subCategoryOptionMap.get(chartResult.getSubCategoryAxis()));
            } else {
                chartResult.setSubCategoryAxisName(chartResult.getSubCategoryAxis());
            }

            if (chartResult.getValueAxis() == null) {
                chartResult.setValueAxis(0);
            }

            if (categoryAxisField.isLocation()) {
                chartResult.setCategoryAxisName(RegionUtils.codeToName(chartResult.getCategoryAxis()));
                chartResult.setCategoryAxis(RegionUtils.getCode(chartResult.getCategoryAxis()));
            }

            if (subCategoryAxisField != null && subCategoryAxisField.isLocation()) {
                chartResult.setSubCategoryAxisName(RegionUtils.codeToName(chartResult.getSubCategoryAxis()));
                chartResult.setSubCategoryAxis(RegionUtils.getCode(chartResult.getSubCategoryAxis()));
            }
        }

        if (categoryAxisField.isLocation()) {
            // 合并CategoryAxis相同的项
            chartResults = mergeResult(chartResults, ChartResult::getCategoryAxis);
        }

        if (subCategoryAxisField != null && subCategoryAxisField.isLocation()) {
            // 合并SubCategoryAxis相同的项
            chartResults = mergeResult(chartResults, ChartResult::getSubCategoryAxis);
        }

        return chartResults;
    }

    private Map<String, List<OptionDTO>> getChartOptionMap(ModuleFormConfigDTO formConfig, List<ChartResult> chartResults,
                                                           ChartCategoryAxisDbParam categoryAxisParam,
                                                           ChartCategoryAxisDbParam subCategoryAxisParam) {
        BaseField categoryAxisField = getBaseField(formConfig.getFields(), categoryAxisParam.getFieldId());
        List<BaseModuleFieldValue> moduleFieldValues = new ArrayList<>();
        for (ChartResult chartResult : chartResults) {
            if (categoryAxisField.hasOptions()) {
                BaseModuleFieldValue categoryFieldValue = getBaseModuleFieldValue(categoryAxisParam.getFieldId(), categoryAxisField);
                categoryFieldValue.setFieldValue(chartResult.getCategoryAxis());
                moduleFieldValues.add(categoryFieldValue);
            }

            if (subCategoryAxisParam != null) {
                BaseField subCategoryAxisField = getBaseField(formConfig.getFields(), subCategoryAxisParam.getFieldId());
                if (subCategoryAxisField.hasOptions()) {
                    BaseModuleFieldValue subCategoryValue = getBaseModuleFieldValue(subCategoryAxisParam.getFieldId(), subCategoryAxisField);
                    subCategoryValue.setFieldValue(chartResult.getSubCategoryAxis());
                    moduleFieldValues.add(subCategoryValue);
                }
            }
        }

        moduleFieldValues = moduleFieldValues.stream()
                .filter(BaseModuleFieldValue::valid)
                .distinct().toList();

        if (CollectionUtils.isEmpty(moduleFieldValues)) {
            return Map.of();
        }

        // 获取选项值对应的 option
        return moduleFormService.getOptionMap(formConfig, moduleFieldValues);
    }
}