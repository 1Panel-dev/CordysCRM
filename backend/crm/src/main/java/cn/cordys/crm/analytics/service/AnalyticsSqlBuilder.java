package cn.cordys.crm.analytics.service;

import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.*;
import cn.cordys.crm.analytics.service.AnalyticsAccessService.Access;
import cn.cordys.crm.analytics.service.AnalyticsSchemaService.Catalog;
import cn.cordys.crm.analytics.service.AnalyticsSchemaService.Field;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.cordys.crm.analytics.service.AnalyticsAccessService.invalid;

/**
 * 将受限查询编译为参数化 SQL。所有标识符来自服务端元数据，用户 alias 不进入 SQL。
 * 以记录 ID 半连接列表可见范围，避免部门/关联表重复行放大金额；动态单值重复则由数据库拒绝。
 */
public final class AnalyticsSqlBuilder {
    private static final String NUMBER_PATTERN = "^[+-]?[0-9]{1,65}([.][0-9]{1,65})?([eE][+-]?[0-9]{1,3})?$";
    private static final String DATE_PATTERN = "^-?[0-9]{1,15}$";

    private AnalyticsSqlBuilder() { }

    public static Plan compile(Access access, Catalog catalog, AnalyticsQueryRequest request) {
        return compile(access, catalog, request, null);
    }

