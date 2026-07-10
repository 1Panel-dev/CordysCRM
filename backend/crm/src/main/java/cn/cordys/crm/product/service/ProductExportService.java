package cn.cordys.crm.product.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportFieldParam;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.crm.product.dto.request.ProductPageRequest;
import cn.cordys.crm.product.dto.response.ProductListResponse;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.product.utils.ProductUtils;
import cn.cordys.crm.system.excel.domain.MergeResult;
import cn.cordys.crm.system.service.ModuleFormService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Service
@Transactional(rollbackFor = Exception.class)
public class ProductExportService extends BaseExportService {

    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private ProductService productService;
    @Resource
    private ModuleFormService moduleFormService;

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        var dataList = productService.buildListData(exportList);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(dataList, exportParam.getExportFieldParam());
        return buildExportMergeResult(taskId, exportParam, dataList,
                ProductListResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        ProductUtils.getSystemFieldMap(detail, optionMap), cache));
    }

    private List<ProductListResponse> collectExportList(ExportDTO exportParam) {
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extProductMapper.selectByIds(exportParam.getSelectIds());
        }
        var request = (ProductPageRequest) exportParam.getPageRequest();
        return extProductMapper.list(request, exportParam.getOrgId());
    }

    private Map<String, List<OptionDTO>> buildOptionMap(List<ProductListResponse> dataList,
                                                        ExportFieldParam exportFieldParam) {
        List<BaseModuleFieldValue> moduleFieldValues =
                moduleFormService.getBaseModuleFieldValues(dataList, ProductListResponse::getModuleFields);
        return moduleFormService.getOptionMap(exportFieldParam.getFormConfig(), moduleFieldValues);
    }
}
