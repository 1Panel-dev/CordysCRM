package cn.cordys.crm.system.service;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.crm.system.constants.InternalDetailTab;
import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.DetailTabPageRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.detailtab.DetailTabDataHandler;
import cn.cordys.crm.system.service.detailtab.DetailTabQueryContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DetailTabDataServiceTest {

    private final ModuleFormCacheService moduleFormCacheService = mock(ModuleFormCacheService.class);
    private final DetailTabDataHandler handler = mock(DetailTabDataHandler.class);
    private final DetailTabDataService service = new DetailTabDataService(moduleFormCacheService, List.of(handler));

    @Test
    void shouldResolveEnabledInternalTabFromServerConfig() {
        FormDetailTab tab = internalTab(true);
        when(moduleFormCacheService.getConfig("customer", "org-1")).thenReturn(config(tab));
        when(handler.supports(any(DetailTabQueryContext.class))).thenReturn(true);
        PagerWithOption page = new PagerWithOption<>();
        page.setList(List.of("row"));
        doReturn(page).when(handler).page(any(DetailTabQueryContext.class));

        DetailTabPageRequest request = new DetailTabPageRequest();
        request.setRelatedFormId("opportunity");
        PagerWithOption<?> response = service.page("customer", "customer-1", request, "user-1", "org-1");

        assertEquals(page, response);
    }

    @Test
    void shouldRejectDisabledTabBeforeCallingHandler() {
        when(moduleFormCacheService.getConfig("customer", "org-1")).thenReturn(config(internalTab(false)));
        DetailTabPageRequest request = new DetailTabPageRequest();
        request.setRelatedFormId("opportunity");

        assertThrows(GenericException.class,
                () -> service.page("customer", "customer-1", request, "user-1", "org-1"));
        verify(handler, never()).page(any());
    }

    @Test
    void shouldMatchCustomTabByRelatedFormAndFieldTogether() {
        FormDetailTab tab = new FormDetailTab();
        tab.setEnable(true);
        tab.setRelatedForm(new OptionDTO("custom-form-1", "售后单"));
        tab.setRelatedField(new OptionDTO("field-1", "关联客户"));
        when(moduleFormCacheService.getConfig("customer", "org-1")).thenReturn(config(tab));
        when(handler.supports(any(DetailTabQueryContext.class))).thenReturn(true);
        PagerWithOption<?> page = new PagerWithOption<>();
        doReturn(page).when(handler).page(any(DetailTabQueryContext.class));

        DetailTabPageRequest request = new DetailTabPageRequest();
        request.setRelatedFormId("custom-form-1");
        request.setRelatedFieldId("field-1");
        PagerWithOption<?> response = service.page("customer", "customer-1", request, "user-1", "org-1");

        assertEquals(page, response);
    }

    @Test
    void shouldUseInternalKeyAsRelatedFormIdForStandaloneTab() {
        FormDetailTab tab = new FormDetailTab();
        tab.setEnable(true);
        tab.setInternalKey(InternalDetailTab.CUSTOMER_OWNER_RECORD.name());
        when(moduleFormCacheService.getConfig("customer", "org-1")).thenReturn(config(tab));
        when(handler.supports(any(DetailTabQueryContext.class))).thenReturn(true);
        PagerWithOption<?> page = new PagerWithOption<>();
        doReturn(page).when(handler).page(any(DetailTabQueryContext.class));

        DetailTabPageRequest request = new DetailTabPageRequest();
        request.setRelatedFormId(InternalDetailTab.CUSTOMER_OWNER_RECORD.name());
        PagerWithOption<?> response = service.page("customer", "customer-1", request, "user-1", "org-1");

        assertEquals(page, response);
    }

    private FormDetailTab internalTab(boolean enabled) {
        FormDetailTab tab = new FormDetailTab();
        tab.setEnable(enabled);
        tab.setInternalKey(InternalDetailTab.CUSTOMER_OPPORTUNITY.name());
        tab.setRelatedForm(new OptionDTO("opportunity", "商机"));
        return tab;
    }

    private ModuleFormConfigDTO config(FormDetailTab tab) {
        FormProp prop = new FormProp();
        prop.setDetailTabs(List.of(tab));
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFormProp(prop);
        return config;
    }
}
