package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.FieldExportMeta;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import cn.cordys.crm.system.service.ModuleFieldExtService;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        var dataList = contractPaymentRecordService.buildListExtra(exportList, exportParam.getOrgId());
        return buildExportMergeResult(taskId, exportParam, dataList,
                ContractPaymentRecordResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        getSystemFieldMap(detail, metas, exportParam.getLocale()), cache));
    }


    private List<ContractPaymentRecordResponse> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extContractPaymentRecordMapper.getListByIds(exportParam.getSelectIds(), userId, orgId, deptDataPermission);
        }
        var request = (ContractPaymentRecordPageRequest) exportParam.getPageRequest();
        PageHelper.startPage(request.getCurrent(), request.getPageSize(), false);
        return extContractPaymentRecordMapper.list(request, orgId, userId, deptDataPermission);
    }


    public LinkedHashMap<String, Object> getSystemFieldMap(ContractPaymentRecordResponse data, List<FieldExportMeta> exportMetas, Locale locale) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("id", data.getId());
        systemFieldMap.put("no", data.getNo());
        systemFieldMap.put("contractId", data.getContractName());
        systemFieldMap.put("paymentPlanId", data.getPaymentPlanName());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("departmentId", data.getDepartmentName());
        Map<String, FieldExportMeta> metaMap = exportMetas.stream()
                .collect(Collectors.toMap(FieldExportMeta::getBusinessKey, Function.identity(), (a, b) -> a));
        resolveAndPutTimeField(systemFieldMap, metaMap, "recordAmount", data.getRecordAmount());

        systemFieldMap.put("recordEndTime", getInternalDateStr(data.getRecordEndTime(), FormKey.CONTRACT_PAYMENT_RECORD.getKey(),
                data.getOrganizationId(), BusinessModuleField.CONTRACT_PAYMENT_RECORD_END_TIME.getKey()));

        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        return systemFieldMap;
    }

    /**
     * 获取内部日期字符串
     *
     * @param timestamp   毫秒
     * @param formKey     表单Key
     * @param orgId       组织ID
     * @param internalKey 内部Key
     * @return 日期字符串
     */
    private String getInternalDateStr(Long timestamp, String formKey, String orgId, String internalKey) {
        String dateType = moduleFieldExtService.getDateFieldType(formKey, orgId, internalKey);
        return switch (dateType) {
            case "date" -> TimeUtils.getDateStr(timestamp);
            case "month" -> TimeUtils.getMonthStr(timestamp);
            default -> TimeUtils.getDateTimeStr(timestamp);
        };
    }
}
