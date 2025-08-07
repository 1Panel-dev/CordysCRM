package io.cordys.crm.system.excel.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.domain.BaseResourceField;
import io.cordys.common.exception.GenericException;
import io.cordys.common.mapper.CommonMapper;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.excel.domain.ExcelErrData;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CustomFieldCheckEventListener<T extends BaseResourceField> extends AnalysisEventListener<Map<Integer, String>> {

	@Getter
	protected Integer success = 0;
	@Getter
	protected List<ExcelErrData<?>> errList = new ArrayList<>();
	private Map<Integer, String> headMap;
	private final Map<String, BaseField> fieldMap;
	/**
	 * 源数据表
	 */
	private final String sourceTable;
	private final String currentOrg;
	/**
	 * 必填
	 */
	private final List<String> requires = new ArrayList<>();
	/**
	 * 数据库唯一属性缓存
	 */
	private final Map<String, BaseField> uniques = new HashMap<>();
	private final Map<String, Set<String>> uniqueCheckSet = new ConcurrentHashMap<>();
	private final CommonMapper commonMapper;
	private final BaseMapper<T> fieldMapper;
	/**
	 * 值缓存, 用来校验Excel字段值是否唯一
	 */
	private final Map<String, Set<String>> excelValueCache = new ConcurrentHashMap<>();
	private Map<String, BusinessModuleField> businessFieldMap;
	/**
	 * 限制数据长度的字段
	 */
	private final Map<String, Integer> fieldLenLimit = new HashMap<>();

	public CustomFieldCheckEventListener(List<BaseField> fields, String source, String currentOrg, BaseMapper<T> fieldMapper) {
		fields.forEach(field -> {
			if (field.needRequireCheck()) {
				requires.add(field.getName());
			}
			if (field.needRepeatCheck()) {
				uniques.put(field.getName(), field);
			}
			if (StringUtils.equalsAny(field.getType(), FieldType.INPUT.name(), FieldType.INPUT_NUMBER.name(), FieldType.DATE_TIME.name(),
					FieldType.MEMBER.name(), FieldType.DEPARTMENT.name(), FieldType.DATA_SOURCE.name(), FieldType.RADIO.name(),
					FieldType.SELECT.name(), FieldType.PHONE.name(), FieldType.LOCATION.name())) {
				fieldLenLimit.put(field.getName(), 255);
			}
			if (StringUtils.equals(field.getType(), FieldType.TEXTAREA.name())) {
				fieldLenLimit.put(field.getName(), 1000);
			}
		});
		this.fieldMap = fields.stream().collect(Collectors.toMap(BaseField::getName, v -> v));
		this.sourceTable = source;
		this.currentOrg = currentOrg;
		this.commonMapper = CommonBeanFactory.getBean(CommonMapper.class);
		this.fieldMapper = fieldMapper;
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
		cacheUniqueSet();
	}

	@Override
	public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
		Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
		// validate row data
		validateRowData(rowIndex, data);
	}

	@Override
	public void doAfterAllAnalysed(AnalysisContext analysisContext) {

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
					List<String> valList = commonMapper.getValList(sourceTable, fieldName, currentOrg);
					uniqueCheckSet.put(field.getName(), new HashSet<>(valList.stream().distinct().toList()));
				} else {
					LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
					wrapper.eq(BaseResourceField::getFieldId, field.getId());
					List<T> sourceList = fieldMapper.selectListByLambda(wrapper);
					uniqueCheckSet.put(field.getName(), new HashSet<>(sourceList.stream().map(BaseModuleFieldValue::getFieldValue).map(String::valueOf).distinct().toList()));
				}
			});
		}
	}

	/**
	 * 校验行数据
	 * @param rowIndex 行索引
	 * @param rowData 行数据
	 */
	private void validateRowData(Integer rowIndex, Map<Integer, String> rowData) {
		StringBuilder errText = new StringBuilder();
		headMap.forEach((k, v) -> {
			if (requires.contains(v) && StringUtils.isEmpty(rowData.get(k))) {
				errText.append(v).append(Translator.get("cannot_be_null")).append(";");
			}
			if (uniques.containsKey(v) && !checkFieldValUnique(rowData.get(k), uniques.get(v))) {
				errText.append(v).append(Translator.get("cell.not.unique")).append(";");
			}
			if (fieldLenLimit.containsKey(v) && rowData.get(k).length() > fieldLenLimit.get(v)) {
				errText.append(v).append(Translator.getWithArgs("over.length", fieldLenLimit.get(v))).append(";");
			}
		});
		if (StringUtils.isNotEmpty(errText)) {
			ExcelErrData<?> excelErrData = new ExcelErrData<>(rowIndex,
					Translator.getWithArgs("row.error.tip", rowIndex + 1).concat(" " + errText));
			//错误信息
			errList.add(excelErrData);
		} else {
			success++;
		}
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
	 * 表头是否非法
	 * @param headMap 表头集合
	 * @return 是否非法
	 */
	private boolean checkIllegalHead(Map<Integer, String> headMap) {
		boolean illegal = false;
		Collection<String> values = headMap.values();
		for (BaseField field : fieldMap.values()) {
			if (!field.canImport() || StringUtils.equals(field.getType(), FieldType.TEXTAREA.name())) {
				continue;
			}
			if (!values.contains(field.getName())) {
				illegal = true;
				break;
			}
		}
		return illegal;
	}
}
