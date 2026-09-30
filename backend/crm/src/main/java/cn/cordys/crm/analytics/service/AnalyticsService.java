package cn.cordys.crm.analytics.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.Granularity;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.View;
import cn.cordys.crm.analytics.dto.response.AnalyticsQueryResponse;
import cn.cordys.crm.analytics.dto.response.AnalyticsQueryResponse.Row;
import cn.cordys.crm.analytics.dto.response.AnalyticsSchemaResponse;
import cn.cordys.crm.analytics.mapper.AnalyticsQueryMapper;
import cn.cordys.crm.system.service.ModuleFormService;
import jakarta.annotation.Resource;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.util.*;

import static cn.cordys.crm.analytics.service.AnalyticsAccessService.invalid;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    @Resource private AnalyticsAccessService accessService;
    @Resource private AnalyticsSchemaService schemaService;
    @Resource private AnalyticsQueryMapper queryMapper;
    @Resource private ModuleFormService moduleFormService;
    @Resource private Validator validator;

    public AnalyticsSchemaResponse schema(String formKey, View view, String poolId, String subTableId,
                                          boolean includeRelated, String userId, String orgId) {
        var access = accessService.resolve(formKey, view, poolId, userId, orgId);
        var root = schemaService.load(formKey, access.source(), orgId);
        var response = root.dataset(subTableId).response();
        List<AnalyticsSchemaResponse.Dataset> related = new ArrayList<>();
        if (includeRelated) {
            // 仅发现已授权固定模块的真实关联；自定义表单可按已知 formKey 直接发现。
            for (String source : new TreeSet<>(AnalyticsAccessService.SOURCES.keySet())) {
                AnalyticsSchemaService.Catalog catalog;
                try {
                    var sourceAccess = accessService.resolve(source, View.ALL, null, userId, orgId);
                    catalog = source.equals(formKey) ? root : schemaService.load(source, sourceAccess.source(), orgId);
                } catch (GenericException e) {
                    if (e.getErrorCode() == CrmHttpResultCode.FORBIDDEN) continue;
                    throw e;
                }
                List<AnalyticsSchemaService.Catalog> datasets = new ArrayList<>(List.of(catalog));
                datasets.addAll(catalog.subtables().values());
                for (var dataset : datasets) {
                    dataset.fields().values().stream().filter(f -> formKey.equals(f.schema().relatedFormKey()))
                            .forEach(f -> related.add(new AnalyticsSchemaResponse.Dataset(source,
                                    dataset.response().subTableFieldId(), dataset.response().grain(), f.schema().name(),
                                    f.schema().key(), formKey)));
                }
            }
        }
        return new AnalyticsSchemaResponse(response.formKey(), response.subTableFieldId(), response.grain(), response.fields(),
                response.unsupportedFields(), response.limits(), response.notes(), root.response().datasets(), List.copyOf(related));
    }

    public AnalyticsQueryResponse query(AnalyticsQueryRequest request, String userId, String orgId) {
        StopWatch timing = new StopWatch();
        boolean success = false;
        timing.start("validation");
        try {
            if (request == null || !validator.validate(request).isEmpty()) throw invalid("INVALID_REQUEST");
            timing.stop();
            timing.start("scope");
            var access = accessService.resolve(request.getFormKey(), request.getView(), request.getPoolId(), userId, orgId);
            timing.stop();
            timing.start("schema");
            var catalog = schemaService.load(request.getFormKey(), access.source(), orgId).dataset(request.getSubTableFieldId());
            timing.stop();
            timing.start("compileAndRelatedScope");
            String relatedFormKey = request.getRelationField() == null ? null
                    : catalog.require(request.getRelationField()).schema().relatedFormKey();
            if (request.getRelationField() != null && relatedFormKey == null) throw invalid("INVALID_RELATION");
            var related = relatedFormKey == null ? null : accessService.resolve(relatedFormKey, View.ALL, null, userId, orgId);
            var plan = AnalyticsSqlBuilder.compile(access, catalog, request, related);
            timing.stop();
            timing.start("sql");
            List<Map<String, Object>> values;
            try {
                values = queryMapper.query(plan);
            } catch (DataAccessException e) {
                // 不记录 SQL、绑定值或底层异常文本，避免筛选值和客户数据进入日志。
                log.warn("统计查询执行失败, formKey={}, errorType={}", request.getFormKey(), e.getClass().getSimpleName());
                throw new GenericException(CrmHttpResultCode.FAILED,
                        Map.of("reason", e instanceof QueryTimeoutException ? "QUERY_TIMEOUT" : "QUERY_EXECUTION_FAILED"));
            }
            timing.stop();
            timing.start("labelsAndResult");
            if (values == null || values.isEmpty()) throw invalid("MISSING_QUERY_RESULT");
            if (number(values.getFirst(), "_invalid_total") > 0) throw invalid("INVALID_PERSISTED_VALUE_OR_TIMEZONE");
            long groups = number(values.getFirst(), "_groups");
            long matched = number(values.getFirst(), "_matched");

            List<BaseModuleFieldValue> optionValues = new ArrayList<>();
            for (Map<String, Object> row : values) {
                if (row.get("_records") == null) continue;
                for (int i = 0; i < request.getDimensions().size(); i++) {
                    var dimension = request.getDimensions().get(i);
                    var field = catalog.require(dimension.getField());
                    Object value = row.get("d" + i);
                    if (field.base().hasOptions() && value != null && dimension.getGranularity() == Granularity.RAW) {
                        optionValues.add(new BaseModuleFieldValue(field.base().getId(), field.schema().multiple() ? List.of(value) : value));
                    }
                }
            }
            Map<String, List<OptionDTO>> options = optionValues.isEmpty() ? Map.of()
                    : moduleFormService.getOptionMap(catalog.config(), optionValues);
            List<Row> rows = new ArrayList<>();
            Set<String> warnings = new LinkedHashSet<>();
            for (Map<String, Object> row : values) {
                if (row.get("_records") == null) continue;
                Map<String, Object> keys = new LinkedHashMap<>();
                Map<String, Object> labels = new LinkedHashMap<>();
                for (int i = 0; i < request.getDimensions().size(); i++) {
                    var dimension = request.getDimensions().get(i);
                    var field = catalog.require(dimension.getField());
                    Object key = normalized(row.get("d" + i));
                    Object label = key;
                    if (key != null && field.base().hasOptions() && dimension.getGranularity() == Granularity.RAW) {
                        label = options.getOrDefault(field.base().getId(), List.of()).stream()
                                .filter(option -> Objects.equals(option.getIdAsString(), key.toString()))
                                .map(OptionDTO::getName).filter(Objects::nonNull).findFirst().orElse(null);
                        if (label == null) warnings.add("UNRESOLVED_DIMENSION_LABEL:" + dimension.getField());
                    }
                    keys.put(dimension.getField(), key);
                    labels.put(dimension.getField(), label);
                }
                Map<String, Object> metrics = new LinkedHashMap<>();
                for (int i = 0; i < request.getMetrics().size(); i++) {
                    metrics.put(request.getMetrics().get(i).getAlias(), normalized(row.get("m" + i)));
                }
                rows.add(new Row(keys, labels, metrics));
            }
            boolean top = request.getOrderBy() != null;
            boolean truncated = !top && groups > request.getLimit();
            var response = new AnalyticsQueryResponse(truncated ? "partial" : matched == 0 ? "no_data" : "ok",
                    !truncated, truncated, warnings.isEmpty(), top ? "TOP_N" : "ALL_GROUPS", groups, matched,
                    number(values.getFirst(), "_matched_rows"), System.currentTimeMillis(), request, access.scope(), rows, List.copyOf(warnings),
                    catalog.response().grain(), request.getDimensions().stream().anyMatch(d -> catalog.require(d.getField()).schema().multiple())
                    ? "EXPLODE_MULTI_VALUE" : "SINGLE_VALUE", relatedFormKey, related == null ? null : related.scope(),
                    request.getDimensions().stream().filter(d -> catalog.require(d.getField()).schema().numeric())
                            .map(AnalyticsQueryRequest.Dimension::getField).toList());
            success = true;
            return response;
        } finally {
            if (timing.isRunning()) timing.stop();
            // 仅记录阶段与耗时，失败路径也保留；不记录条件、结果或 SQL。
            log.info("统计查询耗时, success={}, totalMs={}, stages={}", success, timing.getTotalTimeMillis(),
                    Arrays.stream(timing.getTaskInfo()).map(task -> task.getTaskName() + "=" + task.getTimeMillis() + "ms").toList());
        }
    }

    private static long number(Map<String, Object> row, String name) {
        Object value = row.get(name);
        if (!(value instanceof Number)) throw invalid("INVALID_QUERY_RESULT");
        return new BigDecimal(value.toString()).longValueExact();
    }

    private static Object normalized(Object value) {
        // 十进制结果作为字符串输出，避免浏览器或 MCP 的 JSON number 解析再次丢失金额精度。
        return value instanceof BigDecimal decimal ? decimal.stripTrailingZeros().toPlainString() : value;
    }
}
