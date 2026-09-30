package cn.cordys.crm.analytics.service;

import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.Aggregate;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.Operator;
import cn.cordys.crm.analytics.dto.response.AnalyticsSchemaResponse;
import cn.cordys.crm.analytics.dto.response.AnalyticsSchemaResponse.UnsupportedField;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.system.dto.field.*;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.common.util.JSON;
import cn.cordys.mybatis.EntityTableMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Modifier;
import java.util.*;

import static cn.cordys.crm.analytics.service.AnalyticsAccessService.invalid;

/** 从当前表单发布可执行字段和数据粒度；引用展示字段不冒充持久化值。 */
@Service
public class AnalyticsSchemaService {
    @Resource private ModuleFormCacheService moduleFormCacheService;
    @Resource private ModuleFormService moduleFormService;
    @Resource private OpportunityService opportunityService;

    public Catalog load(String formKey, AnalyticsAccessService.Source source, String orgId) {
        ModuleFormConfigDTO config = moduleFormCacheService.getConfig(formKey, orgId);
        if (config == null || config.getFields() == null) throw invalid("SCHEMA_UNAVAILABLE");
        // 统计只读持久化字段；不加载详情页签、引用展示字段、默认候选和价格子表配置。
        // 复制后沿用原业务键与引用选项解析，不能污染组织级表单缓存。
        List<BaseField> fields = JSON.parseArray(JSON.toJSONString(config.getFields()), BaseField.class);
        fields.forEach(field -> {
            moduleFormService.setFieldBusinessParam(field);
            moduleFormService.setFieldRefOption(field);
        });
        if (formKey.equals("opportunity")) {
            for (BaseField field : opportunityService.getChartFields(orgId)) {
                if (!contains(fields, field.getBusinessKey())) {
                    field.setName("商机阶段");
                    field.setReadable(true);
                    fields.add(field);
                }
            }
        }
        Map<String, String> columns = new LinkedHashMap<>();
        for (var field : EntityTableMapper.filterFieldsWithoutNoColumnAnnotation(
                EntityTableMapper.getAllFields(source.entity(), f -> !Modifier.isStatic(f.getModifiers())))) {
            columns.put(field.getName(), EntityTableMapper.getColumnName(field));
        }
        addSystemField(fields, "id", "记录 ID", "INPUT");
        addSystemField(fields, "createTime", "创建时间", "DATE_TIME");
        addSystemField(fields, "updateTime", "更新时间", "DATE_TIME");
        addSystemField(fields, "createUser", "创建人", "MEMBER");
        addSystemField(fields, "updateUser", "更新人", "MEMBER");
        if (columns.containsKey("owner") && fields.stream().anyMatch(f -> "owner".equals(f.getBusinessKey())
                && Boolean.TRUE.equals(f.getReadable()))) {
            addSystemField(fields, "departmentId", "负责人部门", "DEPARTMENT");
        }
        return catalog(formKey, fields, columns);
    }

    static Catalog catalog(String formKey, List<BaseField> fields, Map<String, String> columns) {
        return catalog(formKey, fields, columns, null, List.of());
    }

