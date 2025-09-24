package cn.cordys.common.service;

import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.*;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.PicService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
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

    public static boolean isNullOrEmpty(Object obj) {
        return obj == null || (obj instanceof String && ((String) obj).isEmpty());
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
        List<BaseField> allFields = CommonBeanFactory.getBean(ModuleFormService.class)
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

                    if (field.isPic()) {
                        preProcessWithPic(orgId, resourceId, userId, fieldValue.getFieldValue());
                    }

                    if (field.needRepeatCheck()) {
                        checkUnique(fieldValue, field);
                    }

                    // 获取字段解析器
                    AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
                    // 校验参数值
                    customFieldResolver.validate(field, fieldValue.getFieldValue());
                    // 将参数值转换成字符串入库
                    String strValue = customFieldResolver.parse2String(field, fieldValue.getFieldValue());

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
                businessFieldRepeatCheck(orgId, resource, updateIds, field, businessModuleField);
            }
        });
    }

    private <K> void businessFieldRepeatCheck(String orgId, K resource, List<String> updateIds, BaseField field, BusinessModuleField businessModuleField) {
        if (!field.needRepeatCheck()) {
            return;
        }
        String fieldName = businessModuleField.getBusinessKey();
        Class<?> clazz = resource.getClass();
        String tableName = CaseFormatUtils.camelToUnderscore(clazz.getSimpleName());

        Object fieldValue = getResourceFieldValue(resource, fieldName);

        if (!isNullOrEmpty(fieldValue)) {
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
                             List<K> originResourceList,
                             Class<K> clazz,
                             String logModule,
                             BiConsumer<List<String>, K> batchInsertFunc,
                             String userId,
                             String orgId) {
        BusinessModuleField businessModuleField = getBusinessModuleField(request.getFieldId());

        K resource = newInstance(clazz);

        // 修改更新时间和用户
        setResourceFieldValue(resource, "updateTime", System.currentTimeMillis());
        setResourceFieldValue(resource, "updateUser", userId);

        BaseField field;

        List<BaseField> allFields = CommonBeanFactory.getBean(ModuleFormService.class)
                .getAllFields(getFormKey(), OrganizationContext.getOrganizationId());
        if (businessModuleField != null) {
           field = allFields.stream()
                   .filter(f -> businessModuleField.getBusinessKey().equals(f.getBusinessKey()))
                   .findFirst()
                   .orElseThrow(() -> new GenericException(Translator.get("module.field.not_exist")));
        } else {
            field = allFields.stream()
                    .filter(f -> request.getFieldId().equals(f.getId()))
                    .findFirst()
                    .orElseThrow(() -> new GenericException(Translator.get("module.field.not_exist")));
        }

        if (field.needRepeatCheck() && request.getIds().size() > 1) {
            // 如果字段唯一，则校验不能同时修改多条
            throw new GenericException(Translator.getWithArgs("common.field_value.repeat", field.getName()));
        }

        if (businessModuleField != null) {
            setResourceFieldValue(resource, businessModuleField.getBusinessKey(), request.getFieldValue());
            // 字段唯一性校验
            businessFieldRepeatCheck(orgId, resource, request.getIds(), field, businessModuleField);
            // 添加日志
            addBusinessFieldBatchUpdateLog(originResourceList, request, logModule, userId, orgId);
        } else {
            if (field.needRepeatCheck()) {
                // 字段唯一性校验
                checkUnique(BeanUtils.copyBean(new BaseModuleFieldValue(), request), field);
            }
            // 获取字段解析器
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
            // 校验参数值
            customFieldResolver.validate(field, request.getFieldValue());
            // 将参数值转换成字符串入库
            String strValue = customFieldResolver.parse2String(field, request.getFieldValue());
            request.setFieldValue(strValue);

            // 查询修改前的字段，记录日志
            List<? extends BaseResourceField> originFields;
            if (field.isBlob()) {
                originFields = getResourceFieldBlob(request.getIds(), request.getFieldId());
            } else {
                originFields = getResourceField(request.getIds(), request.getFieldId());
            }

            batchUpdate(request);

            // 添加日志
            addCustomFieldBatchUpdateLog(originResourceList, originFields, request, field, logModule, userId, orgId);
        }

        // 批量修改业务字段和更新时间等
        batchInsertFunc.accept(request.getIds(), resource);
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
     * @param originResourceList
     * @param request
     * @param userId
     * @param orgId
     * @param <K>
     */
    private <K> void addBusinessFieldBatchUpdateLog(List<K> originResourceList,
                                                    ResourceBatchEditRequest request,
                                                    String logModule,
                                                    String userId,
                                                    String orgId) {
        // 记录日志
        List<LogDTO> logs = originResourceList.stream()
                .map(resource -> {
                    Object originResource = newInstance(resource.getClass());
                    setResourceFieldValue(originResource, request.getFieldId(), getResourceFieldValue(resource, request.getFieldId()));
                    Object modifiedResource = newInstance(resource.getClass());
                    setResourceFieldValue(modifiedResource, request.getFieldId(), request.getFieldValue());

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
                    originResource.put(request.getFieldId(), baseResourceField == null ? null : baseResourceField.getFieldValue());

                    Map modifiedResource = new HashMap();
                    modifiedResource.put(request.getFieldId(), request.getFieldValue());

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

    private <K> Object setResourceFieldValue(K resource, String fieldName, Object value) {
        Class<?> clazz = resource.getClass();
        // 设置字段值
        Object fieldValue = null;
        try {
            fieldValue = clazz.getMethod("set" + CaseFormatUtils.capitalizeFirstLetter(fieldName), value.getClass())
                    .invoke(resource, value);
        } catch (Exception e) {
            LogUtils.error(e);
        }
        return fieldValue;
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
    public Map<String, List<BaseModuleFieldValue>> getResourceFieldMap(List<String> resourceIds) {
        return getResourceFieldMap(resourceIds, false);
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
        Map<String, BaseField> fieldConfigMap = CommonBeanFactory.getBean(ModuleFormService.class).
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
                Object objectValue = customFieldResolver.parse2Value(fieldConfig, resourceField.getFieldValue().toString());
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
                Object objectValue = customFieldResolver.parse2Value(fieldConfig, resourceFieldBlob.getFieldValue().toString());

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

    public <T extends BaseModuleFieldValue> List<T> getCustomerFields(String formKey,
                                                                      List<BaseModuleFieldValue> moduleFieldValues,
                                                                      Class<T> clazz) {
        if (CollectionUtils.isEmpty(moduleFieldValues)) {
            return List.of();
        }

        Map<String, BaseField> fieldConfigMap = CommonBeanFactory.getBean(ModuleFormService.class)
                .getAllFields(formKey, OrganizationContext.getOrganizationId())
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));

        List<T> customerFields = new ArrayList<>();
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
                    String strValue = customFieldResolver.parse2String(fieldConfig, fieldValue.getFieldValue());

                    try {
                        T moduleField = clazz.getConstructor().newInstance();
                        moduleField.setFieldId(fieldValue.getFieldId());
                        moduleField.setFieldValue(strValue);
                        customerFields.add(moduleField);
                    } catch (Exception e) {
                        LogUtils.error(e);
                    }
                });
        return customerFields;
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
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        getResourceFieldMapper().deleteByLambda(wrapper);

        LambdaQueryWrapper<V> blobWrapper = new LambdaQueryWrapper();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
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

        Map<String, BaseField> fieldConfigMap = CommonBeanFactory.getBean(ModuleFormService.class)
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
                    String strValue = customFieldResolver.parse2String(fieldConfig, fieldValue.getFieldValue());
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
     * 图片类型字段的前置处理
     */
    private void preProcessWithPic(String orgId, String resourceId, String userId, Object processValue) {
        try {
            // 图片类型前置处理
            List<String> tmpPicIds = new ArrayList<>();
            if (processValue instanceof String) {
                tmpPicIds.add(processValue.toString());
            } else if (processValue instanceof List) {
                tmpPicIds.addAll((List<String>) processValue);
            }
            PicService picService = CommonBeanFactory.getBean(PicService.class);
            UploadTransferRequest transferRequest = new UploadTransferRequest(orgId, resourceId, userId, tmpPicIds);
            if (picService != null) {
                picService.processTemp(transferRequest);
            }
        } catch (Exception e) {
            LogUtils.error("图片字段处理失败", e);
        }
    }

    /**
     * 批量更新自定义字段
     *
     * @param request
     */
    public void batchUpdate(ResourceBatchEditRequest request) {
        ModuleField moduleField = moduleFieldMapper.selectByPrimaryKey(request.getFieldId());
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
                        resourceField.setFieldValue(request.getFieldValue());
                        return resourceField;
                    }).collect(Collectors.toList());
            // 先删除
            BaseMapper<V> resourceFieldMapper = getResourceFieldBlobMapper();
            LambdaQueryWrapper<V> example = new LambdaQueryWrapper<>();
            example.eq(BaseResourceField::getFieldId, request.getFieldId());
            example.in(BaseResourceField::getResourceId, request.getIds());
            resourceFieldMapper.deleteByLambda(example);
            // 再添加
            resourceFieldMapper.batchInsert(resourceFields);
        } else {
            List<T> resourceFields = request.getIds().stream()
                    .map(id -> {
                        T resourceField = newResourceField();
                        resourceField.setId(IDGenerator.nextStr());
                        resourceField.setResourceId(id);
                        resourceField.setFieldId(request.getFieldId());
                        resourceField.setFieldValue(request.getFieldValue());
                        return resourceField;
                    }).collect(Collectors.toList());
            // 先删除
            BaseMapper<T> resourceFieldMapper = getResourceFieldMapper();
            LambdaQueryWrapper<T> example = new LambdaQueryWrapper<>();
            example.eq(BaseResourceField::getFieldId, request.getFieldId());
            example.in(BaseResourceField::getResourceId, request.getIds());
            resourceFieldMapper.deleteByLambda(example);
            // 再添加
            resourceFieldMapper.batchInsert(resourceFields);
        }
    }

    public BusinessModuleField getBusinessModuleField(String fieldId) {
        return Arrays.stream(BusinessModuleField.values())
                .filter(field -> field.getBusinessKey().equals(fieldId))
                .findFirst()
                .orElse(null);
    }
}