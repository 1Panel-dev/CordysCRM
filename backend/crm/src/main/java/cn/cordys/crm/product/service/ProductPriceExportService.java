package cn.cordys.crm.product.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportFieldParam;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;
import cn.cordys.crm.product.mapper.ExtProductPriceMapper;
import cn.cordys.crm.product.utils.ProductPriceUtils;
import cn.cordys.crm.system.excel.domain.MergeResult;
import cn.cordys.crm.system.service.ModuleFormService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 价格表导出
 *
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProductPriceExportService extends BaseExportService {

    @Resource
    private ExtProductPriceMapper extProductPriceMapper;
    @Resource
    private ProductPriceService productPriceService;
    @Resource
    private ModuleFormService moduleFormService;

    @Override
    protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) {
        var exportList = collectExportList(exportParam);
        if (CollectionUtils.isEmpty(exportList)) {
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(0).build();
        }
        var dataList = productPriceService.buildList(exportList);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(dataList, exportParam.getExportFieldParam());
        return buildExportMergeResult(taskId, exportParam, dataList,
                ProductPriceResponse::getModuleFields,
                (detail, fieldParam, metas, cache) -> buildDataWithSub(detail.getModuleFields(), fieldParam, metas,
                        ProductPriceUtils.getSystemFieldMap(detail, optionMap), cache));
    }

    private List<ProductPriceResponse> collectExportList(ExportDTO exportParam) {
        if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
            return extProductPriceMapper.selectByIds(exportParam.getSelectIds());
        }
        var request = (ProductPricePageRequest) exportParam.getPageRequest();
        return extProductPriceMapper.list(request, exportParam.getOrgId());
    }

    private Map<String, List<OptionDTO>> buildOptionMap(List<ProductPriceResponse> dataList,
                                                        ExportFieldParam exportFieldParam) {
        List<BaseModuleFieldValue> moduleFieldValues =
                moduleFormService.getBaseModuleFieldValues(dataList, ProductPriceResponse::getModuleFields);
        return moduleFormService.getOptionMap(exportFieldParam.getFormConfig(), moduleFieldValues);
    }
}
