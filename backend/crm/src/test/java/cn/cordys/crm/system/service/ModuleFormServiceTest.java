package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.handler.ResultResponseBodyAdvice;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.controller.ModuleFormController;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.SerialNumberField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModuleFormServiceTest {
    private MessageSource previousMessageSource;
    private LocaleContext previousLocaleContext;

    @BeforeEach
    void configureMessages() {
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        previousLocaleContext = LocaleContextHolder.getLocaleContext();
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/cordys-crm");
        messages.setDefaultEncoding("UTF-8");
        new Translator().setMessageSource(messages);
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    @AfterEach
    void restoreMessages() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        new Translator().setMessageSource(previousMessageSource);
        LocaleContextHolder.setLocaleContext(previousLocaleContext);
    }

    @ParameterizedTest
    @ValueSource(strings = { "yyyy-Q", "MM-dd", "yyMMdd", "yyyy-dd", "yyyy-M-d", "YYYYMMdd", "yyyy-MM-dd'",
            "yyyy-MM-dd HH:mm:ss", "yyyyMMddHHmmss", "yyyyMMdd'T'HHmmss", "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd h:mm:ss a" })
    @SuppressWarnings("unchecked")
    void invalidSerialDateRejectsTheEntireSaveBeforeAnyMapperCall(String pattern) {
        BaseMapper<ModuleForm> formMapper = mock(BaseMapper.class);
        BaseMapper<ModuleFormBlob> formBlobMapper = mock(BaseMapper.class);
        BaseMapper<ModuleField> fieldMapper = mock(BaseMapper.class);
        ExtModuleFieldMapper extFieldMapper = mock(ExtModuleFieldMapper.class);
        ModuleFormService service = new ModuleFormService();
        ReflectionTestUtils.setField(service, "moduleFormMapper", formMapper);
        ReflectionTestUtils.setField(service, "moduleFormBlobMapper", formBlobMapper);
        ReflectionTestUtils.setField(service, "moduleFieldMapper", fieldMapper);
        ReflectionTestUtils.setField(service, "extModuleFieldMapper", extFieldMapper);
        ModuleFormSaveRequest request = new ModuleFormSaveRequest();
        request.setFormKey("custom-form-id");
        request.setFormProp(new FormProp());
        request.setFields(formFields(serialField("field-1", pattern)));

        GenericException error = assertThrows(GenericException.class,
                () -> service.saveWithoutLog(request, "user", "org"));

        assertEquals("日期规则不合法", error.getMessage());
        verifyNoInteractions(formMapper, formBlobMapper, fieldMapper, extFieldMapper);
        assertNull(service.previewSerialNumberDate(pattern));
    }

    @ParameterizedTest
    @ValueSource(strings = { "yyyy", "yyyyMM", "yyyy/MM", "yyyyMMdd", "yyyy-MM-dd", "yyyy/MM/dd",
            "yyyy-MM" })
    void validPatternsPassTheSharedSaveValidation(String pattern) {
        ModuleFormService service = new ModuleFormService();
        assertDoesNotThrow(() -> service.preCheckForFieldSave("custom-form-id",
                formFields(serialField("field-1", pattern)), "org"));
        String before = new SimpleDateFormat(pattern).format(new Date());
        String preview = service.previewSerialNumberDate(pattern);
        String after = new SimpleDateFormat(pattern).format(new Date());
        assertTrue(preview.equals(before) || preview.equals(after));
    }

    @Test
    @SuppressWarnings("unchecked")
    void customSerialDateSurvivesFieldPersistenceAndReadback() {
        BaseMapper<ModuleField> fieldMapper = mock(BaseMapper.class);
        BaseMapper<ModuleFieldBlob> blobMapper = mock(BaseMapper.class);
        ModuleFormService service = new ModuleFormService();
        ReflectionTestUtils.setField(service, "moduleFieldMapper", fieldMapper);
        ReflectionTestUtils.setField(service, "moduleFieldBlobMapper", blobMapper);
        SerialNumberField serial = serialField("field-1", "yyyy-MM-dd");
        serial.setPrefixType("custom");
        List<ModuleField> storedFields = new ArrayList<>();
        List<ModuleFieldBlob> storedBlobs = new ArrayList<>();
        doAnswer(invocation -> {
            storedFields.addAll(invocation.getArgument(0));
            return null;
        }).when(fieldMapper).batchInsert(any());
        doAnswer(invocation -> {
            storedBlobs.addAll(invocation.getArgument(0));
            return null;
        }).when(blobMapper).batchInsert(any());
        when(fieldMapper.selectListByLambda(any())).thenReturn(storedFields);
        when(blobMapper.selectListByLambda(any())).thenReturn(storedBlobs);

        service.saveFields(List.of(serial), "form", "user");
        SerialNumberField restored = (SerialNumberField) service.getAllFields("form").getFirst();

        assertEquals(serial.getSerialNumberRules(), restored.getSerialNumberRules());
        assertEquals("yyyy-MM-dd", restored.getSerialNumberRules().get(2));
    }

    @Test
    void savingChangedPrefixCleansThePreviousCounters() {
        SerialNumberField previous = serialField("field-1", "yyyyMMdd");
        SerialNumberField current = serialField("field-1", "yyyyMMdd");
        current.setSerialNumberRules(List.of("X", "-", "yyyyMMdd", "-", "6"));
        var fixture = serialSaveFixture(previous, current);
        TransactionSynchronizationManager.initSynchronization();

        fixture.service().saveWithoutLog(fixture.request(), "user", "org");

        verifyNoInteractions(fixture.generator(), fixture.transactionManager());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(fixture.generator()).cleanUnusedRules("org", "custom-form-id", current.getSerialNumberRules(), false);
        verify(fixture.transactionManager()).getTransaction(argThat(definition -> definition.isReadOnly()
                && definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @Test
    void dateChangeAndFormulaPrefixChangeTriggerCleanupButRollbackDoesNot() {
        var fixture = serialSaveFixture(serialField("field-1", "yyyyMM"), serialField("field-1", "yyyyMMdd"));
        TransactionSynchronizationManager.initSynchronization();
        fixture.service().saveWithoutLog(fixture.request(), "user", "org");

        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(fixture.generator(), fixture.transactionManager());
        TransactionSynchronizationManager.clearSynchronization();

        SerialNumberField formula = serialField("field-1", "yyyyMM");
        formula.setPrefixType(SerialNumberField.FORMULA);
        formula.setFormula("客户名称");
        var formulaFixture = serialSaveFixture(serialField("field-1", "yyyyMM"), formula);
        formulaFixture.service().saveWithoutLog(formulaFixture.request(), "user", "org");
        verify(formulaFixture.generator()).cleanUnusedRules("org", "custom-form-id", formula.getSerialNumberRules(),
                true);
    }

    @ParameterizedTest
    @ValueSource(strings = { "yyyy/MM/dd", "yyyy-MM-dd" })
    void unchangedPrefixAndPeriodDoNotCleanWhenDisplayPropertiesChange(String pattern) {
        SerialNumberField previous = serialField("field-1", "yyyy/MM/dd");
        SerialNumberField current = serialField("field-1", pattern);
        previous.setPrefixType("custom");
        current.setName("新的字段名称");
        current.setSerialNumberRules(List.of("Opp", "/", pattern, "_", "8"));
        var fixture = serialSaveFixture(previous, current);

        fixture.service().saveWithoutLog(fixture.request(), "user", "org");

        verifyNoInteractions(fixture.generator(), fixture.transactionManager());
    }

    @Test
    void historicalDateRuleCanBeCorrectedToSupportedPattern() {
        SerialNumberField current = serialField("field-1", "yyyy-MM-dd");
        var fixture = serialSaveFixture(serialField("field-1", "yyyy-MM-dd HH:mm:ss"), current);

        assertDoesNotThrow(() -> fixture.service().saveWithoutLog(fixture.request(), "user", "org"));

        verify(fixture.generator()).cleanUnusedRules("org", "custom-form-id", current.getSerialNumberRules(), false);
    }

    @Test
    void propertyOnlySaveDoesNotReadOldFieldsOrCleanCounters() {
        var fixture = serialSaveFixture(serialField("field-1", "yyyyMM"), serialField("field-1", "yyyyMMdd"));
        fixture.request().setFields(null);

        fixture.service().saveWithoutLog(fixture.request(), "user", "org");

        verify(fixture.service(), never()).getAllFields("form-row-id");
        verifyNoInteractions(fixture.generator(), fixture.transactionManager());
    }

    @Test
    void delayedCallbackUsesTheLatestCommittedRulesInsteadOfTheSavedSnapshot() {
        SerialNumberField saved = serialField("field-1", "yyyyMMdd");
        var fixture = serialSaveFixture(serialField("field-1", "yyyyMM"), saved);
        TransactionSynchronizationManager.initSynchronization();
        fixture.service().saveWithoutLog(fixture.request(), "user", "org");
        SerialNumberField latest = serialField("field-1", "yyyyMMdd");
        latest.setSerialNumberRules(List.of("X", "-", "yyyyMMdd", "-", "6"));
        doReturn(formFields(latest)).when(fixture.service()).getAllFields("form-row-id");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(fixture.generator()).cleanUnusedRules("org", "custom-form-id", latest.getSerialNumberRules(), false);
    }

    @Test
    void invalidSaveAndFailedReadbackDoNotCleanCounters() {
        var fixture = serialSaveFixture(serialField("field-1", "yyyyMM"), serialField("field-1", "yyyyMMddHHmmss"));
        assertThrows(GenericException.class, () -> fixture.service().saveWithoutLog(fixture.request(), "user", "org"));
        verifyNoInteractions(fixture.generator(), fixture.transactionManager());

        fixture.request().setFields(formFields(serialField("field-1", "yyyyMMdd")));
        doThrow(new IllegalStateException("配置读取失败"))
                .when(fixture.service()).getConfig("custom-form-id", "org");
        assertThrows(IllegalStateException.class,
                () -> fixture.service().saveWithoutLog(fixture.request(), "user", "org"));
        verifyNoInteractions(fixture.generator(), fixture.transactionManager());
    }

    private record SerialSaveFixture(ModuleFormService service, ModuleFormSaveRequest request,
            SerialNumGenerator generator, PlatformTransactionManager transactionManager) {
    }

    @SuppressWarnings("unchecked")
    private SerialSaveFixture serialSaveFixture(SerialNumberField previous, SerialNumberField current) {
        BaseMapper<ModuleForm> formMapper = mock(BaseMapper.class);
        BaseMapper<ModuleField> fieldMapper = mock(BaseMapper.class);
        ModuleFormService service = spy(new ModuleFormService());
        ReflectionTestUtils.setField(service, "moduleFormMapper", formMapper);
        ReflectionTestUtils.setField(service, "moduleFieldMapper", fieldMapper);
        SerialNumGenerator generator = mock(SerialNumGenerator.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        ReflectionTestUtils.setField(service, "serialNumGenerator", generator);
        ReflectionTestUtils.setField(service, "transactionManager", transactionManager);
        ModuleForm form = new ModuleForm();
        form.setId("form-row-id");
        form.setFormKey("custom-form-id");
        when(formMapper.selectListByLambda(any())).thenReturn(List.of(form));
        ModuleField storedSerial = new ModuleField();
        storedSerial.setType("SERIAL_NUMBER");
        when(fieldMapper.selectListByLambda(any())).thenReturn(List.of(storedSerial));
        ReflectionTestUtils.setField(service, "extModuleFieldMapper", mock(ExtModuleFieldMapper.class));
        doReturn(formFields(previous), formFields(current)).when(service).getAllFields("form-row-id");
        doNothing().when(service).saveFields(any(), any(), any());
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(formFields(current));
        doReturn(config).when(service).getConfig("custom-form-id", "org");
        ModuleFormSaveRequest request = new ModuleFormSaveRequest();
        request.setFormKey("custom-form-id");
        request.setFields(formFields(current));

        return new SerialSaveFixture(service, request, generator, transactionManager);
    }

    @Test
    void datePreviewEndpointWrapsValidDatesAndInvalidFormats() throws Exception {
        ModuleFormController controller = new ModuleFormController();
        ReflectionTestUtils.setField(controller, "moduleFormService", new ModuleFormService());
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ResultResponseBodyAdvice()).build();

        mvc.perform(get("/module/form/serial-number/date-preview").param("datePattern", "yyyy/MM/dd"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.data").isString());
        mvc.perform(get("/module/form/serial-number/date-preview").param("datePattern", "YYYYMM"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"data\":null")));
        mvc.perform(get("/module/form/serial-number/date-preview").param("datePattern", "yyyyMMddHHmmss"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"data\":null")));
    }

    @Test
    void multipleCountersAndMissingRulesAreRejected() {
        ModuleFormService service = new ModuleFormService();
        GenericException multiple = assertThrows(GenericException.class,
                () -> service.preCheckForFieldSave("custom-form-id", List.of(serialField("field-1", "yyyyMMdd"),
                        serialField("field-2", "yyyy-MM-dd")), "org"));
        assertTrue(multiple.getMessage().contains("只能配置一个"));
        SerialNumberField missingRules = serialField("field-1", "yyyyMM");
        missingRules.setSerialNumberRules(null);
        assertThrows(GenericException.class,
                () -> service.preCheckForFieldSave("custom-form-id", List.of(missingRules), "org"));
    }

    private static List<BaseField> formFields(SerialNumberField serial) {
        InputField name = new InputField();
        name.setType("INPUT");
        name.setInternalKey(BusinessModuleField.CUSTOM_FORM_DATA_NAME.getKey());
        name.setName("名称");
        InputField owner = new InputField();
        owner.setType("INPUT");
        owner.setInternalKey(BusinessModuleField.CUSTOM_FORM_DATA_OWNER.getKey());
        owner.setName("负责人");
        return List.of(name, owner, serial);
    }

    private static SerialNumberField serialField(String id, String pattern) {
        SerialNumberField field = new SerialNumberField();
        field.setId(id);
        field.setType("SERIAL_NUMBER");
        field.setName("流水号" + id);
        field.setSerialNumberRules(List.of("Opp", "-", pattern, "-", "6"));
        return field;
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteFormRemovesFieldPropertiesFieldsBlobAndForm() {
        BaseMapper<ModuleForm> formMapper = mock(BaseMapper.class);
        BaseMapper<ModuleFormBlob> formBlobMapper = mock(BaseMapper.class);
        BaseMapper<ModuleField> fieldMapper = mock(BaseMapper.class);
        ExtModuleFieldMapper extFieldMapper = mock(ExtModuleFieldMapper.class);
        ModuleFormService service = new ModuleFormService();
        ReflectionTestUtils.setField(service, "moduleFormMapper", formMapper);
        ReflectionTestUtils.setField(service, "moduleFormBlobMapper", formBlobMapper);
        ReflectionTestUtils.setField(service, "moduleFieldMapper", fieldMapper);
        ReflectionTestUtils.setField(service, "extModuleFieldMapper", extFieldMapper);

        ModuleForm form = new ModuleForm();
        form.setId("form-row-id");
        ModuleField first = new ModuleField();
        first.setId("field-1");
        ModuleField second = new ModuleField();
        second.setId("field-2");
        when(formMapper.selectListByLambda(any())).thenReturn(List.of(form));
        when(fieldMapper.selectListByLambda(any()))
                .thenReturn(List.of(first, second));

        service.deleteForm("custom-form-id", "org-1");

        verify(extFieldMapper).deletePropByIds(List.of("field-1", "field-2"));
        verify(extFieldMapper).deleteByIds(List.of("field-1", "field-2"));
        verify(formBlobMapper).deleteByPrimaryKey("form-row-id");
        verify(formMapper).deleteByPrimaryKey("form-row-id");
    }
}
