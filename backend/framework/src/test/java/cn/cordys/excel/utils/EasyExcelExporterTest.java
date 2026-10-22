package cn.cordys.excel.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EasyExcelExporterTest {

    @Test
    void disambiguatesOnlyLastLevelAcrossGroupsWithoutChangingSource() {
        List<List<String>> heads = List.of(
                List.of("产品", "名称"),
                List.of("产品", "价格"),
                List.of("联系人", "名称"),
                List.of("名称"));

        assertEquals(List.of(
                List.of("产品", "名称"),
                List.of("产品", "价格"),
                List.of("联系人", "名称\u00A0"),
                List.of("名称\u00A0\u00A0")), EasyExcelExporter.processDuplicateLastLevelHeads(heads));
        assertEquals(List.of("联系人", "名称"), heads.get(2));
        assertEquals(List.of("名称"), heads.get(3));
    }

    @Test
    void preservesEmptyHeaders() {
        assertEquals(List.of(), EasyExcelExporter.processDuplicateLastLevelHeads(List.of()));
    }
}
