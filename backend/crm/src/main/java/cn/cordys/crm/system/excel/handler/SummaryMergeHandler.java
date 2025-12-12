package cn.cordys.crm.system.excel.handler;

import cn.cordys.common.util.Translator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.util.List;

/**
 * 动态合并单元格策略(汇总)
 * @author song-cc-rock
 */
public record SummaryMergeHandler(List<int[]> mergeRegions, List<Integer> mergeColumns, int offset) {

	public void merge(Sheet sheet) {
		if (CollectionUtils.isEmpty(mergeRegions) || CollectionUtils.isEmpty(mergeColumns)) {
			return;
		}

		// 筛选汇总列
		Row headRow = sheet.getRow(offset - 1);
		if (headRow != null) {
			List<Integer> summaryCols = mergeColumns.stream().filter(col -> {
				Cell cell = headRow.getCell(col);
				if (cell == null) {
					return false;
				}
				String head = cell.getStringCellValue();
				return head != null && head.startsWith(Translator.get("sum"));
			}).toList();
			// 合并行区域内的汇总列的值
			for (int[] region : mergeRegions) {
				int start = region[0] + offset;
				int end = region[1] + offset;
				for (Integer colIndex : summaryCols) {
					// 合计
					BigDecimal total = BigDecimal.ZERO;
					for (int r = start; r <= end; r++) {
						Cell cell = sheet.getRow(r).getCell(colIndex);
						if (cell != null) {
							String val = String.valueOf(cell.getNumericCellValue());
							if (StringUtils.isNotEmpty(val) && NumberUtils.isParsable(val)) {
								total = total.add(new BigDecimal(val));
							}
						}
					}
					// 只写入第一行, 清空其他行的汇总值 (不同值合并展示有误)
					Cell sumCell = sheet.getRow(start).getCell(colIndex);
					sumCell.setCellValue(total.toPlainString());
					Workbook wb = sheet.getWorkbook();
					CellStyle style = wb.createCellStyle();
					style.setAlignment(HorizontalAlignment.RIGHT);
					style.setVerticalAlignment(VerticalAlignment.CENTER);
					sumCell.setCellStyle(style);
					for (int r = start + 1; r <= end; r++) {
						sheet.getRow(r).getCell(colIndex).setCellValue("");
					}
				}
			}
		}

		// 合并单元格
		for (int[] region : mergeRegions) {
			int start = region[0] + offset;
			int end = region[1] + offset;
			for (Integer colIndex : mergeColumns) {
				sheet.addMergedRegionUnsafe(new CellRangeAddress(start, end, colIndex, colIndex));
			}
		}
	}
}
