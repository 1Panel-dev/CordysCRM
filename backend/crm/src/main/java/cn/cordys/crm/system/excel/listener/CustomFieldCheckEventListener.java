package cn.cordys.crm.system.excel.listener;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.ControlRuleProp;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.excel.domain.ExcelErrData;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.metadata.CellExtra;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */
public class CustomFieldCheckEventListener extends AnalysisEventListener<Map<Integer, String>> {

    /**
     * 表头字段集合
     */
    protected final Map<String, BaseField> fieldMap = new HashMap<>();
    /**
     * 源数据表
     */
    private final String sourceTable;
    protected final String fieldTable;
    protected final String currentOrg;
    /**
     * 类型
     */
    protected final String importType;
    /**
     * 必填校验
     */
    private final List<String> requires = new ArrayList<>();
    /**
     * 唯一校验&amp;&amp;数据库属性值缓存&amp;&amp;Excel列值缓存
     */
    private final Map<String, BaseField> uniques = new HashMap<>();
    private final Map<String, BigDecimal> numberMax = new HashMap<>();
    private final Map<String, Set<BaseResourceSubField>> uniqueCheckSet = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> excelValueCache = new ConcurrentHashMap<>();
    protected final CommonMapper commonMapper;
    /**
     * 长度校验
     */
    private final Map<String, Integer> fieldLenLimit = new HashMap<>();
    /**
     * 错误, 成功信息
     */
    @Getter
    protected Integer success = 0;
    @Getter
    protected List<ExcelErrData> errList = new ArrayList<>();
    /**
     * 表头字段集合 &amp;&amp; 业务字段集合映射
     */
    protected Map<Integer, String> headMap;
    protected Map<Integer, String> checkHeadMap;
    protected Map<String, BusinessModuleField> businessFieldMap;
    /**
     * 错误行号集合
     */
    @Getter
    protected List<Integer> errRows = new ArrayList<>();
    /**
     * 子字段引用映射(子字段名称 -> 子表格字段ID)
     */
    protected final Map<String, String> refSubMap = new HashMap<>();
    /**
     * 合并单元格信息
     */
    protected final Map<Integer, List<CellExtra>> mergeCellMap;
    /**
     * 是否至少有一行数据 &amp;&amp; 表头行数
     */
    protected boolean atLeastOne = false;
    protected int maxHeadRow;
    protected final Map<Integer, Map<Integer, String>> mergeRowDataMap;
    protected Map<Integer, String> firstHeadMap = new HashMap<>();
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");
    protected Map<String, BaseField> priceSubRefFieldMap = new HashMap<>();

    private final List<String> subFields = new ArrayList<>();

    protected Map<String, String> subTable = new HashMap<>();

    /**
     * 可导入字段缓存(字段ID -> 字段), 用于解析显隐规则控制字段的选项
     */
    private final Map<String, BaseField> fieldsById = new HashMap<>();
    /**
     * 显隐规则缓存: 被控字段ID -> (控制字段ID -> 触发显示的控制字段值集合)
     */
    private final Map<String, Map<String, Set<String>>> showControlMap = new HashMap<>();
    /**
     * 字段ID -> 表头列下标, 表头解析完成后构建, 用于按行判断被控字段是否显示
     */
    private final Map<String, Integer> fieldColumnIndexMap = new HashMap<>();

    public CustomFieldCheckEventListener(List<BaseField> fields, String sourceTable, String fieldTable,
            String currentOrg, String importType) {
        this(fields, sourceTable, fieldTable, currentOrg, null, null, importType);
    }

