package cn.cordys.common.statistic;

import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.util.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设计器筛选弹窗的条件结构 -> 列表页高级搜索结构({@link CombineSearch})。
 *
 * <p>统计字段的「统计范围」与「更新范围」存的都是设计器筛选弹窗({@code filterModal.vue})的产物,
 * 也就是数据源字段那套「字段对字段」结构:</p>
 *
 * <pre>
 * {searchMode, conditions: [{leftFieldId, leftFieldType, operator, matchType,
 *                            rightFieldId, rightFieldCustom, rightFieldCustomValue, rightFieldType}]}
 * </pre>
 *
 * <p>它与 {@link CombineSearch} 的 {@code conditions[{name, value, operator, type, multipleValue}]} 不是
 * 同一套键名。两边结构不同却共用 {@code Map} 存(与 {@code DatasourceField#combineSearch} 一致),
 * 所以必须在真正要拼 SQL 之前转一次 —— 不转的话 {@code leftFieldId} 在 {@code FilterCondition} 上
 * 无处安放, {@code name} 落成 null, {@code getConditions()} 按 {@code valid()} 一过滤整条条件被丢掉,
 * 「符合条件」静默退化成「不过滤」, 统计值偏大且从结果上看不出是配置问题。</p>
 *
 * <p>为什么是转换而不是靠 Jackson 直接绑定: 键名对不上是结构差异, 不是序列化配置问题。
 * 给 {@code FilterCondition} 加 {@code @JsonAlias} 只能解决 {@code leftFieldId} 一个键,
 * {@code MATCH_FIELD}(匹配字段)时右值要从「另一个字段的ID」变成「取值引用对象」, 那一步绑定做不了。</p>
 */
public final class StatisticConditionConverter {

    private static final String SEARCH_MODE = "searchMode";
    private static final String CONDITIONS = "conditions";
    private static final String NAME = "name";
    private static final String LEFT_FIELD_ID = "leftFieldId";
    private static final String LEFT_FIELD_TYPE = "leftFieldType";
    private static final String OPERATOR = "operator";
    private static final String MATCH_TYPE = "matchType";
    private static final String MATCH_FIELD = "MATCH_FIELD";
    private static final String RIGHT_FIELD_ID = "rightFieldId";
    private static final String RIGHT_FIELD_TYPE = "rightFieldType";
    private static final String RIGHT_FIELD_CUSTOM_VALUE = "rightFieldCustomValue";
    private static final String REF_FIELD_ID = "refFieldId";
    private static final String REF_FIELD_TYPE = "refFieldType";

    private StatisticConditionConverter() {
    }

    /**
     * 把弹窗结构转成高级搜索结构。
     *
     * <p>没有条件、结构不认识、单条条件本身是半成品, 都不抛异常: 配错了字段的配置在刷新时
     * 抛出比「静默不过滤」更难排查, 而且保存校验已经拦过一道。这里只做结构转换, 由调用方按
     * {@code getConditions()} 的结果判空。</p>
     *
     * @param raw 弹窗产出的原始结构, 可以为 null
     *
     * @return 转换后的条件, 不为 null; 没有可用的条件时 conditions 为空集合
     */
    public static CombineSearch toCombineSearch(Map<String, Object> raw) {
        // 1) 整体为空: 返回空条件而不是 null。条件片段里写着 `${conditions}.size() > 0`,
        //    传 null 会让 OGNL 直接抛, 而不是短路成「不过滤」。
        CombineSearch combineSearch = new CombineSearch();
        if (raw == null || raw.isEmpty()) {
            return combineSearch;
        }

        // 2) searchMode 两边同名, 直接带过去; 缺省时交给 CombineSearch 自己兜底成 AND
        Object searchMode = raw.get(SEARCH_MODE);
        if (searchMode != null) {
            combineSearch.setSearchMode(String.valueOf(searchMode));
        }

        // 3) 逐条转换。转不出来的(不是对象、左字段都没选)直接跳过:
        //    它们过不了 valid(), 留着也会被下游丢, 不如在这里就丢干净。
        List<FilterCondition> conditions = new ArrayList<>();
        for (Object item : asList(raw.get(CONDITIONS))) {
            if (item instanceof Map<?, ?> condition) {
                conditions.add(toCondition(condition));
            }
        }
        combineSearch.setConditions(conditions);
        return combineSearch;
    }

    /**
     * 转换单条条件。
     *
     * <p>已经是高级搜索结构的({@code name} 有值)原样绑定: 历史上按 {@link CombineSearch} 存过的配置、
     * 以及将来前端直接产出这种结构的情况都要能继续吃, 不能因为这次改造把旧配置读废。</p>
     */
    private static FilterCondition toCondition(Map<?, ?> raw) {
        if (raw.get(NAME) != null) {
            return JSON.parseObject(JSON.toJSONString(raw), FilterCondition.class);
        }

        // 键名的对应关系: 左边是「拿目标表单的哪个字段比」, 右边是「跟什么比」。
        // type 必须带上: 各表单的 condition 片段靠它区分 JSON 数组列(如线索的 products)与普通标量列。
        FilterCondition condition = new FilterCondition();
        condition.setName(toStringValue(raw.get(LEFT_FIELD_ID)));
        condition.setType(toStringValue(raw.get(LEFT_FIELD_TYPE)));
        condition.setOperator(toStringValue(raw.get(OPERATOR)));
        condition.setValue(resolveValue(raw));
        return condition;
    }

    /**
     * 解析条件右值。
     *
     * <p>{@code MATCH_FIELD}(设计器里的「匹配字段」)时右值不是字面量, 而是宿主表单上的另一个字段:
     * 保存配置的时候宿主记录还不存在, 拿不到值, 只能先存一个引用对象
     * (形如 {@code {"refFieldId": 宿主字段ID, "refFieldType": 宿主字段类型}}),
     * 等刷新时按每条数据现取 —— 消费端靠「值是 Map 且带 refFieldId」认出这一支。</p>
     *
     * <p>其余情况({@code MATCH_VALUE})右值就是用户在弹窗里填的那个字面量。</p>
     */
    private static Object resolveValue(Map<?, ?> raw) {
        if (!MATCH_FIELD.equals(toStringValue(raw.get(MATCH_TYPE)))) {
            return raw.get(RIGHT_FIELD_CUSTOM_VALUE);
        }
        // refFieldType 一并带上: 取值时要靠它判断宿主那侧是自定义字段还是业务字段
        Map<String, Object> ref = new HashMap<>(2);
        ref.put(REF_FIELD_ID, toStringValue(raw.get(RIGHT_FIELD_ID)));
        ref.put(REF_FIELD_TYPE, toStringValue(raw.get(RIGHT_FIELD_TYPE)));
        return ref;
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
