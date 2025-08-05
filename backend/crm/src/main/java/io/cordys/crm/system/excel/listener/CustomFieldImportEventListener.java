package io.cordys.crm.system.excel.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.domain.BaseResourceField;
import io.cordys.common.exception.GenericException;
import io.cordys.common.mapper.CommonMapper;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.*;
import io.cordys.common.utils.RegionUtils;
import io.cordys.crm.system.dto.field.DateTimeField;
import io.cordys.crm.system.dto.field.InputNumberField;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.field.base.HasOption;
import io.cordys.crm.system.dto.field.base.OptionProp;
import io.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import io.cordys.excel.domain.ExcelErrData;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CustomFieldImportEventListener <T, F extends BaseResourceField> extends AnalysisEventListener<Map<Integer, String>> {

	/**
	 * 主表数据
	 */
	@Getter
	private List<T> dataList;
	/**
	 * 自定义字段数据
	 */
	private List<BaseResourceField> fields;
	private List<BaseResourceField> blobFields;
	/**
	 * 源数据表
	 */
	private final Class<T> entityClass;
	private final Map<String, Method> setterCache = new HashMap<>();
	@Getter
	protected List<ExcelErrData<?>> errList = new ArrayList<>();
	private final String currentOrg;
	private final String operator;
	private Map<Integer, String> headMap;
	private final CommonMapper commonMapper;
	private final BaseMapper<F> fieldMapper;
	private final Map<String, BaseField> fieldMap;
	private final Map<String, BusinessModuleField> businessFieldMap;
	/**
	 * 必填
	 */
	private final List<String> requires = new ArrayList<>();
	/**
	 * 唯一
	 */
	private final List<String> uniques = new ArrayList<>();
	private final CustomImportAfterDoConsumer<T, BaseResourceField> consumer;
	public static final String ARRAY_PREFIX = "[";

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
		this.entityClass = clazz;
		this.currentOrg = currentOrg;
		this.operator = operator;
		this.commonMapper = CommonBeanFactory.getBean(CommonMapper.class);
		this.fieldMap = fields.stream().collect(Collectors.toMap(BaseField::getName, v -> v));
		this.businessFieldMap = Arrays.stream(BusinessModuleField.values()).
				collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));
		this.fieldMapper = fieldMapper;
		this.consumer = consumer;
		cacheSetterMethods();
	}

	@Override
	public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
		this.headMap = headMap;
	}

	@Override
	public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
		if (headMap == null) {
			throw new GenericException(Translator.get("user_import_table_header_missing"));
		}
		Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
		if (rowIndex >= 1) {
			// transfer row data to model
			boolean skip = checkAndSkip(rowIndex, data);
			if (!skip) {
				buildEntityFromRow(rowIndex, data);
			}
		}
	}

	private void buildEntityFromRow(Integer rowIndex, Map<Integer, String> rowData) {
		String rowKey = IDGenerator.nextStr();
		try {
			T entity = entityClass.getDeclaredConstructor().newInstance();
			setInternal(entity, rowKey);
			headMap.forEach((k, v) -> {
				BaseField field = fieldMap.get(v);
				String val;
				try {
					val = convertValue(rowData.get(k), field);
				} catch (Exception e) {
					LogUtils.error("transfer error: " + e.getMessage());
					return;
				}
				if (StringUtils.isEmpty(val)) {
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
						blobFields.add(resourceField);
					} else {
						fields.add(resourceField);
					}
				}
			});
			dataList.add(entity);
		} catch (Exception e) {
			LogUtils.error("clue import error: " + e.getMessage());
			throw new GenericException(Translator.getWithArgs("import.error", rowIndex + 1).concat(" " + e.getMessage()));
		}
	}

	private String convertValue(String val, BaseField field) throws Exception{
		if (field.skipImportTransfer() || StringUtils.isEmpty(val)) {
			return val;
		}
		return switch (field.getType()) {
			case "INPUT_NUMBER" -> setNumberVal(val, (InputNumberField) field);
			case "DATE_TIME" -> setDateVal(val, (DateTimeField) field);
			case "LOCATION" -> RegionUtils.nameToCode(val);
			default -> setOption(val, field);
		};
	}

	@Override
	public void doAfterAllAnalysed(AnalysisContext analysisContext) {
		consumer.accept(this.dataList, this.fields, this.blobFields);
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

	private void cacheSetterMethods() {
		for (Method method : entityClass.getMethods()) {
			if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
				String fieldName = method.getName().substring(3);
				String property = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
				setterCache.put(property, method);
			}
		}
	}

	public void setInternal(T instance, String rowKey) throws Exception {
		setterCache.get("id").invoke(instance, rowKey);
		setterCache.get("createUser").invoke(instance, operator);
		setterCache.get("updateUser").invoke(instance, operator);
		setterCache.get("organizationId").invoke(instance, currentOrg);
	}

	public void setPropertyValue(T instance, String fieldName, Object value) throws Exception {
		Method setter = setterCache.get(fieldName);
		if (setter != null) {
			setter.invoke(instance, value);
		}
	}

	private String setNumberVal(String val, InputNumberField field) {
		if (StringUtils.equals(field.getNumberFormat(), "percent")) {
			return val.replace("%", StringUtils.EMPTY);
		}
		return val;
	}

	private String setDateVal(String val, DateTimeField field) {
		DateTimeFormatter formatter;
		if (StringUtils.equals(field.getDateType(), "datetime")) {
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		} else if (StringUtils.equals(field.getDateType(), "date")) {
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		} else {
			formatter = DateTimeFormatter.ofPattern("yyyy-MM");
		}
		LocalDateTime parse = LocalDateTime.parse(val, formatter);
		return String.valueOf(parse.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
	}

	private String setOption(String val, BaseField field) {
		List<String> rawList = new ArrayList<>();
		if (val.startsWith(ARRAY_PREFIX)) {
			rawList = JSON.parseArray(val, String.class);
		}
		if (field instanceof HasOption optionField) {
			List<OptionProp> options = optionField.getOptions();
			Map<String, String> optionMap = options.stream().collect(Collectors.toMap(OptionProp::getLabel, OptionProp::getValue));
			if (CollectionUtils.isNotEmpty(rawList)) {
				List<String> valList = new ArrayList<>();
				rawList.forEach(raw -> {
					if (optionMap.containsKey(raw)) {
						valList.add(optionMap.get(raw));
					}
				});
				return JSON.toJSONString(valList);
			} else {
				return optionMap.get(val);
			}
		} else {
			// TODO
		}
		return null;
	}
}
