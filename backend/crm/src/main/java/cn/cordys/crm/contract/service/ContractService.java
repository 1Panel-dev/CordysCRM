package cn.cordys.crm.contract.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.dto.RolePermissionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.constants.ArchivedStatus;
import cn.cordys.crm.contract.constants.ContractStatus;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractSnapshot;
import cn.cordys.crm.contract.dto.request.ContractAddRequest;
import cn.cordys.crm.contract.dto.request.ContractArchivedRequest;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.request.ContractUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.dto.response.ContractResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationGetResponse;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractService {

    @Resource
    private ContractFieldService contractFieldService;
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<ContractSnapshot> snapshotBaseMapper;
    @Resource
    private ExtContractMapper extContractMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private PermissionCache permissionCache;

    /**
     * 新建合同
     *
     * @param request
     * @param operatorId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.ADD, resourceName = "{#request.name}")
    public Contract add(ContractAddRequest request, String operatorId, String orgId) {
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        Contract contract = new Contract();
        String id = IDGenerator.nextStr();
        contract.setId(id);
        contract.setName(request.getName());
        contract.setCustomerId(request.getCustomerId());
        contract.setAmount(request.getAmount());
        contract.setOwner(request.getOwner());
        //todo number
        contract.setNumber(id);
        contract.setStatus(ContractStatus.SIGNED.name());
        contract.setOrganizationId(orgId);
        contract.setArchivedStatus(ArchivedStatus.UN_ARCHIVED.name());
        contract.setCreateTime(System.currentTimeMillis());
        contract.setCreateUser(operatorId);
        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(operatorId);

        //自定义字段
        contractFieldService.saveModuleField(contract, orgId, operatorId, moduleFields, false);
        contractMapper.insert(contract);

        baseService.handleAddLog(contract, request.getModuleFields());

        // 保存表单配置快照
        OpportunityQuotationGetResponse response = getContractResponse(contract, moduleFields, moduleFormConfigDTO);
        saveSnapshot(contract, moduleFormConfigDTO, response);

        return contract;
    }


    /**
     * 保存合同快照
     *
     * @param contract
     * @param moduleFormConfigDTO
     * @param response
     */
    private void saveSnapshot(Contract contract, ModuleFormConfigDTO moduleFormConfigDTO, OpportunityQuotationGetResponse response) {
        ContractSnapshot snapshot = new ContractSnapshot();
        snapshot.setId(IDGenerator.nextStr());
        snapshot.setContractId(contract.getId());
        snapshot.setContractProp(JSON.toJSONString(moduleFormConfigDTO));
        snapshot.setContractValue(JSON.toJSONString(response));
        snapshotBaseMapper.insert(snapshot);

    }


    /**
     * 获取合同详情
     *
     * @param contract
     * @param moduleFields
     * @param moduleFormConfigDTO
     * @return
     */
    private OpportunityQuotationGetResponse getContractResponse(Contract contract, List<BaseModuleFieldValue> moduleFields, ModuleFormConfigDTO moduleFormConfigDTO) {
        OpportunityQuotationGetResponse response = BeanUtils.copyBean(new OpportunityQuotationGetResponse(), contract);
        response.setModuleFields(moduleFields);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(moduleFormConfigDTO, moduleFields);
        response.setOptionMap(optionMap);
        Map<String, List<Attachment>> attachmentMap = moduleFormService.getAttachmentMap(moduleFormConfigDTO, moduleFields);
        response.setAttachmentMap(attachmentMap);
        return baseService.setCreateAndUpdateUserName(response);
    }


    /**
     * 编辑合同
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public Contract update(ContractUpdateRequest request, String userId, String orgId) {
        Contract oldContract = contractMapper.selectByPrimaryKey(request.getId());
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        Optional.ofNullable(oldContract).ifPresentOrElse(item -> {
            if (Strings.CI.equals(oldContract.getArchivedStatus(), ArchivedStatus.ARCHIVED.name())) {
                throw new GenericException(Translator.get("contract.archived.cannot.edit"));
            }
            if (Strings.CI.equals((oldContract.getStatus()), ContractStatus.VOID.name())) {
                throw new GenericException(Translator.get("contract.void.cannot.edit"));
            }
            List<BaseModuleFieldValue> originFields = contractFieldService.getModuleFieldValuesByResourceId(request.getId());
            Contract contract = BeanUtils.copyBean(new Contract(), request);
            contract.setUpdateTime(System.currentTimeMillis());
            contract.setUpdateUser(userId);
            updateFields(moduleFields, contract, orgId, userId);
            contractMapper.update(contract);
            // 处理日志上下文
            baseService.handleUpdateLog(oldContract, contract, originFields, moduleFields, request.getId(), contract.getName());

            //删除快照
            LambdaQueryWrapper<ContractSnapshot> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(ContractSnapshot::getContractId, request.getId());
            snapshotBaseMapper.deleteByLambda(delWrapper);
            //保存快照
            OpportunityQuotationGetResponse response = getContractResponse(contract, moduleFields, request.getModuleFormConfigDTO());
            saveSnapshot(contract, request.getModuleFormConfigDTO(), response);


        }, () -> {
            throw new GenericException(Translator.get("contract.not.exist"));
        });
        return contractMapper.selectByPrimaryKey(request.getId());
    }


    /**
     * 更新自定义字段
     *
     * @param moduleFields
     * @param contract
     * @param orgId
     * @param userId
     */
    private void updateFields(List<BaseModuleFieldValue> moduleFields, Contract contract, String orgId, String userId) {
        if (moduleFields == null) {
            return;
        }
        contractFieldService.deleteByResourceId(contract.getId());
        contractFieldService.saveModuleField(contract, orgId, userId, moduleFields, false);
    }


    /**
     * 删除合同
     *
     * @param id
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        if (Strings.CI.equals(contract.getArchivedStatus(), ArchivedStatus.ARCHIVED.name())) {
            throw new GenericException(Translator.get("contract.archived.cannot.delete"));
        }

        contractFieldService.deleteByResourceId(id);
        contractMapper.deleteByPrimaryKey(id);

        //删除快照
        LambdaQueryWrapper<ContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractSnapshot::getContractId, id);
        snapshotBaseMapper.deleteByLambda(wrapper);
        // 添加日志上下文
        OperationLogContext.setResourceName(contract.getName());
    }


    /**
     * 合同详情
     *
     * @param id
     * @return
     */
    public ContractResponse get(String id) {
        ContractResponse response = new ContractResponse();
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }

        LambdaQueryWrapper<ContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractSnapshot::getContractId, id);
        ContractSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getContractValue(), ContractResponse.class);
        }

        return response;
    }


    /**
     * 合同列表
     *
     * @param request
     * @param userId
     * @param orgId
     * @param deptDataPermission
     * @return
     */
    public PagerWithOption<List<ContractListResponse>> list(ContractPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ContractListResponse> list = extContractMapper.list(request, orgId, userId, deptDataPermission);
        List<ContractListResponse> results = buildList(list);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, results);

        return PageUtils.setPageInfoWithOption(page, results, optionMap);
    }

    private Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<ContractListResponse> list, List<ContractListResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ContractListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);
        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                ContractListResponse::getOwner, ContractListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey(), ownerFieldOption);
        return optionMap;
    }

    private ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT.getKey(), orgId);
    }

    private List<ContractListResponse> buildList(List<ContractListResponse> list) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        List<String> opportunityIds = list.stream().map(ContractListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> contractFiledMap = contractFieldService.getResourceFieldMap(opportunityIds, true);

        List<String> ownerIds = list.stream()
                .map(ContractListResponse::getOwner)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(ownerIds);

        list.forEach(item -> {
            item.setOwnerName(userNameMap.get(item.getOwner()));
            // 获取自定义字段
            List<BaseModuleFieldValue> contractFields = contractFiledMap.get(item.getId());
            item.setModuleFields(contractFields);
        });
        return baseService.setCreateAndUpdateUserName(list);
    }


    /**
     * 作废
     *
     * @param id
     * @param userId
     * @return
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.VOIDED, resourceId = "{#id}")
    public void voidContract(String id, String userId) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }

        if (Strings.CI.contains(contract.getArchivedStatus(), ArchivedStatus.ARCHIVED.name())) {
            throw new GenericException(Translator.get("contract.archived.cannot.voided"));
        }

        contract.setStatus(ContractStatus.VOID.name());
        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(userId);
        contractMapper.updateById(contract);

        // 添加日志上下文
        OperationLogContext.setResourceName(contract.getName());
    }


    /**
     * 归档/取消归档
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.ARCHIVED, resourceId = "{#request.id}")
    public void archivedContract(ContractArchivedRequest request, String userId) {
        Contract contract = contractMapper.selectByPrimaryKey(request.getId());
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        //todo 审核通过才能归档 （目前没有审核
        contract.setArchivedStatus(request.getArchivedStatus());
        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(userId);
        contractMapper.updateById(contract);

        // 添加日志上下文
        OperationLogContext.setResourceName(contract.getName());
    }

    /**
     * 获取表单快照
     *
     * @param id
     * @param orgId
     * @return
     */
    public ModuleFormConfigDTO getFormSnapshot(String id, String orgId) {
        ModuleFormConfigDTO moduleFormConfigDTO = new ModuleFormConfigDTO();
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("contract.not.exist"));
        }
        LambdaQueryWrapper<ContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractSnapshot::getContractId, id);
        ContractSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            moduleFormConfigDTO = JSON.parseObject(snapshot.getContractProp(), ModuleFormConfigDTO.class);
        } else {
            moduleFormConfigDTO = moduleFormCacheService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), orgId);
        }
        return moduleFormConfigDTO;

    }


    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CONTRACT_READ, rolePermissions);
    }
}
