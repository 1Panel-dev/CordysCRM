package cn.cordys.crm.system.excel.listener;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.excel.domain.ExcelErrData;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.metadata.CellExtra;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */
public class CustomFieldCheckEventListener extends AnalysisEventListener<Map<Integer, String>> {

    protected final Map<String, BaseField> fieldMap = new HashMap<>();
    /**
     * 源数据表
     */
    private final String sourceTable;
    private final String fieldTable;
	protected final String currentOrg;
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
    /**
     * 值缓存, 用来校验Excel字段值是否唯一
     */
    private final Map<String, Set<String>> excelValueCache = new ConcurrentHashMap<>();
    /**
     * 限制数据长度的字段
     */
    private final Map<String, Integer> fieldLenLimit = new HashMap<>();
    @Getter
    protected Integer success = 0;
    @Getter
    protected List<ExcelErrData> errList = new ArrayList<>();
	protected Map<Integer, String> headMap;
	protected Map<String, BusinessModuleField> businessFieldMap;
	/**
	 * 错误行号集合
	 */
	@Getter
	protected List<Integer> errRows = new ArrayList<>();
	protected final Map<String, String> refSubMap = new HashMap<>();
	/**
	 * 所有行数据 && 合并单元格信息
	 */
	protected boolean atLeastOne = false;
	private final Map<Integer, List<CellExtra>> mergeCellMap;
	protected int maxHeadRow;

    public CustomFieldCheckEventListener(List<BaseField> fields, String sourceTable, String fieldTable, String currentOrg,
										 Map<Integer, List<CellExtra>> mergeCellMap) {
		for (BaseField field : fields) {
			if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
				for (BaseField f : subField.getSubFields()) {
					this.fieldMap.put(f.getName(), f);
					refSubMap.put(f.getName(), subField.getId());
					setCheckLimit(f);
				}
				continue;
			}
			this.fieldMap.put(field.getName(), field);
			setCheckLimit(field);
		}
        this.sourceTable = sourceTable;
        this.currentOrg = currentOrg;
        this.commonMapper = CommonBeanFactory.getBean(CommonMapper.class);
        this.fieldTable = fieldTable;
		this.mergeCellMap = mergeCellMap;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
		maxHeadRow = context.readWorkbookHolder().getHeadRowNumber();
		if (context.readRowHolder().getRowIndex() != maxHeadRow - 1) {
			return;
		}
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
        cacheUniqueSet();
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
		if (data == null) {
			return;
		}
		atLeastOne = true;
		Integer rowIndex = context.readRowHolder().getRowIndex();
		validateRowData(rowIndex, data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        if (!atLeastOne) {
            throw new GenericException(Translator.get("import.data.cannot_be_null"));
        }
    }

    /**
     * 缓存一些比对值
     */
    private void cacheUniqueSet() {
        if (!uniques.isEmpty()) {
            uniques.values().forEach(field -> {
                if (businessFieldMap.containsKey(field.getInternalKey()) && !refSubMap.containsKey(field.getName())) {
					// 子表格字段不走业务唯一性校验
                    BusinessModuleField businessModuleField = businessFieldMap.get(field.getInternalKey());
                    String fieldName = businessModuleField.getBusinessKey();
                    List<String> valList = commonMapper.getCheckValList(sourceTable, fieldName, currentOrg);
                    uniqueCheckSet.put(field.getName(), new HashSet<>(valList.stream().distinct().toList()));
                } else {
                    List<String> valList = commonMapper.getCheckFieldValList(sourceTable, fieldTable, field.getId(), currentOrg);
                    uniqueCheckSet.put(field.getName(), new HashSet<>(valList));
                }
            });
        }
    }

    /**
     * 校验行数据
     *
     * @param rowIndex 行索引
     * @param rowData  行数据
     */
    private void validateRowData(Integer rowIndex, Map<Integer, String> rowData) {
        StringBuilder errText = new StringBuilder();
        headMap.forEach((k, v) -> {
			if (!isValidateCell(rowIndex, k)) {
				return;
			}
            if (requires.contains(v) && StringUtils.isEmpty(rowData.get(k))) {
                errText.append(v).append(Translator.get("cannot_be_null")).append(";");
            }
            if (uniques.containsKey(v) && !checkFieldValUnique(rowData.get(k), uniques.get(v))) {
                errText.append(v).append(Translator.get("cell.not.unique")).append(";");
            }
            if (fieldLenLimit.containsKey(v) && StringUtils.isNotEmpty(rowData.get(k)) &&
                    rowData.get(k).length() > fieldLenLimit.get(v)) {
                errText.append(v).append(Translator.getWithArgs("over.length", fieldLenLimit.get(v))).append(";");
            }
        });
		if (StringUtils.isNotEmpty(errText)) {
            ExcelErrData excelErrData = new ExcelErrData(rowIndex,
                    Translator.getWithArgs("row.error.tip", rowIndex + 1).concat(" " + errText));
            //错误信息
            errList.add(excelErrData);
			errRows.add(rowIndex);
        } else {
            success++;
        }
    }

	/**
	 * 判断单元格是否需要校验
	 * @param rowIndex 行序号
	 * @param colIndex 列序号
	 * @return 是否需要校验
	 */
	private boolean isValidateCell(int rowIndex, int colIndex) {
		List<CellExtra> cellExtras = mergeCellMap.get(rowIndex);
		if (cellExtras != null) {
			for (CellExtra extra : cellExtras) {
				// 属于合并列的区域内
				if (colIndex >= extra.getFirstColumnIndex() && colIndex <= extra.getLastColumnIndex()) {
					// 合并第一行也需校验
					return rowIndex == extra.getFirstRowIndex();
				}
			}
		}
		// 不属于合并单元格，直接校验
		return true;
	}

    /**
     * 检查字段值唯一
     *
     * @param val   值
     * @param field 字段
     *
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
     *
     * @param headMap 表头集合
     *
     * @return 是否非法
     */
    private String checkIllegalHead(Map<Integer, String> headMap) {
        for (BaseField field : fieldMap.values()) {
            if (!field.canImport() || Strings.CS.equals(field.getType(), FieldType.TEXTAREA.name())) {
                continue;
            }
            if (!headMap.containsValue(field.getName())) {
                return field.getName();
            }
        }
        return null;
    }

	/**
	 * 设置校验信息
	 * @param field 自定义字段
	 */
	private void setCheckLimit(BaseField field) {
		if (field.needRequireCheck()) {
			requires.add(field.getName());
		}
		if (field.needRepeatCheck()) {
			uniques.put(field.getName(), field);
		}
		if (Strings.CS.equalsAny(field.getType(), FieldType.INPUT.name(), FieldType.INPUT_NUMBER.name(), FieldType.DATE_TIME.name(),
				FieldType.MEMBER.name(), FieldType.DEPARTMENT.name(), FieldType.DATA_SOURCE.name(), FieldType.RADIO.name(),
				FieldType.SELECT.name(), FieldType.PHONE.name(), FieldType.LOCATION.name(), FieldType.INDUSTRY.name())) {
			fieldLenLimit.put(field.getName(), 255);
		}
		if (Strings.CS.equals(field.getType(), FieldType.TEXTAREA.name())) {
			fieldLenLimit.put(field.getName(), 3000);
		}
	}
}
