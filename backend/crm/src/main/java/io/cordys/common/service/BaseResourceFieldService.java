package io.cordys.common.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.domain.BaseResourceField;
import io.cordys.common.exception.GenericException;
import io.cordys.common.resolver.field.AbstractModuleFieldResolver;
import io.cordys.common.resolver.field.ModuleFieldResolverFactory;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.uid.SerialNumGenerator;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.Translator;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.field.SerialNumberField;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.request.UploadTransferRequest;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.crm.system.service.PicService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    protected abstract String getFormKey();

    protected abstract BaseMapper<T> getResourceFieldMapper();

    protected abstract BaseMapper<V> getResourceFieldBlobMapper();

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
     * @return
     */
    public List<BaseModuleFieldValue> getModuleFieldValuesByResourceId(String resourceId) {
        List<BaseModuleFieldValue> fieldValues = getResourceFieldMap(List.of(resourceId), true).get(resourceId);
        return fieldValues == null ? List.of() : fieldValues;
    }

    /**
     * @param moduleFieldValues
     */
    public void saveModuleField(String resourceId, String orgId, String userId, List<BaseModuleFieldValue> moduleFieldValues, boolean update) {
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
                .filter(item -> {
                    BaseField fieldConfig = fieldConfigMap.get(item.getFieldId());
                    return item.valid() || (fieldConfig != null && fieldConfig.isSerialNumber());
                })
                .forEach(fieldValue -> {
                    BaseField fieldConfig = fieldConfigMap.get(fieldValue.getFieldId());
                    if (fieldConfig == null) {
                        return;
                    }

                    if (fieldConfig.isSerialNumber() && !update) {
                        String serialNo = serialNumGenerator.generateByRules(((SerialNumberField) fieldConfig).getSerialNumberRules());
                        fieldValue.setFieldValue(serialNo);
                    }

                    if (fieldConfig.isPic()) {
                        preProcessWithPic(orgId, resourceId, userId, fieldValue.getFieldValue());
                    }

                    if (fieldConfig.needRepeatCheck()) {
                        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
                        wrapper.eq(BaseResourceField::getFieldId, fieldValue.getFieldId());
                        wrapper.eq(BaseResourceField::getFieldValue, fieldValue.getFieldValue());
                        if (!getResourceFieldMapper().selectListByLambda(wrapper).isEmpty()) {
                            throw new GenericException(Translator.getWithArgs("common.field_value.repeat", fieldConfig.getName()));
                        }
                    }

                    // 获取字段解析器
                    AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
                    // 校验参数值
                    customFieldResolver.validate(fieldConfig, fieldValue.getFieldValue());
                    // 将参数值转换成字符串入库
                    String strValue = customFieldResolver.parse2String(fieldConfig, fieldValue.getFieldValue());

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

                });

        if (CollectionUtils.isNotEmpty(customerFields)) {
            getResourceFieldMapper().batchInsert(customerFields);
        }

        if (CollectionUtils.isNotEmpty(customerFieldBlobs)) {
            getResourceFieldBlobMapper().batchInsert(customerFieldBlobs);
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
     * @return
     */
    public Map<String, List<BaseModuleFieldValue>> getResourceFieldMap(List<String> resourceIds) {
        return getResourceFieldMap(resourceIds, false);
    }

    /**
     * 查询指定资源的模块字段值
     *
     * @param resourceIds
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

    private List<V> getResourceFieldBlob(List<String> resourceIds) {
        LambdaQueryWrapper<V> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
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
}