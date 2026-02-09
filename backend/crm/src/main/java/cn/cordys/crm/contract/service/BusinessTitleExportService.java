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
        var orgId = exportDTO.getOrgId();
        var userId = exportDTO.getUserId();
        var allList = extBusinessTitleMapper.getListByIds(ids, userId, orgId);
        baseService.setCreateAndUpdateUserName(allList);
        return buildExportResult(allList, taskId, exportDTO);
    }

    private List<List<Object>> buildExportResult(List<BusinessTitleListResponse> responses, String taskId, ExportDTO exportDTO) throws InterruptedException {
        var headList = exportDTO.getHeadList();
        var result = new ArrayList<List<Object>>(responses.size());
        for (var response : responses) {
            if (ExportThreadRegistry.isInterrupted(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            result.add(buildData(headList, response));
        }
        return result;
    }

    private List<Object> buildData(List<ExportHeadDTO> headList, BusinessTitleListResponse data) {
        var dataList = new ArrayList<Object>(headList.size());
        var systemFieldMap = getSystemFieldMap(data);
        return transModuleFieldValue(headList, systemFieldMap, new HashMap<>(), dataList, new HashMap<>());

    }

    public LinkedHashMap<String, Object> getSystemFieldMap(BusinessTitleListResponse data) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("identificationNumber", data.getIdentificationNumber());
        systemFieldMap.put("openingBank", data.getOpeningBank());
        systemFieldMap.put("type", Translator.get(data.getType()));
        systemFieldMap.put("bankAccount", data.getBankAccount());
        systemFieldMap.put("registrationAddress", data.getRegistrationAddress());
        systemFieldMap.put("phoneNumber", data.getPhoneNumber());
        systemFieldMap.put("registeredCapital", data.getRegisteredCapital());
        systemFieldMap.put("companySize", data.getCompanySize());
        systemFieldMap.put("registrationNumber", data.getRegistrationNumber());
        systemFieldMap.put("unapprovedReason", data.getUnapprovedReason());

        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        return systemFieldMap;
    }


    /**
     * 构建导出的数据
     *
     * @return 导出数据列表
     */
    @Override
    public List<List<Object>> getExportData(String taskId, ExportDTO exportDTO) throws InterruptedException {
        var pageRequest = (BusinessTitlePageRequest) exportDTO.getPageRequest();
        var orgId = exportDTO.getOrgId();
        PageHelper.startPage(pageRequest.getCurrent(), pageRequest.getPageSize());
        var dataList = extBusinessTitleMapper.list(pageRequest, orgId, exportDTO.getUserId());
        baseService.setCreateAndUpdateUserName(dataList);
        return buildExportResult(dataList, taskId, exportDTO);
    }
}
