package cn.cordys.crm.system.excel.handler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;

/**
 * 动态合并单元格策略
 * @author song-cc-rock
 */
public record MergeHandler(List<int[]> mergeRegions, List<Integer> mergeColumns, int offset) {

	public void merge(Sheet sheet) {
		if (CollectionUtils.isEmpty(mergeRegions) || CollectionUtils.isEmpty(mergeColumns)) {
			return;
		}

		for (int[] region : mergeRegions) {
			int start = region[0] + offset;
			int end = region[1] + offset;
			for (Integer colIndex : mergeColumns) {
				sheet.addMergedRegionUnsafe(new CellRangeAddress(start, end, colIndex, colIndex));
			}
		}
	}
}
