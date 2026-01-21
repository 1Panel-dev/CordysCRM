package cn.cordys.crm.contract.excel.listener;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.excel.constants.BusinessTitleImportFiled;
import cn.cordys.excel.domain.ExcelErrData;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BusinessTitleCheckEventListener extends AnalysisEventListener<Map<Integer, String>> {

    /**
     * 错误, 成功信息
     */
    @Getter
    protected Integer success = 0;
    @Getter
    protected List<ExcelErrData> errList = new ArrayList<>();
    protected static final int LENGTH_LIMIT = 255;
    protected final String orgId;
    private final CommonMapper commonMapper;
    @Getter
    protected List<Integer> errRows = new ArrayList<>();

    private final Class<?> excelDataClass;
    private final Map<String, Boolean> requiredFieldMap;
    private final Map<String, String> excelHeadToFieldNameDic = new HashMap<>();
    protected Map<Integer, String> headMap;
    private List<List<String>> heads;
    protected boolean atLeastOne = false;
    private final Map<String, Boolean> excelValueCache = new ConcurrentHashMap<>();

    public BusinessTitleCheckEventListener(Class<?> clazz, Map<String, Boolean> requiredFieldMap, String orgId, List<List<String>> heads) {
        this.excelDataClass = clazz;
        this.requiredFieldMap = requiredFieldMap;
        this.orgId = orgId;
        this.commonMapper = CommonBeanFactory.getBean(CommonMapper.class);
        this.heads = heads;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        if (headMap == null) {
            throw new GenericException(Translator.get("user_import_table_header_missing"));
        }

        List<String> headList = heads.stream().flatMap(List::stream).toList();

        headList.forEach(head -> {
            if (!headMap.containsValue(head)) {
                throw new GenericException(Translator.getWithArgs("illegal_header", head));
            }
        });


        this.headMap = headMap;
        try {
            genExcelHeadToFieldNameDicAndGetNotRequiredFields();
        } catch (NoSuchFieldException e) {
            log.error(e.getMessage(), e);
        }
        formatHeadMap();
        super.invokeHeadMap(headMap, context);
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
        if (data == null) {
            return;
        }
        atLeastOne = true;
        Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
        validateRowData(rowIndex, data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        if (!atLeastOne) {
            throw new GenericException(Translator.get("import.data.cannot_be_null"));
        }
    }


    private void validateRowData(Integer rowIndex, Map<Integer, String> rowData) {
        StringBuilder errText = new StringBuilder();
        headMap.forEach((k, v) -> {
            validateRequired(rowData.get(k), errText, v);
            validateLenLimit(rowData.get(k), errText, v);
            validateNameUniques(rowData.get(k), errText, v);
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

    private void validateRequired(String data, StringBuilder errText, String v) {
        if (BusinessTitleImportFiled.fromHeader(v) != null) {
            String key = BusinessTitleImportFiled.fromHeader(v).name().toLowerCase();
            if (requiredFieldMap.containsKey(key) && requiredFieldMap.get(key) && StringUtils.isBlank(data)) {
                errText.append(v).append(Translator.get("required")).append(";");
            }
        }
    }

    private void validateNameUniques(String data, StringBuilder errText, String v) {
        if (data != null && BusinessTitleImportFiled.NAME.equals(BusinessTitleImportFiled.fromHeader(v))) {
            Boolean existed = excelValueCache.putIfAbsent(data, true);
            if (existed != null) {
                errText.append(v).append(":").append(Translator.get("business_title.exist")).append(";");
                return;
            }

            boolean repeat = commonMapper.checkAddExist("business_title", BusinessTitleImportFiled.NAME.name().toLowerCase(), data, orgId);
            if (repeat) {
                errText.append(v).append(":").append(Translator.get("business_title.exist")).append(";");
            }
        }
    }

    private void validateLenLimit(String data, StringBuilder errText, String v) {
        if (StringUtils.isNotBlank(data) && data.length() > LENGTH_LIMIT) {
            errText.append(v).append(Translator.getWithArgs("over.length", LENGTH_LIMIT)).append(";");
        }
    }


    public void genExcelHeadToFieldNameDicAndGetNotRequiredFields() throws NoSuchFieldException {
        Field[] fields = excelDataClass.getDeclaredFields();
        for (Field f : fields) {
            Field field = excelDataClass.getDeclaredField(f.getName());
            field.setAccessible(true);
            ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
            if (excelProperty == null) {
                continue;
            }
            StringBuilder headValue = new StringBuilder();
            for (String v : excelProperty.value()) {
                headValue.append(v);
            }
            String head = headValue.toString();
            excelHeadToFieldNameDic.put(head, field.getName());
        }
    }

    private void formatHeadMap() {
        if (headMap == null || headMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
            String name = entry.getValue();
            if (excelHeadToFieldNameDic.containsKey(name)) {
                entry.setValue(excelHeadToFieldNameDic.get(name));
            }
        }
    }
}
