package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.registry.ExportThreadRegistry;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
	private ContractPaymentRecordService contractPaymentRecordService;
	@Resource
	private ExtContractPaymentRecordMapper extContractPaymentRecordMapper;

	@Override
	public List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {
		ContractPaymentRecordPageRequest request = (ContractPaymentRecordPageRequest) exportDTO.getPageRequest();
		String orgId = exportDTO.getOrgId();
		PageHelper.startPage(request.getCurrent(), request.getPageSize());
		List<ContractPaymentRecordResponse> list = extContractPaymentRecordMapper.list(request, exportDTO.getUserId(), orgId, exportDTO.getDeptDataPermission());
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

	private List<Object> buildData(List<ExportHeadDTO> headList, ContractPaymentRecordResponse data, Map<String, BaseField> fieldConfigMap) {
		List<Object> dataList = new ArrayList<>();
		LinkedHashMap<String, Object> systemFiledMap = getSystemFieldMap(data);
		Map<String, Object> moduleFieldMap = getFieldIdValueMap(data.getModuleFields());
		return transModuleFieldValue(headList, systemFiledMap, moduleFieldMap, dataList, fieldConfigMap);
	}

	public LinkedHashMap<String, Object> getSystemFieldMap(ContractPaymentRecordResponse data) {
		LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
		systemFiledMap.put("contractId", data.getContractName());
		systemFiledMap.put("paymentPlanId", data.getPaymentPlanName());
		systemFiledMap.put("owner", data.getOwnerName());
		systemFiledMap.put("departmentId", data.getDepartmentName());
		systemFiledMap.put("recordAmount", data.getRecordAmount());
		systemFiledMap.put("recordEndTime", TimeUtils.getDataTimeStr(data.getRecordEndTime()));
		systemFiledMap.put("recordBank", data.getRecordBank());
		systemFiledMap.put("recordBankNo", data.getRecordBankNo());

		systemFiledMap.put("createUser", data.getCreateUserName());
		systemFiledMap.put("createTime", TimeUtils.getDataTimeStr(data.getCreateTime()));
		systemFiledMap.put("updateUser", data.getUpdateUserName());
		systemFiledMap.put("updateTime", TimeUtils.getDataTimeStr(data.getUpdateTime()));
		return systemFiledMap;
	}

	/**
	 * 选中回款计划数据
	 *
	 * @return 导出数据列表
	 */
	@Override
	public List<List<Object>> getSelectExportData(List<String> ids, String taskId, ExportDTO exportDTO) throws InterruptedException {
		String orgId = exportDTO.getOrgId();
		String userId = exportDTO.getUserId();
		//获取数据
		List<ContractPaymentRecordResponse> allList = extContractPaymentRecordMapper.getListByIds(ids, userId, orgId, exportDTO.getDeptDataPermission());
		List<ContractPaymentRecordResponse> dataList = contractPaymentRecordService.buildListExtra(allList, orgId);
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
}
