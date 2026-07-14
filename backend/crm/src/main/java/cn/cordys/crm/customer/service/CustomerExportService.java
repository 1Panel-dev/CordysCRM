package cn.cordys.crm.customer.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.utils.PoolCustomerFieldUtils;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CustomerExportService extends BaseExportService {

    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private CustomerService customerService;

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        // 构建自定义字段数据
        var dataList = customerService.buildListData(exportList, exportParam.getOrgId());
        return buildExportMergeResult(taskId, exportParam, dataList,
                CustomerListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        PoolCustomerFieldUtils.getSystemFieldMap(detail), cache));
    }

    private List<CustomerListResponse> collectExportList(ExportDTO exportParam) {
        var orgId = exportParam.getOrgId();
        var userId = exportParam.getUserId();
        var deptDataPermission = exportParam.getDeptDataPermission();
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extCustomerMapper.getListByIds(exportParam.getSelectIds());
        }
        var request = (CustomerPageRequest) exportParam.getPageRequest();
        PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return extCustomerMapper.list(request, orgId, userId, deptDataPermission);
    }
}
