package cn.cordys.crm.search;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.ClueFieldService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.service.OpportunityFieldService;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.search.domain.SearchFieldMaskConfig;
import cn.cordys.crm.search.response.global.GlobalCluePoolResponse;
import cn.cordys.crm.search.response.global.GlobalClueResponse;
import cn.cordys.crm.search.response.global.GlobalOpportunityResponse;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.search.service.global.GlobalCluePoolSearchService;
import cn.cordys.crm.search.service.global.GlobalClueSearchService;
import cn.cordys.crm.search.service.global.GlobalOpportunitySearchService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalSearchEntityQueryTest {

    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-id";
    private static final String RECORD_ID = "record-id";

    @Test
    void opportunityQueriesCurrentOrganizationAndPreservesContactName() {
        GlobalOpportunitySearchService service = new GlobalOpportunitySearchService();
        DataScopeService dataScopeService = prepare(service);
        when(dataScopeService.hasDataPermission(USER_ID, ORG_ID, "owner-id",
                PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)).thenReturn(true);
        ReflectionTestUtils.setField(service, "baseService", mock(BaseService.class));
        ReflectionTestUtils.setField(service, "opportunityFieldService", mock(OpportunityFieldService.class));
        @SuppressWarnings("unchecked")
        BaseMapper<Opportunity> mapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "opportunityMapper", mapper);
        Opportunity opportunity = new Opportunity();
        opportunity.setId(RECORD_ID);
        opportunity.setContactId("contact-id");
        when(mapper.selectListByLambda(any())).thenReturn(List.of(opportunity));
        GlobalOpportunityResponse response = new GlobalOpportunityResponse();
        response.setId(RECORD_ID);
        response.setOwner("owner-id");
        response.setContactName("测试联系人");

        service.buildListData(List.of(response), ORG_ID, USER_ID, List.of(), Set.of(),
                Map.of("contact-field", "contactId"));

        assertScopedQuery(mapper);
        assertEquals("测试联系人", response.getModuleFields().getFirst().getFieldValue());
        assertTrue(response.isHasPermission());
    }

    @Test
    void clueQueriesCurrentOrganizationAndPreservesFieldMasking() {
        GlobalClueSearchService service = new GlobalClueSearchService();
        prepare(service);
        ReflectionTestUtils.setField(service, "baseService", mock(BaseService.class));
        ReflectionTestUtils.setField(service, "clueFieldService", mock(ClueFieldService.class));
        @SuppressWarnings("unchecked")
        BaseMapper<Clue> mapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "clueMapper", mapper);
        Clue clue = new Clue();
        clue.setId(RECORD_ID);
        clue.setContact("测试联系人");
        when(mapper.selectListByLambda(any())).thenReturn(List.of(clue));
        GlobalClueResponse response = new GlobalClueResponse();
        response.setId(RECORD_ID);
        response.setOwner("owner-id");
        SearchFieldMaskConfig mask = new SearchFieldMaskConfig();
        mask.setFieldId("contact-field");
        mask.setBusinessKey("contact");
        mask.setType("INPUT");

        service.buildListData(List.of(response), ORG_ID, USER_ID, List.of(mask), Set.of(),
                Map.of("contact-field", "contact"));

        assertScopedQuery(mapper);
        assertEquals("测****", response.getModuleFields().getFirst().getFieldValue());
    }

    @Test
    void cluePoolQueriesCurrentOrganizationAndPreservesPhoneMasking() {
        GlobalCluePoolSearchService service = new GlobalCluePoolSearchService();
        prepare(service);
        ReflectionTestUtils.setField(service, "clueFieldService", mock(ClueFieldService.class));
        ReflectionTestUtils.setField(service, "cluePoolMapper", mock(BaseMapper.class));
        @SuppressWarnings("unchecked")
        BaseMapper<Clue> mapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "clueMapper", mapper);
        Clue clue = new Clue();
        clue.setId(RECORD_ID);
        clue.setContact("13800001111");
        when(mapper.selectListByLambda(any())).thenReturn(List.of(clue));
        GlobalCluePoolResponse response = new GlobalCluePoolResponse();
        response.setId(RECORD_ID);
        response.setPhone("13800001111");

        service.buildListData(List.of(response), ORG_ID, USER_ID, List.of(), Set.of(), Map.of("phone-field", "contact"),
                List.of("phone-field"));

        assertScopedQuery(mapper);
        assertEquals("13800******", response.getModuleFields().getFirst().getFieldValue());
        assertEquals("13800******", response.getPhone());
    }

    private DataScopeService prepare(BaseSearchService<?, ?> service) {
        ModuleFormCacheService forms = mock(ModuleFormCacheService.class);
        when(forms.getBusinessFormConfig(anyString(), anyString())).thenReturn(new ModuleFormConfigDTO());
        ReflectionTestUtils.setField(service, "moduleFormCacheService", forms);
        ReflectionTestUtils.setField(service, "extProductMapper", mock(ExtProductMapper.class));
        DataScopeService dataScopeService = mock(DataScopeService.class);
        ReflectionTestUtils.setField(service, "dataScopeService", dataScopeService);
        return dataScopeService;
    }

    private <E> void assertScopedQuery(BaseMapper<E> mapper) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<E>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectListByLambda(query.capture());
        assertEquals(List.of(RECORD_ID), query.getValue().getParams().get("array_id"));
        assertEquals(ORG_ID, query.getValue().getParams().get("organization_id_eq"));
        assertTrue(query.getValue().getWhereClause().contains("id IN"));
        assertTrue(query.getValue().getWhereClause().contains("organization_id = #{organization_id_eq}"));
    }
}
