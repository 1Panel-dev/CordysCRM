package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.InternalDetailTab;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClueTransitionDetailTabTest {
    private static final String ORG_ID = "clue-tab-org";
    private MessageSource previousMessageSource;
    private LocaleContext previousLocaleContext;
    private ModuleFormService service;
    private BaseMapper<ModuleForm> formMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void configureService() {
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        previousLocaleContext = LocaleContextHolder.getLocaleContext();
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/cordys-crm");
        messages.setDefaultEncoding("UTF-8");
        new Translator().setMessageSource(messages);
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        formMapper = mock(BaseMapper.class);
        when(formMapper.selectListByLambda(any())).thenReturn(List.of());
        service = new ModuleFormService();
        ReflectionTestUtils.setField(service, "moduleFormMapper", formMapper);
    }

    @AfterEach
    void restoreMessages() {
        new Translator().setMessageSource(previousMessageSource);
        LocaleContextHolder.setLocaleContext(previousLocaleContext);
    }

    @ParameterizedTest
    @CsvSource({ "zh-CN,转换记录", "en-US,Conversion records" })
    void initializesEnabledStandaloneTabAndResolvesDefaultName(String locale, String expectedName) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag(locale));
        FormProp prop = new FormProp();

        service.resolveDetailTabs(FormKey.CLUE.getKey(), ORG_ID, prop);

        FormDetailTab tab = transitionTab(prop);
        assertEquals(InternalDetailTab.CLUE_TRANSITION_RECORD.getLabelKey(), tab.getName());
        assertTrue(tab.getEnable());
        assertNull(tab.getRelatedForm());
        assertNull(tab.getRelatedField());

        service.resolveDetailTabs(FormKey.CLUE.getKey(), ORG_ID, prop);
        assertEquals(expectedName, transitionTab(prop).getName());
        assertEquals(1, prop.getDetailTabs().stream()
                .filter(item -> InternalDetailTab.CLUE_TRANSITION_RECORD.name().equals(item.getInternalKey())).count());
    }

    @Test
    void preservesRenamedAndDisabledTransitionTabWhenSaving() {
        FormProp prop = new FormProp();
        FormDetailTab tab = new FormDetailTab();
        tab.setInternalKey(InternalDetailTab.CLUE_TRANSITION_RECORD.name());
        tab.setName("客户转换历史");
        tab.setEnable(false);
        prop.setDetailTabs(List.of(tab));

        service.validateAndResolveDetailTabs(FormKey.CLUE.getKey(), ORG_ID, prop);
        service.resolveDetailTabs(FormKey.CLUE.getKey(), ORG_ID, prop);

        assertEquals("客户转换历史", transitionTab(prop).getName());
        assertFalse(transitionTab(prop).getEnable());
        assertNull(transitionTab(prop).getRelatedForm());
        assertNull(transitionTab(prop).getRelatedField());
    }

    @Test
    void rejectsTransitionTabWithModifiedRelation() {
        FormProp prop = new FormProp();
        FormDetailTab tab = new FormDetailTab();
        tab.setInternalKey(InternalDetailTab.CLUE_TRANSITION_RECORD.name());
        tab.setName("转换记录");
        tab.setEnable(true);
        prop.setDetailTabs(List.of(tab));
        tab.setRelatedForm(new OptionDTO(FormKey.CUSTOMER.getKey(), "客户"));

        assertThrows(GenericException.class,
                () -> service.validateAndResolveDetailTabs(FormKey.CLUE.getKey(), ORG_ID, prop));

        tab.setRelatedForm(null);
        tab.setRelatedField(new OptionDTO("changed-field", "关联字段"));
        assertThrows(GenericException.class,
                () -> service.validateAndResolveDetailTabs(FormKey.CLUE.getKey(), ORG_ID, prop));
    }

    @ParameterizedTest
    @CsvSource({ "负责人历史,负责人历史", "module.form.detail_tab.owner_record,Owner history" })
    @SuppressWarnings("unchecked")
    void bulkInitializationAddsTabOnceAndPreservesExistingConfiguration(String ownerName, String expectedEnglishName) {
        ModuleForm clueForm = new ModuleForm();
        clueForm.setId("clue-form");
        clueForm.setFormKey(FormKey.CLUE.getKey());
        clueForm.setOrganizationId(ORG_ID);
        ModuleForm relatedForm = new ModuleForm();
        relatedForm.setId("follow-record-form");
        relatedForm.setFormKey(FormKey.FOLLOW_RECORD.getKey());
        relatedForm.setOrganizationId(ORG_ID);
        when(formMapper.selectListByLambda(any())).thenAnswer(
                invocation -> invocation.getArgument(0, LambdaQueryWrapper.class).getParams().containsValue(ORG_ID)
                        ? List.of(relatedForm)
                        : List.of(clueForm));

        ModuleField relatedField = new ModuleField();
        relatedField.setId("related-field");
        relatedField.setFormId(relatedForm.getId());
        relatedField.setName("线索关联字段");
        relatedField.setType(FieldType.DATA_SOURCE.name());
        DatasourceField fieldProp = new DatasourceField();
        fieldProp.setType(FieldType.DATA_SOURCE.name());
        fieldProp.setDataSourceType(FieldSourceType.CLUE.name());
        ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
        fieldBlob.setId(relatedField.getId());
        fieldBlob.setProp(JSON.toJSONString(fieldProp));
        BaseMapper<ModuleField> fieldMapper = mock(BaseMapper.class);
        BaseMapper<ModuleFieldBlob> fieldBlobMapper = mock(BaseMapper.class);
        when(fieldMapper.selectListByLambda(any())).thenReturn(List.of(relatedField));
        when(fieldBlobMapper.selectListByLambda(any())).thenReturn(List.of(fieldBlob));
        ReflectionTestUtils.setField(service, "moduleFieldMapper", fieldMapper);
        ReflectionTestUtils.setField(service, "moduleFieldBlobMapper", fieldBlobMapper);

        FormDetailTab ownerTab = new FormDetailTab();
        ownerTab.setInternalKey(InternalDetailTab.CLUE_OWNER_RECORD.name());
        ownerTab.setName(ownerName);
        ownerTab.setEnable(false);
        FormDetailTab customTab = new FormDetailTab();
        customTab.setName("线索关联记录");
        customTab.setEnable(true);
        customTab.setRelatedForm(new OptionDTO(relatedForm.getFormKey(), "旧表单名称"));
        customTab.setRelatedField(new OptionDTO(relatedField.getId(), "旧字段名称"));
        FormProp prop = new FormProp();
        prop.setLayout(2);
        prop.setDetailTabs(List.of(ownerTab, customTab));
        ModuleFormBlob formBlob = new ModuleFormBlob();
        formBlob.setId(clueForm.getId());
        formBlob.setProp(JSON.toJSONString(prop));
        BaseMapper<ModuleFormBlob> formBlobMapper = mock(BaseMapper.class);
        when(formBlobMapper.selectByIds(List.of(clueForm.getId()))).thenReturn(List.of(formBlob));
        ReflectionTestUtils.setField(service, "moduleFormBlobMapper", formBlobMapper);

        service.initInternalDetailTabs();
        service.initInternalDetailTabs();

        FormProp initialized = JSON.parseObject(formBlob.getProp(), FormProp.class);
        assertEquals(2, initialized.getLayout());
        assertEquals(3, initialized.getDetailTabs().size());
        assertEquals(ownerName, initialized.getDetailTabs().getFirst().getName());
        assertFalse(initialized.getDetailTabs().getFirst().getEnable());
        FormDetailTab initializedCustom = initialized.getDetailTabs().get(1);
        assertEquals("线索关联记录", initializedCustom.getName());
        assertNull(initializedCustom.getInternalKey());
        assertEquals(relatedForm.getFormKey(), initializedCustom.getRelatedForm().getIdAsString());
        assertEquals(relatedField.getId(), initializedCustom.getRelatedField().getIdAsString());
        assertEquals(relatedField.getName(), initializedCustom.getRelatedField().getName());
        assertTrue(transitionTab(initialized).getEnable());
        assertNull(transitionTab(initialized).getRelatedForm());
        assertNull(transitionTab(initialized).getRelatedField());
        assertEquals(InternalDetailTab.CLUE_TRANSITION_RECORD.getLabelKey(), transitionTab(initialized).getName());

        LocaleContextHolder.setLocale(Locale.US);
        service.resolveDetailTabs(FormKey.CLUE.getKey(), ORG_ID, initialized);
        assertEquals(expectedEnglishName, initialized.getDetailTabs().getFirst().getName());
        assertEquals("Conversion records", transitionTab(initialized).getName());
        verify(formBlobMapper, times(2)).updateById(formBlob);
    }

    private FormDetailTab transitionTab(FormProp prop) {
        return prop.getDetailTabs().stream()
                .filter(tab -> InternalDetailTab.CLUE_TRANSITION_RECORD.name().equals(tab.getInternalKey()))
                .findFirst().orElseThrow();
    }
}
