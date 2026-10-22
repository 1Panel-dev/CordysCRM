package cn.cordys.excel.utils;

import cn.cordys.common.exception.GenericException;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.handler.WriteHandler;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class EasyExcelExporter {

    /**
     * 为同名末级表头追加不换行空格，避免导入、导出时相邻同名列被合并。
     */
    public static List<List<String>> processDuplicateLastLevelHeads(List<List<String>> exportHeads) {
        Map<String, Integer> countMap = new HashMap<>();
        List<List<String>> result = new ArrayList<>(exportHeads.size());
        for (List<String> head : exportHeads) {
            List<String> newHead = new ArrayList<>(head);
            int lastIndex = newHead.size() - 1;
            String lastHead = newHead.get(lastIndex);
            int count = countMap.getOrDefault(lastHead, 0);
            if (count > 0) {
                newHead.set(lastIndex, lastHead + "\u00A0".repeat(count));
            }
            countMap.put(lastHead, count + 1);
            result.add(newHead);
        }
        return result;
    }

    public void buildExportResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-disposition",
                "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + ".xlsx");
    }

    public void exportByCustomWriteHandler(HttpServletResponse response, List<List<String>> headList,
            List<List<Object>> data,
            String fileName, String sheetName, WriteHandler writeHandler) {
        buildExportResponse(response, fileName);
        try {
            EasyExcel.write(response.getOutputStream())
                    .head(Optional.ofNullable(headList).orElse(new ArrayList<>()))
                    .registerWriteHandler(writeHandler)
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new GenericException(e.getMessage());
        }
    }

    public void exportMultiSheetTplWithSharedHandler(HttpServletResponse response, List<List<String>> headList,
            String fileName,
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
            log.error(e.getMessage(), e);
            throw new GenericException(e.getMessage());
        }
    }
}
