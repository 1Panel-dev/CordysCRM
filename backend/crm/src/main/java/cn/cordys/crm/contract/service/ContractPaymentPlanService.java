package cn.cordys.crm.contract.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.*;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.contract.constants.ContractPaymentPlanStatus;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanGetResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.dto.response.CustomerPaymentPlanStatisticResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
    private BaseMapper<Contract> contractMapper;
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
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private DataScopeService dataScopeService;
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

        // 合同
        List<OptionDTO> contractFieldOption = moduleFormService.getBusinessFieldOption(list,
                ContractPaymentPlanListResponse::getContractId, ContractPaymentPlanListResponse::getContractName);
        optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_PLAN_CONTRACT.getBusinessKey(), contractFieldOption);

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
            List<BaseModuleFieldValue> contractPaymentPlanFields = caseCustomFiledMap.get(planListResponse.getId());
            planListResponse.setModuleFields(contractPaymentPlanFields);

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

    public ContractPaymentPlanGetResponse getWithDataPermissionCheck(String id, String userId, String orgId) {
        ContractPaymentPlanGetResponse getResponse = get(id, orgId);
        dataScopeService.checkDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.CONTRACT_PAYMENT_PLAN_READ);
        return getResponse;
    }

    public ContractPaymentPlanGetResponse get(String id, String orgId) {
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(id);
        ContractPaymentPlanGetResponse contractPaymentPlanGetResponse = BeanUtils.copyBean(new ContractPaymentPlanGetResponse(), contractPaymentPlan);
        contractPaymentPlanGetResponse = baseService.setCreateUpdateOwnerUserName(contractPaymentPlanGetResponse);
        Contract contract = contractMapper.selectByPrimaryKey(contractPaymentPlanGetResponse.getContractId());
        // 获取模块字段
        List<BaseModuleFieldValue> contractPaymentPlanFields = contractPaymentPlanFieldService.getModuleFieldValuesByResourceId(id);
        ModuleFormConfigDTO contractPaymentPlanFormConfig = getFormConfig(orgId);

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(contractPaymentPlanFormConfig, contractPaymentPlanFields);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(contractPaymentPlanGetResponse,
                ContractPaymentPlanGetResponse::getOwner, ContractPaymentPlanGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        if (contract != null) {
            contractPaymentPlanGetResponse.setContractName(contract.getName());
            // 合同
            List<OptionDTO> contractFieldOption = moduleFormService.getBusinessFieldOption(contractPaymentPlanGetResponse,
                    ContractPaymentPlanGetResponse::getContractId, ContractPaymentPlanGetResponse::getContractName);
            optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_PLAN_CONTRACT.getBusinessKey(), contractFieldOption);
        }

        contractPaymentPlanGetResponse.setOptionMap(optionMap);
        contractPaymentPlanGetResponse.setModuleFields(contractPaymentPlanFields);

        if (contractPaymentPlanGetResponse.getOwner() != null) {
            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(contractPaymentPlanGetResponse.getOwner(), orgId);
            if (userDeptDTO != null) {
                contractPaymentPlanGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                contractPaymentPlanGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        return contractPaymentPlanGetResponse;
    }

    @OperationLog(module = LogModule.CONTRACT_PAYMENT, type = LogType.ADD)
    public ContractPaymentPlan add(ContractPaymentPlanAddRequest request, String userId, String orgId) {
        ContractPaymentPlan contractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), request);
        if (StringUtils.isBlank(request.getOwner())) {
            contractPaymentPlan.setOwner(userId);
        }
        if (StringUtils.isBlank(request.getPlanStatus())) {
            contractPaymentPlan.setPlanStatus(ContractPaymentPlanStatus.PENDING.name());
        }
        contractPaymentPlan.setCreateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser(userId);
        contractPaymentPlan.setCreateUser(userId);
        contractPaymentPlan.setOrganizationId(orgId);
        contractPaymentPlan.setId(IDGenerator.nextStr());

        //保存自定义字段
        contractPaymentPlanFieldService.saveModuleField(contractPaymentPlan, orgId, userId, request.getModuleFields(), false);

        contractPaymentPlanMapper.insert(contractPaymentPlan);

        Contract contract = contractMapper.selectByPrimaryKey(contractPaymentPlan.getContractId());

        String resourceName = contract == null ? contractPaymentPlan.getContractId() : contract.getName();

        baseService.handleAddLog(contractPaymentPlan, request.getModuleFields());
        OperationLogContext.getContext().setResourceName(resourceName);
        OperationLogContext.getContext().setResourceId(contractPaymentPlan.getId());

        // 通知
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CONTRACT,
                NotificationConstants.Event.CONTRACT_PAYMENT_PLAN, resourceName, userId,
                orgId, List.of(contractPaymentPlan.getOwner()), true);
        return contractPaymentPlan;
    }

    @OperationLog(module = LogModule.CONTRACT_PAYMENT, type = LogType.UPDATE, resourceId = "{#request.id}")
    public ContractPaymentPlan update(ContractPaymentPlanUpdateRequest request, String userId, String orgId) {
        ContractPaymentPlan originContractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(request.getId());
        dataScopeService.checkDataPermission(userId, orgId, originContractPaymentPlan.getOwner(), PermissionConstants.CONTRACT_PAYMENT_PLAN_UPDATE);

        ContractPaymentPlan contractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), request);
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser(userId);

        // 获取模块字段
        List<BaseModuleFieldValue> originContractPaymentPlanFields = contractPaymentPlanFieldService.getModuleFieldValuesByResourceId(request.getId());

        if (BooleanUtils.isTrue(request.getAgentInvoke())) {
            contractPaymentPlanFieldService.updateModuleFieldByAgent(contractPaymentPlan, originContractPaymentPlanFields, request.getModuleFields(), orgId, userId);
        } else {
            // 更新模块字段
            updateModuleField(contractPaymentPlan, request.getModuleFields(), orgId, userId);
        }

        contractPaymentPlanMapper.update(contractPaymentPlan);

        contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(request.getId());
        Contract contract = contractMapper.selectByPrimaryKey(contractPaymentPlan.getContractId());

        String resourceName = contract == null ? originContractPaymentPlan.getContractId() : contract.getName();

        baseService.handleUpdateLog(originContractPaymentPlan, contractPaymentPlan, originContractPaymentPlanFields, request.getModuleFields(), originContractPaymentPlan.getId(), resourceName);
        return contractPaymentPlan;
    }

    private void updateModuleField(ContractPaymentPlan contractPaymentPlan, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        contractPaymentPlanFieldService.deleteByResourceId(contractPaymentPlan.getId());
        // 再保存
        contractPaymentPlanFieldService.saveModuleField(contractPaymentPlan, orgId, userId, moduleFields, true);
    }

    @OperationLog(module = LogModule.CONTRACT_PAYMENT, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        ContractPaymentPlan originContractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(id);
        dataScopeService.checkDataPermission(userId, orgId, originContractPaymentPlan.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_DELETE);

        Contract contract = contractMapper.selectByPrimaryKey(originContractPaymentPlan.getContractId());

        String resourceName = contract == null ? originContractPaymentPlan.getContractId() : contract.getName();

        contractPaymentPlanMapper.deleteByPrimaryKey(id);

        // 设置操作对象
        OperationLogContext.setResourceName(resourceName);

        commonNoticeSendService.sendNotice(NotificationConstants.Module.CONTRACT,
                NotificationConstants.Event.CONTRACT_PAYMENT_PLAN_DELETE, resourceName, userId,
                orgId, List.of(originContractPaymentPlan.getOwner()), true);
    }

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CONTRACT_PAYMENT_PLAN_READ, rolePermissions);
    }

    public CustomerPaymentPlanStatisticResponse calculateCustomerPaymentPlanStatisticByCustomerId(String accountId, String userId, String organizationId, DeptDataPermissionDTO deptDataPermission) {
        return extContractPaymentPlanMapper.calculateCustomerPaymentPlanStatisticByCustomerId(accountId, userId, organizationId, deptDataPermission);
    }
}