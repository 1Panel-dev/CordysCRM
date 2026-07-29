package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.FieldExportMeta;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.contract.dto.request.ContractInvoicePageRequest;
import cn.cordys.crm.contract.dto.response.ContractInvoiceListResponse;
import cn.cordys.crm.contract.mapper.ExtContractInvoiceMapper;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ContractInvoiceExportService extends BaseExportService {

    @Resource
    private ContractInvoiceService contractInvoiceService;
    @Resource
    private ExtContractInvoiceMapper extContractInvoiceMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;

    /**
     * 构建导出的数据
     *
     * @return 导出数据列表
     */
    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var queryResult = collectExportList(exportParam);
        var filteredList = queryResult.getLeft();
        var queryCount = queryResult.getRight();
        if (CollectionUtils.isEmpty(filteredList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).queryCount(queryCount).build();
        }
        var dataList = contractInvoiceService.buildList(filteredList, exportParam.getOrgId());
        var result = buildExportMergeResult(taskId, exportParam, dataList,
                ContractInvoiceListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        getSystemFieldMap(detail, metas), cache));
        result.setQueryCount(queryCount);
        return result;
    }

    private Pair<List<ContractInvoiceListResponse>, Integer> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        List<ContractInvoiceListResponse> exportList;
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            exportList = extContractInvoiceMapper.getListByIds(exportParam.getSelectIds(), userId, orgId, deptDataPermission);
            return Pair.of(exportList, exportList.size());
        } else {
            var request = (ContractInvoicePageRequest) exportParam.getPageRequest();
            PageHelper.startPage(request.getCurrent(), request.getPageSize(), false);
            exportList = extContractInvoiceMapper.list(request, orgId, userId, deptDataPermission);
            int queryCount = exportList.size();
            var filtered = filterExportPermission(exportList, orgId);
            return Pair.of(filtered, queryCount);
        }
    }

    public LinkedHashMap<String, Object> getSystemFieldMap(ContractInvoiceListResponse data, List<FieldExportMeta> exportMetas) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("contractId", data.getContractName());
        systemFieldMap.put("id", data.getId());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("departmentId", data.getDepartmentName());
        systemFieldMap.put("amount", data.getAmount());

        Map<String, FieldExportMeta> metaMap = exportMetas.stream()
                .collect(Collectors.toMap(FieldExportMeta::getBusinessKey, Function.identity(), (a, b) -> a));
        FieldExportMeta taxRate = metaMap.values().stream().filter(meta -> Strings.CI.equals(meta.getBusinessKey(), BusinessModuleField.INVOICE_TAX_RATE.getBusinessKey())).findFirst().orElse(null);
        if (taxRate != null && taxRate.getField() != null && data.getTaxRate() != null) {
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(taxRate.getField().getType());
            systemFieldMap.put("taxRate", customFieldResolver.transformToValue(taxRate.getField(), data.getTaxRate().stripTrailingZeros().toPlainString()));
        }
        systemFieldMap.put("businessTitleId", data.getBusinessTitleName());
        systemFieldMap.put("approvalStatus", data.getApprovalStatus() == null ? null : Translator.get("contract.approval_status." + data.getApprovalStatus().toLowerCase()));


        FieldExportMeta invoiceType = metaMap.values().stream().filter(meta -> Strings.CI.equals(meta.getBusinessKey(), BusinessModuleField.INVOICE_INVOICE_TYPE.getBusinessKey())).findFirst().orElse(null);
        if (invoiceType != null && invoiceType.getField() != null
                && invoiceType.getField() instanceof SelectField invoiceTypeField) {
            String invoiceTypeName = getOptionLabel(data.getInvoiceType(), invoiceTypeField.getOptions());
            systemFieldMap.put("invoiceType", invoiceTypeName);
        }

        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        return systemFieldMap;
    }

    /**
     * 根据审批流状态权限过滤可导出的数据
     *
     * @param exportList 原始导出数据列表
     * @param orgId      组织ID
     * @return 过滤后可导出的数据列表
     */
    private List<ContractInvoiceListResponse> filterExportPermission(List<ContractInvoiceListResponse> exportList, String orgId) {
        return filterApprovalExportPermission(exportList, orgId, FormKey.INVOICE.getKey(),
                ContractInvoiceListResponse::getId, ContractInvoiceListResponse::getApprovalStatus,
                approvalFlowService);
    }
}
