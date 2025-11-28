package cn.cordys.crm.system.excel.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.enums.CellExtraTypeEnum;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.metadata.CellExtra;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预收集合并单元格信息
 * @author song-cc-rock
 */
public class CustomFieldMergeCellEventListener extends AnalysisEventListener<Map<Integer, String>> {

	/**
	 * 头行号
	 */
	private int maxHeadRow;

	/**
	 * 合并单元格信息 (按行号分组, 方便查询)
	 */
	@Getter
	private final Map<Integer, List<CellExtra>> mergeCellMap = new HashMap<>();

	@Override
	public void invoke(Map<Integer, String> integerStringMap, AnalysisContext analysisContext) {

	}

	@Override
	public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
		maxHeadRow = context.readWorkbookHolder().getHeadRowNumber();
	}

	@Override
	public void extra(CellExtra extra, AnalysisContext context) {
		if (extra.getType() == CellExtraTypeEnum.MERGE && extra.getRowIndex() >= maxHeadRow) {
			for (int row = extra.getFirstRowIndex(); row <= extra.getLastRowIndex(); row++) {
				mergeCellMap.computeIfAbsent(row, k -> new ArrayList<>()).add(extra);
			}
		}
	}

	@Override
	public void doAfterAllAnalysed(AnalysisContext analysisContext) {

	}
}
