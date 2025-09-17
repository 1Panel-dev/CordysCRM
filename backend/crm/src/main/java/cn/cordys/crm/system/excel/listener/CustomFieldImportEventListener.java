package cn.cordys.crm.system.excel.listener;

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
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.excel.domain.ExcelErrData;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CustomFieldImportEventListener <T, F extends BaseResourceField> extends AnalysisEventListener<Map<Integer, String>> {

	/**
	 * 主表数据
	 */
	@Getter
	private final List<T> dataList;
	/**
	 * 自定义字段集合&Blob字段集合
	 */
	private final List<BaseResourceField> fields;
	private final List<BaseResourceField> blobFields;
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
	 * 系统参数 {当前组织, 操作人}
	 */
	private final String currentOrg;
	private final String operator;
	/**
	 * 初始化集合参数 {表头, 字段, 内置业务字段}
	 */
	private Map<Integer, String> headMap;
	private final Map<String, BaseField> fieldMap;
	private Map<String, BusinessModuleField> businessFieldMap;
	/**
	 * 校验属性 {必填, 唯一}
	 */
	private final List<String> requires = new ArrayList<>();
	private final Map<String, BaseField> uniques = new HashMap<>();
	private final Map<String, Set<String>> uniqueCheckSet = new ConcurrentHashMap<>();
	/**
	 * 值缓存(校验Excel字段值唯一)
	 */
	private final Map<String, Set<String>> excelValueCache = new ConcurrentHashMap<>();
	/**
	 * 限制数据长度的字段
	 */
	private final Map<String, Integer> fieldLenLimit = new HashMap<>();
	/**
	 * 数据库Mapper(校验数据库值唯一)
	 */
	private final CommonMapper commonMapper;
	private final BaseMapper<F> fieldMapper;
	/**
	 * 正则校验
	 */
	private final Map<String, Pattern> regexMap = new HashMap<>();
	/**
	 * 除开特定的区号和格式校验, 还需兜底的其他手机格式
	 */
	public static final String PHONE_REGEX =
			"^[(（]\\+86[)）]\\s?1[3-9]\\d{9}$"
					+ "|^[(（]\\+(852|853)[)）]\\s?\\d{8}$"
					+ "|^[(（]\\+886[)）]\\s?\\d{9,10}$"
					+ "|^(?![(（]\\+(86|852|853|886)[)）])(?:[(（]\\+\\d{1,4}[)）])?\\s?[\\d\\-\\s]{4,20}$";
	private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
	/**
	 * 校验错误信息
	 */
	@Getter
	protected List<ExcelErrData> errList = new ArrayList<>();
	/**
	 * 后置处理函数
	 */
	private final CustomImportAfterDoConsumer<T, BaseResourceField> consumer;
	/**
	 * 序列化字段及生成器
	 */
	private BaseField serialField;
	private final SerialNumGenerator serialNumGenerator;
	/**
	 * 成功条数
	 */
	private int successCount;

	public CustomFieldImportEventListener(List<BaseField> fields, Class<T> clazz, String currentOrg, String operator,
										  BaseMapper<F> fieldMapper, CustomImportAfterDoConsumer<T, BaseResourceField> consumer, int batchSize) {
		fields.forEach(field -> {
			if (field.needRequireCheck()) {
				requires.add(field.getName());
			}
			if (field.needRepeatCheck()) {
				uniques.put(field.getName(), field);
			}
			if (Strings.CS.equalsAny(field.getType(), FieldType.INPUT.name(), FieldType.INPUT_NUMBER.name(), FieldType.DATE_TIME.name(),
					FieldType.MEMBER.name(), FieldType.DEPARTMENT.name(), FieldType.DATA_SOURCE.name(), FieldType.RADIO.name(),
					FieldType.SELECT.name(), FieldType.PHONE.name(), FieldType.LOCATION.name())) {
				fieldLenLimit.put(field.getName(), 255);
			}
			if (Strings.CS.equals(field.getType(), FieldType.TEXTAREA.name())) {
				fieldLenLimit.put(field.getName(), 1000);
			}
			if (Strings.CS.equals(field.getType(), FieldType.PHONE.name())) {
				regexMap.put(field.getName(), PHONE_PATTERN);
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
		if (headMap == null) {
			throw new GenericException(Translator.get("user_import_table_header_missing"));
		}
		String errHead = checkIllegalHead(headMap);
		if (StringUtils.isNotEmpty(errHead)) {
			throw new GenericException(Translator.getWithArgs("illegal_header", errHead));
		}
		this.headMap = headMap;
		this.businessFieldMap = Arrays.stream(BusinessModuleField.values()).
				collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));
		Optional<BaseField> anySerial = this.fieldMap.values().stream().filter(BaseField::isSerialNumber).findAny();
		anySerial.ifPresent(field -> serialField = field);
		cacheUniqueSet();
	}

	@Override
	public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
		Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
		// build entity by row-data
		boolean skip = checkAndSkip(rowIndex, data);
		if (!skip) {
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
	 * 缓存一些比对值
	 */
	private void cacheUniqueSet() {
		if (!uniques.isEmpty()) {
			uniques.values().forEach(field -> {
				if (businessFieldMap.containsKey(field.getInternalKey())) {
					BusinessModuleField businessModuleField = businessFieldMap.get(field.getInternalKey());
					String fieldName = businessModuleField.getBusinessKey();
					List<String> valList = commonMapper.getCheckValList(CaseFormatUtils.camelToUnderscore(entityClass.getSimpleName()), fieldName, currentOrg);
					uniqueCheckSet.put(field.getName(), new HashSet<>(valList.stream().distinct().toList()));
				} else {
					LambdaQueryWrapper<F> wrapper = new LambdaQueryWrapper<>();
					wrapper.eq(BaseResourceField::getFieldId, field.getId());
					List<F> sourceList = fieldMapper.selectListByLambda(wrapper);
					uniqueCheckSet.put(field.getName(), new HashSet<>(sourceList.stream().map(BaseModuleFieldValue::getFieldValue).map(String::valueOf).distinct().toList()));
				}
			});
		}
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
			// 批次插入成功统计
			successCount += this.dataList.size();
			this.dataList.clear();
			this.fields.clear();
			this.blobFields.clear();
		}
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
				if (field == null || field.isSerialNumber()) {
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
						LogUtils.error("import error, cannot set property. {}", e.getMessage());
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
			LogUtils.error("import error: {}", e.getMessage());
			throw new GenericException(Translator.getWithArgs("import.error", rowIndex + 1).concat(" " + e.getMessage()));
		}
	}

	/**
	 * 自定义字段文本转换
	 * @param text 文本
	 * @param field 字段
	 * @return 值
	 */
	@SuppressWarnings("unchecked")
	private Object convertValue(String text, BaseField field) {
		if (StringUtils.isEmpty(text)) {
			return null;
		}
		try {
			AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
			return customFieldResolver.text2Value(field, text);
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
			if (uniques.containsKey(v) && !checkFieldValUnique(rowData.get(k), fieldMap.get(v))) {
				errText.append(v).append(Translator.get("cell.not.unique")).append(";");
			}
			if (fieldLenLimit.containsKey(v) && StringUtils.isNotEmpty(rowData.get(k)) &&
					rowData.get(k).length() > fieldLenLimit.get(v)) {
				errText.append(v).append(Translator.getWithArgs("over.length", fieldLenLimit.get(v))).append(";");
			}
			if (regexMap.containsKey(v) && StringUtils.isNotEmpty(rowData.get(k))) {
				Pattern pattern = regexMap.get(v);
				if (!pattern.matcher(rowData.get(k)).matches()) {
					errText.append(v).append(Translator.get("phone.wrong.format")).append(";");
				}
			}
		});
		if (StringUtils.isNotEmpty(errText)) {
			ExcelErrData excelErrData = new ExcelErrData(rowIndex,
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
		// Excel 唯一性校验
		excelValueCache.putIfAbsent(field.getId(), ConcurrentHashMap.newKeySet());
		Set<String> valueSet = excelValueCache.get(field.getId());
		if (!valueSet.add(val)) {
			return false;
		}
		// 数据库唯一性校验
		Set<String> uniqueCheck = uniqueCheckSet.get(field.getName());
		return !uniqueCheck.contains(val);
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
		setterCache.get("createTime").invoke(instance, System.currentTimeMillis());
		setterCache.get("updateUser").invoke(instance, operator);
		setterCache.get("updateTime").invoke(instance, System.currentTimeMillis());
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
	private String checkIllegalHead(Map<Integer, String> headMap) {
		Collection<String> values = headMap.values();
		for (BaseField field : fieldMap.values()) {
			if (!field.canImport() || Strings.CS.equals(field.getType(), FieldType.TEXTAREA.name())) {
				continue;
			}
			if (!values.contains(field.getName())) {
				return field.getName();
			}
		}
		return null;
	}
}
