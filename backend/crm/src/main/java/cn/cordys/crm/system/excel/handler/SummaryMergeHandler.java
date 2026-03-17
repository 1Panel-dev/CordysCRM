package cn.cordys.crm.system.excel.handler;

import cn.cordys.crm.system.dto.field.InputNumberField;
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
public record SummaryMergeHandler(List<int[]> mergeRegions, List<Integer> mergeColumns, List<Integer> summaryCols, int offset) {

	public void merge(Sheet sheet) {
		if (CollectionUtils.isEmpty(mergeRegions) || CollectionUtils.isEmpty(mergeColumns)) {
			return;
		}

		// 合并行区域内的汇总列的值
		for (int[] region : mergeRegions) {
			int start = region[0] + offset;
			int end = region[1] + offset;
			for (Integer colIndex : summaryCols) {
				// 合计
				BigDecimal total = BigDecimal.ZERO;
				boolean showThousandsSeparator = false;
				boolean hasPercent = false;
				int decimalPlaces = 0;
				for (int r = start; r <= end; r++) {
					Cell cell = sheet.getRow(r).getCell(colIndex);
					if (cell != null) {
						String val = switch (cell.getCellType()) {
							case STRING -> cell.getStringCellValue();
							case NUMERIC -> String.valueOf(cell.getNumericCellValue());
							default -> null;
						};
						if (StringUtils.isEmpty(val)) {
							continue;
						}
						// 记住汇总列格式
						if (val.contains(",") && !showThousandsSeparator) {
							showThousandsSeparator = true;
						}
						if (val.contains("%") && !hasPercent) {
							hasPercent = true;
						}
						val = val.replace(",", StringUtils.EMPTY).replace("%", StringUtils.EMPTY);
						if (NumberUtils.isParsable(val)) {
							if (decimalPlaces == 0 && val.contains(".")) {
								decimalPlaces = val.length() - val.indexOf(".") - 1;
							}
							total = total.add(new BigDecimal(val));
						}
					}
				}
				// 只写入第一行, 清空其他行的汇总值 (不同值合并展示有误)
				Cell sumCell = sheet.getRow(start).getCell(colIndex);
				sumCell.setCellValue(InputNumberField.formatNumber(total, decimalPlaces, showThousandsSeparator, hasPercent));
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
