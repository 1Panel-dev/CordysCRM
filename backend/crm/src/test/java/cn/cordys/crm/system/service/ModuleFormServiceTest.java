package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.InternalDetailTab;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleFormServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void defaultDetailTabNameFollowsRequestLocaleAfterPreviousLanguageWasCached() {
        MessageSource previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
        StaticMessageSource messageSource = new StaticMessageSource();
        String labelKey = InternalDetailTab.CLUE_OWNER_RECORD.getLabelKey();
        messageSource.addMessage(labelKey, Locale.US, "Owner history");
        messageSource.addMessage(labelKey, Locale.SIMPLIFIED_CHINESE, "负责人记录");
        ReflectionTestUtils.setField(Translator.class, "messageSource", messageSource);

        try {
            ModuleFormService service = new ModuleFormService();
            BaseMapper<ModuleForm> formMapper = mock(BaseMapper.class);
            ReflectionTestUtils.setField(service, "moduleFormMapper", formMapper);
            when(formMapper.selectListByLambda(any())).thenReturn(List.of());

            FormDetailTab tab = new FormDetailTab();
            tab.setInternalKey(InternalDetailTab.CLUE_OWNER_RECORD.name());
            tab.setName("Owner history");
            FormProp formProp = new FormProp();
            formProp.setDetailTabs(List.of(tab));

            LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            service.resolveDetailTabs(FormKey.CLUE.getKey(), "org-1", formProp);
            assertEquals("负责人记录", formProp.getDetailTabs().getFirst().getName());

            LocaleContextHolder.setLocale(Locale.US);
            service.resolveDetailTabs(FormKey.CLUE.getKey(), "org-1", formProp);
            assertEquals("Owner history", formProp.getDetailTabs().getFirst().getName());

            tab.setName("自定义名称");
            LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            service.resolveDetailTabs(FormKey.CLUE.getKey(), "org-1", formProp);
            assertEquals("自定义名称", formProp.getDetailTabs().getFirst().getName());
        } finally {
            ReflectionTestUtils.setField(Translator.class, "messageSource", previousMessageSource);
            LocaleContextHolder.setLocaleContext(previousLocaleContext);
        }
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
