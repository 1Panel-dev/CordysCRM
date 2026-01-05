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
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ContractPaymentRecordService {

	@Resource
	private BaseService baseService;
	@Resource
	private ModuleFormService moduleFormService;
	@Resource
	private ModuleFormCacheService moduleFormCacheService;
	@Resource
	private ExtContractPaymentRecordMapper extContractPaymentRecordMapper;
	@Resource
	private ContractPaymentRecordFieldService contractPaymentRecordFieldService;

	/**
	 * 价格列表
	 *
	 * @param request    请求参数
	 * @param currentOrg 当前组织
	 *
	 * @return 价格列表
	 */
	public PagerWithOption<List<ContractPaymentRecordResponse>> list(ContractPaymentRecordPageRequest request, String currentUser, String currentOrg,
																	 DeptDataPermissionDTO deptDataPermission) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
		List<ContractPaymentRecordResponse> list = extContractPaymentRecordMapper.list(request, currentUser, currentOrg, deptDataPermission);
		List<ContractPaymentRecordResponse> buildList = buildList(list, currentOrg);
		// 处理自定义字段选项
		Map<String, List<OptionDTO>> optionMap = buildOptionMap(buildList, currentOrg);
		return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
	}

	private List<ContractPaymentRecordResponse> buildList(List<ContractPaymentRecordResponse> list, String currentOrg) {
		if (CollectionUtils.isEmpty(list)) {
			return list;
		}
		List<String> recordIds = list.stream().map(ContractPaymentRecordResponse::getId).collect(Collectors.toList());
		Map<String, List<BaseModuleFieldValue>> resourceFieldMap = contractPaymentRecordFieldService.getResourceFieldMap(recordIds, true);
		Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = contractPaymentRecordFieldService.setBusinessRefFieldValue(list,
				moduleFormService.getFlattenFormFields(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), currentOrg), resourceFieldMap);

		List<String> ownerIds = list.stream().map(ContractPaymentRecordResponse::getOwner).distinct().toList();
		List<String> createUserIds = list.stream().map(ContractPaymentRecordResponse::getCreateUser).distinct().toList();
		List<String> updateUserIds = list.stream().map(ContractPaymentRecordResponse::getUpdateUser).distinct().toList();
		List<String> userIds = Stream.of(ownerIds, createUserIds, updateUserIds)
				.flatMap(Collection::stream).distinct().toList();
		Map<String, String> userNameMap = baseService.getUserNameMap(userIds);
		Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, currentOrg);
		list.forEach(item -> {
			item.setModuleFields(resolvefieldValueMap.get(item.getId()));
			item.setCreateUserName(baseService.getAndCheckOptionName(userNameMap.get(item.getCreateUser())));
			item.setUpdateUserName(baseService.getAndCheckOptionName(userNameMap.get(item.getUpdateUser())));
			item.setOwnerName(baseService.getAndCheckOptionName(userNameMap.get(item.getOwner())));
			if (userDeptMap.containsKey(item.getOwner())) {
				UserDeptDTO userDept = userDeptMap.get(item.getOwner());
				item.setDepartmentId(userDept.getDeptId());
				item.setDepartmentName(userDept.getDeptName());
			}
		});
		return list;
	}

	private Map<String, List<OptionDTO>> buildOptionMap(List<ContractPaymentRecordResponse> list, String currentOrg) {
		// 处理自定义字段选项数据
		ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), currentOrg);
		// 获取所有模块字段的值
		List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ContractPaymentRecordResponse::getModuleFields);
		// 获取选项值对应的 option
		Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFieldValues);

		// 补充负责人选项
		List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(list,
				ContractPaymentRecordResponse::getOwner, ContractPaymentRecordResponse::getOwnerName);
		optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_OWNER.getBusinessKey(), ownerFieldOption);
		// 合同
		List<OptionDTO> contractFieldOption = moduleFormService.getBusinessFieldOption(list,
				ContractPaymentRecordResponse::getContractId, ContractPaymentRecordResponse::getContractName);
		optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_CONTRACT.getBusinessKey(), contractFieldOption);
		// 回款计划
		List<OptionDTO> planFieldOption = moduleFormService.getBusinessFieldOption(list,
				ContractPaymentRecordResponse::getPaymentPlanId, ContractPaymentRecordResponse::getPaymentPlanName);
		optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_PLAN.getBusinessKey(), planFieldOption);

		return optionMap;
	}
}
