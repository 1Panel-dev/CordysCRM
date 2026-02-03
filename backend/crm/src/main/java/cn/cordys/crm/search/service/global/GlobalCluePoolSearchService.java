package cn.cordys.crm.search.service.global;

import cn.cordys.common.constants.*;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.service.ClueFieldService;
import cn.cordys.crm.search.constants.SearchModuleEnum;
import cn.cordys.crm.search.constants.SearchPhoneEnum;
import cn.cordys.crm.search.domain.SearchFieldMaskConfig;
import cn.cordys.crm.search.domain.UserSearchConfig;
import cn.cordys.crm.search.response.global.GlobalCluePoolResponse;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.SystemResultCode;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GlobalCluePoolSearchService extends BaseSearchService<BasePageRequest, GlobalCluePoolResponse> {

    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;


    @Override
    public Pager<List<GlobalCluePoolResponse>> startSearchNoOption(BasePageRequest request, String orgId, String userId) {
        //获取查询关键字
        String keyword = request.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
            return PageUtils.setPageInfo(page, List.of());
        }
        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果有商机读取权限但商机模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.CLUE.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }
        //查询当前用户搜索配置
        List<UserSearchConfig> userSearchConfigs = getUserSearchConfigs(userId, orgId);
        List<String> phoneTypeFieldIds = userSearchConfigs.stream().filter(config -> Strings.CI.equals(config.getType(), FieldType.PHONE.name())).map(UserSearchConfig::getFieldId).toList();
        //记住当前一共有多少字段，避免固定展示列与自由选择列字段重复
        Set<String> fieldIdSet = new HashSet<>();
        //记住选择的内置列,除自定义字段外，用户还会选择一些内置字段
        Map<String, String> internalKeyMap = new HashMap<>();
        List<FilterCondition> conditions = new ArrayList<>();
        //用户配置设置:
        // 1.用户没配置过，设置默认查询条件;
        // 2.用户有配置，使用用户配置的查询条件;
        // 3.用户当前模块没配置，直接返回;
        if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
            List<UserSearchConfig> cluePoolSearchConfigs = userSearchConfigs.stream().filter(t -> Strings.CI.equals(t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL)).toList();
            if (CollectionUtils.isEmpty(cluePoolSearchConfigs)) {
                Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
                return PageUtils.setPageInfo(page, List.of());
            }
            for (UserSearchConfig userSearchConfig : cluePoolSearchConfigs) {
                //如果和固定展示列名重复不加入fieldIdSet
                if (StringUtils.isBlank(userSearchConfig.getBusinessKey())) {
                    fieldIdSet.add(userSearchConfig.getFieldId());
                } else if (!Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_NAME.getBusinessKey()) &&
                        !Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_PRODUCTS.getBusinessKey()) &&
                        !Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_CONTACT_PHONE.getBusinessKey())) {
                    fieldIdSet.add(userSearchConfig.getFieldId());
                }
                if (StringUtils.isNotBlank(userSearchConfig.getBusinessKey()) && !Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_NAME.getBusinessKey()) &&
                        !Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_PRODUCTS.getBusinessKey()) &&
                        !Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_CONTACT_PHONE.getBusinessKey())) {
                    internalKeyMap.put(userSearchConfig.getFieldId(), userSearchConfig.getBusinessKey());
                }
                buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
            }
        } else {
            //设置默认查询属性
            FilterCondition nameCondition = getFilterCondition("name", keyword, FilterCondition.CombineConditionOperator.CONTAINS.toString(), FieldType.INPUT.toString());
            conditions.add(nameCondition);
            StringUtils.deleteWhitespace(keyword);
            List<String> phoneList = new ArrayList<>();
            phoneList.add(keyword);
            for (String value : SearchPhoneEnum.VALUES) {
                phoneList.add(value + keyword);
            }
            FilterCondition phoneCondition = getFilterCondition("phone", phoneList, FilterCondition.CombineConditionOperator.IN.toString(), FieldType.DATA_SOURCE.toString());
            conditions.add(phoneCondition);
        }
        if (CollectionUtils.isEmpty(conditions)) {
            Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
            return PageUtils.setPageInfo(page, List.of());
        }
        //构造查询参数
        buildCombineSearch(conditions, request);
        // 查询重复商机列表
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<GlobalCluePoolResponse> globalCluePoolResponses = extClueMapper.globalPoolSearchList(request, orgId);
        if (CollectionUtils.isEmpty(globalCluePoolResponses)) {
            return PageUtils.setPageInfo(page, List.of());
        }
        //获取系统设置的脱敏字段
        List<SearchFieldMaskConfig> searchFieldMaskConfigs = getSearchFieldMaskConfigs(orgId, SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL);
        List<GlobalCluePoolResponse> buildList = buildListData(globalCluePoolResponses, orgId, userId, searchFieldMaskConfigs, fieldIdSet, internalKeyMap, phoneTypeFieldIds);
        return PageUtils.setPageInfo(page, buildList);
    }


    public List<GlobalCluePoolResponse> buildListData(List<GlobalCluePoolResponse> list, String orgId, String userId, List<SearchFieldMaskConfig> searchFieldMaskConfigs, Set<String> fieldIdSet, Map<String, String> internalKeyMap, List<String> phoneTypeFieldIds) {
        List<String> clueIds = list.stream().map(GlobalCluePoolResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> clueFiledMap = clueFieldService.getResourceFieldMap(clueIds, true);

        Map<String, String> productNameMap = getProductNameMap(orgId);

        Map<String, String> userPoolMap = getUserCluePool(orgId, userId);
        //处理内置字段的选项
        List<Clue> clues = new ArrayList<>();
        if (MapUtils.isNotEmpty(internalKeyMap)) {
            List<String> columns = new ArrayList<>();
            for (String value : internalKeyMap.values()) {
                String result = value.replaceAll("([A-Z])", "_$1").toLowerCase();
                columns.add(result);
            }
            columns.add("id");
            clues = extClueMapper.searchColumnsByIds(columns, clueIds);
        }
        Map<String, Clue> internalKeyValueMap = clues.stream().collect(Collectors.toMap(Clue::getId, t -> t));
        // 处理自定义字段选项数据
        ModuleFormConfigDTO clueFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
        Map<String, SearchFieldMaskConfig> searchFieldMaskConfigMap = searchFieldMaskConfigs.stream().collect(Collectors.toMap(SearchFieldMaskConfig::getFieldId, t -> t));
        list.forEach(globalClueResponse -> {
            // 判断该数据是否有权限
            boolean hasPermission = getHasPermission(userId, orgId, globalClueResponse, userPoolMap);
            // 处理自定义字段数据
            if (CollectionUtils.isNotEmpty(fieldIdSet)) {
                List<BaseModuleFieldValue> returnCluePoolFields = getBaseModuleFieldValues(fieldIdSet, globalClueResponse.getId(), clueFiledMap, clueFormConfig, searchFieldMaskConfigMap, hasPermission);

                globalClueResponse.setModuleFields(returnCluePoolFields);
            }
            //处理内置字段
            Clue clue = internalKeyValueMap.get(globalClueResponse.getId());
            List<BaseModuleFieldValue> baseModuleFieldValues = buildInternalField(internalKeyMap, searchFieldMaskConfigMap, hasPermission, clue, Clue.class);
            List<BaseModuleFieldValue> moduleFields = globalClueResponse.getModuleFields();
            if (CollectionUtils.isEmpty(moduleFields)) {
                globalClueResponse.setModuleFields(baseModuleFieldValues);
            } else {
                moduleFields.addAll(baseModuleFieldValues);
                globalClueResponse.setModuleFields(moduleFields);
            }
            //固定展示列脱敏设置
            List<String> productNames = getProductNames(globalClueResponse.getProducts(), productNameMap);
            globalClueResponse.setProducts(productNames);

            if (CollectionUtils.isNotEmpty(globalClueResponse.getModuleFields())) {
                globalClueResponse.getModuleFields().stream().forEach(moduleField -> {
                    if (phoneTypeFieldIds.contains(moduleField.getFieldId()) && StringUtils.isNotBlank(moduleField.getFieldValue().toString())) {
                        moduleField.setFieldValue((getPhoneFieldValue(moduleField.getFieldValue(), moduleField.getFieldValue().toString().length())));
                    }
                });
            }
            if (StringUtils.isNotBlank(globalClueResponse.getPhone())) {
                globalClueResponse.setPhone((String) getPhoneFieldValue(globalClueResponse.getPhone(), globalClueResponse.getPhone().length()));
            }

            searchFieldMaskConfigMap.forEach((fieldId, searchFieldMaskConfig) -> {
                if (Strings.CI.equals(searchFieldMaskConfig.getBusinessKey(), "name") && !hasPermission) {
                    globalClueResponse.setName((String) getInputFieldValue(globalClueResponse.getName(), globalClueResponse.getName().length()));
                }
                if (Strings.CI.equals(searchFieldMaskConfig.getBusinessKey(), "products") && !hasPermission) {
                    List<String> maskProductNames = productNames.stream().map(t -> (String) getInputFieldValue(t, t.length())).toList();
                    globalClueResponse.setProducts(maskProductNames);
                }
            });


            globalClueResponse.setHasPermission(hasPermission);
        });
        return list;
    }

    private boolean getHasPermission(String userId, String orgId, GlobalCluePoolResponse clueListResponse, Map<String, String> userPoolMap) {
        if (Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
            return true;
        }
        if (MapUtils.isEmpty(userPoolMap)) {
            return false;
        }
        boolean hasPool = userPoolMap.containsKey(clueListResponse.getPoolId());
        boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, new ArrayList<>(), PermissionConstants.CLUE_MANAGEMENT_POOL_READ);

        return hasPool && hasPermission;
    }


}
