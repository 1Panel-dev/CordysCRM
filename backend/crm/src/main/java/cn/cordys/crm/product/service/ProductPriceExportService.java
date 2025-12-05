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
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 价格表导出
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
	protected MergeResult getExportMergeData(String taskId, ExportDTO exportParam) throws InterruptedException {
		List<ProductPriceResponse> exportList;
		if (CollectionUtils.isNotEmpty(exportParam.getSelectIds())) {
			exportList = extProductPriceMapper.selectByIds(exportParam.getSelectIds());
		} else {
			ProductPricePageRequest request = (ProductPricePageRequest) exportParam.getPageRequest();
			exportList = extProductPriceMapper.list(request, exportParam.getOrgId());
		}
		if (CollectionUtils.isEmpty(exportList)) {
			return MergeResult.builder().dataList(new ArrayList<>()).mergeRegions(new ArrayList<>()).build();
		}
		List<ProductPriceResponse> dataList = productPriceService.buildList(exportList);
		List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(dataList, ProductPriceResponse::getModuleFields);
		ExportFieldParam exportFieldParam = exportParam.getExportFieldParam();
		Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(exportFieldParam.getFormConfig(), moduleFieldValues);
		// 构建导出数据
		List<List<Object>> data = new ArrayList<>();
		List<int[]> mergeRegions = new ArrayList<>();
		int offset = 0;
		for (ProductPriceResponse response : dataList) {
			if (ExportThreadRegistry.isInterrupted(taskId)) {
				throw new InterruptedException("导出中断");
			}
			List<List<Object>> buildData = buildData(response, optionMap, exportFieldParam, exportParam.getMergeHeads());
			if (buildData.size() > 1) {
				// 多行需要合并
				mergeRegions.add(new int[]{offset, offset + buildData.size() - 1});
			}
			offset += buildData.size();
			data.addAll(buildData);
		}
		return MergeResult.builder().mergeRegions(mergeRegions).dataList(data).build();
	}

	@SuppressWarnings("unchecked")
	private List<List<Object>> buildData(ProductPriceResponse detail, Map<String, List<OptionDTO>> optionMap,
										 ExportFieldParam exportFieldParam, List<String> heads) {
		LinkedHashMap<String, Object> systemFieldMap = ProductPriceUtils.getSystemFieldMap(detail, optionMap);
		return buildDataWithSub(detail.getModuleFields(), exportFieldParam, heads, systemFieldMap);
	}
}
