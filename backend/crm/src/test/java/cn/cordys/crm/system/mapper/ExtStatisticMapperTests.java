package cn.cordys.crm.system.mapper;

import cn.cordys.crm.base.BaseTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统计字段聚合 SQL 的冒烟测试。
 *
 * <p>这里的语句是动态拼的({@code <choose>} + {@code <include>}), 拼错在编译期和启动期都发现不了,
 * 只有真正执行才会暴露, 所以每种取值口径都跑一遍: 关联字段/被统计字段各分业务字段与自定义字段,
 * 业务字段走主表列(驼峰列名还要过一次下划线转换), 自定义字段走字段值表。</p>
 *
 * <p>全部是只读查询, 用不存在的ID跑, 不依赖也不产生任何测试数据。</p>
 */
class ExtStatisticMapperTests extends BaseTest {

    private static final String TARGET_TABLE = "opportunity";
    private static final String TARGET_FIELD_TABLE = "opportunity_field";
    private static final String TARGET_BLOB_TABLE = "opportunity_field_blob";

    @Resource
    private ExtStatisticMapper extStatisticMapper;

    /**
     * 关联字段与被统计字段都是业务字段: 整条语句不应出现字段值表。
     */
    @Test
    void aggregateByBusinessRelationAndBusinessValue() {
        assertNull(aggregate("customer_id", "amount"));
    }

    /**
     * 多单词的业务字段列名过的是下划线形式。
     *
     * <p>枚举里写的是 {@code planAmount} 这种驼峰, 直接拼进 SQL 会变成 {@code `planAmount`} 而报表不存在,
     * 单单词的列名(amount/price)测不出这个转换, 必须拿一个多单词的来跑。</p>
     */
    @Test
    void aggregateByBusinessValueWithMultiWordColumn() {
        assertNull(extStatisticMapper.selectAggregate(
                "contract_payment_plan", "contract_payment_plan_field", "contract_payment_plan_field_blob",
                DEFAULT_ORGANIZATION_ID, "contract_id", null, null, "plan_amount",
                "no-such-data-id", "SUM", false, List.of(), "AND"));
    }

    /**
     * 关联字段是自定义字段时, 只能按 field_id + field_value 去字段值表里反查数据行。
     */
    @Test
    void aggregateByCustomRelation() {
        assertNull(extStatisticMapper.selectAggregate(
                TARGET_TABLE, TARGET_FIELD_TABLE, TARGET_BLOB_TABLE, DEFAULT_ORGANIZATION_ID,
                "related-field-id", null, "statistic-field-id", null,
                "no-such-data-id", "SUM", false, List.of(), "AND"));
    }

    /**
     * 关联字段是自定义字段、被统计字段是业务字段。
     */
    @Test
    void aggregateByCustomRelationAndBusinessValue() {
        assertNull(extStatisticMapper.selectAggregate(
                TARGET_TABLE, TARGET_FIELD_TABLE, TARGET_BLOB_TABLE, DEFAULT_ORGANIZATION_ID,
                "related-field-id", null, null, "amount",
                "no-such-data-id", "SUM", false, List.of(), "AND"));
    }

    /**
     * AVG 在跳过空值 / 空值当 0 两种口径下是两条不同的 SQL。
     */
    @Test
    void aggregateByBusinessRelationForAvg() {
        assertNull(extStatisticMapper.selectAggregate(
                TARGET_TABLE, TARGET_FIELD_TABLE, TARGET_BLOB_TABLE, DEFAULT_ORGANIZATION_ID,
                "customer_id", "amount", null, null,
                "no-such-data-id", "AVG", true, List.of(), "AND"));
    }

    /**
     * COUNT 不取值、不 join 取值用的表, 只数关联数据条数, 没有数据时返回 0 而不是 null。
     */
    @Test
    void aggregateByCount() {
        BigDecimal value = extStatisticMapper.selectAggregate(
                TARGET_TABLE, TARGET_FIELD_TABLE, TARGET_BLOB_TABLE, DEFAULT_ORGANIZATION_ID,
                "customer_id", "amount", null, null,
                "no-such-data-id", "COUNT", false, List.of(), "AND");
        assertEquals(0, value.compareTo(BigDecimal.ZERO));
    }

    /**
     * 业务字段直接按主键读主表列, 数据不存在时返回 null。
     */
    @Test
    void selectBusinessFieldValueReturnsNullWhenDataAbsent() {
        assertNull(extStatisticMapper.selectBusinessFieldValue(TARGET_TABLE, "amount", "no-such-data-id"));
    }

    private BigDecimal aggregate(String relatedBusinessKey, String statisticBusinessKey) {
        return extStatisticMapper.selectAggregate(
                TARGET_TABLE, TARGET_FIELD_TABLE, TARGET_BLOB_TABLE, DEFAULT_ORGANIZATION_ID,
                null, relatedBusinessKey, null, statisticBusinessKey,
                "no-such-data-id", "SUM", false, List.of(), "AND");
    }
}
