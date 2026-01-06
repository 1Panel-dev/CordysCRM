package cn.cordys.common.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.context.SourceDetailResolveContext;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.dto.BatchUpdateDbParam;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.*;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.annotation.Lazy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 资源与模块字段值的公共处理类
 *
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Slf4j
public abstract class BaseResourceFieldService<T extends BaseResourceField, V extends BaseResourceField> extends BaseResourceService {

    @Resource
    private SerialNumGenerator serialNumGenerator;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Lazy
    @Resource
    private FieldSourceServiceProvider fieldSourceServiceProvider;

    private static final String SOURCE_DETAIL_ID = "id";
    private static final String ROW_BIZ_ID = "id";
    private static final String DETAIL_FIELD_PARAM_NAME = "moduleFields";
    private static final String PRICE_SUB_ROW_KEY = "price_sub";

    /**
     * 获取资源字段类型 (T)
     *
     * @return 资源字段类型
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Class<T> getResourceFieldClass() {
        Type type = getGenericType(0);
        if (type instanceof Class tClass) {
            return tClass;
        } else {
            throw new IllegalArgumentException("Type cannot be converted to Class: " + type);
        }
    }

    /**
     * 获取资源大字段类型 (V)
     *
     * @return 资源大字段类型
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Class<V> getResourceFieldBlobClass() {
        Type type = getGenericType(1);
        if (type instanceof Class vClass) {
            return vClass;
        } else {
            throw new IllegalArgumentException("Type cannot be converted to Class: " + type);
        }
    }

    /**
     * 获取泛型类型
     *
     * @param index 泛型参数索引
     *
     * @return 泛型类型
     */
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
     * 获取表单Key
     *
     * @return 表单Key
     */
    protected abstract String getFormKey();

    /**
     * 获取资源字段Mapper
     *
     * @return FieldMapper
     */
    protected abstract BaseMapper<T> getResourceFieldMapper();

    /**
     * 获取资源大字段Mapper
     *
     * @return BlobFieldMapper
     */
    protected abstract BaseMapper<V> getResourceFieldBlobMapper();

    /**
     * 获取资源所有字段值
     *
     * @param resourceId 资源ID
     *
     * @return 字段值集合
     */
    public List<BaseModuleFieldValue> getModuleFieldValuesByResourceId(String resourceId) {
        List<BaseModuleFieldValue> fieldValues = getResourceFieldMap(List.of(resourceId), true).get(resourceId);
        return fieldValues == null ? new ArrayList<>(0) : fieldValues;
    }

