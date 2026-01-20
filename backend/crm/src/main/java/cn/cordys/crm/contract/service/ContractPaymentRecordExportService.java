package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.service.ModuleFieldExtService;
import cn.cordys.registry.ExportThreadRegistry;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ContractPaymentRecordExportService extends BaseExportService {

	@Resource
	private ModuleFieldExtService moduleFieldExtService;
	@Resource
	private ContractPaymentRecordService contractPaymentRecordService;
	@Resource
	private ExtContractPaymentRecordMapper extContractPaymentRecordMapper;

	@Override
	public List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {
		// 分页查询数据
		ContractPaymentRecordPageRequest request = (ContractPaymentRecordPageRequest) exportDTO.getPageRequest();
		PageHelper.startPage(request.getCurrent(), request.getPageSize());
		List<ContractPaymentRecordResponse> list = extContractPaymentRecordMapper.list(request, exportDTO.getUserId(), exportDTO.getOrgId(), exportDTO.getDeptDataPermission());
		return expandList(list, taskId, exportDTO);
	}

	/**
	 * 获取系统字段的值
	 * @param data 详情
	 * @return 字段值映射
	 */
	public LinkedHashMap<String, Object> getSystemFieldMap(ContractPaymentRecordResponse data) {
		LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
		systemFiledMap.put("name", data.getName());
		systemFiledMap.put("no", data.getNo());
		systemFiledMap.put("contractId", data.getContractName());
		systemFiledMap.put("paymentPlanId", data.getPaymentPlanName());
		systemFiledMap.put("owner", data.getOwnerName());
		systemFiledMap.put("departmentId", data.getDepartmentName());
		systemFiledMap.put("recordAmount", data.getRecordAmount());
		systemFiledMap.put("recordEndTime", getInternalDateStr(data.getRecordEndTime(), FormKey.CONTRACT_PAYMENT_RECORD.getKey(),
				data.getOrganizationId(), BusinessModuleField.CONTRACT_PAYMENT_RECORD_END_TIME.getKey()));
		systemFiledMap.put("recordBank", processInternalOptions(data.getRecordBank(), FormKey.CONTRACT_PAYMENT_RECORD.getKey(),
				data.getOrganizationId(), BusinessModuleField.CONTRACT_PAYMENT_RECORD_BANK.getKey()));
		systemFiledMap.put("recordBankNo", processInternalOptions(data.getRecordBankNo(), FormKey.CONTRACT_PAYMENT_RECORD.getKey(),
				data.getOrganizationId(), BusinessModuleField.CONTRACT_PAYMENT_RECORD_BANK_NO.getKey()));

		systemFiledMap.put("createUser", data.getCreateUserName());
		systemFiledMap.put("createTime", TimeUtils.getDataTimeStr(data.getCreateTime()));
		systemFiledMap.put("updateUser", data.getUpdateUserName());
		systemFiledMap.put("updateTime", TimeUtils.getDataTimeStr(data.getUpdateTime()));
		return systemFiledMap;
	}

	/**
	 * 获取勾选的回款记录导出数据
	 * @return 导出数据列表
	 */
	@Override
	public List<List<Object>> getSelectExportData(List<String> ids, String taskId, ExportDTO exportDTO) throws InterruptedException {
		List<ContractPaymentRecordResponse> allList = extContractPaymentRecordMapper.getListByIds(ids, exportDTO.getUserId(), exportDTO.getOrgId(), exportDTO.getDeptDataPermission());
		return expandList(allList, taskId, exportDTO);
	}

	/**
	 * 展开列表数据
	 * @param list 列表数据
	 * @param taskId 任务ID
	 * @param exportDTO 导出参数
	 * @return 导出数据
	 * @throws InterruptedException 异常信息
	 */
	private List<List<Object>> expandList(List<ContractPaymentRecordResponse> list, String taskId, ExportDTO exportDTO) throws InterruptedException {
		String orgId = exportDTO.getOrgId();
		List<ContractPaymentRecordResponse> dataList = contractPaymentRecordService.buildListExtra(list, orgId);
		Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), orgId);
		//构建导出数据
		List<List<Object>> data = new ArrayList<>();
		for (ContractPaymentRecordResponse response : dataList) {
			if (ExportThreadRegistry.isInterrupted(taskId)) {
				throw new InterruptedException("线程已被中断，主动退出");
			}
			List<Object> value = buildData(exportDTO.getHeadList(), response, fieldConfigMap);
			data.add(value);
		}
		return data;
	}

	/**
	 * 构建导出数据
	 * @param headList 表头集合
	 * @param data 数据详情
	 * @param fieldConfigMap 字段配置
	 * @return 导出数据
	 */
	private List<Object> buildData(List<ExportHeadDTO> headList, ContractPaymentRecordResponse data, Map<String, BaseField> fieldConfigMap) {
		LinkedHashMap<String, Object> systemFiledMap = getSystemFieldMap(data);
		Map<String, Object> moduleFieldMap = getFieldIdValueMap(data.getModuleFields());
		return transModuleFieldValue(headList, systemFiledMap, moduleFieldMap, new ArrayList<>(), fieldConfigMap);
	}

	/**
	 * 处理内部选项值
	 * @param value 选项值
	 * @param formKey 表单Key
	 * @param orgId 组织ID
	 * @param internalKey 字段内部Key
	 * @return 名称
	 */
	private String processInternalOptions(String value, String formKey, String orgId, String internalKey) {
		List<OptionProp> options = moduleFieldExtService.getFieldOptions(formKey, orgId, internalKey);
		for (OptionProp option : options) {
			if (Strings.CS.equals(option.getValue(), value)) {
				return option.getLabel();
			}
		}
		return value;
	}

	/**
	 * 获取内部日期字符串
	 * @param timestamp 毫秒
	 * @param formKey 表单Key
	 * @param orgId 组织ID
	 * @param internalKey 内部Key
	 * @return 日期字符串
	 */
	private String getInternalDateStr(Long timestamp, String formKey, String orgId, String internalKey) {
		String dateType = moduleFieldExtService.getDateFieldType(formKey, orgId, internalKey);
		return switch (dateType) {
			case "date" -> TimeUtils.getDataStr(timestamp);
			case "month" -> TimeUtils.getMonthStr(timestamp);
			default -> TimeUtils.getDataTimeStr(timestamp);
		};
	}
}
