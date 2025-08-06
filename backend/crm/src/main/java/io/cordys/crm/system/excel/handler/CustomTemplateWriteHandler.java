package io.cordys.crm.system.excel.handler;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.metadata.data.DataFormatData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.util.BooleanUtils;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.handler.RowWriteHandler;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.handler.context.RowWriteHandlerContext;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteTableHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.metadata.style.WriteFont;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.dto.field.DateTimeField;
import io.cordys.crm.system.dto.field.InputNumberField;
import io.cordys.crm.system.dto.field.LocationField;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.field.base.HasOption;
import io.cordys.crm.system.dto.field.base.OptionProp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义模板处理器
 */
public class CustomTemplateWriteHandler implements RowWriteHandler, SheetWriteHandler, CellWriteHandler {

	private Sheet mainSheet;
	private Drawing<?> drawingPatriarch;
	private final Map<BaseField, Integer> fieldIndexMap = new HashMap<>();
	private final List<String> requires = new ArrayList<>();
	private final List<String> uniques = new ArrayList<>();
	private final List<String> multiples = new ArrayList<>();
	private final List<String> dateHeads = new ArrayList<>();
	private final int totalColumns;

	public CustomTemplateWriteHandler(List<BaseField> fields) {
		int index = 0;
		for (BaseField field : fields) {
			fieldIndexMap.put(field, index++);
			if (field.needRequireCheck()) {
				requires.add(field.getName());
			}
			if (field.needRepeatCheck()) {
				uniques.add(field.getName());
			}
			if (field.multiple()) {
				multiples.add(field.getName());
			}
		}
		totalColumns = index;
	}

	@Override
	public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
		Sheet sheet = writeSheetHolder.getSheet();
		if (StringUtils.equals(sheet.getSheetName(), Translator.get("sheet.comment"))) {
			Row row1 = sheet.createRow(0);
			row1.setHeightInPoints(80);
			Cell cell1 = row1.createCell(0);
			cell1.setCellValue("""
                说明：
                1、表头字体红色表示 "必填", 加粗表示 "唯一", 下划线表示支持 "多值"。
                2、多值采用以下格式: [标签1, 标签2], 其余格式请参考表头注释。
                3、导入时会过滤格式错误或非法字符。
                """);
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalColumns - 1));
			CellStyle style = writeWorkbookHolder.getWorkbook().createCellStyle();
			style.setWrapText(true);
			Font font = writeWorkbookHolder.getWorkbook().createFont();
			font.setFontHeightInPoints((short) 12);
			style.setFont(font);
			cell1.setCellStyle(style);
		}
	}

	@Override
	public void afterRowDispose(RowWriteHandlerContext context) {
		Sheet sheet = context.getWriteSheetHolder().getSheet();
		if (BooleanUtils.isTrue(context.getHead()) && StringUtils.equals(sheet.getSheetName(), Translator.get("sheet.data"))) {
			mainSheet = sheet;
			drawingPatriarch = sheet.createDrawingPatriarch();
			fieldIndexMap.forEach((k, v) -> {
				if (k.needComment()) {
					setComment(v, buildComment(k));
				}
			});
		}
	}

	@Override
	public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
								 List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
		// 设置样式
		WriteCellStyle headStyle = new WriteCellStyle();
		WriteFont font = new WriteFont();
		if (isHead && requires.contains(cell.getStringCellValue())) {
			font.setColor(IndexedColors.RED.getIndex());
		}
		if (isHead && uniques.contains(cell.getStringCellValue())) {
			font.setBold(true);
		}
		if (isHead && multiples.contains(cell.getStringCellValue())) {
			font.setUnderline(Font.U_SINGLE);
		}
		font.setFontHeightInPoints((short) 12);
		headStyle.setWriteFont(font);
		if (CollectionUtils.isNotEmpty(cellDataList)) {
			WriteCellData<?> writeCellData = cellDataList.getFirst();
			writeCellData.setWriteCellStyle(headStyle);
		}
	}

	private void setComment(Integer index, String text) {
		if (index == null) {
			return;
		}
		Comment comment = drawingPatriarch.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, index, 0, index + 3, 1));
		comment.setString(new XSSFRichTextString(text));
		mainSheet.getRow(0).getCell(0).setCellComment(comment);
	}

	private String buildComment(BaseField field) {
		return switch (field.getType()) {
			case "INPUT_NUMBER" -> getNumberComment(field);
			case "DATE_TIME" -> getDateTimeComment(field);
			case "LOCATION" -> getLocationComment(field);
			case "PHONE" -> getPhoneComment();
			default -> getOptionComment(field);
		};
	}

	private String getNumberComment(BaseField field) {
		StringBuilder sb = new StringBuilder();
		InputNumberField numberField = (InputNumberField) field;
		if (StringUtils.equals(numberField.getNumberFormat(), "percent")) {
			sb.append(Translator.get("format.preview")).append(": 99%, ");
		} else {
			sb.append(Translator.get("format.preview")).append(": 999, ");
		}
		if (numberField.getDecimalPlaces()) {
			sb.append(Translator.getWithArgs("keep.decimal.places", numberField.getPrecision()));
		}
		return sb.toString();
	}

	private String getDateTimeComment(BaseField field) {
		StringBuilder sb = new StringBuilder();
		DateTimeField dateTimeField = (DateTimeField) field;
		if (StringUtils.equals(dateTimeField.getDateType(), "datetime")) {
			sb.append(Translator.get("format.preview")).append(": 2025-08-04 18:47:59");
		} else if (StringUtils.equals(dateTimeField.getDateType(), "date")) {
			sb.append(Translator.get("format.preview")).append(": 2025-08-04");
		} else {
			sb.append(Translator.get("format.preview")).append(": 2025-08");
		}
		return sb.toString();
	}

	private String getLocationComment(BaseField field) {
		StringBuilder sb = new StringBuilder();
		LocationField locationField = (LocationField) field;
		if (StringUtils.equals(locationField.getLocationType(), "PC")) {
			sb.append(Translator.get("format.preview")).append(": ").append(Translator.get("location.pc"));
		} else if (StringUtils.equals(locationField.getLocationType(), "PCD")) {
			sb.append(Translator.get("format.preview")).append(": ").append(Translator.get("location.pcd"));
		} else {
			sb.append(Translator.get("format.preview")).append(": ").append(Translator.get("location.pcd.detail"));
		}
		return sb.toString();
	}

	private String getPhoneComment() {
		return Translator.get("phone.tips") + "\n" + Translator.get("format.preview") + ": (+86)12345678901";
	}

	private String getOptionComment(BaseField field) {
		if (field instanceof HasOption) {
			List<OptionProp> options = ((HasOption) field).getOptions();
			return Translator.get("option") + ": " + options.stream().map(OptionProp::getLabel).toList();
		}
		return null;
	}
}