    public static Plan compile(Access access, Catalog catalog, AnalyticsQueryRequest request, Access related) {
        ZoneId zone;
        try {
            zone = ZoneId.of(request.getTimezone());
        } catch (DateTimeException | NullPointerException e) {
            throw invalid("INVALID_TIMEZONE");
        }
        Map<String, Field> used = new LinkedHashMap<>();
        Set<String> aliases = new HashSet<>();
        for (Metric metric : request.getMetrics()) {
            if (metric == null) throw invalid("INVALID_METRIC");
            if (!aliases.add(metric.getAlias())) throw invalid("DUPLICATE_METRIC_ALIAS");
            if (metric.getField() == null) {
                if (metric.getOp() != Aggregate.COUNT) throw invalid("METRIC_FIELD_REQUIRED");
            } else {
                Field field = use(catalog, used, metric.getField());
                if (!field.schema().aggregates().contains(metric.getOp())) throw invalid("UNSUPPORTED_AGGREGATE");
            }
        }
        Set<String> dimensions = new HashSet<>();
        for (Dimension dimension : request.getDimensions()) {
            if (dimension == null || !dimensions.add(dimension.getField())) throw invalid("INVALID_DIMENSION");
            Field field = use(catalog, used, dimension.getField());
            if (!field.schema().date() && dimension.getGranularity() != Granularity.RAW) throw invalid("DATE_DIMENSION_REQUIRED");
        }
        for (Condition condition : request.getConditions()) {
            if (condition == null) throw invalid("INVALID_CONDITION");
            Field field = use(catalog, used, condition.getField());
            if (!field.schema().operators().contains(condition.getOperator())) throw invalid("UNSUPPORTED_OPERATOR");
        }
        TimeRange timeRange = request.getTimeRange();
        if (timeRange != null && !use(catalog, used, timeRange.getField()).schema().date()) throw invalid("DATE_FIELD_REQUIRED");
        if (request.getOrderBy() != null && !aliases.contains(request.getOrderBy().getMetric())) throw invalid("UNKNOWN_ORDER_METRIC");

        if (request.getSubTableFieldId() != null && !access.source().hasSubtableFields()) throw invalid("UNSUPPORTED_SUBTABLE_STORAGE");
        if (request.getRelationField() != null) {
            Field relation = use(catalog, used, request.getRelationField());
            if (related == null || relation.schema().relatedFormKey() == null) throw invalid("INVALID_RELATION");
        }
        List<Object> parameters = new ArrayList<>();
        Map<String, String> names = new LinkedHashMap<>();
        List<String> rawColumns = new ArrayList<>(List.of("a.id"));
        int index = 0;
        for (var entry : used.entrySet()) {
            String name = "f" + index++;
            names.put(entry.getKey(), name);
            Field field = entry.getValue();
            String value = rawValue(access, field, request.getSubTableFieldId(), parameters);
            rawColumns.add("nullif(cast(" + value + " as char), '') as " + name);
        }
        String join = "";
        if (request.getSubTableFieldId() != null) {
            String table = access.source().table();
            // UNION 保留只有 blob/空行标记的明细，并按来源记录与行号消除字段级重复。
            join = " join (select resource_id, row_id from `" + table + "_field` where ref_sub_id = ?"
                    + " union select resource_id, row_id from `" + table + "_field_blob` where ref_sub_id = ?) r"
                    + " on r.resource_id = a.id and r.row_id is not null and r.row_id <> ''";
            parameters.add(request.getSubTableFieldId());
            parameters.add(request.getSubTableFieldId());
            rawColumns.add("r.row_id as _row_id");
        }
        String raw = "select " + String.join(", ", rawColumns) + " from `" + access.source().table()
                + "` a" + join + " where a.organization_id = ? and a.id in (select id from analytics_visible)"
                + (access.empty() ? " and 1 = 0" : "");
        parameters.add(access.orgId());

        List<String> typedColumns = new ArrayList<>(List.of("id"));
        if (request.getSubTableFieldId() != null) typedColumns.add("_row_id");
        List<String> invalidValues = new ArrayList<>();
        for (var entry : used.entrySet()) {
            String name = names.get(entry.getKey());
            Field field = entry.getValue();
            if (field.schema().multiple()) {
                String valid = "case when json_valid(" + name + ") then json_type(" + name + ") = 'ARRAY' else false end";
                typedColumns.add("case when " + valid + " then " + name + " else '[]' end as " + name);
                invalidValues.add("case when " + name + " is not null and not (" + valid + ") then 1 else 0 end");
            } else if (field.schema().numeric() || field.schema().date()) {
                String valid = field.schema().date() ? name + " regexp '" + DATE_PATTERN + "'" : numericValid(name);
                String type = field.schema().date() ? "decimal(20,0)" : "decimal(65,20)";
                typedColumns.add("case when " + valid + " then cast(" + name + " as " + type + ") end as " + name);
                invalidValues.add("case when " + name + " is not null and not (" + valid + ") then 1 else 0 end");
            } else {
                typedColumns.add(name);
            }
        }
        typedColumns.add((invalidValues.isEmpty() ? "0" : String.join(" + ", invalidValues)) + " as _invalid");

        List<String> buckets = new ArrayList<>(List.of("analytics_typed.*"));
        List<String> groupNames = new ArrayList<>();
        List<String> quality = new ArrayList<>(List.of("_invalid"));
        List<String> expansions = new ArrayList<>();
        List<String> expandedColumns = new ArrayList<>(List.of("analytics_filtered.*"));
        for (int i = 0; i < request.getDimensions().size(); i++) {
            Dimension dimension = request.getDimensions().get(i);
            String name = names.get(dimension.getField());
            String groupName = "d" + i;
            if (used.get(dimension.getField()).schema().multiple()) {
                expansions.add(" left join " + members(name, "mv" + i) + " on true");
                expandedColumns.add("mv" + i + ".value as " + groupName);
            } else {
                buckets.add(bucket(name, dimension.getGranularity(), zone, parameters) + " as " + groupName);
            }
            groupNames.add(groupName);
            if (dimension.getGranularity() != Granularity.RAW) {
                quality.add("case when " + name + " is not null and " + groupName + " is null then 1 else 0 end");
            }
        }

        List<String> predicates = new ArrayList<>();
        for (Condition condition : request.getConditions()) {
            predicates.add(condition(names.get(condition.getField()), used.get(condition.getField()), condition, zone, parameters,
                    related != null && condition.getField().equals(request.getRelationField())));
        }
        if (timeRange != null) {
            long from = instant(timeRange.getFrom(), zone);
            long to = instant(timeRange.getToExclusive(), zone);
            if (from >= to) throw invalid("INVALID_TIME_RANGE");
            String name = names.get(timeRange.getField());
            predicates.add(name + " >= ? and " + name + " < ?");
            parameters.add(from);
            parameters.add(to);
        }
        if (related != null) {
            String name = names.get(request.getRelationField());
            String visible = "select id from analytics_related";
            Field relation = used.get(request.getRelationField());
            predicates.add(related.empty() ? "1 = 0" : relation.schema().multiple()
                    ? "exists (select 1 from " + members(name, "rel") + " where rel.value in (" + visible + "))"
                    : name + " in (" + visible + ")");
        }
        List<String> aggregates = new ArrayList<>(groupNames);
        aggregates.add("count(*) as _records");
        for (int i = 0; i < request.getMetrics().size(); i++) {
            Metric metric = request.getMetrics().get(i);
            String name = metric.getField() == null ? "*" : names.get(metric.getField());
            if (metric.getField() != null && used.get(metric.getField()).schema().multiple()) {
                name = "case when json_length(" + name + ") > 0 then 1 end";
            }
            String expression = metric.getOp() == Aggregate.COUNT_DISTINCT ? "count(distinct " + name + ")"
                    : metric.getOp().name() + "(" + name + ")";
            aggregates.add(expression + " as m" + i);
        }
        List<String> ordering = new ArrayList<>();
        if (request.getOrderBy() != null) {
            for (int i = 0; i < request.getMetrics().size(); i++) {
                if (request.getMetrics().get(i).getAlias().equals(request.getOrderBy().getMetric())) {
                    ordering.add("g.m" + i + " " + request.getOrderBy().getDirection().name());
                }
            }
        }
        // 相同指标的组按原始维度键稳定排序，分页容量不影响 Top N 的正确性。
        groupNames.forEach(name -> ordering.add("g." + name + " asc"));

        // 关联多选维度只展示可见关联对象；不把同一集合里的无权限 ID 带进结果。
        String expandedFilter = "";
        if (related != null && used.get(request.getRelationField()).schema().multiple()) {
            for (int i = 0; i < request.getDimensions().size(); i++) {
                if (request.getRelationField().equals(request.getDimensions().get(i).getField())) {
                    expandedFilter = " where mv" + i + ".value in (select id from analytics_related)";
                }
            }
        }
        String sql = "with analytics_visible as (" + access.boundSql().getSql() + "), "
                + (related == null ? "" : "analytics_related as (" + related.boundSql().getSql() + "), ")
                + "analytics_raw as (" + raw + "), "
                + "analytics_typed as (select " + String.join(", ", typedColumns) + " from analytics_raw), "
                + "analytics_bucketed as (select " + String.join(", ", buckets) + " from analytics_typed), "
                + "analytics_quality as (select coalesce(sum(" + String.join(" + ", quality) + "), 0) as _invalid_total from analytics_bucketed), "
                + "analytics_filtered as (select * from analytics_bucketed"
                + (predicates.isEmpty() ? "" : " where " + predicates.stream().map(p -> "(" + p + ")").collect(Collectors.joining(" and "))) + "), "
                + "analytics_expanded as (select distinct " + String.join(", ", expandedColumns)
                + " from analytics_filtered" + String.join("", expansions) + expandedFilter + "), "
                + "analytics_groups as (select " + String.join(", ", aggregates) + " from analytics_expanded"
                + (groupNames.isEmpty() ? "" : " group by " + String.join(", ", groupNames)) + ") "
                + "select g.*, q._invalid_total, count(g._records) over() as _groups, "
                + "(select count(distinct id) from analytics_filtered) as _matched, "
                + "(select count(*) from analytics_filtered) as _matched_rows from analytics_quality q left join analytics_groups g on true"
                + (ordering.isEmpty() ? "" : " order by " + String.join(", ", ordering)) + " limit ?";
        parameters.add(request.getLimit());
        return new Plan(access, related, sql, List.copyOf(parameters));
    }

