package cn.cordys.crm.system.excel.handler;

import cn.cordys.common.resolver.field.DateTimeResolver;
import cn.cordys.common.resolver.field.LocationResolver;
import cn.cordys.common.resolver.field.NumberResolver;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.dto.field.DateTimeField;
import cn.cordys.crm.system.dto.field.InputNumberField;
import cn.cordys.crm.system.dto.field.LocationField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.idev.excel.metadata.Head;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
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

    private final Map<BaseField, Integer> fieldIndexMap = new HashMap<>();
    private final List<String> requires = new ArrayList<>();
    private final List<String> uniques = new ArrayList<>();
    private final List<String> multiples = new ArrayList<>();
    private final Map<Integer, List<String>> validationOptionMap = new HashMap<>();
    private final int totalColumns;
    private Sheet mainSheet;
    private Drawing<?> drawingPatriarch;

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
            if (field instanceof HasOption optionField && CollectionUtils.isNotEmpty(optionField.getOptions())) {
                // set options for data validation
                validationOptionMap.put(index, optionField.getOptions().stream().map(OptionProp::getLabel).toList());
            }
        }
        totalColumns = index;
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        if (Strings.CS.equals(sheet.getSheetName(), Translator.get(SheetKey.COMMENT))) {
            Row row1 = sheet.createRow(0);
            row1.setHeightInPoints(80);
            Cell cell1 = row1.createCell(0);
            cell1.setCellValue(Translator.get("sheet.instruction"));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalColumns - 1));
            CellStyle style = writeWorkbookHolder.getWorkbook().createCellStyle();
            style.setWrapText(true);
            Font font = writeWorkbookHolder.getWorkbook().createFont();
            font.setFontHeightInPoints((short) 12);
            style.setFont(font);
            cell1.setCellStyle(style);
        }
        if (Strings.CS.equals(sheet.getSheetName(), Translator.get(SheetKey.DATA))) {
            // set data validation
            DataValidationHelper dataValidationHelper = sheet.getDataValidationHelper();
            validationOptionMap.forEach((k, v) -> {
                DataValidationConstraint dvc = dataValidationHelper.createExplicitListConstraint(v.toArray(String[]::new));
                DataValidation dataValidation = dataValidationHelper.createValidation(dvc, new CellRangeAddressList(1, 1048575, k - 1, k - 1));
                sheet.addValidationData(dataValidation);
            });
        }
    }

    @Override
    public void afterRowDispose(RowWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        if (BooleanUtils.isTrue(context.getHead()) && Strings.CS.equals(sheet.getSheetName(), Translator.get(SheetKey.DATA))) {
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
        if (isHead) {
            if (requires.contains(cell.getStringCellValue())) {
                font.setColor(IndexedColors.RED.getIndex());
            }
            if (uniques.contains(cell.getStringCellValue())) {
                font.setBold(true);
            }
            if (multiples.contains(cell.getStringCellValue())) {
                font.setUnderline(Font.U_SINGLE);
            }
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
        if (Strings.CS.equals(numberField.getNumberFormat(), NumberResolver.PERCENT_FORMAT)) {
            sb.append(Translator.get("format.preview")).append(": 99.9999%, ").append(Translator.get("keep.decimal.places"));
        } else {
            sb.append(Translator.get("format.preview")).append(": 9999999999.9, ").append(Translator.get("keep.decimal.places"));
        }
        return sb.toString();
    }

    private String getDateTimeComment(BaseField field) {
        StringBuilder sb = new StringBuilder();
        DateTimeField dateTimeField = (DateTimeField) field;
        if (Strings.CS.equals(dateTimeField.getDateType(), DateTimeResolver.DATETIME)) {
            sb.append(Translator.get("format.preview")).append(": 2025/8/6 20:23:59");
        } else if (Strings.CS.equals(dateTimeField.getDateType(), DateTimeResolver.DATE)) {
            sb.append(Translator.get("format.preview")).append(": 2025/8/6");
        } else {
            sb.append(Translator.get("format.preview")).append(": 2025/8");
        }
        return sb.toString();
    }

    private String getLocationComment(BaseField field) {
        StringBuilder sb = new StringBuilder();
        LocationField locationField = (LocationField) field;
        if (Strings.CS.equals(locationField.getLocationType(), LocationResolver.PC)) {
            sb.append(Translator.get("format.preview")).append(": ").append(Translator.get("location.pc"));
        } else if (Strings.CS.equals(locationField.getLocationType(), LocationResolver.PCD)) {
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