    /**
     * 保存字段值
     *
     * @param resource          资源
     * @param orgId             组织ID
     * @param userId            用户ID
     * @param moduleFieldValues 自定义字段值
     * @param update            是否更新
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
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
            fieldValueMap = new HashMap<>(8);
        }

        // 校验业务字段，字段值是否重复
        businessFieldRepeatCheck(orgId, resource, update ? List.of(resourceId) : List.of(), allFields);

        List<T> customerFields = new ArrayList<>();
        List<V> customerFieldBlobs = new ArrayList<>();
        allFields.stream()
                .filter(field -> {
                    BaseModuleFieldValue fieldValue = fieldValueMap.get(field.getId());
                    return (fieldValue != null && fieldValue.valid()) ||
                            (field.isSerialNumber() && StringUtils.isEmpty(field.getBusinessKey())) || field.isSubField();
                })
                .forEach(field -> {
                    BaseModuleFieldValue fieldValue = processSpecialFieldValue(field, fieldValueMap, update, orgId);
                    if (fieldValue == null || fieldValue.getFieldValue() == null || StringUtils.isNotEmpty(field.getResourceFieldId())) {
                        return;
                    }
                    // 处理子表格值
                    if (field.isSubField() && fieldValue.getFieldValue() != null) {
                        saveSubFieldValue(resourceId, (SubField) field, fieldValue, customerFields, customerFieldBlobs);
                        return;
                    }

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
                        customerFieldBlobs.add(supplyNewResource(this::newResourceFieldBlob, resourceId, fieldValue.getFieldId(), strValue));
                    } else {
                        customerFields.add(supplyNewResource(this::newResourceField, resourceId, fieldValue.getFieldId(), strValue));
                    }
                });

        // process all attachment field
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
     * 保存子字段值
     *
     * @param resourceId 资源ID
     * @param subField   子字段信息
     * @param fieldValue 子字段值
     * @param fields     字段值集合
     * @param fieldBlobs 大字段值集合
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void saveSubFieldValue(String resourceId, SubField subField, BaseModuleFieldValue fieldValue,
                                  List<T> fields, List<V> fieldBlobs) {
        List<Map<String, Object>> subValues = (List) fieldValue.getFieldValue();
        if (CollectionUtils.isEmpty(subValues)) {
            return;
        }
        Map<String, BaseField> subFieldMap = new HashMap<>(subField.getSubFields().size());
        subField.getSubFields().forEach(f -> {
            if (StringUtils.isNotEmpty(f.getResourceFieldId())) {
                return;
            }
            if (StringUtils.isNotEmpty(f.getBusinessKey())) {
                subFieldMap.put(f.getBusinessKey(), f);
            } else {
                subFieldMap.put(f.getId(), f);
            }
        });
        int rowId = 1;
        for (Map<String, Object> subValue : subValues) {
            String bizId = IDGenerator.nextStr();
            for (Map.Entry<String, Object> kv : subValue.entrySet()) {
				if (Strings.CS.equals(kv.getKey(), PRICE_SUB_ROW_KEY) && kv.getValue() != null) {
					T t = supplyNewResource(this::newResourceField, resourceId, kv.getKey(), kv.getValue().toString());
					setResourceFieldValue(t, "rowId", String.valueOf(rowId));
					setResourceFieldValue(t, "refSubId", subField.getId());
					setResourceFieldValue(t, "bizId", bizId);
					fields.add(t);
					continue;
				}
                BaseField field = subFieldMap.get(kv.getKey());
                if (field == null) {
                    continue;
                }
                AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
                customFieldResolver.validate(field, kv.getValue());
                if (kv.getValue() == null) {
                    continue;
                }
                String strValue = customFieldResolver.convertToString(field, kv.getValue());
                if (field.isBlob()) {
                    V v = supplyNewResource(this::newResourceFieldBlob, resourceId, kv.getKey(), strValue);
                    setResourceFieldValue(v, "rowId", String.valueOf(rowId));
                    setResourceFieldValue(v, "refSubId", subField.getId());
                    setResourceFieldValue(v, "bizId", bizId);
                    fieldBlobs.add(v);
                } else {
                    T t = supplyNewResource(this::newResourceField, resourceId, kv.getKey(), strValue);
                    setResourceFieldValue(t, "rowId", String.valueOf(rowId));
                    setResourceFieldValue(t, "refSubId", subField.getId());
                    setResourceFieldValue(t, "bizId", bizId);
                    fields.add(t);
                }
            }
            rowId++;
        }
    }

    /**
     * 校验业务字段，字段值是否重复
     *
     * @param orgId     组织ID
     * @param resource  资源
     * @param updateIds 资源ID集合
     * @param allFields 所有字段
     * @param <K>       资源类型
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

    /**
     * 批量更新自定义字段或业务字段值
     *
     * @param request            批量参数
     * @param field              字段信息
     * @param originResourceList 旧的字段值
     * @param clazz              资源类对象
     * @param logModule          日志类型
     * @param batchInsertFunc    批量更新回调函数
     * @param userId             当前用户ID
     * @param orgId              当前组织ID
     * @param <K>                资源类型
     */
    public <K> void batchUpdate(ResourceBatchEditRequest request,
                                BaseField field,
                                List<K> originResourceList,
                                Class<K> clazz,
                                String logModule,
                                Consumer<BatchUpdateDbParam> batchInsertFunc,
                                String userId,
                                String orgId) {

        if (field.needRepeatCheck() && request.getIds().size() > 1 && isNotBlank(request.getFieldValue())) {
            // 如果字段唯一，则校验不能同时修改多条
            throw new GenericException(Translator.getWithArgs("common.field_value.repeat", field.getName()));
        }

        BatchUpdateDbParam updateParam = new BatchUpdateDbParam();
        updateParam.setIds(request.getIds());
        // 修改更新时间和用户
        updateParam.setUpdateTime(System.currentTimeMillis());
        updateParam.setUpdateUser(userId);

        if (StringUtils.isNotBlank(field.getBusinessKey())) {
            // 设置值&&唯一性校验
            K resource = newInstance(clazz);
            setResourceFieldValue(resource, field.getBusinessKey(), request.getFieldValue());
            businessFieldRepeatCheck(orgId, resource, request.getIds(), field, field.getBusinessKey());
            updateParam.setFieldName(field.getBusinessKey());
            updateParam.setFieldValue(request.getFieldValue());
            // 添加日志
            addBusinessFieldBatchUpdateLog(originResourceList, field, request, logModule, userId, orgId);
        } else {
            ModuleField moduleField = moduleFieldMapper.selectByPrimaryKey(request.getFieldId());
            List<? extends BaseResourceField> originFields;
            if (field.isBlob()) {
                originFields = getResourceFieldBlob(request.getIds(), request.getFieldId());
            } else {
                originFields = getResourceField(request.getIds(), request.getFieldId());
            }
            // 删除旧值 && 唯一性校验 && 添加新值
            batchDeleteFieldValues(request, moduleField);
            if (field.needRepeatCheck() && isNotBlank(request.getFieldValue())) {
                checkUnique(BeanUtils.copyBean(new BaseModuleFieldValue(), request), field);
            }
            if (isNotBlank(request.getFieldValue())) {
                batchUpdateFieldValues(request, field, moduleField);
            }
            // 添加日志
            addCustomFieldBatchUpdateLog(originResourceList, originFields, request, field, logModule, userId, orgId);
        }
        // 批量修改业务字段和更新时间等
        batchInsertFunc.accept(updateParam);
    }

