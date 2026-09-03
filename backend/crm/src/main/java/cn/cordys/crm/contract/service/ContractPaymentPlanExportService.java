package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.FieldExportMeta;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        var dataList = contractPaymentPlanService.buildListData(exportList, exportParam.getOrgId());
        return buildExportMergeResult(taskId, exportParam, dataList,
                ContractPaymentPlanListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        getSystemFieldMap(detail, metas, exportParam.getLocale()), cache));
    }

    private List<ContractPaymentPlanListResponse> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extContractPaymentPlanMapper.getListByIds(exportParam.getSelectIds(), userId, orgId, deptDataPermission);
        }
        var request = (ContractPaymentPlanPageRequest) exportParam.getPageRequest();
        PageHelper.startPage(request.getCurrent(), request.getPageSize(), false);
        return extContractPaymentPlanMapper.list(request, orgId, userId, deptDataPermission);
    }


    public LinkedHashMap<String, Object> getSystemFieldMap(ContractPaymentPlanListResponse data, List<FieldExportMeta> exportMetas, Locale locale) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("id", data.getId());
        systemFieldMap.put("contractId", data.getContractName());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("departmentId", data.getDepartmentName());
        Map<String, FieldExportMeta> metaMap = exportMetas.stream()
                .collect(Collectors.toMap(FieldExportMeta::getBusinessKey, Function.identity(), (a, b) -> a));
        resolveAndPutTimeField(systemFieldMap, metaMap, "planAmount", data.getPlanAmount());

        FieldExportMeta planEndTime = metaMap.values().stream().filter(meta -> Strings.CI.equals(meta.getBusinessKey(), BusinessModuleField.CONTRACT_PAYMENT_PLAN_PLAN_END_TIME.getBusinessKey())).findFirst().orElse(null);
        if (planEndTime != null) {
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(planEndTime.getField().getType());
            systemFieldMap.put("planEndTime", customFieldResolver.transformToValue(planEndTime.getField(), String.valueOf(data.getPlanEndTime())));
        }
        systemFieldMap.put("planStatus", Translator.get("contract.payment_plan.status." + data.getPlanStatus().toLowerCase(), locale));

        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        return systemFieldMap;
    }
}