    private static String rawValue(Access access, Field field, String subTableId, List<Object> parameters) {
        String value;
        if (field.line() && "_rowId".equals(field.column())) return "concat(a.id, ':', r.row_id)";
        if (field.department()) {
            value = "(select department_id from sys_organization_user where user_id = a.owner and organization_id = ?)";
            parameters.add(access.orgId());
        } else if (field.column() != null) {
            value = "a." + field.column();
        } else {
            String suffix = field.base().isBlob() ? "_field_blob" : "_field";
            // 标量子查询不使用 LIMIT/MAX；重复持久化值必须报错。
            value = "(select field_value from `" + access.source().table() + suffix + "` where resource_id = a.id and field_id = ?";
            parameters.add(field.line() ? field.base().idOrBusinessKey() : field.base().getId());
            if (field.line()) {
                value += " and ref_sub_id = ? and row_id = r.row_id";
                parameters.add(subTableId);
            } else if (access.source().hasSubtableFields()) {
                value += " and (ref_sub_id is null or ref_sub_id = '') and (row_id is null or row_id = '')";
            }
            value += ")";
        }
        if (access.source().table().equals("customer") && !field.line()
                && (field.department() || "owner".equals(field.base().getBusinessKey()))) {
            value = "if(a.in_shared_pool, null, " + value + ")";
        }
        return value;
    }

