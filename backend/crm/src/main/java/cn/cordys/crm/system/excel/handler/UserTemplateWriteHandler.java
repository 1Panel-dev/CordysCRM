package cn.cordys.crm.system.excel.handler;

import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.excel.constants.UserImportFiled;
import cn.idev.excel.util.BooleanUtils;
import cn.idev.excel.write.handler.RowWriteHandler;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.handler.context.RowWriteHandlerContext;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author wx
 */
public class UserTemplateWriteHandler implements RowWriteHandler, SheetWriteHandler {

    private final Map<String, Integer> fieldMap = new HashMap<>();
    private Sheet sheet;
    private Drawing<?> drawingPatriarch;
    private int totalColumns; // 记录总列数

    public UserTemplateWriteHandler(List<List<String>> headList) {
        initIndex(headList);
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
        this.totalColumns = index; // 初始化总列数
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        sheet = writeSheetHolder.getSheet();

        // 创建第二行并合并单元格
        Row row1 = sheet.createRow(1);
        Cell cell1 = row1.createCell(0);
        cell1.setCellValue("""
                第二行说明文字：
                1、不能再Excel中对成员信息类别进行增加、修改、删除
                2、上下级部门间用‘/’隔开，且从最上级部门开始，例如"XX公司/XX事业部/研发部\"""");
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, totalColumns - 1));
    }

    @Override
    public void afterRowDispose(RowWriteHandlerContext context) {
        if (BooleanUtils.isTrue(context.getHead())) {
            sheet = context.getWriteSheetHolder().getSheet();
            drawingPatriarch = sheet.createDrawingPatriarch();

            for (Map.Entry<String, Integer> entry : fieldMap.entrySet()) {
                //默认字段
                if (UserImportFiled.NAME.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("required"));
                }
                if (UserImportFiled.DEPARTMENT.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("department_comment"));
                }
                if (UserImportFiled.PHONE.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("required"));
                }
                if (UserImportFiled.EMAIL.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("required"));
                }
                if (UserImportFiled.EMPLOYEE_TYPE.containsHead(entry.getKey())) {
                    setComment(fieldMap.get(entry.getKey()), Translator.get("employee_type_comment"));
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
