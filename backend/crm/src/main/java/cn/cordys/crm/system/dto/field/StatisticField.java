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
     * 过滤的是「目标表单」中的关联数据, 条件结构为数据源字段的字段对字段比较结构
     * ({@code searchMode + conditions[{leftFieldId, leftFieldType, operator, matchType, rightFieldId, rightFieldCustomValue}]}),
     * 与 {@link #updateScopeCondition} 的高级搜索结构不同, 两者不可互换。
     */
    @Schema(description = "符合条件时的过滤条件, 同数据源字段 combineSearch")
    private Map<String, Object> combineSearch;

    @EnumValue(enumClass = StatisticUpdateScope.class)
    @Schema(description = "更新范围", allowableValues = {"NONE", "ALL", "CONDITION"})
    private String updateScope;

    /**
     * 过滤的是「当前表单」自身的存量数据, 条件结构为该表单高级搜索的
     * {@code CombineSearch} ({@code searchMode + conditions[{name, value, operator, type, multipleValue}]}),
     * 与 {@link #combineSearch} 的字段对字段结构不同, 两者不可互换。
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
