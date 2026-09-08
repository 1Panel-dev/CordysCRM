package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleFormServiceTest {

    @Test
    void rejectsUnknownDynamicFormKeyBeforeReadingItsFields() {
        @SuppressWarnings("unchecked")
        BaseMapper<ModuleForm> moduleFormMapper = mock(BaseMapper.class);
        when(moduleFormMapper.selectOne(any(ModuleForm.class))).thenReturn(null);
        ModuleFormService service = new ModuleFormService();
        ReflectionTestUtils.setField(
                service, "moduleFormMapper", moduleFormMapper);
        new Translator().setMessageSource(new StaticMessageSource());

        assertThrows(GenericException.class,
                () -> service.getAllFields("missing-form", "org-1"));
    }
}
