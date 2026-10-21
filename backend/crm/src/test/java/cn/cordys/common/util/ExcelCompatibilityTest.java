package cn.cordys.common.util;

import cn.cordys.excel.utils.EasyExcelExporter;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.write.handler.WriteHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelCompatibilityTest {

    @Test
    void exportedWorkbookCanBeReadWithUpgradedPoiAndXmlBeans() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new EasyExcelExporter().exportByCustomWriteHandler(response,
                List.of(List.of("客户名称"), List.of("金额")),
                List.of(List.of("兼容性客户", new BigDecimal("123.45"))),
                "客户导出", "客户", new WriteHandler() { });

        assertTrue(response.getContentAsByteArray().length > 0);
        List<Map<Integer, String>> rows = EasyExcel.read(new ByteArrayInputStream(response.getContentAsByteArray()))
                .sheet().doReadSync();
        assertEquals(List.of(Map.of(0, "兼容性客户", 1, "123.45")), rows);
    }
}
