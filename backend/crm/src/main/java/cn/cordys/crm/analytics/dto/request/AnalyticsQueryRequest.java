package cn.cordys.crm.analytics.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 只描述统计意图；用户、组织、物理表名和权限范围不接受客户端赋值。 */
@Data
public class AnalyticsQueryRequest extends AnalyticsInput {
    @NotBlank
    @Size(max = 64)
    @Schema(description = "固定模块 formKey 或当前组织的自定义表单 ID")
    private String formKey;

    @Size(max = 64)
    @Schema(description = "Schema 返回的子表格 ID；省略时为记录粒度，指定时为已持久化明细行粒度")
    private String subTableFieldId;

    @Size(max = 128)
    @Schema(description = "Schema 返回的数据源字段 key；关联统计同时限定来源和关联对象的可见范围")
    private String relationField;

    @NotNull
    private View view = View.ALL;

    @Size(max = 64)
    @Schema(description = "仅线索池、公海统计使用；后端校验对应池成员权限")
    private String poolId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "默认固定东八区；命名时区需要数据库已加载对应时区数据")
    private String timezone = "+08:00";

    @Valid
    private TimeRange timeRange;

    @NotNull
    @Size(max = 20)
    @Valid
    @Schema(description = "条件之间为 AND；字段键和合法操作符来自统计 Schema")
    private List<Condition> conditions = new ArrayList<>();

    @NotNull
    @Size(max = 4)
    @Valid
    private List<Dimension> dimensions = new ArrayList<>();

    @NotEmpty
    @Size(max = 8)
    @Valid
    private List<Metric> metrics;

    @Valid
    private OrderBy orderBy;

    @Min(1)
    @Max(500)
    @Schema(description = "返回组数；有 orderBy 时表示完整聚合后的 Top N，否则超限明确标记截断")
    private int limit = 200;

    public enum View { ALL, SELF, DEPARTMENT, VISIBLE }
    public enum Aggregate { COUNT, COUNT_DISTINCT, SUM, AVG, MIN, MAX }
    public enum Granularity { RAW, DAY, WEEK, MONTH, QUARTER, YEAR }
    public enum Operator { EQUALS, NOT_EQUALS, IN, NOT_IN, GT, GE, LT, LE, BETWEEN, CONTAINS, NOT_CONTAINS, EMPTY, NOT_EMPTY }
    public enum Direction { ASC, DESC }

    @Data
    public static class TimeRange extends AnalyticsInput {
        @NotBlank @Size(max = 128)
        private String field;
        @NotBlank @Size(max = 64)
        @Schema(description = "包含起点；ISO 日期或时间，无偏移时使用 timezone")
        private String from;
        @NotBlank @Size(max = 64)
        @Schema(description = "不包含终点；ISO 日期或时间")
        private String toExclusive;
    }

    @Data
    public static class Condition extends AnalyticsInput {
        @NotBlank @Size(max = 128)
        private String field;
        @NotNull
        private Operator operator;
        @Schema(description = "标量或最多 100 项数组；EMPTY/NOT_EMPTY 不传值，日期支持毫秒时间戳或 ISO 时间")
        @JsonDeserialize(using = ValueDeserializer.class)
        private Object value;
    }

    /** 金额条件直接按十进制解析，不能先经 Double 丢失精度；仅影响新增统计接口。 */
    public static class ValueDeserializer extends JsonDeserializer<Object> {
        @Override
        public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.isExpectedStartArrayToken()) return scalar(parser, context);
            List<Object> values = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (values.size() == 100) return context.reportInputMismatch(Object.class, "At most 100 condition values");
                values.add(scalar(parser, context));
            }
            return values;
        }

        private Object scalar(JsonParser parser, DeserializationContext context) throws IOException {
            return switch (parser.currentToken()) {
                case VALUE_STRING -> parser.getText();
                case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> parser.getDecimalValue();
                case VALUE_NULL -> null;
                default -> context.reportInputMismatch(Object.class, "Condition values must be strings or numbers");
            };
        }
    }

    @Data
    public static class Dimension extends AnalyticsInput {
        @NotBlank @Size(max = 128)
        private String field;
        @NotNull
        private Granularity granularity = Granularity.RAW;
    }

    @Data
    public static class Metric extends AnalyticsInput {
        @NotNull
        private Aggregate op;
        @Size(max = 128)
        @Schema(description = "COUNT 不传字段时统计记录数；传字段时统计非空值数量")
        private String field;
        @NotBlank @Size(max = 64)
        private String alias;
    }

    @Data
    public static class OrderBy extends AnalyticsInput {
        @NotBlank @Size(max = 64)
        private String metric;
        @NotNull
        private Direction direction = Direction.DESC;
    }
}

/** 拼错 conditions 等参数必须报错，不能静默丢弃后扩大查询范围。 */
abstract class AnalyticsInput {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown analytics parameter: " + name);
    }
}
