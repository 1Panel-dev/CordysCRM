package cn.cordys.crm.analytics.dto.response;

import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest;

import java.util.List;
import java.util.Map;

/** 完整性针对本次授权数据范围；Top N 选择与计算不完整分别表达。 */
public record AnalyticsQueryResponse(String status, boolean complete, boolean truncated, boolean displayResolutionComplete, String selection,
                                     long totalGroups, long matchedRecords, long matchedRows, long generatedAt,
                                     AnalyticsQueryRequest query, Scope scope, List<Row> rows,
                                     List<String> warnings, String grain, String groupingMode,
                                     String relatedFormKey, Scope relationScope, List<String> numericDimensions) {
    public record Scope(String view, String effectiveScope, String poolId, String description) { }

    public record Row(Map<String, Object> dimensionKeys, Map<String, Object> dimensions,
                      Map<String, Object> metrics) { }
}
