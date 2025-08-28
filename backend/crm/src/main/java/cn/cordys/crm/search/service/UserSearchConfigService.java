package cn.cordys.crm.search.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.search.constants.DefaultSearchFieldEnum;
import cn.cordys.crm.search.constants.SearchModuleEnum;
import cn.cordys.crm.search.domain.UserSearchConfig;
import cn.cordys.crm.search.mapper.ExtUserSearchConfigMapper;
import cn.cordys.crm.search.request.UserSearchConfigAddRequest;
import cn.cordys.crm.search.response.SearchFieldResponse;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.DatasourceMultipleField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
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
public class UserSearchConfigService {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private BaseMapper<UserSearchConfig> userSearchConfigMapper;
    @Resource
    private ExtUserSearchConfigMapper extUserSearchConfigMapper;


    /**
     * 保存配置
     *
     * @param request
     * @param userId
     * @param orgId
     */
    public void save(UserSearchConfigAddRequest request, String userId, String orgId) {
        boolean allEmpty = request.getSearchFields().values().stream()
                .allMatch(list -> list == null || list.isEmpty());
        if (allEmpty) {
            throw new GenericException(Translator.get("search.config.empty"));
        }
        deleteUserSearchConfig(userId, orgId);
        request.getSearchFields().forEach((key, value) -> {
            if (value.size() > 5) {
                throw new GenericException(Translator.get("search.config.limit"));
            }
            switch (key) {
                case SearchModuleEnum.SEARCH_ADVANCED_CLUE:
                    saveSearchFields(value, userId, orgId, FormKey.CLUE.getKey(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER:
                    saveSearchFields(value, userId, orgId, FormKey.CUSTOMER.getKey(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CONTACT:
                    saveSearchFields(value, userId, orgId, FormKey.CONTACT.getKey(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_PUBLIC:
                    saveSearchFields(value, userId, orgId, FormKey.CUSTOMER.getKey(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL:
                    saveSearchFields(value, userId, orgId, FormKey.CLUE.getKey(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY:
                    saveSearchFields(value, userId, orgId, FormKey.OPPORTUNITY.getKey(), key, request);
                    break;
                default:
                    break;
            }
        });
    }


    /**
     * 清空用户配置
     *
     * @param userId
     * @param orgId
     */
    private void deleteUserSearchConfig(String userId, String orgId) {
        LambdaQueryWrapper<UserSearchConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSearchConfig::getUserId, userId).eq(UserSearchConfig::getOrganizationId, orgId);
        userSearchConfigMapper.deleteByLambda(queryWrapper);
    }


    private void saveSearchFields(List<String> fieldIds, String userId, String orgId, String formKey, String moduleType, UserSearchConfigAddRequest request) {
        if (CollectionUtils.isNotEmpty(fieldIds)) {
            ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(formKey, orgId);
            List<BaseField> fields = businessFormConfig.getFields();
            List<UserSearchConfig> searchConfigs = new ArrayList<>();
            for (String fieldId : fieldIds) {
                BaseField baseField = fields.stream().filter(field -> Strings.CI.equals(field.getId(), fieldId)).findFirst().orElse(null);
                if (baseField == null) {
                    continue;
                }
                UserSearchConfig userSearchConfig = new UserSearchConfig();
                userSearchConfig.setId(IDGenerator.nextStr());
                userSearchConfig.setFieldId(baseField.getId());
                userSearchConfig.setType(baseField.getType());
                userSearchConfig.setBusinessKey(baseField.getBusinessKey());
                if (baseField.getType().equals("DATA_SOURCE")) {
                    userSearchConfig.setDataSourceType(((DatasourceField) baseField).getDataSourceType());
                }
                if (baseField.getType().equals("DATA_SOURCE_MULTIPLE")) {
                    userSearchConfig.setDataSourceType(((DatasourceMultipleField) baseField).getDataSourceType());
                }
                userSearchConfig.setUserId(userId);
                userSearchConfig.setModuleType(moduleType);
                userSearchConfig.setSortSetting(JSON.toJSONString(request.getSortSetting()));
                userSearchConfig.setResultDisplay(request.getResultDisplay());
                userSearchConfig.setOrganizationId(orgId);
                userSearchConfig.setCreateUser(userId);
                userSearchConfig.setUpdateUser(userId);
                userSearchConfig.setCreateTime(System.currentTimeMillis());
                userSearchConfig.setUpdateTime(System.currentTimeMillis());
                searchConfigs.add(userSearchConfig);
            }

            userSearchConfigMapper.batchInsert(searchConfigs);
        }
    }


    /**
     * 重置配置
     *
     * @param userId
     * @param orgId
     */
    public void reset(String userId, String orgId) {
        deleteUserSearchConfig(userId, orgId);
        List<UserSearchConfig> defaultSearchConfigs = new ArrayList<>();
        //客户&公海
        buildCustomerFields(FormKey.CUSTOMER.name(), userId, orgId, defaultSearchConfigs);
        //线索&线索池
        buildClueFields(FormKey.CLUE.name(), userId, orgId, defaultSearchConfigs);
        //商机
        buildOpportunityFields(FormKey.OPPORTUNITY.name(), userId, orgId, defaultSearchConfigs);
        //联系人
        buildContactFields(FormKey.CONTACT.name(), userId, orgId, defaultSearchConfigs);

        userSearchConfigMapper.batchInsert(defaultSearchConfigs);

    }

    /**
     * 联系人默认字段
     *
     * @param formKey
     * @param userId
     * @param orgId
     * @param defaultSearchConfigs
     */
    private void buildContactFields(String formKey, String userId, String orgId, List<UserSearchConfig> defaultSearchConfigs) {
        ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(formKey, orgId);
        businessFormConfig.getFields().stream().forEach(field -> {
            if (Strings.CI.equals(field.getInternalKey(), DefaultSearchFieldEnum.CONTACT_CUSTOMER)) {
                buildDto(field, userId, orgId, SearchModuleEnum.SEARCH_ADVANCED_CONTACT, defaultSearchConfigs);
            }
            if (Strings.CI.equals(field.getInternalKey(), DefaultSearchFieldEnum.CONTACT_PHONE)) {
                buildDto(field, userId, orgId, SearchModuleEnum.SEARCH_ADVANCED_CONTACT, defaultSearchConfigs);
            }
        });
    }


    /**
     * 线索/线索池默认字段
     *
     * @param formKey
     * @param userId
     * @param orgId
     * @param defaultSearchConfigs
     */
    private void buildClueFields(String formKey, String userId, String orgId, List<UserSearchConfig> defaultSearchConfigs) {
        ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(formKey, orgId);
        businessFormConfig.getFields().stream().forEach(field -> {
            if (Strings.CI.equals(field.getInternalKey(), DefaultSearchFieldEnum.CLUE_NAME)) {
                buildDto(field, userId, orgId, SearchModuleEnum.SEARCH_ADVANCED_CLUE, defaultSearchConfigs);
                buildDto(field, userId, orgId, SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL, defaultSearchConfigs);

            }
        });
    }


    /**
     * 客户/公海默认字段
     *
     * @param formKey
     * @param userId
     * @param orgId
     * @param defaultSearchConfigs
     */
    private void buildCustomerFields(String formKey, String userId, String orgId, List<UserSearchConfig> defaultSearchConfigs) {
        ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(formKey, orgId);
        businessFormConfig.getFields().stream().forEach(field -> {
            if (Strings.CI.equals(field.getInternalKey(), DefaultSearchFieldEnum.CUSTOMER_NAME)) {
                buildDto(field, userId, orgId, SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER, defaultSearchConfigs);
                buildDto(field, userId, orgId, SearchModuleEnum.SEARCH_ADVANCED_PUBLIC, defaultSearchConfigs);

            }
        });
    }


    /**
     * 商机默认字段
     *
     * @param formKey
     * @param userId
     * @param orgId
     * @param defaultSearchConfigs
     */
    private void buildOpportunityFields(String formKey, String userId, String orgId, List<UserSearchConfig> defaultSearchConfigs) {
        ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(formKey, orgId);
        businessFormConfig.getFields().stream().forEach(field -> {
            if (Strings.CI.equals(field.getInternalKey(), DefaultSearchFieldEnum.OPPORTUNITY_NAME)) {
                buildDto(field, userId, orgId, SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY, defaultSearchConfigs);
            }
            if (Strings.CI.equals(field.getInternalKey(), DefaultSearchFieldEnum.OPPORTUNITY_CUSTOMER)) {
                buildDto(field, userId, orgId, SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY, defaultSearchConfigs);
            }
        });
    }


    private void buildDto(BaseField field, String userId, String orgId, String moduleType, List<UserSearchConfig> defaultSearchConfigs) {
        UserSearchConfig userSearchConfig = new UserSearchConfig();
        userSearchConfig.setId(IDGenerator.nextStr());
        userSearchConfig.setFieldId(field.getId());
        userSearchConfig.setType(field.getType());
        userSearchConfig.setBusinessKey(field.getBusinessKey());
        if (field.getType().equals("DATA_SOURCE")) {
            userSearchConfig.setDataSourceType(((DatasourceField) field).getDataSourceType());
        } else if (field.getType().equals("DATA_SOURCE_MULTIPLE")) {
            userSearchConfig.setDataSourceType(((DatasourceMultipleField) field).getDataSourceType());
        } else {
            userSearchConfig.setDataSourceType(null);
        }
        userSearchConfig.setUserId(userId);
        userSearchConfig.setModuleType(moduleType);
        userSearchConfig.setSortSetting(JSON.toJSONString(SearchModuleEnum.VALUES));
        userSearchConfig.setResultDisplay(false);
        userSearchConfig.setOrganizationId(orgId);
        userSearchConfig.setCreateUser(userId);
        userSearchConfig.setUpdateUser(userId);
        userSearchConfig.setCreateTime(System.currentTimeMillis());
        userSearchConfig.setUpdateTime(System.currentTimeMillis());
        defaultSearchConfigs.add(userSearchConfig);
    }


    /**
     * 获取搜索字段配置
     *
     * @param userId
     * @param orgId
     * @return
     */
    public SearchFieldResponse get(String userId, String orgId) {
        if (!configCount(userId, orgId)) {
            reset(userId, orgId);
        }
        List<UserSearchConfig> searchConfigs = getConfigList(userId, orgId);

        SearchFieldResponse response = new SearchFieldResponse();
        Map<String, List<String>> searchFields = searchConfigs.stream()
                .collect(Collectors.groupingBy(
                        UserSearchConfig::getModuleType,
                        Collectors.mapping(UserSearchConfig::getFieldId,
                                Collectors.toList())
                ));

        response.setSearchFields(searchFields);
        response.setResultDisplay(searchConfigs.getFirst().getResultDisplay());
        response.setSortSetting(JSON.parseArray(searchConfigs.getFirst().getSortSetting()));

        return response;

    }

    private boolean configCount(String userId, String orgId) {
        UserSearchConfig example = new UserSearchConfig();
        example.setUserId(userId);
        example.setOrganizationId(orgId);
        return userSearchConfigMapper.countByExample(example) > 0;
    }


    /**
     * 获取用户搜索配置字段数据
     *
     * @param userId
     * @param orgId
     * @return
     */
    private List<UserSearchConfig> getConfigList(String userId, String orgId) {
        LambdaQueryWrapper<UserSearchConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSearchConfig::getUserId, userId).eq(UserSearchConfig::getOrganizationId, orgId);
        List<UserSearchConfig> searchConfigs = userSearchConfigMapper.selectListByLambda(queryWrapper);
        return searchConfigs;
    }
}
