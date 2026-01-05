package cn.cordys.crm.contract.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.dto.request.BusinessTitlePageRequest;
import cn.cordys.crm.contract.dto.response.BusinessTitleListResponse;
import cn.cordys.crm.contract.mapper.ExtBusinessTitleMapper;
import cn.cordys.registry.ExportThreadRegistry;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class BusinessTitleExportService extends BaseExportService {

    @Resource
    private ExtBusinessTitleMapper extBusinessTitleMapper;
    @Resource
    private BaseService baseService;


    /**
     * 选中工商抬头数据
     *
     * @return 导出数据列表
     */
    @Override
    public List<List<Object>> getSelectExportData(List<String> ids, String taskId, ExportDTO exportDTO) throws InterruptedException {
        String orgId = exportDTO.getOrgId();
        String userId = exportDTO.getUserId();
        //获取数据
        List<BusinessTitleListResponse> allList = extBusinessTitleMapper.getListByIds(ids, userId, orgId);
        baseService.setCreateAndUpdateUserName(allList);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (BusinessTitleListResponse response : allList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(exportDTO.getHeadList(), response);
            data.add(value);
        }
        return data;
    }

    private List<Object> buildData(List<ExportHeadDTO> headList, BusinessTitleListResponse data) {
        List<Object> dataList = new ArrayList<>();
        //固定字段map
        LinkedHashMap<String, Object> systemFiledMap = getSystemFieldMap(data);
        //处理数据转换
        return transModuleFieldValue(headList, systemFiledMap, new HashMap<>(), dataList, new HashMap<>());

    }

    public LinkedHashMap<String, Object> getSystemFieldMap(BusinessTitleListResponse data) {
        LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
        systemFiledMap.put("businessName", data.getBusinessName());
        systemFiledMap.put("identificationNumber", data.getIdentificationNumber());
        systemFiledMap.put("openingBank", data.getOpeningBank());
        systemFiledMap.put("bankAccount", data.getBankAccount());
        systemFiledMap.put("registrationAddress", data.getRegistrationAddress());
        systemFiledMap.put("phoneNumber", data.getPhoneNumber());
        systemFiledMap.put("registeredCapital", data.getRegisteredCapital());
        systemFiledMap.put("customerSize", data.getCustomerSize());
        systemFiledMap.put("registrationNumber", data.getRegistrationNumber());
        systemFiledMap.put("unapprovedReason", data.getUnapprovedReason());
        systemFiledMap.put("approvalStatus", Translator.get("contract.approval_status." + data.getApprovalStatus().toLowerCase()));

        systemFiledMap.put("createUser", data.getCreateUserName());
        systemFiledMap.put("createTime", TimeUtils.getDataTimeStr(data.getCreateTime()));
        systemFiledMap.put("updateUser", data.getUpdateUserName());
        systemFiledMap.put("updateTime", TimeUtils.getDataTimeStr(data.getUpdateTime()));
        return systemFiledMap;
    }


    /**
     * 构建导出的数据
     *
     * @return 导出数据列表
     */
    @Override
    public List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {
        BusinessTitlePageRequest pageRequest = (BusinessTitlePageRequest) exportDTO.getPageRequest();
        String orgId = exportDTO.getOrgId();
        PageHelper.startPage(pageRequest.getCurrent(), pageRequest.getPageSize());
        //获取数据
        List<BusinessTitleListResponse> dataList = extBusinessTitleMapper.list(pageRequest, orgId, exportDTO.getUserId());
        baseService.setCreateAndUpdateUserName(dataList);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (BusinessTitleListResponse response : dataList) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(exportDTO.getHeadList(), response);
            data.add(value);
        }

        return data;
    }
}
