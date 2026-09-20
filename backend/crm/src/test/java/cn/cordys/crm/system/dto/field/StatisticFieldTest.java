package cn.cordys.crm.system.dto.field;

import cn.cordys.common.resolver.field.StatisticResolver;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.field.base.BaseField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticFieldTest {

    @Test
    void excludesDerivedPredicatesFromSerializedJson() {
        StatisticField field = new StatisticField();
        field.setId("field-1");
        field.setName("关联订单总额");
        field.setType("STATISTIC");
        field.setStatisticType("SUM");
        field.setTargetFormId("order");
        field.setRelatedFieldId("field-related");

        String json = JSON.toJSONString(field);

        assertTrue(json.contains("\"type\":\"STATISTIC\""));
        // isSum/isCount/isAvg 等派生方法会污染字段配置与前端回显, 必须被忽略
        assertFalse(json.contains("\"sum\""));
        assertFalse(json.contains("\"count\""));
        assertFalse(json.contains("\"avg\""));
        assertFalse(json.contains("\"needStatisticField\""));
        assertFalse(json.contains("\"hasStatisticField\""));
    }

    @Test
    void deserializesPolymorphicStatisticFieldByType() {
        String json = """
                {"type":"STATISTIC","id":"field-1","name":"关联订单总额","statisticType":"SUM",
                 "targetFormId":"order","relatedFieldId":"field-related","dataScope":"CONDITION",
                 "combineSearch":{"searchMode":"AND","conditions":[]}}
                """;

        BaseField field = JSON.parseObject(json, BaseField.class);

        assertTrue(field instanceof StatisticField);
        StatisticField statisticField = (StatisticField) field;
        assertEquals("field-1", statisticField.getId());
        assertTrue(statisticField.isSum());
        assertFalse(statisticField.isCount());
        assertTrue(statisticField.needStatisticField());
    }

    @Test
    void countStatisticRequiresNoAggregatedField() {
        StatisticField field = new StatisticField();
        field.setStatisticType("COUNT");

        assertTrue(field.isCount());
        assertFalse(field.isSum());
        assertFalse(field.isAvg());
        assertFalse(field.needStatisticField());
    }

    @Test
    void formatsStatisticValueAsNumberAndPercent() {
        StatisticField field = new StatisticField();
        field.setDecimalPlaces(true);
        field.setPrecision(2);
        field.setShowThousandsSeparator(true);
        StatisticResolver resolver = new StatisticResolver();

        assertEquals("1,234.57", resolver.transformToValue(field, "1234.567"));

        field.setNumberFormat("percent");
        assertEquals("1,234.57%", resolver.transformToValue(field, "1234.567"));
    }

    @Test
    void treatsBlankStatisticValueAsNull() {
        StatisticResolver resolver = new StatisticResolver();

        assertNull(resolver.convertToValue(new StatisticField(), ""));
        assertNull(resolver.transformToValue(new StatisticField(), null));
    }
}
