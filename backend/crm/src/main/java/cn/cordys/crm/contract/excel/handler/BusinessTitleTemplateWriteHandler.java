package cn.cordys.crm.contract.excel.handler;

import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.excel.constants.BusinessTitleImportFiled;
import cn.idev.excel.util.BooleanUtils;
import cn.idev.excel.write.handler.RowWriteHandler;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.handler.context.RowWriteHandlerContext;
import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BusinessTitleTemplateWriteHandler implements RowWriteHandler, SheetWriteHandler {

    private final Map<String, Integer> fieldMap = new HashMap<>();
    private Sheet sheet;
    private Drawing<?> drawingPatriarch;

    private final Map<String, Boolean> requiredMap;

    public BusinessTitleTemplateWriteHandler(List<List<String>> headList, Map<String, Boolean> requiredMap) {
        initIndex(headList);
        this.requiredMap = requiredMap;
    }

    public static HorizontalCellStyleStrategy getHorizontalWrapStrategy() {
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // 设置自动换行
        contentWriteCellStyle.setWrapped(true);
        return new HorizontalCellStyleStrategy(null, contentWriteCellStyle);
    }

    private void initIndex(List<List<String>> headList) {
        int index = 0;
        for (List<String> list : headList) {
            for (String head : list) {
                this.fieldMap.put(head, index);
                index++;
            }
        }
    }

    @Override
    public void afterRowDispose(RowWriteHandlerContext context) {
        if (BooleanUtils.isTrue(context.getHead())) {
            sheet = context.getWriteSheetHolder().getSheet();
            drawingPatriarch = sheet.createDrawingPatriarch();

            for (Map.Entry<String, Integer> entry : fieldMap.entrySet()) {
                String key = BusinessTitleImportFiled.fromHeader(entry.getKey()).name().toLowerCase();
                if (requiredMap.containsKey(key)&&requiredMap.get(key)) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("required"));
                }
            }
        }

    }

    private void setComment(Integer index, String text) {
        if (index == null) {
            return;
        }
        Comment comment = drawingPatriarch.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, index, 0, index + 3, 1));
        comment.setString(new XSSFRichTextString(text));
        sheet.getRow(0).getCell(0).setCellComment(comment);
    }
}
