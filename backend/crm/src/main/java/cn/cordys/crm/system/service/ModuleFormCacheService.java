package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.ChartAnalysisDbRequest;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.chart.ChartCategoryAxisDbParam;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.dto.chart.ChartValueAxisDbParam;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ModuleFormCacheService {

    @Resource
    private ModuleFormService moduleFormService;

    /**
     * 保存表单配置并更新缓存
     *
     * @param saveParam     保存参数
     * @param currentUserId 当前用户ID
     *
     * @return 表单配置
     */
    @CachePut(value = "form_cache", key = "#currentOrgId + ':' + #saveParam.formKey", unless = "#result == null")
    public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
        return moduleFormService.save(saveParam, currentUserId, currentOrgId);
    }

    /**
     * 获取表单配置(缓存)
     *
     * @param formKey      表单Key
     * @param currentOrgId 当前组织ID
     *
     * @return 表单配置
     */
    @Cacheable(value = "form_cache", key = "#currentOrgId + ':' + #formKey", unless = "#result == null")
    public ModuleFormConfigDTO getConfig(String formKey, String currentOrgId) {
        return moduleFormService.getConfig(formKey, currentOrgId);
    }

    /**
     * 获取业务表单配置
     *
     * @param formKey        表单Key
     * @param organizationId 组织ID
     *
     * @return 表单配置
     */
    public ModuleFormConfigDTO getBusinessFormConfig(String formKey, String organizationId) {
        ModuleFormConfigDTO config = CommonBeanFactory.getBean(this.getClass()).getConfig(formKey, organizationId);
        ModuleFormConfigDTO businessModuleFormConfig = new ModuleFormConfigDTO();
        businessModuleFormConfig.setFormProp(config.getFormProp());

        // 获取特殊的业务字段
        Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
                collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));

        businessModuleFormConfig.setFields(
                config.getFields()
                        .stream()
                        .peek(moduleFieldDTO -> {
                            BusinessModuleField businessModuleFieldEnum = businessModuleFieldMap.get(moduleFieldDTO.getInternalKey());
                            if (businessModuleFieldEnum != null) {
                                // 设置特殊的业务字段 key
                                moduleFieldDTO.setBusinessKey(businessModuleFieldEnum.getBusinessKey());
                                moduleFieldDTO.setDisabledProps(businessModuleFieldEnum.getDisabledProps());
                            }
                        })
                        .toList()
        );
        return businessModuleFormConfig;
    }

    public List<ChartResult> translateAxisName(ModuleFormConfigDTO formConfig, ChartAnalysisDbRequest chartAnalysisDbRequest, List<ChartResult> chartResults) {
        ChartCategoryAxisDbParam categoryAxisParam = chartAnalysisDbRequest.getCategoryAxisParam();
        ChartCategoryAxisDbParam subCategoryAxisParam = chartAnalysisDbRequest.getSubCategoryAxisParam();
        ChartValueAxisDbParam valueAxisParam = chartAnalysisDbRequest.getValueAxisParam();

        List<BaseModuleFieldValue> moduleFieldValues = new ArrayList<>();
        for (ChartResult chartResult : chartResults) {
            BaseModuleFieldValue categoryFieldValue = new BaseModuleFieldValue();
            categoryFieldValue.setFieldId(categoryAxisParam.getFieldId());
            categoryFieldValue.setFieldValue(chartResult.getCategoryAxis());
            moduleFieldValues.add(categoryFieldValue);

            BaseModuleFieldValue valueFieldValue = new BaseModuleFieldValue();
            valueFieldValue.setFieldId(valueAxisParam.getFieldId());
            valueFieldValue.setFieldValue(chartResult.getValueAxis());
            moduleFieldValues.add(valueFieldValue);

            if (subCategoryAxisParam != null) {
                BaseModuleFieldValue subCategoryValue = new BaseModuleFieldValue();
                subCategoryValue.setFieldId(subCategoryAxisParam.getFieldId());
                subCategoryValue.setFieldValue(chartResult.getSubCategoryAxis());
                moduleFieldValues.add(subCategoryValue);
            }
        }

        moduleFieldValues = moduleFieldValues.stream()
                .filter(BaseModuleFieldValue::valid)
                .distinct().toList();

        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFieldValues);
        Map<String, String> categoryOptionMap = Optional.ofNullable(optionMap.get(categoryAxisParam.getFieldId()))
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

        Map<String, String> subCategoryOptionMap = null;
        if (subCategoryAxisParam != null) {
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

            if (subCategoryAxisParam != null && subCategoryOptionMap.get(chartResult.getSubCategoryAxis()) != null) {
                chartResult.setSubCategoryAxisName(subCategoryOptionMap.get(chartResult.getSubCategoryAxis()));
            } else {
                chartResult.setSubCategoryAxisName(chartResult.getSubCategoryAxis());
            }
        }
        return chartResults;
    }
}