    /**
     * 查询指定资源的模块字段值
     *
     * @param resourceIds 资源ID集合
     * @param withBlob    是否包含大文本字段
     *
     * @return 字段集合
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, List<BaseModuleFieldValue>> getResourceFieldMap(List<String> resourceIds, boolean withBlob) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            return Map.of();
        }
        SourceDetailResolveContext.start();
        try {
            List<BaseField> flattenFormFields = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class)).
                    getFlattenFormFields(getFormKey(), OrganizationContext.getOrganizationId());
            Map<String, BaseField> fieldConfigMap = flattenFormFields.stream().collect(Collectors.toMap(BaseField::getId, f -> f, (prev, next) -> next));
            Map<String, List<BaseModuleFieldValue>> resourceMap = new HashMap<>(8);
            List<T> resourceFields = getResourceField(resourceIds);
            getSourceDetailMapByIds(flattenFormFields, resourceFields);
            resourceFields.forEach(resourceField -> {
                if (resourceField instanceof BaseResourceSubField subResourceField && StringUtils.isNotEmpty(subResourceField.getRefSubId())) {
                    return;
                }
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
                    if (objectValue == null) {
                        return;
                    }
                    String resourceId = resourceField.getResourceId();
                    resourceMap.putIfAbsent(resourceId, new ArrayList<>());
                    resourceMap.get(resourceId).add(new BaseModuleFieldValue(resourceField.getFieldId(), objectValue));
                    // 处理数据源字段显示值
                    if (fieldConfig instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields())) {
                        // 处理展示列
                        Map<String, Object> detailMap = SourceDetailResolveContext.getSourceMap().get(objectValue.toString());
                        if (MapUtils.isEmpty(detailMap)) {
                            return;
                        }
                        sourceField.getShowFields().forEach(id -> {
                            BaseField showFieldConfig = fieldConfigMap.get(id);
                            if (showFieldConfig == null) {
                                return;
                            }
                            resourceMap.get(resourceId).add(new BaseModuleFieldValue(id, getFieldValueOfDetailMap(showFieldConfig, detailMap)));
                        });
                    }
                }
            });
            // 提前获取大文本字段值
            List<V> resourceFieldBlobs = getResourceFieldBlob(resourceIds);
            // 处理子表格字段值
            setResourceSubFieldValue(resourceMap, fieldConfigMap, ListUtils.union(resourceFields, resourceFieldBlobs));
            if (!withBlob) {
                return resourceMap;
            }
            resourceFieldBlobs.forEach(resourceFieldBlob -> {
                if (resourceFieldBlob instanceof BaseResourceSubField subResourceField && StringUtils.isNotEmpty(subResourceField.getRefSubId())) {
                    return;
                }
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
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Map.of();
        } finally {
            SourceDetailResolveContext.end();
        }
    }

    /**
     * 数据源业务字段引用值回显
     *
     * @param details 详情集合
     * @param fields  表单字段集合
     *
     * @return 字段值集合
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<BaseModuleFieldValue>> setBusinessRefFieldValue(List<?> details, List<BaseField> fields,
                                                                            Map<String, List<BaseModuleFieldValue>> resourceFieldMap) {
        List<BaseField> sourceBusinessFields = fields.stream().filter(field -> field instanceof DatasourceField sourceField &&
                CollectionUtils.isNotEmpty(sourceField.getShowFields()) && StringUtils.isNotEmpty(sourceField.getBusinessKey())).toList();
        if (CollectionUtils.isEmpty(sourceBusinessFields) || MapUtils.isEmpty(resourceFieldMap)) {
            return Map.of();
        }
        Map<String, BaseField> fieldConfigMap = fields.stream().collect(Collectors.toMap(BaseField::getId, f -> f, (prev, next) -> next));
        details.forEach(detail -> {
            Map<String, Object> detailMap = JSON.MAPPER.convertValue(detail, Map.class);
            sourceBusinessFields.forEach(bsf -> {
                DatasourceField sourceField = (DatasourceField) bsf;
                Object val = detailMap.get(sourceField.getBusinessKey());
                if (val == null) {
                    return;
                }
                if (!SourceDetailResolveContext.getSourceMap().containsKey(val.toString())) {
                    FieldSourceType sourceType = FieldSourceType.valueOf(sourceField.getDataSourceType());
                    try {
                        Object sourceObj = fieldSourceServiceProvider.safeGetById(sourceType, val.toString());
                        if (detail != null) {
                            SourceDetailResolveContext.put(val.toString(), JSON.MAPPER.convertValue(sourceObj, Map.class));
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        return;
                    }
                }
                Map<String, Object> sourceDetail = SourceDetailResolveContext.getSourceMap().get(val.toString());
                if (MapUtils.isEmpty(sourceDetail) || detail == null) {
                    return;
                }
                sourceField.getShowFields().forEach(id -> {
                    BaseField showFieldConfig = fieldConfigMap.get(id);
                    if (showFieldConfig == null || !detailMap.containsKey(SOURCE_DETAIL_ID)) {
                        return;
                    }
                    String resourceId = detailMap.get(SOURCE_DETAIL_ID).toString();
                    resourceFieldMap.putIfAbsent(resourceId, new ArrayList<>());
                    resourceFieldMap.get(resourceId).add(new BaseModuleFieldValue(id, getFieldValueOfDetailMap(showFieldConfig, sourceDetail)));
                });
            });
        });
        return resourceFieldMap;
    }

    /**
     * 获取详情中的字段值
     *
     * @param field           字段信息
     * @param sourceDetailMap 来源详情
     *
     * @return 字段值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object getFieldValueOfDetailMap(BaseField field, Map<String, Object> sourceDetailMap) {
        if (MapUtils.isEmpty(sourceDetailMap)) {
            return null;
        }
        if (StringUtils.isNotEmpty(field.getBusinessKey())) {
            return sourceDetailMap.get(field.getBusinessKey());
        }
        if (sourceDetailMap.containsKey(DETAIL_FIELD_PARAM_NAME)) {
            List<Map> fvs = (List<Map>) sourceDetailMap.get(DETAIL_FIELD_PARAM_NAME);
            for (Map fv : fvs) {
                if (field.getId().equals(fv.get("fieldId"))) {
                    return fv.get("fieldValue");
                }
            }
        }
        return null;
    }

    /**
     * 子字段值回显
     *
     * @param targetId     显示的子字段
     * @param sourceDetail 来源详情
     * @param subKey       子表格业务Key
     * @param rowKey 	   行Key
     *
     * @return 匹配值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object matchSubFieldValueOfDetailMap(String targetId, Map<String, Object> sourceDetail, String subKey, String rowKey) {
        if (MapUtils.isEmpty(sourceDetail) || !sourceDetail.containsKey(subKey)) {
            return null;
        }
        // 子表格数据集合
        List<Map<String, Object>> rows = (List) sourceDetail.get(subKey);
        for (Map<String, Object> row : rows) {
            // 匹配行数据
            if (row.containsKey(ROW_BIZ_ID) && Strings.CS.equals(rowKey, row.get(ROW_BIZ_ID).toString())) {
                return row.get(targetId);
            }
        }
        return null;
    }

    /**
     * 删除指定资源的模块字段值
     *
     * @param resourceId 资源ID
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
     * @param resourceIds 资源ID集合
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
     * @param moduleFieldValues 字段值集合
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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
                            customerFieldBlobs.add(supplyNewResource(this::newResourceFieldBlob, resourceId, fieldValue.getFieldId(), strValue));
                        } else {
                            customerFields.add(supplyNewResource(this::newResourceField, resourceId, fieldValue.getFieldId(), strValue));
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
     * 批量更新自定义字段值
     *
     * @param request 批量编辑参数
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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
                    .map(id -> supplyNewResource(this::newResourceFieldBlob, id, request.getFieldId(), strValue)).collect(Collectors.toList());
            getResourceFieldBlobMapper().batchInsert(resourceFields);
        } else {
            List<T> resourceFields = request.getIds().stream()
                    .map(id -> supplyNewResource(this::newResourceField, id, request.getFieldId(), strValue)).collect(Collectors.toList());
            getResourceFieldMapper().batchInsert(resourceFields);
        }
    }

    /**
     * 批量删除自定义字段值
     *
     * @param request 批量参数
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

    /**
     * 更新自定义字段值 (MCP调用)
     *
     * @param resource             资源对象
     * @param originCustomerFields 原有字段值
     * @param moduleFields         新的字段值
     * @param orgId                组织ID
     * @param userId               当前用户ID
     * @param <K>                  资源类型
     */
    public <K> void updateModuleFieldByAgent(K resource, List<BaseModuleFieldValue> originCustomerFields, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        if (CollectionUtils.isEmpty(originCustomerFields)) {
            saveModuleField(resource, orgId, userId, moduleFields, false);
        } else {
            updateModuleField(resource, orgId, userId, moduleFields, true);
        }
    }

