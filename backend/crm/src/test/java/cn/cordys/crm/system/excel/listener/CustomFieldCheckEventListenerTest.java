package cn.cordys.crm.system.excel.listener;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.TextAreaField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.ControlRuleProp;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.field.base.RuleProp;
import cn.idev.excel.context.AnalysisContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomFieldCheckEventListenerTest {

    private static final String TYPE_ID = "type-id";
    private static final String CUSTOMER_ID = "customer-id";
    private static final String CLUE_ID = "clue-id";

    private MessageSource originalMessageSource;

    @BeforeEach
    void setUpTranslator() {
        originalMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        ReflectionTestUtils.setField(Translator.class, "messageSource", new StaticMessageSource());
    }

    @AfterEach
    void restoreTranslator() {
        ReflectionTestUtils.setField(Translator.class, "messageSource", originalMessageSource);
    }

    @Test
    void allowsMissingHeaderForConditionalField() {
        CustomFieldCheckEventListener listener = newListener();
        AnalysisContext context = headRowContext();

        Map<Integer, String> headMap = new LinkedHashMap<>();
        headMap.put(0, "跟进类型");
        headMap.put(1, "客户名称");

        // 公司名称受显隐规则控制, 导入模板中可缺失该列
        assertDoesNotThrow(() -> listener.invokeHeadMap(headMap, context));
    }

    @Test
    void rejectsMissingHeaderForNormalField() {
        CustomFieldCheckEventListener listener = newListener();
        AnalysisContext context = headRowContext();

        Map<Integer, String> headMap = new LinkedHashMap<>();
        headMap.put(0, "客户名称");
        headMap.put(1, "公司名称");

        // 跟进类型是控制字段, 不受显隐规则控制, 缺失时仍需报错
        assertThrows(GenericException.class, () -> listener.invokeHeadMap(headMap, context));
    }

    @Test
    void requiredCheckFollowsShowControlRules() {
        CustomFieldCheckEventListener listener = newListener();
        AnalysisContext context = headRowContext(1, 2, 3);

        Map<Integer, String> headMap = new LinkedHashMap<>();
        headMap.put(0, "跟进类型");
        headMap.put(1, "客户名称");
        headMap.put(2, "公司名称");
        listener.invokeHeadMap(headMap, context);

        // 客户类型: 客户名称显示且已填写, 公司名称隐藏, 不校验必填
        listener.invoke(row("客户", "客户A", ""), context);
        // 线索类型: 公司名称显示且已填写, 客户名称隐藏, 不校验必填
        listener.invoke(row("线索", "", "线索B"), context);
        // 客户类型: 客户名称显示但未填写, 需要报必填错误
        listener.invoke(row("客户", "", ""), context);

        assertTrue(listener.getErrRows().contains(3));
        assertFalse(listener.getErrRows().contains(1));
        assertFalse(listener.getErrRows().contains(2));
    }

    @Test
    void allowsMissingHeaderForOptionalTextArea() {
        CustomFieldCheckEventListener listener = newTextAreaListener(false);
        AnalysisContext context = headRowContext();

        Map<Integer, String> headMap = new LinkedHashMap<>();
        headMap.put(0, "Other");

        assertDoesNotThrow(() -> listener.invokeHeadMap(headMap, context));
    }

    @Test
    void rejectsBlankRequiredTextAreaCell() {
        CustomFieldCheckEventListener listener = newTextAreaListener(true);
        AnalysisContext context = headRowContext(1);

        Map<Integer, String> headMap = new LinkedHashMap<>();
        headMap.put(0, "Content");
        listener.invokeHeadMap(headMap, context);

        Map<Integer, String> row = new LinkedHashMap<>();
        row.put(0, "");
        listener.invoke(row, context);

        assertTrue(listener.getErrRows().contains(1));
    }

    private CustomFieldCheckEventListener newListener() {
        List<BaseField> fields = new ArrayList<>();
        fields.add(controlField());
        fields.add(requiredInput(CUSTOMER_ID, "客户名称"));
        fields.add(requiredInput(CLUE_ID, "公司名称"));
        return new CustomFieldCheckEventListener(fields, "follow_up_record", "follow_up_record_field",
                "org-id", ImportType.ADD.name());
    }

    private SelectField controlField() {
        SelectField field = new SelectField();
        field.setId(TYPE_ID);
        field.setName("跟进类型");
        field.setType("SELECT");
        field.setReadable(true);
        field.setEditable(true);
        field.setRules(new ArrayList<>(List.of(new RuleProp("required"))));

        List<OptionProp> options = new ArrayList<>();
        options.add(new OptionProp("CUSTOMER", "客户"));
        options.add(new OptionProp("CLUE", "线索"));
        field.setOptions(options);

        ControlRuleProp customerRule = new ControlRuleProp();
        customerRule.setValue("CUSTOMER");
        customerRule.setFieldIds(new ArrayList<>(List.of(CUSTOMER_ID)));
        ControlRuleProp clueRule = new ControlRuleProp();
        clueRule.setValue("CLUE");
        clueRule.setFieldIds(new ArrayList<>(List.of(CLUE_ID)));
        field.setShowControlRules(new ArrayList<>(List.of(customerRule, clueRule)));
        return field;
    }

    private InputField requiredInput(String id, String name) {
        InputField field = new InputField();
        field.setId(id);
        field.setName(name);
        field.setType("INPUT");
        field.setReadable(true);
        field.setEditable(true);
        field.setRules(new ArrayList<>(List.of(new RuleProp("required"))));
        return field;
    }

    private CustomFieldCheckEventListener newTextAreaListener(boolean required) {
        TextAreaField field = new TextAreaField();
        field.setId("content-id");
        field.setName("Content");
        field.setType("TEXTAREA");
        field.setReadable(true);
        field.setEditable(true);
        field.setRules(required ? new ArrayList<>(List.of(new RuleProp("required"))) : new ArrayList<>());
        return new CustomFieldCheckEventListener(List.of(field), "follow_up_record", "follow_up_record_field",
                "org-id", ImportType.ADD.name());
    }

    private Map<Integer, String> row(String type, String customer, String clue) {
        Map<Integer, String> row = new LinkedHashMap<>();
        row.put(0, type);
        row.put(1, customer);
        row.put(2, clue);
        return row;
    }

    private AnalysisContext headRowContext(Integer... dataRowIndexes) {
        AnalysisContext context = mock(AnalysisContext.class, RETURNS_DEEP_STUBS);
        when(context.readWorkbookHolder().getHeadRowNumber()).thenReturn(1);
        when(context.readRowHolder().getRowIndex()).thenReturn(0, dataRowIndexes);
        return context;
    }
}
