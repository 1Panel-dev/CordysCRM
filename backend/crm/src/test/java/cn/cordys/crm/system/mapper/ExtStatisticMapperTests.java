package cn.cordys.crm.system.mapper;

import cn.cordys.crm.base.BaseTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统计字段字段值读取语句的冒烟测试。
 *
 * <p>取值有两种存法(业务字段在主表列上, 自定义字段在字段值表里), 两条语句都要能拼出能执行的 SQL。
 * 聚合与游标语句已按表单下放, 覆盖在 {@code StatisticSqlMapperTests}。</p>
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
     * 业务字段直接按主键读主表列, 数据不存在时返回 null。
     */
    @Test
    void selectBusinessFieldValueReturnsNullWhenDataAbsent() {
        assertNull(extStatisticMapper.selectBusinessFieldValue(TARGET_TABLE, "amount", "no-such-data-id"));
    }

    /**
     * 统计范围条件里的「匹配字段」右值读的是宿主记录的字段值表, 没有值时返回 null。
     *
     * <p>返回 null 会被上层当成「这个条件取不到值」丢掉, 所以这里必须是真的 null 而不是空串。</p>
     */
    @Test
    void selectFieldValueReturnsNullWhenValueAbsent() {
        assertNull(extStatisticMapper.selectFieldValue(
                TARGET_FIELD_TABLE, TARGET_BLOB_TABLE, "no-such-field-id", "no-such-data-id", false));
        assertNull(extStatisticMapper.selectFieldValue(
                TARGET_FIELD_TABLE, TARGET_BLOB_TABLE, "no-such-field-id", "no-such-data-id", true));
    }
}
