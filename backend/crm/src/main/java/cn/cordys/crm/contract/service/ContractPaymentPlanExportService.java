package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
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

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ContractPaymentPlanExportService extends BaseExportService {

    @Resource
    private ContractPaymentPlanService contractPaymentPlanService;
    @Resource
    private ExtContractPaymentPlanMapper extContractPaymentPlanMapper;

    /**
     * 构建导出的数据
     *
     * @return 导出数据列表
     */
    @Override
    public List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {
        ContractPaymentPlanPageRequest pageRequest = (ContractPaymentPlanPageRequest) exportDTO.getPageRequest();
        String orgId = exportDTO.getOrgId();
        PageHelper.startPage(pageRequest.getCurrent(), pageRequest.getPageSize());
        //获取数据
        List<ContractPaymentPlanListResponse> allList = extContractPaymentPlanMapper.list(pageRequest, exportDTO.getUserId(), orgId, exportDTO.getDeptDataPermission());
        List<ContractPaymentPlanListResponse> dataList = contractPaymentPlanService.buildListData(allList, orgId);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (ContractPaymentPlanListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(exportDTO.getHeadList(), response, fieldConfigMap);
            data.add(value);
        }

        return data;
    }

    private List<Object> buildData(List<ExportHeadDTO> headList, ContractPaymentPlanListResponse data, Map<String, BaseField> fieldConfigMap) {
        List<Object> dataList = new ArrayList<>();
        //固定字段map
        LinkedHashMap<String, Object> systemFiledMap = getSystemFieldMap(data);
        //自定义字段map
        Map<String, Object> moduleFieldMap = getFieldIdValueMap(data.getModuleFields());
        //处理数据转换
        return transModuleFieldValue(headList, systemFiledMap, moduleFieldMap, dataList, fieldConfigMap);
    }

    public LinkedHashMap<String, Object> getSystemFieldMap(ContractPaymentPlanListResponse data) {
        LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
        systemFiledMap.put("contractId", data.getContractName());
        systemFiledMap.put("owner", data.getOwnerName());
        systemFiledMap.put("departmentId", data.getDepartmentName());
        systemFiledMap.put("planAmount", data.getPlanAmount());
        systemFiledMap.put("planEndTime", TimeUtils.getDateTimeStr(data.getPlanEndTime()));
        systemFiledMap.put("planStatus", Translator.get("contract.payment_plan.status." + data.getPlanStatus().toLowerCase()));

        systemFiledMap.put("createUser", data.getCreateUserName());
        systemFiledMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFiledMap.put("updateUser", data.getUpdateUserName());
        systemFiledMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
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
        List<ContractPaymentPlanListResponse> allList = extContractPaymentPlanMapper.getListByIds(ids, userId, orgId, exportDTO.getDeptDataPermission());
        List<ContractPaymentPlanListResponse> dataList = contractPaymentPlanService.buildListData(allList, orgId);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (ContractPaymentPlanListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(exportDTO.getHeadList(), response, fieldConfigMap);
            data.add(value);
        }
        return data;
    }
}
