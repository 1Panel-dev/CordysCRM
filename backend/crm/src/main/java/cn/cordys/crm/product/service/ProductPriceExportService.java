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
import cn.cordys.registry.ExportThreadRegistry;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 价格表导出
 *
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProductPriceExportService extends BaseExportService {

  @Resource private ExtProductPriceMapper extProductPriceMapper;
  @Resource private ProductPriceService productPriceService;
  @Resource private ModuleFormService moduleFormService;

  @Override
  protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam)
      throws InterruptedException {

    List<ProductPriceResponse> exportList = queryExportData(exportParam);
    if (CollectionUtils.isEmpty(exportList)) {
      return emptyMergeResult();
    }

    List<ProductPriceResponse> dataList = productPriceService.buildList(exportList);

    ExportFieldParam exportFieldParam = exportParam.getExportFieldParam();
    Map<String, List<OptionDTO>> optionMap = buildOptionMap(dataList, exportFieldParam);

    return buildMergeResult(taskId, dataList, optionMap, exportParam);
  }

  /** 查询导出数据 */
  private List<ProductPriceResponse> queryExportData(ExportDTO exportParam) {
    if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
      return extProductPriceMapper.selectByIds(exportParam.getSelectIds());
    }
    ProductPricePageRequest request = (ProductPricePageRequest) exportParam.getPageRequest();
    return extProductPriceMapper.list(request, exportParam.getOrgId());
  }

  /** 构建选项映射 */
  private Map<String, List<OptionDTO>> buildOptionMap(
      List<ProductPriceResponse> dataList, ExportFieldParam exportFieldParam) {

    List<BaseModuleFieldValue> moduleFieldValues =
        moduleFormService.getBaseModuleFieldValues(dataList, ProductPriceResponse::getModuleFields);

    return moduleFormService.getOptionMap(exportFieldParam.getFormConfig(), moduleFieldValues);
  }

  /** 构建合并导出结果 */
  private MergeResult buildMergeResult(
      String taskId,
      List<ProductPriceResponse> dataList,
      Map<String, List<OptionDTO>> optionMap,
      ExportDTO exportParam)
      throws InterruptedException {

    List<List<Object>> data = new ArrayList<>();
    List<int[]> mergeRegions = new ArrayList<>();

    int offset = 0;
    ExportFieldParam exportFieldParam = exportParam.getExportFieldParam();
    List<String> mergeHeads = exportParam.getMergeHeads();

    for (ProductPriceResponse response : dataList) {

      checkInterrupted(taskId);

      List<List<Object>> rows = buildData(response, optionMap, exportFieldParam, mergeHeads);

      if (rows.size() > 1) {
        mergeRegions.add(new int[] {offset, offset + rows.size() - 1});
      }

      offset += rows.size();
      data.addAll(rows);
    }

    return MergeResult.builder().dataList(data).mergeRegions(mergeRegions).build();
  }

  private void checkInterrupted(String taskId) throws InterruptedException {
    if (ExportThreadRegistry.isInterrupted(taskId)) {
      throw new InterruptedException("导出中断");
    }
  }

  private MergeResult emptyMergeResult() {
    return MergeResult.builder()
        .dataList(new ArrayList<>())
        .mergeRegions(new ArrayList<>())
        .build();
  }

  private List<List<Object>> buildData(
      ProductPriceResponse detail,
      Map<String, List<OptionDTO>> optionMap,
      ExportFieldParam exportFieldParam,
      List<String> heads) {

    LinkedHashMap<String, Object> systemFieldMap =
        ProductPriceUtils.getSystemFieldMap(detail, optionMap);

    return buildDataWithSub(detail.getModuleFields(), exportFieldParam, heads, systemFieldMap);
  }
}