    /**
     * 更新自定义字段值
     *
     * @param resource          资源对象
     * @param orgId             组织ID
     * @param userId            当前用户ID
     * @param moduleFieldValues 字段值集合
     * @param update            是否更新操作
     * @param <K>               资源类型
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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

    /**
     * 批量更新字段值
     *
     * @param updateFields     更新字段值
     * @param updateBlobFields 更新大字段值
     */
    private void updateData(List<T> updateFields, List<V> updateBlobFields) {
        for (T resourceField : updateFields) {
            getResourceFieldMapper().update(resourceField);
        }

        for (V updateBlobField : updateBlobFields) {
            getResourceFieldBlobMapper().update(updateBlobField);
        }
    }

    /**
     * 处理子表格字段值 (查询)
     *
     * @param resourceMap    返回的资源字段值
     * @param fieldConfigMap 字段配置
     * @param resourceFields 值集合
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setResourceSubFieldValue(Map<String, List<BaseModuleFieldValue>> resourceMap, Map<String, BaseField> fieldConfigMap,
                                          List<? extends BaseResourceField> resourceFields) {
        Map<String, BaseField> subFieldMap = fieldConfigMap.values().stream().filter(f -> f instanceof SubField).collect(Collectors.toMap(BaseField::getId, Function.identity()));
        if (!subFieldMap.isEmpty()) {
            Set<String> refSubSet = subFieldMap.keySet();
            List<BaseField> subFields = subFieldMap.values().stream().map(subField -> ((SubField) subField).getSubFields()).flatMap(List::stream).toList();
            Map<String, BaseField> subFieldConfigMap = subFields.stream().collect(Collectors.toMap(BaseField::idOrBusinessKey, f -> f, (f1, f2) -> f1));
            Map<String, BaseField> subFieldIdConfigMap = subFields.stream().collect(Collectors.toMap(BaseField::getId, f -> f, (f1, f2) -> f1));

            Map<String, ? extends List<? extends BaseResourceField>> subResourceMap = resourceFields.stream()
                    .filter(r -> refSubSet.contains(((BaseResourceSubField) r).getRefSubId()))
                    .collect(Collectors.groupingBy(BaseResourceField::getResourceId));
            subResourceMap.forEach((resourceId, subResources) -> {
				subResources.sort(Comparator.comparing(resource -> Strings.CS.equals(resource.getFieldId(), PRICE_SUB_ROW_KEY) ? 0 : 1));
                Map<String, List<Map<String, Object>>> subFieldValueMap = new HashMap<>(8);
                subResources.forEach(resource -> {
                    if (resource.getFieldValue() != null) {
						BaseResourceSubField subResource = (BaseResourceSubField) resource;
						subFieldValueMap.putIfAbsent(subResource.getRefSubId(), new ArrayList<>());
						int rowIndex = Integer.parseInt(subResource.getRowId()) - 1;
						if (subFieldValueMap.get(subResource.getRefSubId()).size() <= rowIndex) {
							Map<String, Object> initRowMap = new HashMap<>(8) {{
								put(ROW_BIZ_ID, subResource.getBizId());
							}};
							subFieldValueMap.get(subResource.getRefSubId()).add(initRowMap);
						}
						Map<String, Object> rowMap = subFieldValueMap.get(subResource.getRefSubId()).get(rowIndex);
						if (Strings.CS.equals(resource.getFieldId(), PRICE_SUB_ROW_KEY)) {
							rowMap.put(subResource.getFieldId(), resource.getFieldValue());
							return;
						}
                        BaseField fieldConfig = subFieldConfigMap.get(resource.getFieldId());
                        if (fieldConfig == null) {
                            return;
                        }
                        AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
                        Object objectValue = customFieldResolver.convertToValue(fieldConfig, resource.getFieldValue().toString());
                        rowMap.put(subResource.getFieldId(), objectValue);
                        if (objectValue == null || !SourceDetailResolveContext.getSourceMap().containsKey(objectValue.toString())) {
                            return;
                        }
                        if (fieldConfig instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields())) {
                            // 处理展示列
                            Map<String, Object> detailMap = SourceDetailResolveContext.getSourceMap().get(objectValue.toString());
                            if (MapUtils.isEmpty(detailMap)) {
                                return;
                            }
                            sourceField.getShowFields().forEach(id -> {
                                BaseField showFieldConfig = subFieldIdConfigMap.get(id);
                                if (showFieldConfig == null) {
                                    return;
                                }
                                if (StringUtils.isNotEmpty(showFieldConfig.getSubTableFieldId()) && rowMap.containsKey(PRICE_SUB_ROW_KEY)) {
                                    Object matchVal = matchSubFieldValueOfDetailMap(showFieldConfig.idOrBusinessKey(), detailMap, BusinessModuleField.PRICE_PRODUCT_TABLE.getBusinessKey(),
											rowMap.get(PRICE_SUB_ROW_KEY).toString());
                                    if (matchVal != null) {
                                        rowMap.put(showFieldConfig.getId(), matchVal);
                                    }
                                } else {
                                    rowMap.put(showFieldConfig.getId(), getFieldValueOfDetailMap(showFieldConfig, detailMap));
                                }
                            });
                        }
                        subFieldValueMap.get(subResource.getRefSubId()).set(rowIndex, rowMap);
                    }
                });
                List<BaseModuleFieldValue> subTableFieldValues = new ArrayList<>();
                subFieldValueMap.forEach((subFieldId, subFieldValues) -> {
                    BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
                    fieldValue.setFieldId(subFieldId);
                    fieldValue.setFieldValue(subFieldValues);
                    subTableFieldValues.add(fieldValue);
                });
                resourceMap.putIfAbsent(resourceId, new ArrayList<>());
                resourceMap.get(resourceId).addAll(subTableFieldValues);
            });
        }
    }

    /**
     * 获取数据源详情上下文
     *
     * @param flattenFields  平铺字段集合
     * @param resourceFields 资源字段值集合
     */
    @SuppressWarnings("unchecked")
    private void getSourceDetailMapByIds(List<BaseField> flattenFields, List<T> resourceFields) {
        Map<String, String> sourceIdType = flattenFields.stream().filter(sf -> sf instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields()))
                .collect(Collectors.toMap(BaseField::idOrBusinessKey, f -> ((DatasourceField) f).getDataSourceType(), (prev, next) -> next));
        resourceFields.stream().filter(rf -> sourceIdType.containsKey(rf.getFieldId()) && rf.getFieldValue() != null).forEach(rf -> {
            if (SourceDetailResolveContext.contains(rf.getFieldValue().toString())) {
                return;
            }
            SourceDetailResolveContext.putPlaceholder(rf.getFieldValue().toString());
            try {
                FieldSourceType sourceType = FieldSourceType.valueOf(sourceIdType.get(rf.getFieldId()));
                Object detail = fieldSourceServiceProvider.safeGetById(sourceType, rf.getFieldValue().toString());
                if (detail == null) {
                    SourceDetailResolveContext.remove(rf.getFieldValue().toString());
                } else {
                    SourceDetailResolveContext.put(rf.getFieldValue().toString(), JSON.MAPPER.convertValue(detail, Map.class));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                SourceDetailResolveContext.remove(rf.getFieldValue().toString());
            }
        });
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
            log.error("临时附件处理失败: {}", e.getMessage());
        }
    }

    /**
     * 处理特殊的字段值
     *
     * @param field         字段信息
     * @param fieldValueMap 字段值集合
     * @param update        是否更新操作
     * @param orgId         组织ID
     *
     * @return 字段值
     */
    private BaseModuleFieldValue processSpecialFieldValue(BaseField field, Map<String, BaseModuleFieldValue> fieldValueMap, boolean update, String orgId) {
        // 流水号
        if (field.isSerialNumber() && !update) {
            BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
            fieldValue.setFieldId(field.getId());
            String serialNo = serialNumGenerator.generateByRules(((SerialNumberField) field).getSerialNumberRules(), orgId, getFormKey());
            fieldValue.setFieldValue(serialNo);
            return fieldValue;
        }
        // 子表格 {业务字段来源resource, 字段值来源fieldValueMap}
        if (field.isSubField()) {
            BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
            fieldValue.setFieldId(field.getId());
            String businessKey = field.getBusinessKey();
            if (StringUtils.isNotEmpty(businessKey)) {
                fieldValue.setFieldValue(fieldValueMap.get(businessKey).getFieldValue());
            } else {
                fieldValue = fieldValueMap.get(field.getId());
            }
            return fieldValue;
        }
        // 其他字段直接返回
        return fieldValueMap.get(field.getId());
    }

    /**
     * 获取资源字段值
     *
     * @param resourceId 资源ID
     * @param fieldId    字段ID
     *
     * @return 字段值
     */
    public Object getResourceFieldValue(String resourceId, String fieldId) {
        T example = newResourceField();
        example.setResourceId(resourceId);
        example.setFieldId(fieldId);
        T resourceField = getResourceFieldMapper().selectOne(example);
        if (resourceField != null) {
            return resourceField.getFieldValue();
        }
        return null;
    }

    /**
     * 获取资源字段值集合
     *
     * @param resourceIds 资源ID集合
     *
     * @return 字段值集合
     */
    private List<T> getResourceField(List<String> resourceIds) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        return getResourceFieldMapper().selectListByLambda(wrapper);
    }

    /**
     * 获取资源指定字段值集合
     *
     * @param resourceIds 资源ID集合
     * @param fieldId     字段ID
     *
     * @return 指定字段值集合
     */
    private List<T> getResourceField(List<String> resourceIds, String fieldId) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        wrapper.eq(BaseResourceField::getFieldId, fieldId);
        return getResourceFieldMapper().selectListByLambda(wrapper);
    }

    /**
     * 获取资源大字段值集合
     *
     * @param resourceIds 资源ID集合
     *
     * @return 大字段值集合
     */
    private List<V> getResourceFieldBlob(List<String> resourceIds) {
        LambdaQueryWrapper<V> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        return getResourceFieldBlobMapper().selectListByLambda(wrapper);
    }

    /**
     * 获取资源指定大字段值集合
     *
     * @param resourceIds 资源ID集合
     * @param fieldId     大字段ID
     *
     * @return 指定大字段值集合
     */
    private List<V> getResourceFieldBlob(List<String> resourceIds, String fieldId) {
        LambdaQueryWrapper<V> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseResourceField::getResourceId, resourceIds);
        wrapper.eq(BaseResourceField::getFieldId, fieldId);
        return getResourceFieldBlobMapper().selectListByLambda(wrapper);
    }

    /**
     * 提供新的字段或大字段实例 (通用&新增)
     *
     * @param instance   字段值实例
     * @param resourceId 资源ID
     * @param fieldId    字段ID
     * @param strValue   字段值
     * @param <R>        字段类型
     *
     * @return 字段实例
     */
    private <R extends BaseResourceField> R supplyNewResource(Supplier<R> instance, String resourceId, String fieldId, String strValue) {
        R resourceField = instance.get();
        resourceField.setId(IDGenerator.nextStr());
        resourceField.setResourceId(resourceId);
        resourceField.setFieldId(fieldId);
        resourceField.setFieldValue(strValue);
        return resourceField;
    }

    /**
     * 创建新的资源字段实例
     *
     * @return 资源字段实例
     */
    private T newResourceField() {
        try {
            return getResourceFieldClass().getConstructor().newInstance();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new GenericException(e);
        }
    }

    /**
     * 创建新的资源大字段实例
     *
     * @return 资源大字段实例
     */
    private V newResourceFieldBlob() {
        try {
            return getResourceFieldBlobClass().getConstructor().newInstance();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new GenericException(e);
        }
    }

    /**
     * 字段值唯一性校验
     *
     * @param fieldValue 字段值
     * @param field      字段信息
     */
    protected void checkUnique(BaseModuleFieldValue fieldValue, BaseField field) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseResourceField::getFieldId, fieldValue.getFieldId());
        wrapper.eq(BaseResourceField::getFieldValue, fieldValue.getFieldValue());
        if (!getResourceFieldMapper().selectListByLambda(wrapper).isEmpty()) {
            throw new GenericException(Translator.getWithArgs("common.field_value.repeat", field.getName()));
        }
    }

    /**
     * 获取并校验字段是否存在
     *
     * @param fieldId        字段ID
     * @param organizationId 组织ID
     *
     * @return 字段信息
     */
    public BaseField getAndCheckField(String fieldId, String organizationId) {
        return moduleFormCacheService.getConfig(getFormKey(), organizationId)
                .getFields()
                .stream()
                .filter(f -> fieldId.equals(f.getId()))
                .findFirst()
                .orElseThrow(() -> new GenericException(Translator.get("module.field.not_exist")));
    }

    /**
     * 校验业务字段值是否重复
     *
     * @param orgId     组织ID
     * @param resource  资源对象
     * @param updateIds 排除的ID集合
     * @param field     字段信息
     * @param fieldName 字段名称
     * @param <K>       资源类型
     */
    private <K> void businessFieldRepeatCheck(String orgId, K resource, List<String> updateIds, BaseField field, String fieldName) {
        if (!field.needRepeatCheck()) {
            return;
        }
        Class<?> clazz = resource.getClass();
        String tableName = CaseFormatUtils.camelToUnderscore(clazz.getSimpleName());
        Object fieldValue = getResourceFieldValue(resource, fieldName);
        if (isNotBlank(fieldValue)) {
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
}