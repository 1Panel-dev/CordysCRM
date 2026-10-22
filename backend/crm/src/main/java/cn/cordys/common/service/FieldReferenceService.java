package cn.cordys.common.service;

import cn.cordys.common.context.SourceDetailResolveContext;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.form.service.CustomFormDataFieldService;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 查询数据源详情并回填引用字段，复用当前解析上下文避免循环引用。
 */
@Service
@Slf4j
public class FieldReferenceService {

    public static final String DETAIL_FIELD_PARAM_NAME = "moduleFields";
    public static final String REF_UNDERLINE = "_ref_";
    private static final String SOURCE_DETAIL_ID = "id";
    private static final String ROW_BIZ_ID = "id";

    @Lazy
    @Resource
    private FieldSourceServiceProvider fieldSourceServiceProvider;

    /**
     * 数据源业务字段引用值回显
     *
     * @param details          详情集合
     * @param fields           表单字段集合
     * @param resourceFieldMap 资源已有字段值
     * @return 字段值集合
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<BaseModuleFieldValue>> setBusinessRefFieldValue(
            List<?> details, List<BaseField> fields, Map<String, List<BaseModuleFieldValue>> resourceFieldMap) {
        List<BaseField> sourceBusinessFields = fields.stream()
                .filter(field -> field instanceof DatasourceField sourceField &&
                        CollectionUtils.isNotEmpty(sourceField.getShowFields()) &&
                        StringUtils.isNotEmpty(sourceField.getBusinessKey()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(sourceBusinessFields)) {
            return resourceFieldMap;
        }

        Map<String, BaseField> fieldConfigMap = fields.stream()
                .collect(Collectors.toMap(BaseField::getId, f -> f, (prev, next) -> next));

        // 按照数据源类型来分组
        Map<String, List<String>> idsToFetch = new HashMap<>(FieldSourceType.values().length);
        for (Object detail : details) {
            Map<String, Object> detailMap = JSON.MAPPER.convertValue(detail, Map.class);
            for (BaseField bsf : sourceBusinessFields) {
                DatasourceField sourceField = (DatasourceField) bsf;
                Object val = detailMap.get(sourceField.getBusinessKey());
                if (val == null || StringUtils.isEmpty(val.toString()) ||
                        SourceDetailResolveContext.getSourceMap().containsKey(val.toString())) {
                    continue;
                }
                String dataSourceType = sourceField.getDataSourceType();
                idsToFetch.computeIfAbsent(dataSourceType, k -> new ArrayList<>()).add(val.toString());
            }
        }

        SourceDetailResolveContext.start();
        try {
            idsToFetch.forEach((dataSourceType, ids) -> {
                List<String> uniqueIds = ids.stream().distinct().collect(Collectors.toList());
                List<Object> sourceObjList;
                FieldSourceType sourceType = FieldSourceType.safeValueOf(dataSourceType);
                if (sourceType == FieldSourceType.CUSTOM_FORM) {
                    sourceObjList = CustomFormDataFieldService.withFormKey(dataSourceType,
                            () -> fieldSourceServiceProvider.batchGetSimpleByIds(sourceType, uniqueIds));
                } else {
                    sourceObjList = fieldSourceServiceProvider.batchGetSimpleByIds(sourceType, uniqueIds);
                }
                if (CollectionUtils.isNotEmpty(sourceObjList)) {
                    sourceObjList.stream()
                            .filter(Objects::nonNull)
                            .map(sourceObj -> JSON.MAPPER.convertValue(sourceObj, Map.class))
                            .filter(sourceMap -> sourceMap.containsKey("id"))
                            .forEach(sourceMap -> {
                                String sourceId = sourceMap.get("id").toString();
                                if (uniqueIds.contains(sourceId)) {
                                    SourceDetailResolveContext.put(sourceId, sourceMap);
                                }
                            });
                }
            });

            details.forEach(detail -> {
                Map<String, Object> detailMap = JSON.MAPPER.convertValue(detail, Map.class);
                sourceBusinessFields.forEach(bsf -> {
                    DatasourceField sourceField = (DatasourceField) bsf;
                    Object val = detailMap.get(sourceField.getBusinessKey());
                    if (val == null) {
                        return;
                    }
                    Map<String, Object> sourceDetail = SourceDetailResolveContext.getSourceMap().get(val.toString());
                    if (MapUtils.isEmpty(sourceDetail) || !detailMap.containsKey(SOURCE_DETAIL_ID)) {
                        return;
                    }

                    sourceField.getShowFields().forEach(id -> {
                        BaseField showFieldConfig = fieldConfigMap.get(sourceField.getId() + REF_UNDERLINE + id);
                        if (showFieldConfig != null) {
                            String resourceId = detailMap.get(SOURCE_DETAIL_ID).toString();
                            resourceFieldMap.putIfAbsent(resourceId, new ArrayList<>());
                            Object showFieldValue = getFieldValueOfDetailMap(showFieldConfig, sourceDetail, sourceField);
                            if (showFieldValue != null) {
                                resourceFieldMap.get(resourceId)
                                        .add(new BaseModuleFieldValue(showFieldConfig.getId(), showFieldValue));
                            }
                        }
                    });
                });
            });
            return resourceFieldMap;
        } finally {
            SourceDetailResolveContext.end();
        }
    }

    /**
     * 获取详情中的字段值
     *
     * @param field           字段信息
     * @param sourceDetailMap 来源详情
     * @param refSourceField  被引用的数据源字段信息
     * @return 字段值
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object getFieldValueOfDetailMap(BaseField field, Map<String, Object> sourceDetailMap,
            DatasourceField refSourceField) {
        if (MapUtils.isEmpty(sourceDetailMap)) {
            return null;
        }
        if (Strings.CI.equals(refSourceField.getDataSourceType(), FieldSourceType.BUSINESS_TITLE.name())) {
            // 工商抬头数据源特殊处理
            String fieldKey = field.getId().replace((field.getResourceFieldId() + REF_UNDERLINE), StringUtils.EMPTY)
                    .replace("business_title_", StringUtils.EMPTY);
            return sourceDetailMap.get(fieldKey);
        }
        if (StringUtils.isNotEmpty(field.getBusinessKey())) {
            return sourceDetailMap.get(field.getBusinessKey());
        }
        if (sourceDetailMap.containsKey(DETAIL_FIELD_PARAM_NAME)) {
            List<Map> fvs = (List<Map>) sourceDetailMap.get(DETAIL_FIELD_PARAM_NAME);
            if (CollectionUtils.isEmpty(fvs)) {
                return null;
            }
            for (Map fv : fvs) {
                String actualFieldId = field.getId().replace((field.getResourceFieldId() + REF_UNDERLINE),
                        StringUtils.EMPTY);
                if (Strings.CI.equals(actualFieldId, fv.get("fieldId").toString())) {
                    return fv.get("fieldValue");
                }
            }
        }
        if (field.getResourceFieldId() != null) {
            String fieldKey = field.getId().replace((field.getResourceFieldId() + REF_UNDERLINE), StringUtils.EMPTY);
            if (sourceDetailMap.containsKey(fieldKey)) {
                return sourceDetailMap.get(fieldKey);
            }
        }
        return null;
    }

    /**
     * 子字段值回显
     *
     * @param targetId     显示的子字段
     * @param sourceDetail 来源详情
     * @param subKey       子表格业务Key
     * @param rowKey       行Key
     * @return 匹配值
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object matchSubFieldValueOfDetailMap(String targetId, Map<String, Object> sourceDetail, String subKey,
            String rowKey) {
        if (MapUtils.isEmpty(sourceDetail) || !sourceDetail.containsKey(subKey)) {
            return null;
        }
        // 子表格数据集合
        List<Map<String, Object>> rows = (List) sourceDetail.get(subKey);
        for (Map<String, Object> row : rows) {
            // 匹配行数据
            if (row.containsKey(ROW_BIZ_ID) && Strings.CS.equals(rowKey, row.get(ROW_BIZ_ID).toString())) {
                if (targetId.contains(REF_UNDERLINE)) {
                    return row.get(targetId.split(REF_UNDERLINE)[1]);
                } else {
                    return row.get(targetId);
                }
            }
        }
        return null;
    }

    /**
     * 获取数据源详情上下文
     *
     * @param flattenFields  平铺字段集合
     * @param resourceFields 资源字段值集合
     */
    @SuppressWarnings("unchecked")
    public void loadSourceDetails(List<BaseField> flattenFields,
            List<? extends BaseResourceField> resourceFields) {
        // 深度 > 1, 跳过, 不处理嵌套引用 [A -> B -> C]
        if (SourceDetailResolveContext.getDepth() > 1) {
            return;
        }

        // 收集所有数据源字段的类型和ID映射关系
        var sourceIdType = flattenFields.stream()
                .filter(f -> f instanceof DatasourceField ds && CollectionUtils.isNotEmpty(ds.getShowFields()))
                .collect(Collectors.toMap(
                        BaseField::idOrBusinessKey,
                        f -> ((DatasourceField) f).getDataSourceType(),
                        (prev, next) -> next));

        // 按照数据源类型分组
        Map<String, List<String>> groupedIds = new HashMap<>(FieldSourceType.values().length);
        resourceFields.stream()
                .filter(rf -> sourceIdType.containsKey(rf.getFieldId()) && rf.getFieldValue() != null)
                .forEach(rf -> {
                    String value = rf.getFieldValue().toString();
                    if (SourceDetailResolveContext.contains(value)) {
                        return;
                    }
                    groupedIds.computeIfAbsent(sourceIdType.get(rf.getFieldId()), k -> new ArrayList<>()).add(value);
                });

        // 批量查询每个类型的数据源详情
        groupedIds.forEach((dataSourceType, ids) -> {
            if (CollectionUtils.isEmpty(ids)) {
                return;
            }

            // 去重 & 提前数据源详情上下文占位 (防止相互引用时无限递归 [A -> B -> A])
            List<String> distinctIds = ids.stream().distinct().toList();
            distinctIds.forEach(SourceDetailResolveContext::putPlaceholder);

            // 批量查询
            FieldSourceType sourceType = FieldSourceType.safeValueOf(dataSourceType);
            List<Object> details;
            if (sourceType == FieldSourceType.CUSTOM_FORM) {
                details = CustomFormDataFieldService.withFormKey(dataSourceType,
                        () -> fieldSourceServiceProvider.batchGetSimpleByIds(sourceType, distinctIds));
            } else {
                details = fieldSourceServiceProvider.batchGetSimpleByIds(sourceType, distinctIds);
            }
            if (CollectionUtils.isEmpty(details)) {
                return;
            }

            // 缓存上下文
            for (Object detail : details) {
                if (detail == null) {
                    continue;
                }
                try {
                    // 通过反射获取ID字段
                    Map<String, Object> detailMap = JSON.MAPPER.convertValue(detail, Map.class);
                    Object idValue = detailMap.get("id");
                    if (idValue != null) {
                        SourceDetailResolveContext.put(idValue.toString(), detailMap);
                    }
                } catch (Exception e) {
                    log.error("处理数据源详情异常: {}", e.getMessage());
                }
            }
        });
    }

}
