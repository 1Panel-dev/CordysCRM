package cn.cordys.common.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.ChartAnalysisDbRequest;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.chart.ChartCategoryAxisDbParam;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.utils.IndustryUtils;
import cn.cordys.common.utils.RegionUtils;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.MemberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 图标分析通用类
 * @author song-cc-rock
 */
@Service
public class BaseChartService {

	@Resource
	private ModuleFormService moduleFormService;

	public List<ChartResult> translateAxisName(ModuleFormConfigDTO formConfig, ChartAnalysisDbRequest chartAnalysisDbRequest, List<ChartResult> chartResults) {
		ChartCategoryAxisDbParam categoryAxisParam = chartAnalysisDbRequest.getCategoryAxisParam();
		ChartCategoryAxisDbParam subCategoryAxisParam = chartAnalysisDbRequest.getSubCategoryAxisParam();
		chartResults = chartResults.stream()
				.filter(Objects::nonNull)
				.toList();

		BaseField categoryAxisField = getBaseField(formConfig.getFields(), categoryAxisParam.getFieldId());
		BaseField subCategoryAxisField = null;
		if (subCategoryAxisParam != null) {
			subCategoryAxisField = getBaseField(formConfig.getFields(), subCategoryAxisParam.getFieldId());
		}

		Map<String, List<OptionDTO>> optionMap = getChartOptionMap(formConfig, chartResults, categoryAxisParam,
				subCategoryAxisParam);

		Map<String, String> categoryOptionMap = Optional.ofNullable(optionMap.get(categoryAxisParam.getFieldId()))
				.orElse(List.of())
				.stream()
				.collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

		Map<String, String> subCategoryOptionMap = null;
		if (subCategoryAxisField != null) {
			subCategoryOptionMap = Optional.ofNullable(optionMap.get(subCategoryAxisParam.getFieldId()))
					.orElse(List.of())
					.stream()
					.collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
		}

		for (ChartResult chartResult : chartResults) {
			if (categoryOptionMap.get(chartResult.getCategoryAxis()) != null) {
				chartResult.setCategoryAxisName(categoryOptionMap.get(chartResult.getCategoryAxis()));
			} else {
				chartResult.setCategoryAxisName(chartResult.getCategoryAxis());
			}

			if (subCategoryOptionMap != null && subCategoryOptionMap.get(chartResult.getSubCategoryAxis()) != null) {
				chartResult.setSubCategoryAxisName(subCategoryOptionMap.get(chartResult.getSubCategoryAxis()));
			} else {
				chartResult.setSubCategoryAxisName(chartResult.getSubCategoryAxis());
			}

			if (chartResult.getValueAxis() == null) {
				chartResult.setValueAxis(0);
			}

			if (categoryAxisField.isLocation()) {
				chartResult.setCategoryAxisName(RegionUtils.codeToName(chartResult.getCategoryAxis()));
				chartResult.setCategoryAxis(RegionUtils.getCode(chartResult.getCategoryAxis()));
			}

			if (subCategoryAxisField != null && subCategoryAxisField.isLocation()) {
				chartResult.setSubCategoryAxisName(RegionUtils.codeToName(chartResult.getSubCategoryAxis()));
				chartResult.setSubCategoryAxis(RegionUtils.getCode(chartResult.getSubCategoryAxis()));
			}

			if (categoryAxisField.isIndustry()) {
				chartResult.setCategoryAxisName(IndustryUtils.mapping(chartResult.getCategoryAxis(), false));
			}

			if (subCategoryAxisField != null && subCategoryAxisField.isIndustry()) {
				chartResult.setSubCategoryAxisName(IndustryUtils.mapping(chartResult.getSubCategoryAxis(), false));
			}
		}

		if (categoryAxisField.isLocation()) {
			// 合并CategoryAxis相同的项
			chartResults = mergeResult(chartResults, ChartResult::getCategoryAxis);
		}

		if (subCategoryAxisField != null && subCategoryAxisField.isLocation()) {
			// 合并SubCategoryAxis相同的项
			chartResults = mergeResult(chartResults, ChartResult::getSubCategoryAxis);
		}

		return chartResults;
	}

