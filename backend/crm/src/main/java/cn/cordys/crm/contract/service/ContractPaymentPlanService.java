package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanGetResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ContractPaymentPlanService {

    @Resource
    private BaseMapper<ContractPaymentPlan> contractPaymentPlanMapper;

    @Resource
    private ExtContractPaymentPlanMapper extContractPaymentPlanMapper;

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @Resource
    private ModuleFormService moduleFormService;

    @Resource
    private ContractPaymentPlanFieldService contractPaymentPlanFieldService;

    @Resource
    private BaseService baseService;

    public PagerWithOption<List<ContractPaymentPlanListResponse>> list(ContractPaymentPlanPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ContractPaymentPlanListResponse> list = extContractPaymentPlanMapper.list(request, userId, orgId, deptDataPermission);
        list = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list);
        return PageUtils.setPageInfoWithOption(page, list, optionMap);
    }

    public ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), orgId);
    }

    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<ContractPaymentPlanListResponse> list) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ContractPaymentPlanListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(list,
                ContractPaymentPlanListResponse::getOwner, ContractPaymentPlanListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_PLAN_OWNER.getBusinessKey(), ownerFieldOption);

        return optionMap;
    }

    public List<ContractPaymentPlanListResponse> buildListData(List<ContractPaymentPlanListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> planIds = list.stream().map(ContractPaymentPlanListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = contractPaymentPlanFieldService.getResourceFieldMap(planIds, true);

        List<String> ownerIds = list.stream()
                .map(ContractPaymentPlanListResponse::getOwner)
                .distinct()
                .toList();

        List<String> createUserIds = list.stream()
                .map(ContractPaymentPlanListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(ContractPaymentPlanListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        list.forEach(planListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = caseCustomFiledMap.get(planListResponse.getId());
            planListResponse.setModuleFields(customerFields);

            UserDeptDTO userDeptDTO = userDeptMap.get(planListResponse.getOwner());
            if (userDeptDTO != null) {
                planListResponse.setDepartmentId(userDeptDTO.getDeptId());
                planListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            String createUserName = baseService.getAndCheckOptionName(userNameMap.get(planListResponse.getCreateUser()));
            planListResponse.setCreateUserName(createUserName);
            String updateUserName = baseService.getAndCheckOptionName(userNameMap.get(planListResponse.getUpdateUser()));
            planListResponse.setUpdateUserName(updateUserName);
            planListResponse.setOwnerName(userNameMap.get(planListResponse.getOwner()));
        });

        return list;
    }

    public ContractPaymentPlanGetResponse get(String id) {
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(id);
        ContractPaymentPlanGetResponse contractPaymentPlanGetResponse = BeanUtils.copyBean(new ContractPaymentPlanGetResponse(), contractPaymentPlan);
        // todo
        return baseService.setCreateAndUpdateUserName(contractPaymentPlanGetResponse);
    }

    public ContractPaymentPlan add(ContractPaymentPlanAddRequest request, String userId, String orgId) {
        ContractPaymentPlan contractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), request);
        contractPaymentPlan.setCreateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser(userId);
        contractPaymentPlan.setCreateUser(userId);
        contractPaymentPlan.setOrganizationId(orgId);
        contractPaymentPlan.setId(IDGenerator.nextStr());
        contractPaymentPlanMapper.insert(contractPaymentPlan);
        return contractPaymentPlan;
    }

    public ContractPaymentPlan update(ContractPaymentPlanUpdateRequest request, String userId) {
        ContractPaymentPlan contractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), request);
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser(userId);
        contractPaymentPlanMapper.update(contractPaymentPlan);
        return contractPaymentPlanMapper.selectByPrimaryKey(contractPaymentPlan.getId());
    }

    public void delete(String id) {
        contractPaymentPlanMapper.deleteByPrimaryKey(id);
    }
}