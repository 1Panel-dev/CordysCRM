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
import cn.idev.excel.metadata.CellExtra;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

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
	 * method cache
	 */
	private final Map<String, Method> methodCache = new HashMap<>();
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
	@Getter
    private int successCount;
	/**
	 * 临时合并实体
	 */
	private T mergedTmpEntity;
	private int subRowId;

    public CustomFieldImportEventListener(List<BaseField> fields, Class<T> clazz, String currentOrg, String operator, String fieldTable,
										  CustomImportAfterDoConsumer<T, BaseResourceSubField> consumer, int batchSize,
										  Map<Integer, List<CellExtra>> mergeCellMap, Map<Integer, Map<Integer, String>> mergeRowDataMap) {
		super(fields, CaseFormatUtils.camelToUnderscore(clazz.getSimpleName()), fieldTable, currentOrg, mergeCellMap, mergeRowDataMap);
        this.entityClass = clazz;
        this.operator = operator;
        this.serialNumGenerator = CommonBeanFactory.getBean(SerialNumGenerator.class);
        this.consumer = consumer;
        this.batchSize = batchSize > 0 ? batchSize : 2000;
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
		if (this.errRows.contains(rowIndex)) {
			// 有错误跳过该行
			return;
		}
		// build entity by row-data
		buildEntityFromRow(rowIndex, data);
		if (dataList.size() >= batchSize || fields.size() >= batchSize || blobFields.size() > batchSize) {
			batchProcessData();
		}
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        if (CollectionUtils.isNotEmpty(this.dataList) || CollectionUtils.isNotEmpty(this.fields) || CollectionUtils.isNotEmpty(this.blobFields)) {
            batchProcessData();
        }
        LogUtils.info("数据导入完成, 总行数: {}", successCount);
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
        try {
			if (isNormalRow(rowIndex) || isMergeFirstRow(rowIndex)) {
				// 非合并行才创建实体
				String rowKey = IDGenerator.nextStr();
				mergedTmpEntity = entityClass.getDeclaredConstructor().newInstance();
				setInternal(mergedTmpEntity, rowKey);
				subRowId = 1;
			} else {
				subRowId++;
			}
			if (mergedTmpEntity == null) {
				return;
			}
			Optional<Object> id = Optional.ofNullable(getResourceId(mergedTmpEntity));
			if (id.isEmpty()) {
				return;
			}
			headMap.forEach((k, v) -> {
                BaseField field = fieldMap.get(v);
                if (field == null || field.isSerialNumber()) {
                    return;
                }
                Object val = convertValue(rowData.get(k), field);
                if (val == null) {
                    return;
                }
				if (!refSubMap.containsKey(field.getName()) && !isNormalRow(rowIndex) && !isMergeFirstRow(rowIndex)) {
					// 除合并的首行外, 其余合并行非子表字段都跳过
					return;
				}
                if (businessFieldMap.containsKey(field.getInternalKey()) && !refSubMap.containsKey(field.getName())) {
                    try {
                        setPropertyValue(mergedTmpEntity, businessFieldMap.get(field.getInternalKey()).getBusinessKey(), val);
                    } catch (Exception e) {
                        LogUtils.error("导入错误, 无法设置字段值. {}", e.getMessage());
                        throw new GenericException(e);
                    }
                } else {
					BaseResourceSubField resourceField = new BaseResourceSubField();
                    resourceField.setId(IDGenerator.nextStr());
                    resourceField.setResourceId(id.get().toString());
                    resourceField.setFieldId(field.idOrBusinessKey());
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
			if (isNormalRow(rowIndex) || isMergedLastRow(rowIndex)) {
				if (serialField != null) {
					BaseResourceSubField serialResource = new BaseResourceSubField();
					serialResource.setId(IDGenerator.nextStr());
					serialResource.setResourceId(id.get().toString());
					serialResource.setFieldId(serialField.idOrBusinessKey());
					String serialNo = serialNumGenerator.generateByRules(((SerialNumberField) serialField).getSerialNumberRules(),
							currentOrg, entityClass.getSimpleName().toLowerCase());
					serialResource.setFieldValue(serialNo);
					if (refSubMap.containsKey(serialField.getName())) {
						serialResource.setRefSubId(refSubMap.get(serialField.getName()));
						serialResource.setRowId(String.valueOf(subRowId));
					}
					fields.add(serialResource);
				}
				// 合并最后行, 添加并清除实体
				dataList.add(mergedTmpEntity);
				mergedTmpEntity = null;
			}
        } catch (Exception e) {
            LogUtils.error("导入错误, 原因: {}", e.getMessage());
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
            LogUtils.error(String.format("解析字段[%s]错误, [%s]不能被转换; 原因: %s", field.getName(), text, e.getMessage()));
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
				methodCache.put("set:" + property, method);
            } else if (method.getParameterCount() == 0 && Strings.CS.equals(method.getName(), "getId")) {
				methodCache.put("get:id", method);
			}
        }
    }

	/**
	 * 判断是否合并首行
	 * @param rowIndex 行号
	 * @return 是否为合并首行
	 */
	private boolean isMergeFirstRow(int rowIndex) {
		if (mergeCellMap == null) {
			return false;
		}
		List<CellExtra> cellExtras = mergeCellMap.get(rowIndex);
		return cellExtras != null && cellExtras.stream().anyMatch(extra -> rowIndex == extra.getFirstRowIndex() && extra.getLastRowIndex() > rowIndex);
	}

	/**
	 * 判断当前行是否为合并尾行
	 * @param rowIndex 行号
	 * @return 是否为合并尾行
	 */
	private boolean isMergedLastRow(int rowIndex) {
		if (mergeCellMap == null) {
			return false;
		}
		List<CellExtra> cellExtras = mergeCellMap.get(rowIndex);
		return cellExtras != null && cellExtras.stream().anyMatch(extra -> rowIndex == extra.getLastRowIndex() && extra.getLastRowIndex() > extra.getFirstRowIndex());
	}

	/**
	 * 非合并行
	 * @param rowIndex 行号
	 * @return 是否为正常行
	 */
	private boolean isNormalRow(int rowIndex) {
		return mergeCellMap == null || !mergeCellMap.containsKey(rowIndex);
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
		methodCache.get("set:id").invoke(instance, rowKey);
		methodCache.get("set:createUser").invoke(instance, operator);
		methodCache.get("set:createTime").invoke(instance, System.currentTimeMillis());
		methodCache.get("set:updateUser").invoke(instance, operator);
		methodCache.get("set:updateTime").invoke(instance, System.currentTimeMillis());
		methodCache.get("set:organizationId").invoke(instance, currentOrg);
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
        Method setter = methodCache.get("set:" + fieldName);
        if (setter != null) {
            setter.invoke(instance, value);
        }
    }

	/**
	 * 获取资源ID
	 * @param instance 实例对象
	 * @return 资源ID
	 * @throws Exception 异常信息
	 */
	private Object getResourceId(T instance) throws Exception {
		Method getter = methodCache.get("get:id");
		if (getter != null) {
			return getter.invoke(instance);
		}
		return null;
	}
}
