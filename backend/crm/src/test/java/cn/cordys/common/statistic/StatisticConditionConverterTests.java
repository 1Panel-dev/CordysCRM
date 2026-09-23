package cn.cordys.common.statistic;

import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.util.JSON;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 筛选弹窗条件结构 -> 高级搜索结构的转换测试。
 *
 * <p>不需要 Spring 也不需要数据库: 转换是纯函数, 而这正是它存在的理由 ——
 * 出问题的地方在键名对应关系上, 与 SQL、与库都无关。</p>
 *
 * <p>这里最要紧的一组断言是「转完之后条件还在」。不转换时 {@code leftFieldId} 无处安放,
 * {@code name} 落成 null, {@code getConditions()} 按 {@code valid()} 一过滤整条消失 ——
 * 统计范围会静默退化成「不过滤」, 统计值偏大, 而配置、日志、返回体里都看不出问题。
 * 所以每个用例都断言「条件数量对不对」, 不只看字段值。</p>
 */
class StatisticConditionConverterTests {

    /**
     * 用户在弹窗里选了一个目标表单字段跟一个固定值比: 键名换了位置, 右值原样带过来。
     */
    @Test
    void convertsMatchValueConditionIntoFilterCondition() {
        Map<String, Object> raw = combine("AND", condition("createUser", "USER", "EQUALS", "MATCH_VALUE", null));

        List<FilterCondition> conditions = StatisticConditionConverter.toCombineSearch(raw).getConditions();

        assertEquals(1, conditions.size(), "条件不该在转换中丢失");
        FilterCondition condition = conditions.getFirst();
        assertEquals("createUser", condition.getName(), "leftFieldId 应落到 name 上");
        assertEquals("USER", condition.getType(), "leftFieldType 应落到 type 上");
        assertEquals("EQUALS", condition.getOperator());
        assertEquals("user-1", condition.getValue(), "匹配值时右值就是字面量本身");
    }

    /**
     * 右值取自宿主记录(设计器里的「匹配字段」): 存成取值引用对象, 供刷新时按每条数据现取。
     */
    @Test
    void convertsMatchFieldConditionIntoReferenceValue() {
        Map<String, Object> raw = combine("AND",
                condition("amount", "NUMBER", "GREATER_THAN", "MATCH_FIELD", "host-field-id"));

        FilterCondition condition = StatisticConditionConverter.toCombineSearch(raw).getConditions().getFirst();

        Map<?, ?> ref = assertInstanceOf(Map.class, condition.getValue(),
                "匹配字段时右值应是引用对象, 而不是字段ID本身");
        assertEquals("host-field-id", ref.get("refFieldId"));
        // 断言的是「右侧」字段的类型而不是左侧的: 两者故意取不同的值, 取错了这条就会红
        assertEquals("USER", ref.get("refFieldType"),
                "宿主那侧是业务字段还是自定义字段靠 refFieldType 判断, 不能丢也不能取成左边那个");
    }

    /**
     * 每条条件带上自己的类型 —— 各表单的条件片段靠它区分 JSON 数组列与普通标量列, 认错就是算错。
     */
    @Test
    void keepsConditionTypePerCondition() {
        Map<String, Object> raw = combine("OR",
                condition("products", "SELECT", "CONTAINS", "MATCH_VALUE", null),
                condition("amount", "NUMBER", "EQUALS", "MATCH_VALUE", null));

        CombineSearch combineSearch = StatisticConditionConverter.toCombineSearch(raw);

        assertEquals("OR", combineSearch.getSearchMode(), "searchMode 应原样带过去");
        assertEquals(List.of("SELECT", "NUMBER"),
                combineSearch.getConditions().stream().map(FilterCondition::getType).toList());
    }

    /**
     * 配置里没有 searchMode 时交给 CombineSearch 兜底成 AND, 而不是留一个空串下去。
     */
    @Test
    void defaultsSearchModeToAndWhenAbsent() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("conditions", List.of(condition("createUser", "USER", "EQUALS", "MATCH_VALUE", null)));

