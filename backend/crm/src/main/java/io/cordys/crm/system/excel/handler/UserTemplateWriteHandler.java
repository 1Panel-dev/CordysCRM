package io.cordys.crm.system.excel.handler;

import com.alibaba.excel.util.BooleanUtils;
import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.handler.context.RowWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.excel.constants.UserImportFiled;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author wx
 */
public class UserTemplateWriteHandler implements RowWriteHandler {

    private Sheet sheet;
    private Drawing<?> drawingPatriarch;
    private Map<String, Integer> fieldMap = new HashMap<>();

    public UserTemplateWriteHandler(List<List<String>> headList) {
        initIndex(headList);
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

            Iterator<Map.Entry<String, Integer>> iterator = fieldMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> entry = iterator.next();
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

    public static HorizontalCellStyleStrategy getHorizontalWrapStrategy() {
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // 设置自动换行
        contentWriteCellStyle.setWrapped(true);
        return new HorizontalCellStyleStrategy(null, contentWriteCellStyle);
    }
}