    private static String members(String name, String alias) {
        return "json_table(" + name + ", '$[*]' columns (value varchar(1000) path '$' error on error)) " + alias;
    }

    private static String numericValid(String name) {
        // 原 NumberResolver 直接持久化 Number.toString()，其中可能含指数；全程使用十进制转换。
        String mantissa = "regexp_replace(substring_index(lower(" + name + "), 'e', 1), '^[+-]', '')";
        String digits = "replace(" + mantissa + ", '.', '')";
        String scale = "(case when locate('.', " + mantissa + ") > 0 then char_length(substring_index("
                + mantissa + ", '.', -1)) else 0 end)";
        String exponent = "(case when locate('e', lower(" + name + ")) > 0 then cast(substring_index(lower("
                + name + "), 'e', -1) as signed) else 0 end)";
        // 在 CAST 前检查有效整数/小数位，不能让越界数被数据库悄悄舍入成可用结果。
        return "case when " + name + " regexp '" + NUMBER_PATTERN + "' then (trim(leading '0' from " + digits + ") = '' or ("
                + "char_length(trim(leading '0' from " + digits + ")) - " + scale + " + " + exponent + " <= 45 and "
                + scale + " - char_length(" + digits + ") + char_length(trim(trailing '0' from " + digits + ")) - "
                + exponent + " <= 20)) else false end";
    }

    private static Field use(Catalog catalog, Map<String, Field> used, String key) {
        Field field = catalog.require(key);
        used.putIfAbsent(key, field);
        return field;
    }

    private static String bucket(String field, Granularity granularity, ZoneId zone, List<Object> parameters) {
        if (granularity == Granularity.RAW) return field;
        String timezone = zone.getRules().isFixedOffset() ? zone.getRules().getOffset(Instant.EPOCH).getId() : zone.getId();
        if (timezone.equals("Z")) timezone = "+00:00";
        // CONVERT_TZ 超出 MySQL 8.0.28+ (64 位) 的范围会原样返回，而非报错；必须先拦截，防止错分时间桶。
        String local = "case when " + field + " between 1000 and 32536771199999 then "
                + "convert_tz(timestampadd(microsecond, cast(" + field
                + " * 1000 as signed), cast('1970-01-01 00:00:00' as datetime)), '+00:00', ?) end";
        parameters.add(timezone);
        if (granularity == Granularity.QUARTER) {
            parameters.add(timezone);
            return "concat(date_format(" + local + ", '%Y'), '-Q', quarter(" + local + "))";
        }
        String format = switch (granularity) {
            case DAY -> "%Y-%m-%d";
            case WEEK -> "%x-W%v";
            case MONTH -> "%Y-%m";
            case YEAR -> "%Y";
            default -> throw invalid("INVALID_GRANULARITY");
        };
        return "date_format(" + local + ", '" + format + "')";
    }