        assertEquals("AND", StatisticConditionConverter.toCombineSearch(raw).getSearchMode());
    }

    /**
     * 没有条件(统计范围=全部、或配置被改坏)时返回空条件而不是 null。
     *
     * <p>条件片段里写着 {@code ${conditions}.size() > 0}, 传 null 会让 OGNL 直接抛异常,
     * 而不是短路成「不过滤」。</p>
     */
    @Test
    void returnsEmptyConditionForMissingStructure() {
        for (Map<String, Object> raw : List.of(new HashMap<String, Object>(), combine("AND"))) {
            CombineSearch combineSearch = StatisticConditionConverter.toCombineSearch(raw);
            assertNotNull(combineSearch, "不该返回 null");
            assertTrue(combineSearch.getConditions().isEmpty(), "空结构应转出空条件");
        }
        CombineSearch fromNull = StatisticConditionConverter.toCombineSearch(null);
        assertNotNull(fromNull, "传 null 也要能兜住");
        assertTrue(fromNull.getConditions().isEmpty());
    }

    /**
     * 半成品条件(只选了左字段、还没选操作符)转完之后一条能用的都不剩。
     *
     * <p>这是保存校验与刷新共用的判据: {@code getConditions()} 会按 {@code valid()} 过滤,
     * 校验据此把「一条能用的条件都没有」的配置拦在保存之前, 不至于存进去以后静默不过滤。</p>
     */
    @Test
    void dropsHalfFilledCondition() {
        Map<String, Object> halfFilled = new HashMap<>();
        halfFilled.put("leftFieldId", "amount");
        halfFilled.put("leftFieldType", "NUMBER");
        // operator 与右值都还没填
        Map<String, Object> raw = combine("AND", halfFilled);

        assertTrue(StatisticConditionConverter.toCombineSearch(raw).getConditions().isEmpty(),
                "没填完的条件不该被当成能用的条件");
    }

    /**
     * 已经是高级搜索结构的条件(带 {@code name})原样绑定, 不被当成弹窗结构再转一遍。
     *
     * <p>弹窗结构里没有 {@code name} 这个键, 所以拿它当判据是可靠的; 这条路径是给按新结构存过的配置留的,
     * 不能因为改了存储结构把已有配置读废。</p>
     */
    @Test
    void passesThroughConditionAlreadyInCombineSearchShape() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("name", "amount");
        existing.put("type", "NUMBER");
        existing.put("operator", "GREATER_THAN");
        existing.put("value", "100");
        Map<String, Object> raw = combine("AND", existing);

        FilterCondition condition = StatisticConditionConverter.toCombineSearch(raw).getConditions().getFirst();

        assertEquals("amount", condition.getName(), "已是高级搜索结构时不该被 leftFieldId 覆盖成 null");
        assertEquals("GREATER_THAN", condition.getOperator());
        assertEquals("100", condition.getValue());
    }

    /**
     * 结构不认识时不抛异常: 条件不是对象、conditions 不是数组, 都当作「没有条件」处理。
     *
     * <p>刷新入口的 try/catch 会把这个字段记下来继续跑别的, 但保存校验已经拦过一道,
     * 走到这里的多是历史数据或直接改库 —— 为此整个刷新失败不值当, 丢条件并留痕即可。</p>
     */
    @Test
    void ignoresUnrecognizedStructureInsteadOfFailing() {
        Map<String, Object> raw = new HashMap<>();
        List<Object> conditions = new ArrayList<>();
        conditions.add("不是对象");
        conditions.add(condition("amount", "NUMBER", "EQUALS", "MATCH_VALUE", null));
        raw.put("conditions", conditions);

        List<FilterCondition> converted = StatisticConditionConverter.toCombineSearch(raw).getConditions();
        assertEquals(1, converted.size(), "认得出来的条件仍要转, 不因为邻居是脏数据就整段放弃");
        assertEquals("amount", converted.getFirst().getName());

        Map<String, Object> notAList = new HashMap<>();
        notAList.put("conditions", "也不是数组");
        assertTrue(StatisticConditionConverter.toCombineSearch(notAList).getConditions().isEmpty());
    }

    /**
     * 右侧字段还没选(操作符选「为空」时前端允许这样存)的「匹配字段」条件, 转出来仍是一个引用对象。
     *
     * <p>钉住的是消费端的判据: 认「匹配字段」只能看右值是不是对象, 不能看它里面的 {@code refFieldId}
     * 有没有值 —— 这个引用里的 {@code refFieldId} 就是 null, 按后者判断会把它当成普通字面量放到
     * 绑定参数里, 刷新时直接报错。</p>
     */
    @Test
    void keepsReferenceShapeEvenWhenRightFieldIsNotChosen() {
        Map<String, Object> raw = combine("AND",
                condition("amount", "NUMBER", "EMPTY", "MATCH_FIELD", null));

        FilterCondition condition = StatisticConditionConverter.toCombineSearch(raw).getConditions().getFirst();

        Map<?, ?> ref = assertInstanceOf(Map.class, condition.getValue(),
                "右侧字段没选也仍是引用结构, 不能退化成字面量");
        assertNull(ref.get("refFieldId"));
    }

    /**
     * 整体走过一遍 JSON: 配置是从请求体里反序列化出来的, 拿到的就是这种松散 Map,
     * 而不是这里手搓的类型化对象。顺带钉住整数、布尔之类非字符串右值的原样传递。
     */
    @Test
    void convertsDeserializedJsonStructure() {
        String json = """
                {"searchMode":"OR","conditions":[
                  {"leftFieldId":"createUser","leftFieldType":"USER","operator":"EQUALS",
                   "matchType":"MATCH_FIELD","rightFieldId":"owner","rightFieldType":"USER"},
                  {"leftFieldId":"amount","leftFieldType":"NUMBER","operator":"GREATER_THAN",
                   "matchType":"MATCH_VALUE","rightFieldCustomValue":100}
                ]}""";

        CombineSearch combineSearch = StatisticConditionConverter
                .toCombineSearch(JSON.parseToMap(json));

        assertEquals("OR", combineSearch.getSearchMode());
        assertEquals(2, combineSearch.getConditions().size());
        Map<?, ?> ref = assertInstanceOf(Map.class, combineSearch.getConditions().getFirst().getValue());
        assertEquals("owner", ref.get("refFieldId"));
        assertEquals(100, combineSearch.getConditions().get(1).getValue(), "数字右值不该被转成字符串");
    }

    /**
     * 造一个弹窗产出的条件项。
     *
     * @param matchType   {@code MATCH_FIELD} 时右值取 {@code rightFieldId}, 否则取字面量
     * @param rightFieldId 匹配字段时的右侧字段ID
     */
    private static Map<String, Object> condition(String leftFieldId, String leftFieldType, String operator,
                                                 String matchType, String rightFieldId) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("leftFieldId", leftFieldId);
        condition.put("leftFieldType", leftFieldType);
        condition.put("operator", operator);
        condition.put("matchType", matchType);
        condition.put("rightFieldId", rightFieldId);
        condition.put("rightFieldType", "USER");
        // 字面量固定成一个可辨认的值, MATCH_VALUE 的断言靠它区分「取到了字面量」与「取到了字段ID」
        condition.put("rightFieldCustomValue", "user-1");
        return condition;
    }

    @SafeVarargs
    private static Map<String, Object> combine(String searchMode, Map<String, Object>... conditions) {
        Map<String, Object> raw = new HashMap<>();
        raw.put("searchMode", searchMode);
        raw.put("conditions", List.of(conditions));
        return raw;
    }
}