	private static BaseField getBaseField(List<BaseField> fields, String fieldKey) {
		if (StringUtils.isBlank(fieldKey)) {
			return null;
		}
		return fields.stream()
				.filter(field -> Strings.CI.equals(field.getId(), fieldKey)
						|| Strings.CI.equals(field.getBusinessKey(), fieldKey))
				.findFirst().orElse(null);
	}

	private static List<ChartResult> mergeResult(List<ChartResult> chartResults, Function<ChartResult, String> getKeyFunc) {
		Map<String, ChartResult> mergedResults = new HashMap<>(chartResults.size());
		for (ChartResult chartResult : chartResults) {
			String categoryAxis = getKeyFunc.apply(chartResult);
			if (mergedResults.containsKey(categoryAxis)) {
				ChartResult existingResult = mergedResults.get(categoryAxis);
				if (existingResult.getValueAxis() instanceof Double && chartResult.getValueAxis() instanceof Double) {
					existingResult.setValueAxis((Double) existingResult.getValueAxis() + (Double) chartResult.getValueAxis());
				} else if (existingResult.getValueAxis() instanceof Long && chartResult.getValueAxis() instanceof Long) {
					existingResult.setValueAxis((Long) existingResult.getValueAxis() + (Long) chartResult.getValueAxis());
				} else if (existingResult.getValueAxis() instanceof Integer && chartResult.getValueAxis() instanceof Integer) {
					existingResult.setValueAxis((Integer) existingResult.getValueAxis() + (Integer) chartResult.getValueAxis());
				}
			} else {
				mergedResults.put(categoryAxis, chartResult);
			}
		}
		return new ArrayList<>(mergedResults.values());
	}

	private Map<String, List<OptionDTO>> getChartOptionMap(
			ModuleFormConfigDTO formConfig,
			List<ChartResult> chartResults,
			ChartCategoryAxisDbParam categoryAxisParam,
			ChartCategoryAxisDbParam subCategoryAxisParam) {

		var categoryAxisField = getBaseField(formConfig.getFields(), categoryAxisParam.getFieldId());
		var subCategoryAxisField = subCategoryAxisParam != null
				? getBaseField(formConfig.getFields(), subCategoryAxisParam.getFieldId())
				: null;

		// 收集所有需要转换为 Option 的字段值
		var moduleFieldValues = chartResults.stream()
				.flatMap(chartResult -> {
					List<BaseModuleFieldValue> values = new ArrayList<>(2);

					if (categoryAxisField != null && categoryAxisField.hasOptions()) {
						var categoryValue = getBaseModuleFieldValue(categoryAxisParam.getFieldId());
						categoryValue.setFieldValue(chartResult.getCategoryAxis());
						values.add(categoryValue);
					}

					if (subCategoryAxisField != null && subCategoryAxisField.hasOptions()) {
						var subValue = getBaseModuleFieldValue(subCategoryAxisParam.getFieldId());
						subValue.setFieldValue(chartResult.getSubCategoryAxis());
						values.add(subValue);
					}

					return values.stream();
				})
				.filter(BaseModuleFieldValue::valid)
				.distinct()
				.toList();

		if (moduleFieldValues.isEmpty()) {
			return Map.of();
		}

		return moduleFormService.getOptionMap(formConfig, moduleFieldValues);
	}

	private static BaseModuleFieldValue getBaseModuleFieldValue(String fieldKey) {
		BaseModuleFieldValue categoryFieldValue = new BaseModuleFieldValue();
		categoryFieldValue.setFieldId(fieldKey);
		return categoryFieldValue;
	}

	public static List<BaseField> getChartBaseFields() {
		BaseField createUserField = new MemberField();
		createUserField.setType(FieldType.MEMBER.name());
		createUserField.setId("createUser");
		createUserField.setBusinessKey("createUser");

		BaseField updateUserField = new MemberField();
		updateUserField.setType(FieldType.MEMBER.name());
		updateUserField.setId("updateUser");
		updateUserField.setBusinessKey("updateUser");

		return List.of(createUserField, updateUserField);
	}

}
