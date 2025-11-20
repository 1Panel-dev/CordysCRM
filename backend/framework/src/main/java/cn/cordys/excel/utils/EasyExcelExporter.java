package cn.cordys.excel.utils;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.LogUtils;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.handler.WriteHandler;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EasyExcelExporter {
    public void buildExportResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + ".xlsx");
    }

    public void exportByCustomWriteHandler(HttpServletResponse response, List<List<String>> headList, List<List<Object>> data,
                                           String fileName, String sheetName, WriteHandler writeHandler) {
        buildExportResponse(response, fileName);
        try {
            EasyExcel.write(response.getOutputStream())
                    .head(Optional.ofNullable(headList).orElse(new ArrayList<>()))
                    .registerWriteHandler(writeHandler)
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (IOException e) {
            LogUtils.error(e);
            throw new GenericException(e.getMessage());
        }
    }

    public void exportMultiSheetTplWithSharedHandler(HttpServletResponse response, List<List<String>> headList, String fileName,
                                                     String mainSheetName, String tipSheetName, WriteHandler sharedHandler, WriteHandler styleHandler) {
        buildExportResponse(response, fileName);
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream()).build()) {
            WriteSheet mainSheet = EasyExcel.writerSheet(0, mainSheetName)
                    .head(headList)
                    .registerWriteHandler(sharedHandler)
                    .registerWriteHandler(styleHandler)
                    .build();
            writer.write(new ArrayList<>(), mainSheet);

            WriteSheet tipSheet = EasyExcel.writerSheet(1, tipSheetName)
                    .registerWriteHandler(sharedHandler)
                    .build();
            writer.write(new ArrayList<>(), tipSheet);
        } catch (IOException e) {
            LogUtils.error(e);
            throw new GenericException(e.getMessage());
        }
    }
}
