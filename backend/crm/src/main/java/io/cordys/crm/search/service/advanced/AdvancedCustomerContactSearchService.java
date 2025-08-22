package io.cordys.crm.search.service.advanced;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.ModuleKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.utils.ConditionFilterUtils;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.customer.service.CustomerContactFieldService;
import io.cordys.crm.search.response.advanced.AdvancedCustomerContactResponse;
import io.cordys.crm.search.service.BaseSearchService;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdvancedCustomerContactSearchService extends BaseSearchService<CustomerContactPageRequest, AdvancedCustomerContactResponse> {

    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private BaseService baseService;
    @Resource
    private CustomerContactFieldService customerContactFieldService;
    @Resource
    private ExtCustomerMapper extCustomerMapper;

    @Override
    public PagerWithOption<List<AdvancedCustomerContactResponse>> startSearch(CustomerContactPageRequest request, String orgId, String userId) {
        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果客户模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.CUSTOMER.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }

        ConditionFilterUtils.parseCondition(request);
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomerContactListResponse> list = extCustomerContactMapper.list(request, userId, orgId, null);
        List<AdvancedCustomerContactResponse> buildListData = buildCustomerContactData(list, orgId, userId);
        Map<String, List<OptionDTO>> optionMap = buildCustomerContactOptionMap(orgId, buildListData);
        // 查询重复联系人列表
        return PageUtils.setPageInfoWithOption(page, buildListData, optionMap);
    }

    @Override
    public Pager<List<AdvancedCustomerContactResponse>> startSearchNoOption(CustomerContactPageRequest request, String orgId, String userId) {
        return null;
    }


    private Map<String, List<OptionDTO>> buildCustomerContactOptionMap(String orgId, List<AdvancedCustomerContactResponse> list) {
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTACT.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, AdvancedCustomerContactResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(list,
                AdvancedCustomerContactResponse::getOwner, AdvancedCustomerContactResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_CONTACT_OWNER.getBusinessKey(), ownerFieldOption);

        // 补充客户选项
        List<OptionDTO> customerFieldOption = moduleFormService.getBusinessFieldOption(list,
                AdvancedCustomerContactResponse::getCustomerId, AdvancedCustomerContactResponse::getCustomerName);
        optionMap.put(BusinessModuleField.CUSTOMER_CONTACT_CUSTOMER.getBusinessKey(), customerFieldOption);
        return optionMap;
    }

    private List<AdvancedCustomerContactResponse> buildCustomerContactData(List<CustomerContactListResponse> list, String orgId, String userId) {
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        List<String> customerContactIds = list.stream().map(CustomerContactListResponse::getId)
                .distinct()
                .collect(Collectors.toList());

        List<String> customerIds = list.stream().map(CustomerContactListResponse::getCustomerId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = customerContactFieldService.getResourceFieldMap(customerContactIds, true);

        Map<String, String> customNameMap = extCustomerMapper.selectOptionByIds(customerIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

        List<String> ownerIds = list.stream()
                .map(CustomerContactListResponse::getOwner)
                .distinct()
                .toList();

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        List<AdvancedCustomerContactResponse> returnList = new ArrayList<>();
        list.forEach(customerListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            customerListResponse.setModuleFields(customerFields);

            UserDeptDTO userDeptDTO = userDeptMap.get(customerListResponse.getOwner());
            if (userDeptDTO != null) {
                customerListResponse.setDepartmentId(userDeptDTO.getDeptId());
                customerListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            customerListResponse.setCustomerName(customNameMap.get(customerListResponse.getCustomerId()));
            AdvancedCustomerContactResponse advancedCustomerContactResponse = new AdvancedCustomerContactResponse();
            BeanUtils.copyBean(advancedCustomerContactResponse, customerListResponse);
            boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, advancedCustomerContactResponse.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
            advancedCustomerContactResponse.setHasPermission(hasPermission);
            if (!hasPermission) {
                advancedCustomerContactResponse.setModuleFields(new ArrayList<>());
                advancedCustomerContactResponse.setPhone(null);
                advancedCustomerContactResponse.setDisableReason(null);
            }
            returnList.add(advancedCustomerContactResponse);
        });

        return baseService.setCreateUpdateOwnerUserName(returnList);
    }
}
