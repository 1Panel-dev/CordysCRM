package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.registry.ExportThreadRegistry;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractExportService extends BaseExportService {

    @Resource
    private ContractService contractService;
    @Resource
    private ExtContractMapper extContractMapper;

    /**
     * 构建导出的数据
     *
     * @return 导出数据列表
     */
    @Override
    public List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {
        ContractPageRequest pageRequest = (ContractPageRequest) exportDTO.getPageRequest();
        String orgId = exportDTO.getOrgId();
        PageHelper.startPage(pageRequest.getCurrent(), pageRequest.getPageSize());
        //获取数据
        List<ContractListResponse> allList = extContractMapper.list(pageRequest, orgId, exportDTO.getUserId(), exportDTO.getDeptDataPermission());
        List<ContractListResponse> dataList = contractService.buildList(allList, orgId);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CONTRACT.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (ContractListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(exportDTO.getHeadList(), response, fieldConfigMap);
            data.add(value);
        }

        return data;
    }

    private List<Object> buildData(List<ExportHeadDTO> headList, ContractListResponse data, Map<String, BaseField> fieldConfigMap) {
        List<Object> dataList = new ArrayList<>();
        //固定字段map
        LinkedHashMap<String, Object> systemFiledMap = getSystemFieldMap(data);
        //自定义字段map
        Map<String, Object> moduleFieldMap = getFieldIdValueMap(data.getModuleFields());
        //处理数据转换
        return transModuleFieldValue(headList, systemFiledMap, moduleFieldMap, dataList, fieldConfigMap);
    }

    public LinkedHashMap<String, Object> getSystemFieldMap(ContractListResponse data) {
        LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
        systemFiledMap.put("name", data.getName());
        systemFiledMap.put("owner", data.getOwnerName());
        systemFiledMap.put("departmentId", data.getDepartmentName());
        systemFiledMap.put("customerId", data.getCustomerName());
        systemFiledMap.put("amount", data.getAmount());
        systemFiledMap.put("number", data.getNumber());
        if (StringUtils.isNotBlank(data.getApprovalStatus())) {
            systemFiledMap.put("reviewStatus", Translator.get("contract.review_status." + data.getApprovalStatus().toLowerCase()));
        }
        if (StringUtils.isNotBlank(data.getArchivedStatus())) {
            systemFiledMap.put("archivedStatus", Translator.get("contract.archived_status." + data.getArchivedStatus().toLowerCase()));
        }
        if (StringUtils.isNotBlank(data.getStatus())) {
            systemFiledMap.put("status", Translator.get("contract.status." + data.getStatus().toLowerCase()));
        }
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
    public List<List<Object>> getSelectExportData(List<String> ids, String taskId,  ExportDTO exportDTO) throws InterruptedException {
        String orgId = exportDTO.getOrgId();
        //获取数据
        List<ContractListResponse> allList = extContractMapper.getListByIds(ids, orgId, exportDTO.getDeptDataPermission());
        List<ContractListResponse> dataList = contractService.buildList(allList, orgId);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CONTRACT.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (ContractListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(exportDTO.getHeadList(), response, fieldConfigMap);
            data.add(value);
        }
        return data;
    }
}
