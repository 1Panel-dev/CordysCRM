package cn.cordys.crm.system.dto.form;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.form.base.FormValidationRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormPropTest {

    @Test
    void deserializesValidationRulesAndDefaultsToBlockingSubmit() {
        String json = """
                {"validationRules":[
                  {"formula":"amount > 1000","message":"金额必须大于 1000","allowIgnore":true},
                  {"formula":"remark != ''","message":"备注不能为空"}
                ]}
                """;

        FormProp formProp = JSON.parseObject(json, FormProp.class);

        List<FormValidationRule> rules = formProp.getValidationRules();
        assertEquals(2, rules.size());
        assertEquals("amount > 1000", rules.get(0).getFormula());
        assertEquals("金额必须大于 1000", rules.get(0).getMessage());
        assertTrue(rules.get(0).getAllowIgnore());
        // 未显式配置时默认阻止提交
        assertFalse(rules.get(1).getAllowIgnore());
    }

    @Test
    void roundTripsValidationRulesThroughFormPropJson() {
        FormValidationRule rule = new FormValidationRule();
        rule.setFormula("field_a == 'x'");
        rule.setMessage("提示文案");
        rule.setAllowIgnore(true);
        FormProp formProp = new FormProp();
        formProp.setValidationRules(List.of(rule));

        FormProp parsed = JSON.parseObject(JSON.toJSONString(formProp), FormProp.class);

        assertEquals(1, parsed.getValidationRules().size());
        assertEquals("field_a == 'x'", parsed.getValidationRules().getFirst().getFormula());
        assertEquals("提示文案", parsed.getValidationRules().getFirst().getMessage());
        assertTrue(parsed.getValidationRules().getFirst().getAllowIgnore());
    }

    @Test
    void keepsFormPropWithoutValidationRulesCompatible() {
        FormProp formProp = JSON.parseObject("{\"layout\":1,\"labelPos\":\"top\"}", FormProp.class);

        assertEquals(1, formProp.getLayout());
        assertNull(formProp.getValidationRules());
    }
}
