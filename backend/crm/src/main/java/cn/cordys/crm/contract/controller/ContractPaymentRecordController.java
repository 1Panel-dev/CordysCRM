package cn.cordys.crm.contract.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.ContractPaymentRecord;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordGetResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author song-cc-rock
 */
@Tag(name = "合同回款记录")
@RestController
@RequestMapping("/contract/payment-record")
public class ContractPaymentRecordController {

	@Resource
	private ModuleFormCacheService moduleFormCacheService;
	@Resource
	private DataScopeService dataScopeService;
	@Resource
	private ContractPaymentRecordService contractPaymentRecordService;

	@GetMapping("/module/form")
	@RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
	@Operation(summary = "获取表单配置")
	public ModuleFormConfigDTO getModuleFormConfig() {
		return moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/page")
	@RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
	@Operation(summary = "回款记录列表")
	public PagerWithOption<List<ContractPaymentRecordResponse>> list(@Validated @RequestBody ContractPaymentRecordPageRequest request) {
		ConditionFilterUtils.parseCondition(request);
		DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
				OrganizationContext.getOrganizationId(), request.getViewId(), PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
		return contractPaymentRecordService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), deptDataPermission);
	}

	@PostMapping("/add")
	@RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_ADD)
	@Operation(summary = "添加回款记录")
	public ContractPaymentRecord add(@Validated @RequestBody ContractPaymentRecordAddRequest request) {
		return contractPaymentRecordService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@PostMapping("/update")
	@RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_UPDATE)
	@Operation(summary = "修改回款记录")
	public ContractPaymentRecord update(@Validated @RequestBody ContractPaymentRecordUpdateRequest request) {
		return contractPaymentRecordService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}

	@GetMapping("/delete/{id}")
	@RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_DELETE)
	@Operation(summary = "删除回款记录")
	public void delete(@PathVariable String id) {
		contractPaymentRecordService.delete(id);
	}

	@GetMapping("/get/{id}")
	@RequiresPermissions(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)
	@Operation(summary = "回款记录详情")
	public ContractPaymentRecordGetResponse get(@PathVariable String id) {
		return contractPaymentRecordService.getWithDataPermissionCheck(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
	}
}