    private static Catalog catalog(String formKey, List<BaseField> fields, Map<String, String> columns,
                                   String subTableId, List<BaseField> lineFields) {
        Map<String, Field> supported = new LinkedHashMap<>();
        List<UnsupportedField> unsupported = new ArrayList<>();
        List<BaseField> all = new ArrayList<>(fields);
        all.addAll(lineFields);
        for (BaseField field : all) {
            if (!Boolean.TRUE.equals(field.getReadable()) || field instanceof SubField) continue;
            if (StringUtils.isBlank(field.getId()) || StringUtils.isBlank(field.getType())) throw invalid("INCOMPLETE_SCHEMA");
            boolean line = lineFields.contains(field);
            String key = (line ? "line." : "") + field.idOrBusinessKey();
            String reason = unsupportedReason(field, columns, line);
            if (reason != null) {
                unsupported.add(new UnsupportedField(key, field.getName(), reason));
                continue;
            }
            boolean numeric = field instanceof InputNumberField || field instanceof StatisticField
                    || field instanceof FormulaField formula && "number".equals(formula.getFormulaResultFormat());
            boolean date = "DATE_TIME".equals(field.getType());
            List<Operator> operators = new ArrayList<>(List.of(Operator.EQUALS, Operator.NOT_EQUALS,
                    Operator.IN, Operator.NOT_IN, Operator.EMPTY, Operator.NOT_EMPTY));
            if (numeric || date) operators.addAll(List.of(Operator.GT, Operator.GE, Operator.LT, Operator.LE, Operator.BETWEEN));
            if (!field.multiple() && (List.of("INPUT", "PHONE", "SERIAL_NUMBER").contains(field.getType())
                    || field instanceof FormulaField && !numeric)) {
                operators.addAll(List.of(Operator.CONTAINS, Operator.NOT_CONTAINS));
            }
            List<Aggregate> aggregates = new ArrayList<>(List.of(Aggregate.COUNT));
            if (!field.multiple()) aggregates.add(Aggregate.COUNT_DISTINCT);
            // 主记录金额不能在明细行上重复累加；需要总金额时另查 RECORD 粒度。
            if (numeric && (subTableId == null || line)) aggregates.addAll(List.of(Aggregate.SUM, Aggregate.AVG));
            if (numeric || date) aggregates.addAll(List.of(Aggregate.MIN, Aggregate.MAX));
            String format = field instanceof InputNumberField n ? n.getNumberFormat()
                    : field instanceof StatisticField stat ? stat.getNumberFormat() : null;
            var descriptor = new AnalyticsSchemaResponse.Field(key, field.getId(), field.getName(), field.getType(),
                    numeric, date, field.multiple(), relatedFormKey(field), format,
                    List.copyOf(aggregates), List.copyOf(operators),
                    field instanceof HasOption o && o.getOptions() != null ? List.copyOf(o.getOptions()) : List.of());
            String column = line ? "_rowId".equals(field.getBusinessKey()) ? "_rowId" : null
                    : field.hasBusinessKey() ? columns.get(field.getBusinessKey()) : null;
            if (supported.putIfAbsent(key, new Field(field, descriptor, column,
                    !line && field.isSys() && "departmentId".equals(key), line)) != null) throw invalid("AMBIGUOUS_SCHEMA_FIELD");
        }
        Map<String, Catalog> subtables = new LinkedHashMap<>();
        List<AnalyticsSchemaResponse.Dataset> datasets = new ArrayList<>();
        if (subTableId == null) {
            datasets.add(new AnalyticsSchemaResponse.Dataset(formKey, null, "RECORD", "记录", null, null));
            for (BaseField field : fields) {
                if (!(field instanceof SubField sub) || !Boolean.TRUE.equals(sub.getReadable())) continue;
                if (StringUtils.isBlank(sub.getId()) || sub.getSubFields() == null) throw invalid("INCOMPLETE_SCHEMA");
                List<BaseField> lines = new ArrayList<>(sub.getSubFields());
                addSystemField(lines, "_rowId", "明细行 ID（含来源记录 ID）", "INPUT");
                if (subtables.putIfAbsent(sub.getId(), catalog(formKey, fields, columns, sub.getId(), lines)) != null) {
                    throw invalid("AMBIGUOUS_SCHEMA_FIELD");
                }
                datasets.add(new AnalyticsSchemaResponse.Dataset(formKey, sub.getId(), "SUBTABLE_ROW", sub.getName(), null, null));
            }
        }
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(new ArrayList<>(supported.values().stream().map(Field::base).toList()));
        var response = new AnalyticsSchemaResponse(formKey, subTableId, subTableId == null ? "RECORD" : "SUBTABLE_ROW",
                supported.values().stream().map(Field::schema).toList(), List.copyOf(unsupported),
                Map.of("metrics", 8, "dimensions", 4, "conditions", 20, "inValues", 100, "groups", 500, "timeoutSeconds", 10),
                List.of("COUNT 无字段为当前粒度行数，有字段为非空值数；AVG 跳过空值；SUM 全空返回 null。",
                        "子表使用 line. 字段键；主记录金额在明细粒度禁止 SUM/AVG，避免重复累加。",
                        "多值条件按成员匹配；多值维度去重后展开，各组可能重叠，不可相加为总体。多值集合不支持直接 COUNT_DISTINCT。",
                        "数值使用持久化原值，保留 numberFormat；不进行币种换算，不重算公式，不展开未持久化引用字段。",
                        "ALL 沿用模块列表的数据范围；客户/线索默认不含公海/线索池，池数据须显式提供 poolId。",
                        "relationField 按表单真实数据源关系统计，同时应用来源视图与关联对象 ALL 视图权限。"),
                List.copyOf(datasets), List.of());
        return new Catalog(config, Collections.unmodifiableMap(supported), response, Collections.unmodifiableMap(subtables));
    }

