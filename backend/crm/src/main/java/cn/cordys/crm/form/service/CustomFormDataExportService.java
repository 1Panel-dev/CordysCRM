package cn.cordys.crm.form.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.request.CustomFormExportSelectRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.mapper.ExtCustomFormDataMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CustomFormDataExportService extends BaseExportService {

    @Resource
    private CustomFormDataService customFormDataService;
    @Resource
    private ExtCustomFormDataMapper extCustomFormDataMapper;

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        String customFormId = exportList.getFirst().getCustomFormId();
        CustomFormDataFieldService.setFormKey(customFormId);
        var dataList = customFormDataService.buildList(exportList, customFormId, exportParam.getOrgId());
        return buildExportMergeResult(taskId, exportParam, dataList,
                CustomFormDataListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        getSystemFieldMap(detail, exportParam.getLocale()), cache));
    }


    private List<CustomFormDataListResponse> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            var request = (CustomFormExportSelectRequest) exportParam.getSelectRequest();
            CustomFormRoleKey dataScope = customFormDataService.getDataScope(request.getCustomFormId(), userId, orgId);
            boolean manageOwn = dataScope == CustomFormRoleKey.MANAGE_OWN;
            return extCustomFormDataMapper.getListByIds(exportParam.getSelectIds(), orgId, userId, manageOwn);
        } else {
            var request = (CustomFormDataPageRequest) exportParam.getPageRequest();
            CustomFormRoleKey dataScope = customFormDataService.getDataScope(request.getCustomFormId(), userId, orgId);
            boolean manageOwn = dataScope == CustomFormRoleKey.MANAGE_OWN;
            PageHelper.startPage(request.getCurrent(), request.getPageSize(), false);
            return extCustomFormDataMapper.list(request, orgId, userId, manageOwn);
        }
    }

    private LinkedHashMap<String, Object> getSystemFieldMap(CustomFormDataListResponse data, Locale locale) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("id", data.getId());
        systemFieldMap.put("owner", data.getOwnerName());
        if (StringUtils.isNotBlank(data.getApprovalStatus())) {
            systemFieldMap.put("approvalStatus", Translator.get("contract.approval_status." + data.getApprovalStatus().toLowerCase(), locale));
        }
        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        return systemFieldMap;
    }
}
