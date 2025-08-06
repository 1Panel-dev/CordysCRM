package io.cordys.crm.system.excel.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.domain.BaseResourceField;
import io.cordys.common.exception.GenericException;
import io.cordys.common.mapper.CommonMapper;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.Translator;
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
	 * 唯一
	 */
	private final Map<String, BaseField> uniques = new HashMap<>();
	private final CommonMapper commonMapper;
	private final BaseMapper<T> fieldMapper;
	/**
	 * 值缓存, 用来校验Excel字段值是否唯一
	 */
	private final Map<String, Set<String>> excelValueCache = new ConcurrentHashMap<>();

	public CustomFieldCheckEventListener(List<BaseField> fields, String source, String currentOrg, BaseMapper<T> fieldMapper) {
		fields.forEach(field -> {
			if (field.needRequireCheck()) {
				requires.add(field.getName());
			}
			if (field.needRepeatCheck()) {
				uniques.put(field.getName(), field);
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
	}

	@Override
	public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
		Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
		if (rowIndex >= 1) {
			// validate row data
			validateRowData(rowIndex, data);
		}
	}

	@Override
	public void doAfterAllAnalysed(AnalysisContext analysisContext) {

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
		Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
				collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));
		if (businessModuleFieldMap.containsKey(field.getInternalKey())) {
			BusinessModuleField businessModuleField = businessModuleFieldMap.get(field.getInternalKey());
			String fieldName = businessModuleField.getBusinessKey();
			return !commonMapper.checkAddExist(sourceTable, fieldName, val, currentOrg);
		} else {
			LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
			wrapper.eq(BaseResourceField::getFieldId, field.getId());
			wrapper.eq(BaseResourceField::getFieldValue, val);
			return fieldMapper.selectListByLambda(wrapper).isEmpty();
		}
	}

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
