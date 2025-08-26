package io.cordys.crm.search.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.FormKey;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.Translator;
import io.cordys.crm.search.constants.SearchModuleEnum;
import io.cordys.crm.search.domain.SearchFieldMaskConfig;
import io.cordys.crm.search.request.FieldMaskConfigDTO;
import io.cordys.crm.system.dto.field.DatasourceField;
import io.cordys.crm.system.dto.field.DatasourceMultipleField;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class SearchFieldMaskConfigService {

    @Resource
    private BaseMapper<SearchFieldMaskConfig> searchFieldMaskConfigMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    /**
     * 保存脱敏字段配置
     *
     * @param request
     * @param userId
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void save(FieldMaskConfigDTO request, String userId, String orgId) {
        deleteFieldMaskConfig(orgId);
        request.getSearchFields().forEach((key, value) -> {
            switch (key) {
                case SearchModuleEnum.SEARCH_ADVANCED_CLUE:
                    saveMaskFields(value, userId, orgId, FormKey.CLUE.name(), key);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER:
                    saveMaskFields(value, userId, orgId, FormKey.CUSTOMER.name(), key);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CONTACT:
                    saveMaskFields(value, userId, orgId, FormKey.CONTACT.name(), key);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_PUBLIC:
                    saveMaskFields(value, userId, orgId, FormKey.CUSTOMER.name(), key);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL:
                    saveMaskFields(value, userId, orgId, FormKey.CLUE.name(), key);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY:
                    saveMaskFields(value, userId, orgId, FormKey.OPPORTUNITY.name(), key);
                    break;
                default:
                    break;
            }
        });
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(orgId)
                        .resourceName(Translator.get("module.desensitization_set"))
                        .originalValue(null)
                        .modifiedValue(null)
                        .build()
        );
    }


    /**
     * 保存脱敏字段
     *
     * @param fieldIds
     * @param userId
     * @param orgId
     * @param formKey
     * @param moduleType
     */
    private void saveMaskFields(List<String> fieldIds, String userId, String orgId, String formKey, String moduleType) {
        if (CollectionUtils.isNotEmpty(fieldIds)) {
            ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(formKey, orgId);
            List<BaseField> fields = businessFormConfig.getFields();
            List<SearchFieldMaskConfig> searchConfigs = new ArrayList<>();
            fieldIds.forEach(fieldId -> {
                BaseField baseField = fields.stream().filter(field -> Strings.CI.equals(field.getId(), fieldId)).findFirst().get();

                SearchFieldMaskConfig fieldMaskConfig = new SearchFieldMaskConfig();
                fieldMaskConfig.setId(IDGenerator.nextStr());
                fieldMaskConfig.setFieldId(baseField.getId());
                fieldMaskConfig.setType(baseField.getType());
                fieldMaskConfig.setBusinessKey(baseField.getBusinessKey());
                if (baseField.getType().equals("DATA_SOURCE")) {
                    fieldMaskConfig.setDataSourceType(((DatasourceField) baseField).getDataSourceType());
                }
                if (baseField.getType().equals("DATA_SOURCE_MULTIPLE")) {
                    fieldMaskConfig.setDataSourceType(((DatasourceMultipleField) baseField).getDataSourceType());
                }
                fieldMaskConfig.setModuleType(moduleType);
                fieldMaskConfig.setOrganizationId(orgId);
                fieldMaskConfig.setCreateUser(userId);
                fieldMaskConfig.setUpdateUser(userId);
                fieldMaskConfig.setCreateTime(System.currentTimeMillis());
                fieldMaskConfig.setUpdateTime(System.currentTimeMillis());
                searchConfigs.add(fieldMaskConfig);
            });

            searchFieldMaskConfigMapper.batchInsert(searchConfigs);
        }

    }


    private void deleteFieldMaskConfig(String orgId) {
        LambdaQueryWrapper<SearchFieldMaskConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchFieldMaskConfig::getOrganizationId, orgId);
        searchFieldMaskConfigMapper.deleteByLambda(queryWrapper);
    }


    /**
     * 获取脱敏字段配置
     *
     * @param orgId
     * @return
     */
    public FieldMaskConfigDTO get(String orgId) {
        LambdaQueryWrapper<SearchFieldMaskConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchFieldMaskConfig::getOrganizationId, orgId);
        List<SearchFieldMaskConfig> searchFieldMaskConfigs = searchFieldMaskConfigMapper.selectListByLambda(queryWrapper);

        FieldMaskConfigDTO response = new FieldMaskConfigDTO();
        Map<String, List<String>> searchFields = searchFieldMaskConfigs.stream()
                .collect(Collectors.groupingBy(
                        SearchFieldMaskConfig::getModuleType,
                        Collectors.mapping(SearchFieldMaskConfig::getFieldId,
                                Collectors.toList())
                ));
        response.setSearchFields(searchFields);
        return response;
    }
}
