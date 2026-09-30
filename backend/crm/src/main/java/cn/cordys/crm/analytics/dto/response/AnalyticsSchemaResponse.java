package cn.cordys.crm.analytics.dto.response;

import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.Aggregate;
import cn.cordys.crm.analytics.dto.request.AnalyticsQueryRequest.Operator;
import cn.cordys.crm.system.dto.field.base.OptionProp;

import java.util.List;
import java.util.Map;

public record AnalyticsSchemaResponse(String formKey, String subTableFieldId, String grain, List<Field> fields,
                                      List<UnsupportedField> unsupportedFields, Map<String, Integer> limits,
                                      List<String> notes, List<Dataset> datasets, List<Dataset> relatedDatasets) {
    public record Field(String key, String fieldId, String name, String type, boolean numeric, boolean date,
                        boolean multiple, String relatedFormKey, String numberFormat, List<Aggregate> aggregates, List<Operator> operators,
                        List<OptionProp> options) { }

    public record Dataset(String formKey, String subTableFieldId, String grain, String name,
                          String relationField, String relatedFormKey) { }

    public record UnsupportedField(String key, String name, String reason) { }
}
