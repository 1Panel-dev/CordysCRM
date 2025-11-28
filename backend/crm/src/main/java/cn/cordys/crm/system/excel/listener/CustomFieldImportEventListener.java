package cn.cordys.crm.system.excel.listener;

import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.*;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.idev.excel.context.AnalysisContext;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 自定义字段导入处理器
 * @param <T> 业务实体
 * @author song-cc-rock
 */
public class CustomFieldImportEventListener<T> extends CustomFieldCheckEventListener {

    /**
     * 主表数据
     */
    @Getter
    private final List<T> dataList;
    /**
     * 自定义字段集合&Blob字段集合
     */
    private final List<BaseResourceSubField> fields;
    private final List<BaseResourceSubField> blobFields;
    /**
     * 批次限制
     */
    private final int batchSize;
    /**
     * 业务实体
     */
    private final Class<T> entityClass;
	/**
	 * setter cache
	 */
	private final Map<String, Method> setterCache = new HashMap<>();
	/**
	 * 操作人
	 */
    private final String operator;
    /**
     * 后置处理函数(入库)
     */
    private final CustomImportAfterDoConsumer<T, BaseResourceSubField> consumer;
    /**
     * 序列化字段及生成器
     */
    private BaseField serialField;
	private final SerialNumGenerator serialNumGenerator;
    /**
     * 成功条数
     */
    private int successCount;
	/**
	 * 子表格ID
	 */
	private int subRowId;

    public CustomFieldImportEventListener(List<BaseField> fields, Class<T> clazz, String currentOrg, String operator,
                                          String fieldTable, CustomImportAfterDoConsumer<T, BaseResourceSubField> consumer, int batchSize) {
		super(fields, CaseFormatUtils.camelToUnderscore(clazz.getSimpleName()), fieldTable, currentOrg, null);
        this.entityClass = clazz;
        this.operator = operator;
        this.serialNumGenerator = CommonBeanFactory.getBean(SerialNumGenerator.class);
        this.consumer = consumer;
        this.batchSize = batchSize > 0 ? batchSize : 2000;
		this.subRowId = 1;
        // 初始化大小,扩容有开销
        this.dataList = new ArrayList<>(batchSize);
        this.fields = new ArrayList<>(batchSize);
        this.blobFields = new ArrayList<>(batchSize);
        // 缓存方法, 频繁反射有开销
        cacheSetterMethods();
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
		super.invokeHeadMap(headMap, context);
        Optional<BaseField> anySerial = this.fieldMap.values().stream().filter(BaseField::isSerialNumber).findAny();
        anySerial.ifPresent(field -> serialField = field);
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
		super.invoke(data, analysisContext);
        Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
		if (!this.errRows.contains(rowIndex)) {
			// build entity by row-data
			buildEntityFromRow(rowIndex, data);
			if (dataList.size() >= batchSize || fields.size() >= batchSize || blobFields.size() > batchSize) {
				batchProcessData();
			}
		}
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        if (CollectionUtils.isNotEmpty(this.dataList) || CollectionUtils.isNotEmpty(this.fields) || CollectionUtils.isNotEmpty(this.blobFields)) {
            batchProcessData();
        }
        LogUtils.info("线索导入完成, 总行数: {}", successCount);
    }

    /**
     * 批量入库操作
     */
    private void batchProcessData() {
        try {
            // 执行入库
            consumer.accept(this.dataList, this.fields, this.blobFields);
        } catch (Exception e) {
            // 入库异常,不影响后续批次
            LogUtils.error("批量插入异常: {}", e.getCause().getMessage());
            throw new GenericException(e.getCause());
        } finally {
            // 批次插入成功, 统计&&清理
            successCount += this.dataList.size();
            this.dataList.clear();
            this.fields.clear();
            this.blobFields.clear();
        }
    }

