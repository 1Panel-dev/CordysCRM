package cn.cordys.crm.search;

import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.search.constants.SearchModuleEnum;
import cn.cordys.crm.search.domain.UserSearchConfig;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.search.service.global.GlobalCluePoolSearchService;
import cn.cordys.crm.search.service.global.GlobalClueSearchService;
import cn.cordys.crm.search.service.global.GlobalCustomerContactSearchService;
import cn.cordys.crm.search.service.global.GlobalCustomerPoolSearchService;
import cn.cordys.crm.search.service.global.GlobalCustomerSearchService;
import cn.cordys.crm.search.service.global.GlobalOpportunitySearchService;
import cn.cordys.crm.search.service.global.GlobalSearchCountService;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtUserRoleMapper;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GlobalSearchConfigValidationTest {
    private final BaseSearchService<BasePageRequest, Object> service = new BaseSearchService<>() {};
    private final ModuleFormCacheService forms = mock(ModuleFormCacheService.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<UserSearchConfig> configs = mock(BaseMapper.class);
    private MessageSource previousMessages;

    @BeforeEach
    void setUp() {
        previousMessages = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        new Translator().setMessageSource(messages);
        ReflectionTestUtils.setField(service, "userSearchConfigBaseMapper", configs);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", forms);
    }

    @AfterEach
    void restoreMessages() {
        PageHelper.clearPage();
        new Translator().setMessageSource(previousMessages);
    }

    @ParameterizedTest
    @MethodSource("globalSearchServices")
    void deletedOnlySearchConfigurationReturnsNoResults(BaseSearchService<BasePageRequest, ?> searchService, String moduleType) {
        var search = spy(searchService);
        doReturn(List.of(ModuleKey.BUSINESS.getKey(), ModuleKey.CUSTOMER.getKey(), ModuleKey.CLUE.getKey())).when(search).getEnabledModules();
        ReflectionTestUtils.setField(search, "userSearchConfigBaseMapper", configs);
        ReflectionTestUtils.setField(search, BaseSearchService.class, "moduleFormCacheService", forms, ModuleFormCacheService.class);
        ModuleFormConfigDTO form = new ModuleFormConfigDTO();
        form.setFields(List.of());
        when(forms.getBusinessFormConfig(anyString(), eq("org"))).thenReturn(form);
        when(configs.selectListByLambda(any())).thenReturn(List.of(config(field("deleted-field", null), moduleType)));
        BasePageRequest request = new BasePageRequest();
        request.setKeyword("keyword");
        request.setCurrent(1);
        request.setPageSize(10);

        assertTrue(search.startSearchNoOption(request, "org", "user").getList().isEmpty());
        verify(search, never()).buildCombineSearch(any(), any());
    }

    private static Stream<Arguments> globalSearchServices() {
        return Stream.of(
                Arguments.of(new GlobalCustomerSearchService(), SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER),
                Arguments.of(new GlobalCustomerPoolSearchService(), SearchModuleEnum.SEARCH_ADVANCED_PUBLIC),
                Arguments.of(new GlobalCustomerContactSearchService(), SearchModuleEnum.SEARCH_ADVANCED_CONTACT),
                Arguments.of(new GlobalClueSearchService(), SearchModuleEnum.SEARCH_ADVANCED_CLUE),
                Arguments.of(new GlobalCluePoolSearchService(), SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL),
                Arguments.of(new GlobalOpportunitySearchService(), SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY)
        );
    }

    @Test
    void deletedOnlySearchConfigurationsHaveZeroCountsForEveryModule() {
        GlobalSearchCountService search = spy(new GlobalSearchCountService());
        doReturn(List.of(ModuleKey.BUSINESS.getKey(), ModuleKey.CUSTOMER.getKey(), ModuleKey.CLUE.getKey())).when(search).getEnabledModules();
        doReturn(List.of()).when(search).getCustomerIds("keyword", "org");
        ExtUserRoleMapper roles = mock(ExtUserRoleMapper.class);
        when(roles.selectPermissionsByUserId(any())).thenReturn(List.of(
                PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ,
                PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ, PermissionConstants.CLUE_MANAGEMENT_READ,
                PermissionConstants.CLUE_MANAGEMENT_POOL_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ));
        ReflectionTestUtils.setField(search, "extUserRoleMapper", roles);
        ReflectionTestUtils.setField(search, "userSearchConfigBaseMapper", configs);
        ReflectionTestUtils.setField(search, "moduleFormCacheService", forms);
        ModuleFormConfigDTO form = new ModuleFormConfigDTO();
        form.setFields(List.of());
        when(forms.getBusinessFormConfig(anyString(), eq("org"))).thenReturn(form);
        when(configs.selectListByLambda(any())).thenReturn(SearchModuleEnum.VALUES.stream()
                .map(moduleType -> config(field("deleted-field", null), moduleType)).toList());

        var counts = search.startSearchModuleCount("keyword", "org", "user");

        assertEquals(SearchModuleEnum.VALUES.size(), counts.size());
        assertTrue(counts.stream().allMatch(count -> count.getCount() == 0));
        verify(search, never()).buildCombineSearch(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"searchAdvancedCustomer", "searchAdvancedPublic", "searchAdvancedContact",
            "searchAdvancedOpportunity", "searchAdvancedClue", "searchAdvancedCluePool"})
    void historicalSqlBusinessKeyIsRejectedForEveryAffectedModule(String moduleType) {
        InputField field = field("field", "name");
        UserSearchConfig config = config(field, moduleType);
        config.setBusinessKey("if(1=1,sleep(2),0)");
        configure(field);

        assertThrows(GenericException.class, () -> service.getModuleSearchConfigs(List.of(config), "org", "form", moduleType));
    }

    @ParameterizedTest
    @ValueSource(strings = {"name,(1+1)", "(1+1)", "name from opportunity", "name;select 1"})
    void sqlTextIsRejectedEvenIfOldFormMetadataContainsTheSameValue(String businessKey) {
        InputField field = field("field", businessKey);
        UserSearchConfig config = config(field, "searchAdvancedOpportunity");
        configure(field);

        assertThrows(GenericException.class, () -> service.getModuleSearchConfigs(List.of(config), "org", "form", config.getModuleType()));
    }

    @Test
    void missingFieldIsIgnored() {
        InputField field = field("current-field", "name");
        UserSearchConfig config = config(field("other-field", "name"), "searchAdvancedOpportunity");
        configure(field);

        assertTrue(service.getModuleSearchConfigs(List.of(config), "org", "form", config.getModuleType()).isEmpty());
    }

    @Test
    void staleFieldTypeIsRejected() {
        InputField field = field("field", "name");
        UserSearchConfig config = config(field, "searchAdvancedOpportunity");
        config.setType("DATA_SOURCE");
        configure(field);

        assertThrows(GenericException.class, () -> service.getModuleSearchConfigs(List.of(config), "org", "form", config.getModuleType()));
    }

    @Test
    void changedDataSourceIsRejected() {
        DatasourceField field = new DatasourceField();
        field.setId("field");
        field.setType("DATA_SOURCE");
        field.setBusinessKey("customerId");
        field.setDataSourceType("CUSTOMER");
        UserSearchConfig config = config(field, "searchAdvancedOpportunity");
        config.setDataSourceType("CONTACT");
        configure(field);

        assertThrows(GenericException.class, () -> service.getModuleSearchConfigs(List.of(config), "org", "form", config.getModuleType()));
    }

    @Test
    void validBusinessAndCustomFieldsKeepTheirConfiguration() {
        InputField businessField = field("business-field", "contactId");
        InputField customField = field("custom-field", null);
        List<UserSearchConfig> stored = List.of(config(businessField, "searchAdvancedOpportunity"),
                config(customField, "searchAdvancedOpportunity"));
        ModuleFormConfigDTO form = new ModuleFormConfigDTO();
        form.setFields(List.of(businessField, customField));
        when(forms.getBusinessFormConfig("form", "org")).thenReturn(form);

        assertEquals(stored, service.getModuleSearchConfigs(stored, "org", "form", "searchAdvancedOpportunity"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"searchAdvancedCustomer", "searchAdvancedPublic", "searchAdvancedContact",
            "searchAdvancedClue", "searchAdvancedCluePool", "searchAdvancedOpportunity"})
    void deletedFieldDoesNotPreventSearchingRemainingFields(String moduleType) {
        InputField field = field("current-field", null);
        UserSearchConfig current = config(field, moduleType);
        UserSearchConfig deleted = config(field("deleted-field", null), moduleType);
        UserSearchConfig otherModule = config(field("other-field", null), "otherModule");
        configure(field);

        var valid = service.getModuleSearchConfigs(List.of(deleted, current, otherModule), "org", "form", moduleType);

        assertEquals(List.of(current), valid);
        var conditions = new ArrayList<FilterCondition>();
        valid.forEach(config -> service.buildOtherFilterCondition("org", config, "keyword", conditions));
        BasePageRequest request = new BasePageRequest();
        service.buildCombineSearch(conditions, request);
        assertEquals("current-field", request.getCombineSearch().getConditions().getFirst().getName());
        assertEquals("keyword", request.getCombineSearch().getConditions().getFirst().getValue());
    }

    @Test
    void unconfiguredModuleDoesNotLoadItsFormOrResetOtherConfiguration() {
        List<UserSearchConfig> stored = List.of(config(field("field", "name"), "searchAdvancedClue"));

        assertTrue(service.getModuleSearchConfigs(stored, "org", "form", "searchAdvancedOpportunity").isEmpty());
        verifyNoInteractions(forms);
    }

    @Test
    void invalidConditionsCannotBecomeAnUnfilteredQuery() {
        var condition = service.getFilterCondition("if(1=1,sleep(2),0)", "keyword", "CONTAINS", "INPUT");

        assertThrows(GenericException.class, () -> service.buildCombineSearch(new ArrayList<>(List.of(condition)), new BasePageRequest()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"(1+1)", "name,(1+1)", "if(1=1,sleep(2),0)", "name from opportunity"})
    void sharedConditionBuilderAlsoRejectsSqlBusinessKeysForCountQueries(String businessKey) {
        UserSearchConfig config = config(field("field", businessKey), "searchAdvancedOpportunity");

        assertThrows(GenericException.class, () -> service.buildOtherFilterCondition("org", config, "keyword", new ArrayList<>()));
    }

    @Test
    void validDataSourceConfigurationKeepsItsSource() {
        DatasourceField field = new DatasourceField();
        field.setId("field");
        field.setType("DATA_SOURCE");
        field.setBusinessKey("customerId");
        field.setDataSourceType("CUSTOMER");
        UserSearchConfig config = config(field, "searchAdvancedOpportunity");
        config.setDataSourceType("CUSTOMER");
        configure(field);

        assertSame(config, service.getModuleSearchConfigs(List.of(config), "org", "form", config.getModuleType()).getFirst());
    }

    private void configure(BaseField field) {
        ModuleFormConfigDTO form = new ModuleFormConfigDTO();
        form.setFields(List.of(field));
        when(forms.getBusinessFormConfig("form", "org")).thenReturn(form);
    }

    private InputField field(String id, String businessKey) {
        InputField field = new InputField();
        field.setId(id);
        field.setType("INPUT");
        field.setBusinessKey(businessKey);
        return field;
    }

    private UserSearchConfig config(BaseField field, String moduleType) {
        UserSearchConfig config = new UserSearchConfig();
        config.setFieldId(field.getId());
        config.setBusinessKey(field.getBusinessKey());
        config.setType(field.getType());
        config.setModuleType(moduleType);
        config.setUserId("user");
        config.setOrganizationId("org");
        return config;
    }
}
