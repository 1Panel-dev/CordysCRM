package io.cordys.crm.system.excel.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.domain.BaseResourceField;
import io.cordys.common.exception.GenericException;
import io.cordys.common.mapper.CommonMapper;
import io.cordys.common.resolver.field.AbstractModuleFieldResolver;
import io.cordys.common.resolver.field.ModuleFieldResolverFactory;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.uid.SerialNumGenerator;
import io.cordys.common.util.*;
import io.cordys.crm.system.dto.field.SerialNumberField;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import io.cordys.excel.domain.ExcelErrData;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CustomFieldImportEventListener <T, F extends BaseResourceField> extends AnalysisEventListener<Map<Integer, String>> {

	/**
	 * 主表数据
	 */
	@Getter
	private List<T> dataList = new ArrayList<>();
	/**
	 * 自定义字段集合&Blob字段集合
	 */
	private List<BaseResourceField> fields = new ArrayList<>();
	private List<BaseResourceField> blobFields = new ArrayList<>();
	/**
	 * 业务实体
	 */
	private final Class<T> entityClass;
	/**
	 * setter cache
	 */
	private final Map<String, Method> setterCache = new HashMap<>();
	@Getter
	protected List<ExcelErrData<?>> errList = new ArrayList<>();
	private final String currentOrg;
	private final String operator;
	private Map<Integer, String> headMap;
	private final CommonMapper commonMapper;
	private final BaseMapper<F> fieldMapper;
	private final Map<String, BaseField> fieldMap;
	private Map<String, BusinessModuleField> businessFieldMap;
	/**
	 * 必填
	 */
	private final List<String> requires = new ArrayList<>();
	/**
	 * 唯一
	 */
	private final List<String> uniques = new ArrayList<>();
	/**
	 * 后置处理函数
	 */
	private final CustomImportAfterDoConsumer<T, BaseResourceField> consumer;
	/**
	 * 序列化字段
	 */
	private BaseField serialField;
	/**
	 * 序列化生成器
	 */
	private final SerialNumGenerator serialNumGenerator;

	public CustomFieldImportEventListener(List<BaseField> fields, Class<T> clazz, String currentOrg, String operator,
										  BaseMapper<F> fieldMapper, CustomImportAfterDoConsumer<T, BaseResourceField> consumer) {
		fields.forEach(field -> {
			if (field.needRequireCheck()) {
				requires.add(field.getName());
			}
			if (field.needRepeatCheck()) {
				uniques.add(field.getName());
			}
		});
		this.fieldMap = fields.stream().collect(Collectors.toMap(BaseField::getName, v -> v));
		this.entityClass = clazz;
		this.currentOrg = currentOrg;
		this.operator = operator;
		this.commonMapper = CommonBeanFactory.getBean(CommonMapper.class);
		this.serialNumGenerator = CommonBeanFactory.getBean(SerialNumGenerator.class);
		this.fieldMapper = fieldMapper;
		this.consumer = consumer;
		cacheSetterMethods();
	}

	@Override
	public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
		if (headMap == null) {
			throw new GenericException(Translator.get("user_import_table_header_missing"));
		}
		if (checkIllegalHead(headMap)) {
			throw new GenericException(Translator.get("illegal_header"));
		}
		this.headMap = headMap;
		this.businessFieldMap = Arrays.stream(BusinessModuleField.values()).
				collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));
		Optional<BaseField> anySerial = this.fieldMap.values().stream().filter(BaseField::isSerialNumber).findAny();
		anySerial.ifPresent(field -> serialField = field);
	}

	@Override
	public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
		Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
		if (rowIndex >= 1) {
			// build entity by row-data
			boolean skip = checkAndSkip(rowIndex, data);
			if (!skip) {
				buildEntityFromRow(rowIndex, data);
			}
		}
	}

	@Override
	public void doAfterAllAnalysed(AnalysisContext analysisContext) {
		// 执行入库
		consumer.accept(this.dataList, this.fields, this.blobFields);
	}

	/**
	 * 构建行列数据 => 实体
	 * @param rowIndex 行序号
	 * @param rowData 行数据
	 */
	private void buildEntityFromRow(Integer rowIndex, Map<Integer, String> rowData) {
		String rowKey = IDGenerator.nextStr();
		try {
			T entity = entityClass.getDeclaredConstructor().newInstance();
			setInternal(entity, rowKey);
			headMap.forEach((k, v) -> {
				BaseField field = fieldMap.get(v);
				if (field == null) {
					return;
				}
				Object val = convertValue(rowData.get(k), field);
				if (val == null) {
					return;
				}
				if (businessFieldMap.containsKey(field.getInternalKey())) {
					try {
						setPropertyValue(entity, businessFieldMap.get(field.getInternalKey()).getBusinessKey(), val);
					} catch (Exception e) {
						throw new GenericException(e);
					}
				} else {
					BaseResourceField resourceField = new BaseResourceField();
					resourceField.setId(IDGenerator.nextStr());
					resourceField.setResourceId(rowKey);
					resourceField.setFieldId(field.getId());
					resourceField.setFieldValue(val);
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
				BaseResourceField serialResource = new BaseResourceField();
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
			LogUtils.error("clue import error: " + e.getMessage());
			throw new GenericException(Translator.getWithArgs("import.error", rowIndex + 1).concat(" " + e.getMessage()));
		}
	}

	/**
	 * 自定义字段文本转换
	 * @param text 文本
	 * @param field 字段
	 * @return 值
	 */
	private Object convertValue(String text, BaseField field) {
		if (StringUtils.isEmpty(text)) {
			return text;
		}
		try {
			AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
			Object valObj = customFieldResolver.text2Value(field, text.toString());
			return valObj;
		} catch (Exception e) {
			LogUtils.error(String.format("parse field %s error, %s cannot be transfer, error: %s", field.getName(), text, e.getMessage()));
		}
		return null;
	}

	/**
	 * 校验行数据
	 * @param rowIndex 行索引
	 * @param rowData 行数据
	 */
	private boolean checkAndSkip(Integer rowIndex, Map<Integer, String> rowData) {
		StringBuilder errText = new StringBuilder();
		headMap.forEach((k, v) -> {
			if (requires.contains(v) && StringUtils.isEmpty(rowData.get(k))) {
				errText.append(v).append(Translator.get("cannot_be_null")).append(";");
			}
			if (uniques.contains(v) && !checkFieldValUnique(rowData.get(k), fieldMap.get(v))) {
				errText.append(v).append(Translator.get("cell.not.unique")).append(";");
			}
		});
		if (StringUtils.isNotEmpty(errText)) {
			ExcelErrData<?> excelErrData = new ExcelErrData<>(rowIndex,
					Translator.getWithArgs("row.error.tip", rowIndex + 1).concat(" " + errText));
			//错误信息
			errList.add(excelErrData);
		}
		return StringUtils.isNotEmpty(errText);
	}

	/**
	 * 检查字段值唯一
	 * @param val 值
	 * @param field 字段
	 * @return 是否唯一
	 */
	private boolean checkFieldValUnique(String val, BaseField field) {
		if (StringUtils.isEmpty(val)) {
			return true;
		}
		Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
				collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));
		if (businessModuleFieldMap.containsKey(field.getInternalKey())) {
			BusinessModuleField businessModuleField = businessModuleFieldMap.get(field.getInternalKey());
			String fieldName = businessModuleField.getBusinessKey();
			return !commonMapper.checkAddExist(CaseFormatUtils.camelToUnderscore(entityClass.getSimpleName()), fieldName, val, currentOrg);
		} else {
			LambdaQueryWrapper<F> wrapper = new LambdaQueryWrapper<>();
			wrapper.eq(BaseResourceField::getFieldId, field.getId());
			wrapper.eq(BaseResourceField::getFieldValue, val);
			return fieldMapper.selectListByLambda(wrapper).isEmpty();
		}
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
	 * @param instance 实例对象
	 * @param rowKey 唯一Key
	 * @throws Exception 异常
	 */
	private void setInternal(T instance, String rowKey) throws Exception {
		setterCache.get("id").invoke(instance, rowKey);
		setterCache.get("createUser").invoke(instance, operator);
		setterCache.get("updateUser").invoke(instance, operator);
		setterCache.get("organizationId").invoke(instance, currentOrg);
	}

	/**
	 * 设置属性值
	 * @param instance 实例对象
	 * @param fieldName 字段名
	 * @param value 值
	 * @throws Exception 异常
	 */
	private void setPropertyValue(T instance, String fieldName, Object value) throws Exception {
		Method setter = setterCache.get(fieldName);
		if (setter != null) {
			setter.invoke(instance, value);
		}
	}

	/**
	 * 表头是否非法
	 * @param headMap 表头集合
	 * @return 是否非法
	 */
	private boolean checkIllegalHead(Map<Integer, String> headMap) {
		boolean illegal = false;
		for (String head : headMap.values()) {
			if (!fieldMap.containsKey(head)) {
				illegal = true;
				break;
			}
		}
		return illegal;
	}
}