    /**
     * 构建行列数据 => 实体
     *
     * @param rowIndex 行序号
     * @param rowData  行数据
     */
    private void buildEntityFromRow(Integer rowIndex, Map<Integer, String> rowData) {
        String rowKey = IDGenerator.nextStr();
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            setInternal(entity, rowKey);
            headMap.forEach((k, v) -> {
                BaseField field = fieldMap.get(v);
                if (field == null || field.isSerialNumber()) {
                    return;
                }
                Object val = convertValue(rowData.get(k), field);
                if (val == null) {
                    return;
                }
                if (businessFieldMap.containsKey(field.getInternalKey()) && !refSubMap.containsKey(field.getName())) {
                    try {
                        setPropertyValue(entity, businessFieldMap.get(field.getInternalKey()).getBusinessKey(), val);
                    } catch (Exception e) {
                        LogUtils.error("import error, cannot set property. {}", e.getMessage());
                        throw new GenericException(e);
                    }
                } else {
					BaseResourceSubField resourceField = new BaseResourceSubField();
                    resourceField.setId(IDGenerator.nextStr());
                    resourceField.setResourceId(rowKey);
                    resourceField.setFieldId(field.getId());
                    resourceField.setFieldValue(val);
					if (refSubMap.containsKey(field.getName())) {
						resourceField.setRefSubId(refSubMap.get(field.getName()));
						resourceField.setRowId(String.valueOf(subRowId));
					}
                    if (field.isBlob()) {
                        if (val instanceof List<?> valList) {
                            resourceField.setFieldValue(JSON.toJSONString(valList));
                        }
                        blobFields.add(resourceField);
                    } else {
                        fields.add(resourceField);
                    }
                }
            });
            if (serialField != null) {
				BaseResourceSubField serialResource = new BaseResourceSubField();
                serialResource.setId(IDGenerator.nextStr());
                serialResource.setResourceId(rowKey);
                serialResource.setFieldId(serialField.getId());
                String serialNo = serialNumGenerator.generateByRules(((SerialNumberField) serialField).getSerialNumberRules(),
                        currentOrg, entityClass.getSimpleName().toLowerCase());
                serialResource.setFieldValue(serialNo);
                fields.add(serialResource);
            }
            dataList.add(entity);
        } catch (Exception e) {
            LogUtils.error("import error: {}", e.getMessage());
            throw new GenericException(Translator.getWithArgs("import.error", rowIndex + 1).concat(" " + e.getMessage()));
        }
    }

    /**
     * 自定义字段文本转换
     *
     * @param text  文本
     * @param field 字段
     *
     * @return 值
     */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private Object convertValue(String text, BaseField field) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        try {
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
            return customFieldResolver.textToValue(field, text);
        } catch (Exception e) {
            LogUtils.error(String.format("parse field %s error, %s cannot be transfer, error: %s", field.getName(), text, e.getMessage()));
        }
        return null;
    }

    /**
     * 缓存entity setter
     */
    private void cacheSetterMethods() {
        for (Method method : entityClass.getMethods()) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                String fieldName = method.getName().substring(3);
                String property = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
                setterCache.put(property, method);
            }
        }
    }

    /**
     * 设置entity内部字段
     *
     * @param instance 实例对象
     * @param rowKey   唯一Key
     *
     * @throws Exception 异常
     */
    private void setInternal(T instance, String rowKey) throws Exception {
        setterCache.get("id").invoke(instance, rowKey);
        setterCache.get("createUser").invoke(instance, operator);
        setterCache.get("createTime").invoke(instance, System.currentTimeMillis());
        setterCache.get("updateUser").invoke(instance, operator);
        setterCache.get("updateTime").invoke(instance, System.currentTimeMillis());
        setterCache.get("organizationId").invoke(instance, currentOrg);
    }

    /**
     * 设置属性值
     *
     * @param instance  实例对象
     * @param fieldName 字段名
     * @param value     值
     *
     * @throws Exception 异常
     */
    private void setPropertyValue(T instance, String fieldName, Object value) throws Exception {
        Method setter = setterCache.get(fieldName);
        if (setter != null) {
            setter.invoke(instance, value);
        }
    }
}