    public CustomFieldCheckEventListener(List<BaseField> fields, String sourceTable, String fieldTable,
            String currentOrg,
            Map<Integer, List<CellExtra>> mergeCellMap, Map<Integer, Map<Integer, String>> mergeRowDataMap,
            String importType) {
        for (BaseField field : fields) {
            if (isInvalidField(field)) {
                continue;
            }
            registerField(field);
            if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
                for (BaseField f : subField.getSubFields()) {
                    if (isInvalidField(f)) {
                        continue;
                    }
                    registerField(f);
                    this.fieldMap.put(subField.getName() + "_" + f.getName(), f);
                    refSubMap.put(subField.getName() + "_" + f.getName(), subField.getId());
                    setCheckLimit(f, subField.getName());
                    setNumberMax(f, subField.getName());
                    if (f instanceof DatasourceField priceSource) {
                        if (Strings.CI.equals(priceSource.getDataSourceType(), "PRICE")) {
                            this.subTable.put(subField.getId(), subField.getName() + "_" + f.getName());
                            Set<String> ids = priceSource.getShowFields().stream()
                                    .map(subfield -> priceSource.getId() + "_ref_" + subfield)
                                    .collect(Collectors.toSet());

                            Map<String, BaseField> refFieldMap = priceSource.getRefFields().stream()
                                    .filter(refField -> ids.contains(refField.getId())
                                            && StringUtils.isNotBlank(refField.getSubTableFieldId()))
                                    .collect(Collectors.toMap(
                                            BaseField::getId,
                                            Function.identity()));

                            Map<String, BaseField> priceSubRefFieldMap = subField.getSubFields().stream()
                                    .filter(subBasefield -> refFieldMap.containsKey(subBasefield.getId()))
                                    .collect(Collectors.toMap(
                                            subBasefield -> subField.getName() + "_" + subBasefield.getName(),
                                            subBasefield -> refFieldMap.get(subBasefield.getId())));
                            this.priceSubRefFieldMap.putAll(priceSubRefFieldMap);
                        }
                    }
                }
                continue;
            }
            this.fieldMap.put(field.getName() + "_" + field.getName(), field);
            setCheckLimit(field, null);
            setNumberMax(field, null);
        }
        this.sourceTable = sourceTable;
        this.currentOrg = currentOrg;
        this.commonMapper = CommonBeanFactory.getBean(CommonMapper.class);
        this.fieldTable = fieldTable;
        this.mergeCellMap = mergeCellMap;
        this.mergeRowDataMap = mergeRowDataMap;
        this.importType = importType;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        maxHeadRow = context.readWorkbookHolder().getHeadRowNumber();
        if (context.readRowHolder().getRowIndex() != maxHeadRow - 1) {
            this.firstHeadMap = headMap;
            return;
        }
        if (headMap == null) {
            throw new GenericException(Translator.get("user_import_table_header_missing"));
        }
        if (Strings.CI.equals(importType, ImportType.UPDATE.name()) && !headMap.containsValue("唯一ID")) {
            throw new GenericException(Translator.getWithArgs("illegal_header", "唯一ID"));
        }

