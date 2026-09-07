package cn.cordys.crm.system.service;

import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleFormServiceTest {

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
