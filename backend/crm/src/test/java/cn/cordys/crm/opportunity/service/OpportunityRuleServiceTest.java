package cn.cordys.crm.opportunity.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.opportunity.domain.OpportunityRule;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpportunityRuleServiceTest {

    @Test
    void switchStatusLogsRuleBeforeAndAfterChange() {
        OpportunityRuleService service = new OpportunityRuleService();
        @SuppressWarnings("unchecked")
        BaseMapper<OpportunityRule> mapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "opportunityRuleMapper", mapper);

        OpportunityRule original = new OpportunityRule();
        original.setId("rule-1");
        original.setName("回收规则");
        original.setEnable(false);
        OpportunityRule persisted = new OpportunityRule();
        persisted.setId("rule-1");
        persisted.setEnable(true);
        when(mapper.selectByPrimaryKey("rule-1")).thenReturn(original, persisted);

        OperationLogContext.putEmptySpan();
        try (MockedStatic<Translator> translator = mockStatic(Translator.class)) {
            translator.when(() -> Translator.get("module.opportunity.rule.setting")).thenReturn("商机规则");

            service.switchStatus("rule-1", "operator");

            verify(mapper).updateById(argThat(rule -> "rule-1".equals(rule.getId())
                    && Boolean.TRUE.equals(rule.getEnable())));
            LogContextInfo context = OperationLogContext.getContext();
            assertEquals(false, assertInstanceOf(OpportunityRule.class, context.getOriginalValue()).getEnable());
            assertEquals(true, assertInstanceOf(OpportunityRule.class, context.getModifiedValue()).getEnable());
        } finally {
            OperationLogContext.clear();
        }
    }
}