    private static String condition(String name, Field field, Condition condition, ZoneId zone, List<Object> parameters,
                                    boolean scopedRelation) {
        Operator op = condition.getOperator();
        Object value = condition.getValue();
        if (field.schema().multiple()) {
            // 条件与关联权限必须命中同一成员，不能由不可见 B 满足条件、可见 A 满足权限。
            String visible = scopedRelation ? "cv.value in (select id from analytics_related)" : "true";
            if (op == Operator.EMPTY || op == Operator.NOT_EMPTY) {
                if (value != null) throw invalid("EMPTY_OPERATOR_HAS_VALUE");
                return (op == Operator.EMPTY ? "not " : "") + "exists (select 1 from " + members(name, "cv")
                        + " where " + visible + ")";
            }
            boolean negate = op == Operator.NOT_EQUALS || op == Operator.NOT_IN;
            List<?> values = op == Operator.IN || op == Operator.NOT_IN
                    ? value instanceof List<?> list ? list : List.of() : value == null ? List.of() : List.of(value);
            if (values.isEmpty() || values.size() > 100) throw invalid("INVALID_CONDITION_VALUES");
            values.forEach(v -> parameters.add(value(field, v, zone)));
            return (negate ? "not " : "") + "exists (select 1 from " + members(name, "cv")
                    + " where " + visible + " and cv.value in (" + String.join(",", Collections.nCopies(values.size(), "?")) + "))";
        }
        if (op == Operator.EMPTY || op == Operator.NOT_EMPTY) {
            if (value != null) throw invalid("EMPTY_OPERATOR_HAS_VALUE");
            return name + (op == Operator.EMPTY ? " is null" : " is not null");
        }
        if (op == Operator.IN || op == Operator.NOT_IN || op == Operator.BETWEEN) {
            if (!(value instanceof List<?> values) || values.isEmpty() || values.size() > 100
                    || op == Operator.BETWEEN && values.size() != 2) throw invalid("INVALID_CONDITION_VALUES");
            List<Object> converted = values.stream().map(v -> value(field, v, zone)).toList();
            if (op == Operator.BETWEEN && compare(converted.getFirst(), converted.getLast()) > 0) throw invalid("INVALID_BETWEEN_RANGE");
            parameters.addAll(converted);
            if (op == Operator.BETWEEN) return name + " between ? and ?";
            String in = name + (op == Operator.IN ? " in (" : " not in (") + String.join(",", Collections.nCopies(values.size(), "?")) + ")";
            return op == Operator.NOT_IN ? "(" + name + " is null or " + in + ")" : in;
        }
        Object converted = value(field, value, zone);
        if (op == Operator.CONTAINS || op == Operator.NOT_CONTAINS) {
            // LOCATE 按字面子串查询，百分号、下划线不被解释成通配符。
            parameters.add(converted);
            String contains = "locate(?, " + name + ")" + (op == Operator.CONTAINS ? " > 0" : " = 0");
            return op == Operator.NOT_CONTAINS ? "(" + name + " is null or " + contains + ")" : contains;
        }
        String operator = switch (op) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "<>";
            case GT -> ">";
            case GE -> ">=";
            case LT -> "<";
            case LE -> "<=";
            default -> throw invalid("UNSUPPORTED_OPERATOR");
        };
        parameters.add(converted);
        String comparison = name + " " + operator + " ?";
        return op == Operator.NOT_EQUALS ? "(" + name + " is null or " + comparison + ")" : comparison;
    }

    private static int compare(Object left, Object right) {
        return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString()));
    }

    private static Object value(Field field, Object value, ZoneId zone) {
        if (!(value instanceof String || value instanceof Number) || value.toString().isEmpty() || value.toString().length() > 1000) {
            throw invalid("INVALID_CONDITION_VALUE");
        }
        if (field.schema().date()) {
            if (value instanceof Number n) {
                try { return new BigDecimal(n.toString()).longValueExact(); }
                catch (ArithmeticException | NumberFormatException e) { throw invalid("INVALID_DATE_VALUE"); }
            }
            return instant(value.toString(), zone);
        }
        if (field.schema().numeric()) {
            try {
                BigDecimal number = new BigDecimal(value.toString()).stripTrailingZeros();
                if (number.scale() > 20 || number.precision() - number.scale() > 45) throw invalid("NUMBER_OUT_OF_RANGE");
                return number;
            } catch (NumberFormatException e) { throw invalid("INVALID_NUMBER_VALUE"); }
        }
        if (!(value instanceof String)) throw invalid("STRING_VALUE_REQUIRED");
        return value;
    }

    static long instant(String value, ZoneId zone) {
        try {
            try {
                return OffsetDateTime.parse(value).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored) {
                if (value.length() == 10) return LocalDate.parse(value).atStartOfDay(zone).toInstant().toEpochMilli();
                LocalDateTime local = LocalDateTime.parse(value);
                List<ZoneOffset> offsets = zone.getRules().getValidOffsets(local);
                if (offsets.size() != 1) throw invalid("AMBIGUOUS_LOCAL_TIME");
                return local.toInstant(offsets.getFirst()).toEpochMilli();
            }
        } catch (DateTimeException | ArithmeticException e) { throw invalid("INVALID_DATE_VALUE"); }
    }

    public record Plan(Access access, Access related, String sql, List<Object> parameters) { }
}
