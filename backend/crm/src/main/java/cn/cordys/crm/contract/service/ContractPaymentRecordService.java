package cn.cordys.crm.contract.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.dto.*;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.domain.*;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordAddRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordGetResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import cn.cordys.crm.contract.dto.response.CustomerPaymentRecordStatisticResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
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
	private LogService logService;
	@Resource
	private PermissionCache permissionCache;
	@Resource
	private DataScopeService dataScopeService;
	@Resource
	private ModuleFormService moduleFormService;
	@Resource
	private ModuleFormCacheService moduleFormCacheService;
	@Resource
	private BaseMapper<Contract> contractMapper;
	@Resource
	private SerialNumGenerator serialNumGenerator;
	@Resource
	private BaseMapper<ContractPaymentPlan> contractPaymentPlanMapper;
	@Resource
	private BaseMapper<ContractPaymentRecord> contractPaymentRecordMapper;
	@Resource
	private BaseMapper<ContractPaymentRecordField> contractPaymentRecordFieldMapper;
	@Resource
	private BaseMapper<ContractPaymentRecordFieldBlob> contractPaymentRecordFieldBlobMapper;
	@Resource
	private ExtContractPaymentRecordMapper extContractPaymentRecordMapper;
	@Resource
	private ContractPaymentRecordFieldService contractPaymentRecordFieldService;

	/**
	 * 获取回款记录列表
	 * @param request    			请求参数
	 * @param currentUser 			当前用户
	 * @param currentOrg 			当前组织
	 * @param deptDataPermission    数据权限
	 * @return 回款记录列表
	 */
	public PagerWithOption<List<ContractPaymentRecordResponse>> list(ContractPaymentRecordPageRequest request, String currentUser, String currentOrg,
																	 DeptDataPermissionDTO deptDataPermission) {
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
		List<ContractPaymentRecordResponse> list = extContractPaymentRecordMapper.list(request, currentUser, currentOrg, deptDataPermission);
		List<ContractPaymentRecordResponse> buildList = buildListExtra(list, currentOrg);
		// 处理自定义字段选项
		Map<String, List<OptionDTO>> optionMap = buildOptionMap(buildList, currentOrg);
		return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
	}

	@OperationLog(module = LogModule.CONTRACT_PAYMENT_RECORD, type = LogType.ADD, resourceName = "{#request.name}", operator = "{#currentUser}")
	public ContractPaymentRecord add(ContractPaymentRecordAddRequest request, String currentUser, String currentOrg) {
		checkContractPaymentAmount(request.getContractId(), request.getRecordAmount(), null);
		ContractPaymentRecord paymentRecord = BeanUtils.copyBean(new ContractPaymentRecord(), request);
		paymentRecord.setId(IDGenerator.nextStr());
		if (StringUtils.isEmpty(paymentRecord.getOwner())) {
			paymentRecord.setOwner(currentUser);
		}
		List<String> rules = moduleFormService.getSerialFieldRulesByKey(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), currentOrg, BusinessModuleField.CONTRACT_PAYMENT_RECORD_NO.getKey());
		if (CollectionUtils.isNotEmpty(rules)) {
			paymentRecord.setNo(serialNumGenerator.generateByRules(rules, currentOrg, FormKey.CONTRACT_PAYMENT_RECORD.getKey()));
		}
		paymentRecord.setCreateUser(currentUser);
		paymentRecord.setCreateTime(System.currentTimeMillis());
		paymentRecord.setUpdateUser(currentUser);
		paymentRecord.setUpdateTime(System.currentTimeMillis());
		paymentRecord.setOrganizationId(currentOrg);
		contractPaymentRecordMapper.insert(paymentRecord);
		// 保存自定义字段值
		contractPaymentRecordFieldService.saveModuleField(paymentRecord, currentOrg, currentUser, request.getModuleFields(), false);
		// 日志
		baseService.handleAddLog(paymentRecord, request.getModuleFields());
		return paymentRecord;
	}

	@OperationLog(module = LogModule.CONTRACT_PAYMENT_RECORD, type = LogType.UPDATE, operator = "{#currentUser}")
	public ContractPaymentRecord update(ContractPaymentRecordUpdateRequest request, String currentUser, String currentOrg) {
		ContractPaymentRecord oldRecord = contractPaymentRecordMapper.selectByPrimaryKey(request.getId());
		if (oldRecord == null) {
			throw new GenericException(Translator.get("record.not.exist"));
		}
		checkContractPaymentAmount(request.getContractId(), request.getRecordAmount(), oldRecord.getId());
		ContractPaymentRecord contractPaymentRecord = BeanUtils.copyBean(new ContractPaymentRecord(), request);
		contractPaymentRecord.setNo(oldRecord.getNo());
		contractPaymentRecord.setUpdateTime(System.currentTimeMillis());
		contractPaymentRecord.setUpdateUser(currentUser);
		contractPaymentRecordMapper.update(contractPaymentRecord);
		List<BaseModuleFieldValue> oldFvs = contractPaymentRecordFieldService.getModuleFieldValuesByResourceId(request.getId());
		updateModuleField(contractPaymentRecord, request.getModuleFields(), currentOrg, currentUser);
		baseService.handleUpdateLog(oldRecord, contractPaymentRecord, oldFvs, request.getModuleFields(), oldRecord.getId(), oldRecord.getName());
		return contractPaymentRecord;
	}

	@OperationLog(module = LogModule.CONTRACT_PAYMENT_RECORD, type = LogType.DELETE, resourceId = "{#id}")
	public void delete(String id) {
		ContractPaymentRecord oldRecord = contractPaymentRecordMapper.selectByPrimaryKey(id);
		if (oldRecord == null) {
			throw new GenericException(Translator.get("record.not.exist"));
		}
		contractPaymentRecordMapper.deleteByPrimaryKey(id);
		contractPaymentRecordFieldService.deleteByResourceId(id);
		OperationLogContext.setResourceName(oldRecord.getName());
	}

	public ContractPaymentRecordGetResponse getWithDataPermissionCheck(String id, String currentUser, String currentOrg) {
		ContractPaymentRecordGetResponse response = get(id, currentOrg);
		dataScopeService.checkDataPermission(currentUser, currentOrg, response.getOwner(), PermissionConstants.CONTRACT_PAYMENT_RECORD_READ);
		return response;
	}

	public ContractPaymentRecordGetResponse get(String id, String currentOrg) {
		ContractPaymentRecord paymentRecord = contractPaymentRecordMapper.selectByPrimaryKey(id);
		if (paymentRecord == null) {
			throw new GenericException(Translator.get("record.not.exist"));
		}
		ContractPaymentRecordGetResponse recordDetail = BeanUtils.copyBean(new ContractPaymentRecordGetResponse(), paymentRecord);
		recordDetail = baseService.setCreateUpdateOwnerUserName(recordDetail);
		Contract contract = contractMapper.selectByPrimaryKey(recordDetail.getContractId());
		ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(recordDetail.getPaymentPlanId());
		// 自定义字段值 & 选项值
		List<BaseModuleFieldValue> fvs = contractPaymentRecordFieldService.getModuleFieldValuesByResourceId(id);
		fvs = contractPaymentRecordFieldService.setBusinessRefFieldValue(List.of(recordDetail),
				moduleFormService.getFlattenFormFields(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), paymentRecord.getOrganizationId()), new HashMap<>(Map.of(id, fvs))).get(id);
		ModuleFormConfigDTO recordFormConf = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), paymentRecord.getOrganizationId());
		Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(recordFormConf, fvs);
		optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_OWNER.getBusinessKey(), moduleFormService.getBusinessFieldOption(List.of(recordDetail),
				ContractPaymentRecordGetResponse::getOwner, ContractPaymentRecordGetResponse::getOwnerName));
		if (contract != null) {
			recordDetail.setContractName(contract.getName());
			optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_CONTRACT.getBusinessKey(), moduleFormService.getBusinessFieldOption(List.of(recordDetail),
					ContractPaymentRecordGetResponse::getContractId, ContractPaymentRecordGetResponse::getContractName));
		}
		if (contractPaymentPlan != null) {
			recordDetail.setPaymentPlanName(contractPaymentPlan.getName());
			optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_PLAN.getBusinessKey(), moduleFormService.getBusinessFieldOption(List.of(recordDetail),
					ContractPaymentRecordGetResponse::getPaymentPlanId, ContractPaymentRecordGetResponse::getPaymentPlanName));
		}
		recordDetail.setModuleFields(fvs);
		recordDetail.setOptionMap(optionMap);
		recordDetail.setAttachmentMap(moduleFormService.getAttachmentMap(recordFormConf, recordDetail.getModuleFields()));

		if (recordDetail.getOwner() != null) {
			UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(recordDetail.getOwner(), currentOrg);
			if (userDeptDTO != null) {
				recordDetail.setDepartmentId(userDeptDTO.getDeptId());
				recordDetail.setDepartmentName(userDeptDTO.getDeptName());
			}
		}
		return recordDetail;
	}

	public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
		List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
		return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CONTRACT_PAYMENT_RECORD_READ, rolePermissions);
	}

	/**
	 * 下载导入的模板
	 *
	 * @param response 响应
	 */
	public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
		new EasyExcelExporter().exportMultiSheetTplWithSharedHandler(response,
				moduleFormService.getCustomImportHeadsNoRef(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), currentOrg),
				Translator.get("payment.record.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
				new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), currentOrg)),
				new CustomHeadColWidthStyleStrategy());
	}

	/**
	 * 导入检查
	 *
	 * @param file       导入文件
	 * @param currentOrg 当前组织
	 *
	 * @return 导入检查信息
	 */
	public ImportResponse importPreCheck(MultipartFile file, String currentOrg) {
		if (file == null) {
			throw new GenericException(Translator.get("file_cannot_be_null"));
		}
		return checkImportExcel(file, currentOrg);
	}

	/**
	 * 检查导入的文件
	 *
	 * @param file       文件
	 * @param currentOrg 当前组织
	 *
	 * @return 检查信息
	 */
	private ImportResponse checkImportExcel(MultipartFile file, String currentOrg) {
		try {
			List<BaseField> fields = moduleFormService.getAllCustomImportFields(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), currentOrg);
			CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "contract_payment_record", "contract_payment_record_field", currentOrg);
			FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
			return ImportResponse.builder().errorMessages(eventListener.getErrList())
					.successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
		} catch (Exception e) {
			log.error("Payment record import pre-check error", e);
			throw new GenericException(e);
		}
	}

	/**
	 * 回款记录导入
	 *
	 * @param file        导入文件
	 * @param currentOrg  当前组织
	 * @param currentUser 当前用户
	 *
	 * @return 导入返回信息
	 */
	public ImportResponse realImport(MultipartFile file, String currentOrg, String currentUser) {
		try {
			List<BaseField> fields = moduleFormService.getAllFields(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), currentOrg);
			Optional<BaseField> serialOptional = fields.stream().filter(field -> Strings.CI.equals(field.getInternalKey(), BusinessModuleField.CONTRACT_PAYMENT_RECORD_NO.getKey())).findAny();
			CustomImportAfterDoConsumer<ContractPaymentRecord, BaseResourceSubField> afterDo = (records, recordFields, recordFieldBlobs) -> {
				List<LogDTO> logs = new ArrayList<>();
				records.forEach(record -> {
					if (serialOptional.isPresent()) {
						List<String> serialNumberRules = ((SerialNumberField) serialOptional.get()).getSerialNumberRules();
						record.setNo(serialNumGenerator.generateByRules(serialNumberRules, currentOrg, FormKey.CONTRACT_PAYMENT_RECORD.getKey()));
					}
					logs.add(new LogDTO(currentOrg, record.getId(), currentUser, LogType.ADD, LogModule.CONTRACT_PAYMENT_RECORD, record.getName()));
				});
				contractPaymentRecordMapper.batchInsert(records);
				contractPaymentRecordFieldMapper.batchInsert(recordFields.stream().map(field -> BeanUtils.copyBean(new ContractPaymentRecordField(), field)).toList());
				contractPaymentRecordFieldBlobMapper.batchInsert(recordFieldBlobs.stream().map(field -> BeanUtils.copyBean(new ContractPaymentRecordFieldBlob(), field)).toList());
				// record logs
				logService.batchAdd(logs);
			};
			CustomFieldImportEventListener<ContractPaymentRecord> eventListener = new CustomFieldImportEventListener<>(fields, ContractPaymentRecord.class, currentOrg, currentUser,
					"contract_payment_record_field", afterDo, 2000, null, null);
			FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
			return ImportResponse.builder().errorMessages(eventListener.getErrList())
					.successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
		} catch (Exception e) {
			log.error("Payment record import error", e);
			throw new GenericException(e.getMessage());
		}
	}

	/**
	 * 汇总客户回款记录列表金额
	 * @param customerId 客户ID
	 * @param userId 用户ID
	 * @param organizationId 组织ID
	 * @param deptDataPermission 数据权限
	 * @return 汇总结果
	 */
	public CustomerPaymentRecordStatisticResponse sumCustomerPaymentAmount(String customerId, String userId, String organizationId, DeptDataPermissionDTO deptDataPermission) {
		return extContractPaymentRecordMapper.sumCustomerRecordAmount(customerId, userId, organizationId, deptDataPermission);
	}

	/**
	 * 更新自定义字段值
	 * @param contractPaymentRecord 	回款记录
	 * @param moduleFields 				自定义字段值
	 * @param currentOrg 				当前组织
	 * @param currentUser 				当前用户
	 */
	private void updateModuleField(ContractPaymentRecord contractPaymentRecord, List<BaseModuleFieldValue> moduleFields, String currentOrg, String currentUser) {
		if (moduleFields == null) {
			return;
		}
		// 删除已有的再保存
		contractPaymentRecordFieldService.deleteByResourceId(contractPaymentRecord.getId());
		contractPaymentRecordFieldService.saveModuleField(contractPaymentRecord, currentOrg, currentUser, moduleFields, true);
	}

	/**
	 * 构建列表扩展数据
	 * @param list 			列表数据
	 * @param currentOrg 	当前组织
	 * @return 列表扩展数据
	 */
	public List<ContractPaymentRecordResponse> buildListExtra(List<ContractPaymentRecordResponse> list, String currentOrg) {
		if (CollectionUtils.isEmpty(list)) {
			return list;
		}
		List<String> recordIds = list.stream().map(ContractPaymentRecordResponse::getId).collect(Collectors.toList());
		List<String> refPlanIds = list.stream().map(ContractPaymentRecordResponse::getPaymentPlanId).toList();
		List<ContractPaymentPlan> contractPaymentPlans = contractPaymentPlanMapper.selectByIds(refPlanIds);
		Map<String, String> paymentPlanMap = contractPaymentPlans.stream().collect(Collectors.toMap(ContractPaymentPlan::getId, ContractPaymentPlan::getName));
		Map<String, List<BaseModuleFieldValue>> resourceFieldMap = contractPaymentRecordFieldService.getResourceFieldMap(recordIds, true);
		Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = contractPaymentRecordFieldService.setBusinessRefFieldValue(list,
				moduleFormService.getFlattenFormFields(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), currentOrg), resourceFieldMap);

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
			item.setPaymentPlanName(paymentPlanMap.get(item.getPaymentPlanId()));
			if (userDeptMap.containsKey(item.getOwner())) {
				UserDeptDTO userDept = userDeptMap.get(item.getOwner());
				item.setDepartmentId(userDept.getDeptId());
				item.setDepartmentName(userDept.getDeptName());
			}
		});
		return list;
	}

	/**
	 * 处理选项数据
	 * @param list 			列表数据
	 * @param currentOrg 	当前组织
	 * @return 选项数据
	 */
	private Map<String, List<OptionDTO>> buildOptionMap(List<ContractPaymentRecordResponse> list, String currentOrg) {
		// 处理自定义字段选项数据
		ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), currentOrg);
		Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFormService.getBaseModuleFieldValues(list, ContractPaymentRecordResponse::getModuleFields));

		// 补充业务字段选项数据 {负责人, 合同, 回款计划}
		optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_OWNER.getBusinessKey(), moduleFormService.getBusinessFieldOption(list,
				ContractPaymentRecordResponse::getOwner, ContractPaymentRecordResponse::getOwnerName));
		optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_CONTRACT.getBusinessKey(), moduleFormService.getBusinessFieldOption(list,
				ContractPaymentRecordResponse::getContractId, ContractPaymentRecordResponse::getContractName));
		optionMap.put(BusinessModuleField.CONTRACT_PAYMENT_RECORD_PLAN.getBusinessKey(), moduleFormService.getBusinessFieldOption(list,
				ContractPaymentRecordResponse::getPaymentPlanId, ContractPaymentRecordResponse::getPaymentPlanName));

		return optionMap;
	}

	/**
	 * 校验回款金额是否超出
	 * @param contractId 	合同ID
	 * @param payAmount 	回款金额
	 */
	private void checkContractPaymentAmount(String contractId, BigDecimal payAmount, String excludeRecordId) {
		if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		LambdaQueryWrapper<ContractPaymentRecord> recordLambdaQueryWrapper = new LambdaQueryWrapper<>();
		recordLambdaQueryWrapper.eq(ContractPaymentRecord::getContractId, contractId);
		if (StringUtils.isNotEmpty(excludeRecordId)) {
			recordLambdaQueryWrapper.nq(ContractPaymentRecord::getId, excludeRecordId);
		}
		List<ContractPaymentRecord> contractPaymentRecords = contractPaymentRecordMapper.selectListByLambda(recordLambdaQueryWrapper);
		BigDecimal alreadyPay = contractPaymentRecords.stream().map(ContractPaymentRecord::getRecordAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
		Contract contract = contractMapper.selectByPrimaryKey(contractId);
		if ((alreadyPay.add(payAmount)).compareTo(contract.getAmount()) > 0) {
			throw new GenericException(Translator.getWithArgs("record.amount.exceed", contract.getAmount().subtract(alreadyPay)));
		}
	}
}