        String errHead = checkIllegalHead(headMap);
        if (StringUtils.isNotEmpty(errHead)) {
            throw new GenericException(Translator.getWithArgs("illegal_header", errHead));
        }
        Map<Integer, String> realHeadMap = new HashMap<>();
        if (maxHeadRow == 2) {
            for (Map.Entry<Integer, String> entry : firstHeadMap.entrySet()) {
                Integer key = entry.getKey();
                String value = entry.getValue();
                realHeadMap.put(key, value + "_" + headMap.get(key));
            }
            this.headMap = realHeadMap;
        } else {
            for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
                Integer key = entry.getKey();
                String value = entry.getValue();
                realHeadMap.put(key, value + "_" + value);
            }
            this.headMap = realHeadMap;
        }
        this.checkHeadMap = headMap;
        cacheFieldColumnIndex();
        this.businessFieldMap = Arrays.stream(BusinessModuleField.values())
                .collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));
        cacheUniqueSet();
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        if (data == null) {
            return;
        }
        String sourceId = "";
        Integer key = headMap.entrySet().stream()
                .filter(entry -> Strings.CI.equals(entry.getValue(), "唯一ID_唯一ID"))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (key != null && data.containsKey(key)) {
            sourceId = data.get(key);
        }
        atLeastOne = true;
        Integer rowIndex = context.readRowHolder().getRowIndex();
        if (key != null && mergeRowDataMap != null && mergeRowDataMap.containsKey(rowIndex)
                && mergeRowDataMap.get(rowIndex).containsKey(key)) {
            sourceId = mergeRowDataMap.get(rowIndex).get(key);
        }
        validateRowData(rowIndex, data, sourceId);
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
            uniques.forEach((k, v) -> {
                if (refSubMap.containsKey(k)) {
                    List<BaseResourceSubField> valList = commonMapper.getCheckFieldValList(sourceTable, fieldTable,
                            v.getId(), currentOrg);
                    uniqueCheckSet.put(k, new HashSet<>(valList));
                } else {
                    if (businessFieldMap.containsKey(v.getInternalKey())) {
                        // 子表格字段不走业务唯一性校验
                        BusinessModuleField businessModuleField = businessFieldMap.get(v.getInternalKey());
                        String fieldName = businessModuleField.getBusinessKey();
                        List<BaseResourceSubField> valList = commonMapper.getCheckValList(sourceTable, fieldName,
                                currentOrg);
                        uniqueCheckSet.put(v.getName(), new HashSet<>(valList.stream().distinct().toList()));
                    } else {
                        List<BaseResourceSubField> valList = commonMapper.getCheckFieldValList(sourceTable, fieldTable,
                                v.getId(), currentOrg);
                        uniqueCheckSet.put(v.getName(), new HashSet<>(valList));
                    }
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
    private void validateRowData(Integer rowIndex, Map<Integer, String> rowData, String sourceId) {
        StringBuilder errText = new StringBuilder();
        headMap.forEach((k, v) -> {
            if (!isValidateCell(rowIndex, k)) {
                return;
            }
            if (Strings.CI.equals("唯一ID", v) && Strings.CI.equals(importType, ImportType.UPDATE.name())
                    && StringUtils.isBlank(sourceId)) {
                errText.append(checkHeadMap.get(k)).append(Translator.get("cannot_be_null")).append(";");
            }
            if (Strings.CI.equals("唯一ID", v) && Strings.CI.equals(importType, ImportType.UPDATE.name())
                    && StringUtils.isNotBlank(sourceId)) {
                checkId(checkHeadMap.get(k), sourceId, errText);
            }

            if (requires.contains(v) && StringUtils.isEmpty(rowData.get(k)) && isFieldVisible(v, rowData)) {
                if (subFields.contains(v)) {
                    errText.append(v).append(Translator.get("cannot_be_null")).append(";");
                } else {
                    errText.append(checkHeadMap.get(k)).append(Translator.get("cannot_be_null")).append(";");
                }
            }
            if (uniques.containsKey(v) && !checkFieldValUnique(rowData.get(k), uniques.get(v), sourceId, v)) {
                if (subFields.contains(v)) {
                    errText.append(v).append(Translator.get("cell.not.unique")).append(";");
                } else {
                    errText.append(checkHeadMap.get(k)).append(Translator.get("cell.not.unique")).append(";");
                }
            }
            if (fieldLenLimit.containsKey(v) && StringUtils.isNotEmpty(rowData.get(k)) &&
                    rowData.get(k).length() > fieldLenLimit.get(v)) {
                if (subFields.contains(v)) {
                    errText.append(v).append(Translator.getWithArgs("over.length", fieldLenLimit.get(v))).append(";");
                } else {
                    errText.append(checkHeadMap.get(k))
                            .append(Translator.getWithArgs("over.length", fieldLenLimit.get(v))).append(";");
                }
            }
            if (numberMax.containsKey(v) && checkNumberMax(rowData.get(k), numberMax.get(v))) {
                if (subFields.contains(v)) {
                    errText.append(v).append(Translator.getWithArgs("exceed.max", numberMax.get(v))).append(";");
                } else {
                    errText.append(checkHeadMap.get(k)).append(Translator.getWithArgs("exceed.max", numberMax.get(v)))
                            .append(";");
                }
            }
        });
        if (StringUtils.isNotEmpty(errText)) {
            ExcelErrData excelErrData = new ExcelErrData(rowIndex,
                    Translator.getWithArgs("row.error.tip", rowIndex + 1).concat(" " + errText));
            //错误信息
            errList.add(excelErrData);
            errRows.add(rowIndex);
        } else if (!errRows.contains(rowIndex)) {
            success++;
        }
    }

    /**
     * 校验id
     */
    private void checkId(String v, String resourceId, StringBuilder errText) {
        if (commonMapper.checkIdCount(resourceId, sourceTable) <= 0) {
            errText.append(v).append("不存在;");
        }
    }

    /**
     * 判断单元格是否需要校验
     *
     * @param rowIndex 行序号
     * @param colIndex 列序号
     * @return 是否需要校验
     */
    private boolean isValidateCell(int rowIndex, int colIndex) {
        if (mergeCellMap == null) {
            return true;
        }
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
     * @return 是否唯一
     */
    private boolean checkFieldValUnique(String val, BaseField field, String sourceId, String v) {
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
        if (Strings.CI.equals(importType, ImportType.ADD.name()) && !refSubMap.containsKey(v)) {
            Set<BaseResourceSubField> uniqueCheck = uniqueCheckSet.get(field.getName());
            BaseResourceSubField result = uniqueCheck.stream()
                    .filter(item -> item.getFieldValue() != null
                            && Strings.CI.equals(val, item.getFieldValue().toString()))
                    .findFirst()
                    .orElse(null);
            return result == null;
        }

        if (Strings.CI.equals(importType, ImportType.UPDATE.name())) {
            if (StringUtils.isBlank(sourceId)) {
                return true;
            }
            Set<BaseResourceSubField> uniqueCheck;
            if (refSubMap.containsKey(v)) {
                uniqueCheck = uniqueCheckSet.get(v);
                BaseResourceSubField result = uniqueCheck.stream()
                        .filter(item -> Strings.CI.equals(item.getResourceId(), sourceId)
                                && item.getFieldValue() != null
                                && Strings.CS.equals(val, item.getFieldValue().toString()))
                        .findFirst()
                        .orElse(null);
                return result == null;
            } else {
                uniqueCheck = uniqueCheckSet.get(field.getName());
                BaseResourceSubField result = uniqueCheck.stream()
                        .filter(item -> !Strings.CI.equals(item.getResourceId(), sourceId)
                                && item.getFieldValue() != null
                                && Strings.CS.equals(val, item.getFieldValue().toString()))
                        .findFirst()
                        .orElse(null);
                return result == null;
            }
        }
        return true;
    }

    /**
     * 最大值校验
     */
    private boolean checkNumberMax(String val, BigDecimal max) {
        if (StringUtils.isNotBlank(val)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(val);
                return bigDecimal.compareTo(max) > 0;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 缓存可导入字段及其显隐规则
     *
     * @param field 字段
     */
    private void registerField(BaseField field) {
        if (StringUtils.isEmpty(field.getId())) {
            return;
        }
        fieldsById.put(field.getId(), field);
        if (CollectionUtils.isEmpty(field.getShowControlRules())) {
            return;
        }
        for (ControlRuleProp rule : field.getShowControlRules()) {
            if (rule == null || StringUtils.isEmpty(rule.getValue())
                    || CollectionUtils.isEmpty(rule.getFieldIds())) {
                continue;
            }
            for (String targetFieldId : rule.getFieldIds()) {
                if (StringUtils.isEmpty(targetFieldId)) {
                    continue;
                }
                showControlMap.computeIfAbsent(targetFieldId, key -> new HashMap<>())
                        .computeIfAbsent(field.getId(), key -> new HashSet<>())
                        .add(rule.getValue());
            }
        }
    }

    /**
     * 表头解析完成后缓存字段所在列, 用于按行计算显隐规则
     */
    private void cacheFieldColumnIndex() {
        fieldColumnIndexMap.clear();
        if (headMap == null) {
            return;
        }
        headMap.forEach((columnIndex, columnKey) -> {
            BaseField field = fieldMap.get(columnKey);
            if (field != null && StringUtils.isNotEmpty(field.getId())) {
                fieldColumnIndexMap.putIfAbsent(field.getId(), columnIndex);
            }
        });
    }

    /**
     * 判断指定列对应字段在当前行是否显示(命中显隐规则)。
     * 无显隐规则的字段始终显示; 被控字段需其控制字段当前行取值命中规则值才显示。
     *
     * @param columnKey 列Key (字段名_字段名)
     * @param rowData   行数据
     * @return 是否显示
     */
    private boolean isFieldVisible(String columnKey, Map<Integer, String> rowData) {
        BaseField field = fieldMap.get(columnKey);
        if (field == null) {
            return true;
        }
        Map<String, Set<String>> controlRules = showControlMap.get(field.getId());
        if (controlRules == null || controlRules.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Set<String>> entry : controlRules.entrySet()) {
            Integer columnIndex = fieldColumnIndexMap.get(entry.getKey());
            if (columnIndex == null) {
                continue;
            }
            String cellText = rowData.get(columnIndex);
            if (StringUtils.isEmpty(cellText)) {
                continue;
            }
            // 导入模板中选项列填写的是选项文本, 需转换为选项值后再与规则值比较
            if (entry.getValue().contains(resolveControlValue(fieldsById.get(entry.getKey()), cellText))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将控制字段的单元格文本转换为选项值
     *
     * @param controlField 控制字段
     * @param cellText     单元格文本
     * @return 选项值, 无法匹配选项时返回原文本
     */
    private String resolveControlValue(BaseField controlField, String cellText) {
        if (controlField instanceof HasOption optionField
                && CollectionUtils.isNotEmpty(optionField.getOptions())) {
            return optionField.getOptions().stream()
                    .filter(option -> option.getLabel() != null && Strings.CS.equals(option.getLabel(), cellText))
                    .map(OptionProp::getValue)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .findFirst()
                    .orElse(cellText);
        }
        return cellText;
    }

    /**
     * 表头是否非法
     *
     * @param headMap 表头集合
     * @return 是否非法
     */
    private String checkIllegalHead(Map<Integer, String> headMap) {
        for (BaseField field : fieldMap.values()) {
            if (!field.canImport(field)) {
                continue;
            }
            if (Strings.CS.equals(field.getType(), FieldType.TEXTAREA.name()) && !field.needRequireCheck()) {
                continue;
            }
            // 受显隐规则控制的字段可能因隐藏而缺失对应表头, 不强制要求其表头存在
            if (showControlMap.containsKey(field.getId())) {
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
     *
     * @param field 自定义字段
     */
    private void setCheckLimit(BaseField field, String subFieldName) {
        if (field.needRequireCheck() && StringUtils.isBlank(subFieldName)) {
            requires.add(field.getName() + "_" + field.getName());
        }
        if (field.needRepeatCheck()) {
            uniques.put(StringUtils.isNotEmpty(subFieldName) ? subFieldName + "_" + field.getName()
                    : field.getName() + "_" + field.getName(), field);
        }
        if (Strings.CS.equalsAny(field.getType(), FieldType.MEMBER.name(), FieldType.DEPARTMENT.name(),
                FieldType.DATA_SOURCE.name())) {
            fieldLenLimit.put(StringUtils.isNotEmpty(subFieldName) ? subFieldName + "_" + field.getName()
                    : field.getName() + "_" + field.getName(), 255);
        }
        if (Strings.CS.equalsAny(field.getType(), FieldType.INPUT.name(), FieldType.INPUT_NUMBER.name(),
                FieldType.DATE_TIME.name(), FieldType.RADIO.name(),
                FieldType.SELECT.name(), FieldType.PHONE.name(), FieldType.LOCATION.name(),
                FieldType.INDUSTRY.name())) {
            fieldLenLimit.put(StringUtils.isNotEmpty(subFieldName) ? subFieldName + "_" + field.getName()
                    : field.getName() + "_" + field.getName(), 255);
        }
        if (Strings.CS.equals(field.getType(), FieldType.TEXTAREA.name())) {
            fieldLenLimit.put(StringUtils.isNotEmpty(subFieldName) ? subFieldName + "_" + field.getName()
                    : field.getName() + "_" + field.getName(), 3000);
        }
        if (StringUtils.isNotEmpty(subFieldName)) {
            subFields.add(subFieldName + "_" + field.getName());
        }
    }

    /**
     * 设置数字类型最大值
     */
    private void setNumberMax(BaseField field, String subFieldName) {
        if (Strings.CI.equalsAny(field.getType(), FieldType.INPUT_NUMBER.name(), FieldType.FORMULA.name(),
                FieldType.STATISTIC.name())) {
            numberMax.put(StringUtils.isNotEmpty(subFieldName) ? subFieldName + "_" + field.getName()
                    : field.getName() + "_" + field.getName(), MAX_AMOUNT);
        }
    }

    private boolean isInvalidField(BaseField field) {
        if (StringUtils.isNotEmpty(field.getResourceFieldId())) {
            return true;
        }
        return !field.canImport(field) && !field.isSerialNumber();
    }
}
