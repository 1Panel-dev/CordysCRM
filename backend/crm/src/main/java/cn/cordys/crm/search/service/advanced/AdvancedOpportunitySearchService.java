package cn.cordys.crm.search.service.advanced;


import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.opportunity.constants.StageType;
import cn.cordys.crm.opportunity.domain.OpportunityRule;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.service.OpportunityFieldService;
import cn.cordys.crm.opportunity.service.OpportunityRuleService;
import cn.cordys.crm.search.response.advanced.AdvancedOpportunityResponse;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.SystemResultCode;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtProductMapper;
import cn.cordys.crm.system.service.DictService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdvancedOpportunitySearchService extends BaseSearchService<OpportunityPageRequest, AdvancedOpportunityResponse> {

    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private OpportunityFieldService opportunityFieldService;
    @Resource
    private BaseService baseService;
    @Autowired
    private OpportunityRuleService opportunityRuleService;
    @Resource
    private DictService dictService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ExtProductMapper extProductMapper;

    /**
     * 全局搜索商机
     *
     * @param request
     * @param orgId
     * @param userId
     *
     * @return
     */
    @Override
    public PagerWithOption<List<AdvancedOpportunityResponse>> startSearch(OpportunityPageRequest request, String orgId, String userId) {
        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果有商机读取权限但商机模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.BUSINESS.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }

        ConditionFilterUtils.parseCondition(request);
        // 查询重复商机列表
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<AdvancedOpportunityResponse> list = extOpportunityMapper.advancedSearchList(request, orgId);
        if (CollectionUtils.isEmpty(list)) {
            return PageUtils.setPageInfoWithOption(page, null, null);
        }
        List<AdvancedOpportunityResponse> buildList = buildListData(list, orgId, userId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public List<AdvancedOpportunityResponse> buildListData(List<AdvancedOpportunityResponse> list, String orgId, String userId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> opportunityIds = list.stream().map(OpportunityListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> opportunityFiledMap = opportunityFieldService.getResourceFieldMap(opportunityIds, true);

        List<String> ownerIds = list.stream()
                .map(OpportunityListResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = list.stream()
                .map(OpportunityListResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = list.stream()
                .map(OpportunityListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(OpportunityListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        List<String> contactIds = list.stream()
                .map(OpportunityListResponse::getContactId)
                .distinct()
                .toList();
        Map<String, String> contactMap = baseService.getContactMap(contactIds);

        Map<String, OpportunityRule> ownersDefaultRuleMap = opportunityRuleService.getOwnersDefaultRuleMap(ownerIds, orgId);
        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        // 失败原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.OPPORTUNITY_FAIL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, opportunityListResponse.getOwner(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
            List<BaseModuleFieldValue> opportunityFields = opportunityFiledMap.get(opportunityListResponse.getId());

            opportunityListResponse.setReservedDays(Strings.CS.equalsAny(opportunityListResponse.getStage(), StageType.SUCCESS.name(), StageType.FAIL.name()) ?
                    null : opportunityRuleService.calcReservedDay(ownersDefaultRuleMap.get(opportunityListResponse.getOwner()), opportunityListResponse.getCreateTime()));
            if (!hasPermission) {
                opportunityListResponse.setModuleFields(new ArrayList<>());
                opportunityListResponse.setFailureReason(dictMap.get(null));
                opportunityListResponse.setAmount(null);
                opportunityListResponse.setContactName(null);
            } else {
                opportunityListResponse.setModuleFields(opportunityFields);
                opportunityListResponse.setFailureReason(dictMap.get(opportunityListResponse.getFailureReason()));
                opportunityListResponse.setContactName(contactMap.get(opportunityListResponse.getContactId()));
            }
            opportunityListResponse.setFollowerName(userNameMap.get(opportunityListResponse.getFollower()));
            opportunityListResponse.setCreateUserName(userNameMap.get(opportunityListResponse.getCreateUser()));
            opportunityListResponse.setUpdateUserName(userNameMap.get(opportunityListResponse.getUpdateUser()));
            opportunityListResponse.setOwnerName(userNameMap.get(opportunityListResponse.getOwner()));

            UserDeptDTO userDeptDTO = userDeptMap.get(opportunityListResponse.getOwner());
            if (userDeptDTO != null) {
                opportunityListResponse.setDepartmentId(userDeptDTO.getDeptId());
                opportunityListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            opportunityListResponse.setHasPermission(hasPermission);
        });
        return baseService.setCreateAndUpdateUserName(list);
    }


    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<AdvancedOpportunityResponse> list, List<AdvancedOpportunityResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, OpportunityListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                OpportunityListResponse::getOwner, OpportunityListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                OpportunityListResponse::getContactId, OpportunityListResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);

        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);

        return optionMap;

    }
}
