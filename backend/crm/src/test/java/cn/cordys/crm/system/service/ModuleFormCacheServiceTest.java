package cn.cordys.crm.system.service;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModuleFormCacheServiceTest {
    private ApplicationContext previousContext;

    @BeforeEach
    void saveContext() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
    }

    @AfterEach
    void cleanContext() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        ReflectionTestUtils.setField(CommonBeanFactory.class, "context", previousContext);
    }

    @Test
    void deleteEvictsBothCachesImmediatelyWithoutTransaction() {
        ModuleFormCacheService service = new ModuleFormCacheService();
        ModuleFormService formService = mock(ModuleFormService.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache formCache = mock(Cache.class);
        Cache fieldCache = mock(Cache.class);
        ReflectionTestUtils.setField(service, "moduleFormService", formService);
        ReflectionTestUtils.setField(service, "cacheManager", cacheManager);
        when(cacheManager.getCache("form_cache")).thenReturn(formCache);
        when(cacheManager.getCache("field_cache")).thenReturn(fieldCache);

        service.delete("customer", "org-1");

        verify(formService).deleteForm("customer", "org-1");
        verify(formCache).evict("org-1:customer");
        verify(fieldCache).evict("org-1:customer");
    }

    @Test
    void deleteEvictsBothCachesOnlyAfterTransactionCommit() {
        ModuleFormCacheService service = new ModuleFormCacheService();
        ModuleFormService formService = mock(ModuleFormService.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache formCache = mock(Cache.class);
        Cache fieldCache = mock(Cache.class);
        ReflectionTestUtils.setField(service, "moduleFormService", formService);
        ReflectionTestUtils.setField(service, "cacheManager", cacheManager);
        when(cacheManager.getCache("form_cache")).thenReturn(formCache);
        when(cacheManager.getCache("field_cache")).thenReturn(fieldCache);
        TransactionSynchronizationManager.initSynchronization();

        service.delete("customer", "org-1");

        verify(formService).deleteForm("customer", "org-1");
        verifyNoInteractions(cacheManager, formCache, fieldCache);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        synchronizations.getFirst().afterCommit();

        verify(formCache).evict("org-1:customer");
        verify(fieldCache).evict("org-1:customer");
    }

    @Test
    void getBusinessFormConfigDoesNotMutateCachedConfig() {
        ModuleFormCacheService service = new ModuleFormCacheService();
        ModuleFormService formService = mock(ModuleFormService.class);
        ModuleFieldService fieldService = mock(ModuleFieldService.class);
        ApplicationContext context = mock(ApplicationContext.class);
        ReflectionTestUtils.setField(service, "moduleFormService", formService);
        ReflectionTestUtils.setField(service, "moduleFieldService", fieldService);
        ReflectionTestUtils.setField(CommonBeanFactory.class, "context", context);
        when(context.getBean(ModuleFormCacheService.class)).thenReturn(service);
        when(fieldService.getSubFieldsBySourceType(FieldSourceType.PRICE.name())).thenReturn(List.of());
        when(formService.flattenSourceRefFields(anyList(), anyMap()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InputField cachedField = new InputField();
        cachedField.setId("field-1");
        cachedField.setType("INPUT");
        cachedField.setName("原始名称");
        FormProp cachedProp = new FormProp();
        cachedProp.setViewSize("large");
        ModuleFormConfigDTO cached = new ModuleFormConfigDTO();
        cached.setFields(List.of(cachedField));
        cached.setFormProp(cachedProp);
        when(formService.getConfig("customer", "org-1")).thenReturn(cached);

        ModuleFormConfigDTO business = service.getBusinessFormConfig("customer", "org-1");

        ArgumentCaptor<List<BaseField>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(formService).flattenSourceRefFields(fieldsCaptor.capture(), anyMap());
        assertNotSame(cached.getFields(), fieldsCaptor.getValue());
        assertNotSame(cached.getFields().getFirst(), business.getFields().getFirst());
        assertNotSame(cached.getFormProp(), business.getFormProp());

        business.getFields().getFirst().setName("业务名称");
        business.getFormProp().setViewSize("small");
        assertEquals("原始名称", cached.getFields().getFirst().getName());
        assertEquals("large", cached.getFormProp().getViewSize());
    }
}
