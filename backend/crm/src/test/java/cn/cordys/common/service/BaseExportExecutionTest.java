package cn.cordys.common.service;

import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.context.ExportTaskFunction;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportFieldParam;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.dto.request.BusinessTitlePageRequest;
import cn.cordys.crm.contract.dto.response.BusinessTitleListResponse;
import cn.cordys.crm.contract.mapper.ExtBusinessTitleMapper;
import cn.cordys.crm.contract.service.BusinessTitleExportService;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.excel.domain.MergeResult;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.registry.ExportThreadRegistry;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseExportExecutionTest {

    @TempDir
    Path directory;

    private final ExportTask task = task();
    private final List<String> statuses = new CopyOnWriteArrayList<>();
    private final CompletableFuture<Void> finished = new CompletableFuture<>();
    private ApplicationContext previousContext;
    private MessageSource previousMessageSource;

    @BeforeEach
    void saveContext() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("sum", Locale.CHINA, "合计");
        new Translator().setMessageSource(messages);
    }

    @AfterEach
    void restoreContext() {
        ExportThreadRegistry.remove(task.getId());
        new CommonBeanFactory().setApplicationContext(previousContext);
        new Translator().setMessageSource(previousMessageSource);
    }

    @Test
    void rowFailureMarksErrorInsteadOfCompletingWithMissingRows() throws Exception {
        TempExportService service = new TempExportService();

        runAndAwait(service, () -> service.parallelBuildExportData(task.getId(), List.of("good", "bad"),
                null, List.of(), (value, fields, metas, cache) -> {
                    if ("bad".equals(value)) {
                        throw new IllegalStateException("字段解析失败");
                    }
                    return List.of(List.of(value));
                }));

        assertEquals(List.of("ERROR"), statuses);
    }

    @Test
    void stoppingParallelBuildDoesNotReportSuccessOrError() throws Exception {
        TempExportService service = new TempExportService();
        installRecorder(service);
        CountDownLatch rowStarted = new CountDownLatch(1);
        CountDownLatch releaseRow = new CountDownLatch(1);
        service.runExport("org", "user", "ORDER", Locale.CHINA, task, "result",
                () -> service.parallelBuildExportData(task.getId(), List.of("row"), null, List.of(),
                        (value, fields, metas, cache) -> {
                            rowStarted.countDown();
                            try {
                                assertTrue(releaseRow.await(5, TimeUnit.SECONDS));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(e);
                            }
                            return List.of(List.of(value));
                        }));
        try {
            assertTrue(rowStarted.await(5, TimeUnit.SECONDS));
            ExportThreadRegistry.stop(task.getId());
        } finally {
            releaseRow.countDown();
        }
        awaitCompletion();

        assertEquals(List.of("STOP"), statuses);
    }

    @Test
    void cancellationAfterLastWriteStillStopsTheTask() throws Exception {
        TempExportService service = new TempExportService();

        runAndAwait(service, () -> ExportThreadRegistry.stop(task.getId()));

        assertEquals(List.of("STOP"), statuses);
    }

    @Test
    void interruptedPlainSelectionStopsBeforeTheNextBatch() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        TempExportService service = new TempExportService() {
            @Override
            protected List<List<Object>> getSelectExportData(List<String> ids, String taskId, ExportDTO dto)
                    throws InterruptedException {
                calls.incrementAndGet();
                throw new InterruptedException("停止选中导出");
            }
        };
        ExportDTO dto = exportDTO();

        runAndAwait(service, () -> service.exportSelectData(task, dto));

        assertEquals(List.of("STOP"), statuses);
        assertEquals(1, calls.get());
    }

    @Test
    void interruptedMergedSelectionStopsBeforeTheNextBatch() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        TempExportService service = new TempExportService() {
            @Override
            protected MergeResult getExportMergeData(String taskId, ExportDTO dto) throws InterruptedException {
                calls.incrementAndGet();
                throw new InterruptedException("停止合并导出");
            }

            @Override
            protected ExportFieldParam getExportFieldParam(ExportDTO dto) {
                return ExportFieldParam.builder().fieldConfigMap(Map.of()).build();
            }

            @Override
            public String asyncExport(String fileName, String orgId, String userId, Locale locale, String module,
                    String exportType, ExportExecutor executor) {
                runExport(orgId, userId, module, locale, task, fileName, () -> executor.execute(task));
                return task.getId();
            }
        };
        ExportDTO dto = exportDTO();
        dto.setFormKey("form");
        ModuleFormService forms = mock(ModuleFormService.class);
        when(forms.getAllExportHeads(dto.getHeadList(), "form", "org"))
                .thenReturn(List.of(List.of("名称")));
        when(forms.getExportMergeHeads("form", "org", dto.getHeadList())).thenReturn(List.of("name"));
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(ModuleFormService.class)).thenReturn(forms);
        new CommonBeanFactory().setApplicationContext(context);
        installRecorder(service);

        service.exportSelectWithMergeStrategy(dto);
        awaitCompletion();

        assertEquals(List.of("STOP"), statuses);
        assertEquals(1, calls.get());
    }

    @Test
    void fullyFilteredPageKeepsLaterRowsAndMergeOffsetsInWorkbook() throws Exception {
        TempExportService service = new TempExportService();
        AtomicInteger calls = new AtomicInteger();
        List<List<String>> heads = EasyExcelExporter.processDuplicateLastLevelHeads(List.of(
                List.of("名称"), List.of("产品", "名称"), List.of("合计金额")));

        runAndAwait(service, () -> service.batchHandleDataWithMergeStrategy(heads, task, "result", List.of(0, 2),
                new BasePageRequest(), request -> {
                    calls.incrementAndGet();
                    if (request.getCurrent() == 1) {
                        return MergeResult.builder().dataList(List.of()).mergeRegions(List.of())
                                .queryCount(BaseExportService.EXPORT_MAX_COUNT).build();
                    }
                    String name = request.getCurrent() == 2 ? "订单 A" : "订单 B";
                    MergeResult result = service.parallelBuildExportData(task.getId(), List.of(name), null, List.of(),
                            (value, fields, metas, cache) -> List.of(
                                    List.of(value, "产品一", "1.20"), List.of(value, "产品二", "2.30")));
                    result.setQueryCount(request.getCurrent() == 2 ? BaseExportService.EXPORT_MAX_COUNT : 1);
                    return result;
                }));

        assertEquals(List.of("SUCCESS"), statuses);
        assertEquals(3, calls.get());
        try (Workbook workbook = WorkbookFactory.create(outputFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(5, sheet.getLastRowNum());
            assertEquals("名称\u00A0", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("订单 A", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("订单 B", sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("产品二", sheet.getRow(5).getCell(1).getStringCellValue());
            assertEquals("3.50", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("", sheet.getRow(3).getCell(2).getStringCellValue());
            assertEquals("3.50", sheet.getRow(4).getCell(2).getStringCellValue());
            List<String> merged = sheet.getMergedRegions().stream().map(region -> region.formatAsString()).toList();
            assertTrue(merged.containsAll(List.of("A3:A4", "C3:C4", "A5:A6", "C5:C6")));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, false",
            "false, true, false",
            "true, false, false",
            "true, true, false",
            "true, false, true",
            "true, true, true"
    })
    void emptyExportsKeepWorksheetAndHeaders(boolean merged, boolean selected, boolean filtered) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        TempExportService service = new TempExportService() {
            @Override
            protected MergeResult getExportMergeData(String taskId, ExportDTO dto) {
                int page = calls.incrementAndGet();
                return MergeResult.builder().dataList(List.of()).mergeRegions(List.of())
                        .queryCount(filtered ? (page == 1 ? EXPORT_MAX_COUNT : 1) : 0).build();
            }

            @Override
            protected ExportFieldParam getExportFieldParam(ExportDTO dto) {
                return ExportFieldParam.builder().fieldConfigMap(Map.of()).build();
            }

            @Override
            public String asyncExport(String fileName, String orgId, String userId, Locale locale, String module,
                    String exportType, ExportExecutor executor) {
                runExport(orgId, userId, module, locale, task, fileName, () -> executor.execute(task));
                return task.getId();
            }
        };
        ExportDTO dto = exportDTO();
        if (!filtered) {
            dto.setSelectIds(List.of());
        }
        List<List<String>> heads = List.of(List.of("名称"));

        if (merged && selected) {
            dto.setFormKey("form");
            ModuleFormService forms = mock(ModuleFormService.class);
            when(forms.getAllExportHeads(dto.getHeadList(), "form", "org")).thenReturn(heads);
            when(forms.getExportMergeHeads("form", "org", dto.getHeadList())).thenReturn(List.of("name"));
            ApplicationContext context = mock(ApplicationContext.class);
            when(context.getBean(ModuleFormService.class)).thenReturn(forms);
            new CommonBeanFactory().setApplicationContext(context);
            installRecorder(service);
            service.exportSelectWithMergeStrategy(dto);
            awaitCompletion();
        } else {
            runAndAwait(service, () -> {
                if (selected) {
                    service.exportSelectData(task, dto);
                } else if (merged) {
                    service.batchHandleDataWithMergeStrategy(heads, task, "result", List.of(),
                            new BasePageRequest(), request -> service.getExportMergeData(task.getId(), dto));
                } else {
                    service.batchHandleData("file", heads, task, "result", new BasePageRequest(), request -> {
                        calls.incrementAndGet();
                        return List.of();
                    });
                }
            });
        }

        assertEquals(List.of("SUCCESS"), statuses);
        assertEquals(filtered ? 2 : (selected ? 0 : 1), calls.get());
        try (Workbook workbook = WorkbookFactory.create(outputFile())) {
            assertEquals(1, workbook.getNumberOfSheets());
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("导出数据", sheet.getSheetName());
            assertEquals(1, sheet.getPhysicalNumberOfRows());
            assertEquals("名称", sheet.getRow(0).getCell(0).getStringCellValue());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void businessTitlePlainExportKeepsRowsAndWritesOneSuccess(boolean selected) throws Exception {
        BusinessTitleExportService service = new BusinessTitleExportService() {
            @Override
            public File prepareExportFile(String fileId, String fileName, String orgId) {
                return outputFile();
            }
        };
        ExportDTO dto = exportDTO();
        dto.setSelectIds(List.of("title-1", "title-2"));
        BusinessTitlePageRequest request = new BusinessTitlePageRequest();
        dto.setPageRequest(request);
        BusinessTitleListResponse first = new BusinessTitleListResponse();
        first.setName("工商抬头一");
        first.setType("enterprise");
        BusinessTitleListResponse second = new BusinessTitleListResponse();
        second.setName("工商抬头二");
        second.setType("enterprise");
        ExtBusinessTitleMapper mapper = mock(ExtBusinessTitleMapper.class);
        when(mapper.getListByIds(dto.getSelectIds(), "user", "org")).thenReturn(List.of(first, second));
        when(mapper.list(request, "org", "user")).thenReturn(List.of(first, second));
        ReflectionTestUtils.setField(service, "extBusinessTitleMapper", mapper);
        ReflectionTestUtils.setField(service, "baseService", mock(BaseService.class));

        runAndAwait(service, () -> {
            if (selected) {
                ((BaseExportService) service).exportSelectData(task, dto);
            } else {
                service.exportCustomerData(task, dto);
            }
        });

        assertEquals(List.of("SUCCESS"), statuses);
        try (Workbook workbook = WorkbookFactory.create(outputFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getLastRowNum());
            assertEquals("名称", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("工商抬头一", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("工商抬头二", sheet.getRow(2).getCell(0).getStringCellValue());
        }
    }

    private void runAndAwait(BaseExportService service, ExportTaskFunction work) throws Exception {
        installRecorder(service);
        service.runExport("org", "user", "ORDER", Locale.CHINA, task, "result", work);
        awaitCompletion();
    }

    private void awaitCompletion() throws Exception {
        finished.get(10, TimeUnit.SECONDS);
        assertFalse(ExportThreadRegistry.exists(task.getId()));
    }

    private void installRecorder(BaseExportService service) {
        ReflectionTestUtils.setField(service, "exportTaskService", new ExportTaskService() {
            @Override
            public void update(String taskId, String status, String userId) {
                assertFalse(Thread.currentThread().isInterrupted());
                statuses.add(status);
            }
        });
        ReflectionTestUtils.setField(service, "logService", new LogService() {
            @Override
            public void add(LogDTO log) {
                finished.complete(null);
            }
        });
    }

    private ExportDTO exportDTO() {
        return ExportDTO.builder().orgId("org").userId("user").locale(Locale.CHINA).fileName("result")
                .headList(List.of(new ExportHeadDTO("name", "名称", "INPUT")))
                .selectIds(IntStream.range(0, 501).mapToObj(String::valueOf).toList()).build();
    }

    private File outputFile() {
        return directory.resolve("result.xlsx").toFile();
    }

    private static ExportTask task() {
        ExportTask task = new ExportTask();
        task.setId("export-execution-test");
        task.setOrganizationId("org");
        task.setFileId("file");
        return task;
    }

    private class TempExportService extends BaseExportService {
        @Override
        public File prepareExportFile(String fileId, String fileName, String orgId) {
            return outputFile();
        }
    }
}
