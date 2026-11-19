package cn.cordys.crm.form.service;

import cn.cordys.common.dto.ExportDTO;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.dto.request.CustomFormExportSelectRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.mapper.ExtCustomFormDataMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomFormDataExportServiceTest {

    @AfterEach
    void clearFormContext() {
        CustomFormDataFieldService.clearFormKey();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exportClearsTheFormContextOnSuccessAndFailure(boolean fail) {
        CustomFormDataExportService exportService = new CustomFormDataExportService();
        CustomFormDataFieldService fields = new CustomFormDataFieldService();
        CustomFormDataService dataService = mock(CustomFormDataService.class);
        ExtCustomFormDataMapper mapper = mock(ExtCustomFormDataMapper.class);
        ReflectionTestUtils.setField(exportService, "customFormDataService", dataService);
        ReflectionTestUtils.setField(exportService, "extCustomFormDataMapper", mapper);

        CustomFormExportSelectRequest request = new CustomFormExportSelectRequest();
        request.setCustomFormId("form");
        List<String> ids = List.of("record");
        ExportDTO export = ExportDTO.builder().orgId("org").userId("user")
                .selectIds(ids).selectRequest(request).build();
        CustomFormDataListResponse record = new CustomFormDataListResponse();
        record.setCustomFormId("form");
        List<CustomFormDataListResponse> records = List.of(record);
        when(dataService.getDataScope("form", "user", "org")).thenReturn(CustomFormRoleKey.MANAGE_ALL);
        when(mapper.getListByIds(ids, "org", "user", false, null)).thenReturn(records);
        IllegalStateException failure = new IllegalStateException("export fields failed");
        when(dataService.buildList(records, "form", "org")).thenAnswer(invocation -> {
            assertEquals("form", fields.getFormKey());
            if (fail) {
                throw failure;
            }
            return List.of();
        });

        if (fail) {
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> exportService.getExportMergeData("task", export)));
        } else {
            assertEquals(0, exportService.getExportMergeData("task", export).getHandleCount());
        }
        verify(dataService).buildList(records, "form", "org");
        assertThrows(IllegalStateException.class, fields::getFormKey);
    }
}
