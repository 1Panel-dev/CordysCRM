package cn.cordys.crm.system.dto.field;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.system.constants.StatisticDataScope;
import cn.cordys.crm.system.constants.StatisticEmptyResultMode;
import cn.cordys.crm.system.constants.StatisticEmptyValueMode;
import cn.cordys.crm.system.constants.StatisticType;
import cn.cordys.crm.system.constants.StatisticUpdateScope;
import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 统计字段
 *
 * <p>统计目标表单中通过数据源单选字段关联到当前记录的 N 条数据,
 * 聚合出总和/计数/平均值并持久化到当前记录, 只读不可直接编辑。</p>
 *
 * @author song-cc-rock
 */
@Data
@JsonTypeName(value = "STATISTIC")
@EqualsAndHashCode(callSuper = true)
public class StatisticField extends BaseField {

    @Schema(description = "目标统计表单Key, 取自 /module/form/related/{formKey} 返回的关联表单")
    private String targetFormId;

    @Schema(description = "目标表单中指向当前表单的数据源单选字段ID, 即关联关系字段")
    private String relatedFieldId;

    @EnumValue(enumClass = StatisticType.class)
    @Schema(description = "统计类型", allowableValues = {"SUM", "COUNT", "AVG"})
    private String statisticType;

    @Schema(description = "统计字段ID, 取自目标表单的数值/计算/统计字段; COUNT 时为空")
    private String statisticFieldId;

    @EnumValue(enumClass = StatisticEmptyValueMode.class)
    @Schema(description = "平均值统计时字段为空值的处理方式", allowableValues = {"DEFAULT_ZERO", "SKIP"})
    private String avgEmptyValueMode;

    @EnumValue(enumClass = StatisticEmptyResultMode.class)
    @Schema(description = "统计结果为空时的处理方式", allowableValues = {"EMPTY", "ZERO"})
    private String emptyResultMode;

    @EnumValue(enumClass = StatisticDataScope.class)
    @Schema(description = "统计数据范围", allowableValues = {"ALL", "CONDITION"})
    private String dataScope;

    /**
     * 过滤的是「目标表单」中的关联数据, 与 {@link #updateScopeCondition} 一样, 存的都是设计器筛选弹窗
     * ({@code filterModal.vue}) 产出的「字段对字段」结构:
     * {@code {searchMode, conditions[{leftFieldId, leftFieldType, operator, matchType, rightFieldId,
     * rightFieldCustom, rightFieldCustomValue, rightFieldType}]}}。
     *
     * <p><b>为什么这里不是 {@code CombineSearch}</b>: 两套结构的键名对不上, 而筛选弹窗的
     * {@code MATCH_FIELD}(匹配字段)需要把「右侧字段ID」翻成取值引用对象, 那不是绑定能做的事。
     * 若直接声明成 {@code CombineSearch}, Jackson 在绑定期就把 {@code leftFieldId} 丢掉,
     * 转换器再没有原料可读, 条件会整条消失。所以这里按原样收下, 由
     * {@code StatisticConditionConverter#toCombineSearch} 在拼 SQL 之前转一次。</p>
     *
     * <p><b>右值取自宿主记录时</b>(设计器里的「匹配字段」), 转换后的 {@code value} 里存的不是字面量而是一个
     * 引用对象 {@code {"refFieldId": 宿主字段ID, "refFieldType": 宿主字段类型}} ——
     * 保存配置时宿主记录还不存在, 拿不到值, 只能先记下「去哪个字段取」, 刷新时按每条数据现取。
     * 详见 {@code StatisticFieldService#scopeCondition}。</p>
     */
    @Schema(description = "符合条件时的过滤条件, 同数据源字段 combineSearch")
    private Map<String, Object> combineSearch;

    @EnumValue(enumClass = StatisticUpdateScope.class)
    @Schema(description = "更新范围", allowableValues = {"NONE", "ALL", "CONDITION"})
    private String updateScope;

    /**
     * 过滤的是「当前表单」自身的存量数据。
     *
     * <p>存储结构与 {@link #combineSearch} 相同(同一个筛选弹窗产出、同一个 {@code DataSourceFilterCombine}
     * 类型), 转换也是同一个 {@code StatisticConditionConverter}, 区别只在作用对象: 这个筛的是宿主表单
     * 自己有哪些数据要算, 那个筛的是每条宿主记录关联过来的哪些数据参与计算。</p>
     *
     * <p><b>但两者的能力并不对称</b>: 更新范围是在一条 SQL 里对整个宿主表判定的, 没有「逐条数据现取右值」
     * 这一步, 所以弹窗的「匹配字段」(右值取自本表单另一个字段)在这里用不了, 会在解析时被丢掉并记日志
     * (见 {@code StatisticFieldService#parseHostCondition})。</p>
     */
    @Schema(description = "符合数据范围计算时的过滤条件, 同该表单高级搜索筛选条件")
    private Map<String, Object> updateScopeCondition;

    @Schema(description = "统计字段格式(number/percent)")
    private String numberFormat;

    @Schema(description = "保留小数点位数")
    private Boolean decimalPlaces;

    @Schema(description = "位数")
    private int precision;

    @Schema(description = "显示千分位")
    private Boolean showThousandsSeparator;

    /**
     * 是否需要指定被统计字段: COUNT 只数条数, 其余聚合方式必须指定。
     */
    @JsonIgnore
    public boolean needStatisticField() {
        return !isCount();
    }

    @JsonIgnore
    public boolean isSum() {
        return StatisticType.SUM.name().equals(statisticType);
    }

    @JsonIgnore
    public boolean isCount() {
        return StatisticType.COUNT.name().equals(statisticType);
    }

    @JsonIgnore
    public boolean isAvg() {
        return StatisticType.AVG.name().equals(statisticType);
    }

    @JsonIgnore
    public boolean hasStatisticField() {
        return StringUtils.isNotBlank(statisticFieldId);
    }
}
