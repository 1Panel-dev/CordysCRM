package io.cordys.crm.search.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.crm.search.constants.SearchModuleEnum;
import io.cordys.crm.search.domain.UserSearchConfig;
import io.cordys.crm.search.mapper.ExtUserSearchConfigMapper;
import io.cordys.crm.search.request.UserSearchConfigAddRequest;
import io.cordys.crm.system.dto.field.DatasourceField;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserSearchConfigService {

    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private BaseMapper<UserSearchConfig> userSearchConfigMapper;
    @Resource
    private ExtUserSearchConfigMapper extUserSearchConfigMapper;


    public void add(UserSearchConfigAddRequest request, String userId, String orgId) {

        request.getSearchFields().forEach((key, value) -> {
            switch (key) {
                case SearchModuleEnum.SEARCH_ADVANCED_CLUE:
                    saveSearchFields(value, userId, orgId, FormKey.CLUE.name(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER:
                    saveSearchFields(value, userId, orgId, FormKey.CUSTOMER.name(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CONTACT:
                    saveSearchFields(value, userId, orgId, FormKey.CONTACT.name(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_PUBLIC:
                    saveSearchFields(value, userId, orgId, FormKey.CUSTOMER.name(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL:
                    saveSearchFields(value, userId, orgId, FormKey.CLUE.name(), key, request);
                    break;
                case SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY:
                    saveSearchFields(value, userId, orgId, FormKey.OPPORTUNITY.name(), key, request);
                    break;
                default:
                    break;
            }
        });
    }


    private void saveSearchFields(List<String> fieldIds, String userId, String orgId, String formKey, String moduleType, UserSearchConfigAddRequest request) {
        if (CollectionUtils.isNotEmpty(fieldIds)) {
            ModuleFormConfigDTO businessFormConfig = moduleFormCacheService.getBusinessFormConfig(formKey, orgId);
            List<BaseField> fields = businessFormConfig.getFields();
            List<UserSearchConfig> searchConfigs = new ArrayList<>();
            fieldIds.forEach(fieldId -> {
                BaseField baseField = fields.stream().filter(field -> Strings.CI.equals(field.getId(), fieldId)).findFirst().get();

                UserSearchConfig userSearchConfig = new UserSearchConfig();
                userSearchConfig.setId(IDGenerator.nextStr());
                userSearchConfig.setFieldId(baseField.getId());
                userSearchConfig.setType(baseField.getType());
                userSearchConfig.setBusinessKey(baseField.getBusinessKey());
                if (baseField.getType().contains("DATA_SOURCE")) {
                    userSearchConfig.setDataSourceType(((DatasourceField) baseField).getDataSourceType());
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
            });

            userSearchConfigMapper.batchInsert(searchConfigs);
        }
    }
}