    private static String unsupportedReason(BaseField field, Map<String, String> columns, boolean line) {
        if (StringUtils.isNotBlank(field.getResourceFieldId())) return "REFERENCE_FIELD";
        if (!line && StringUtils.isNotBlank(field.getSubTableFieldId())) return "SUBTABLE_GRAIN";
        if (!Set.of("INPUT", "INPUT_NUMBER", "DATE_TIME", "RADIO", "SELECT", "MEMBER", "DEPARTMENT",
                "PHONE", "DATA_SOURCE", "SERIAL_NUMBER", "FORMULA", "STATISTIC", "CHECKBOX", "SELECT_MULTIPLE",
                "INPUT_MULTIPLE", "MEMBER_MULTIPLE", "DEPARTMENT_MULTIPLE", "DATA_SOURCE_MULTIPLE").contains(field.getType())) {
            return "UNSUPPORTED_FIELD_TYPE";
        }
        if (field instanceof FormulaField formula && !Set.of("number", "text").contains(
                Objects.toString(formula.getFormulaResultFormat(), ""))) return "UNKNOWN_FORMULA_RESULT_TYPE";
        if (!line && field.hasBusinessKey() && !columns.containsKey(field.getBusinessKey())
                && !(field.isSys() && "departmentId".equals(field.getBusinessKey()))) return "NON_PERSISTED_BUSINESS_FIELD";
        return null;
    }

    static String relatedFormKey(BaseField field) {
        if (!(field instanceof DatasourceField source) || StringUtils.isBlank(source.getDataSourceType())) return null;
        FieldSourceType type = FieldSourceType.safeValueOf(source.getDataSourceType());
        if (type == FieldSourceType.CUSTOM_FORM) {
            return "CUSTOM_FORM".equals(source.getDataSourceType()) ? null : source.getDataSourceType();
        }
        return AnalyticsAccessService.SOURCES.entrySet().stream()
                .filter(e -> e.getValue().table().equals(type.getTableName())).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private static boolean contains(List<BaseField> fields, String key) {
        // 已存在的不可读字段同样占位，不能通过补系统字段绕开其 readable 设置。
        return fields.stream().anyMatch(f -> key.equals(f.getBusinessKey()) || key.equals(f.getId()));
    }

    private static void addSystemField(List<BaseField> fields, String key, String name, String type) {
        if (contains(fields, key)) return;
        BaseField field = switch (type) {
            case "DATE_TIME" -> new DateTimeField();
            case "MEMBER" -> new MemberField();
            case "DEPARTMENT" -> new DepartmentField();
            default -> new InputField();
        };
        field.setId(key);
        field.setBusinessKey(key);
        field.setName(name);
        field.setType(type);
        field.setReadable(true);
        field.setSys(true);
        fields.add(field);
    }

    public record Field(BaseField base, AnalyticsSchemaResponse.Field schema, String column, boolean department, boolean line) { }
    public record Catalog(ModuleFormConfigDTO config, Map<String, Field> fields, AnalyticsSchemaResponse response, Map<String, Catalog> subtables) {
        public Catalog dataset(String id) {
            if (id == null) return this;
            Catalog result = subtables.get(id);
            if (result == null) throw invalid("UNKNOWN_OR_UNREADABLE_SUBTABLE");
            return result;
        }
        public Field require(String key) {
            Field field = fields.get(key);
            if (field == null) throw invalid("UNKNOWN_OR_UNREADABLE_FIELD");
            return field;
        }
    }
}
