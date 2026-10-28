package cn.cordys.crm.search;

import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.search.domain.UserSearchConfig;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GlobalSearchConfigValidationTest {
    private final BaseSearchService<BasePageRequest, Object> service = new BaseSearchService<>() {
    };
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
        new Translator().setMessageSource(previousMessages);
    }

    @ParameterizedTest
    @ValueSource(strings = { "searchAdvancedOpportunity", "searchAdvancedClue", "searchAdvancedCluePool" })
    void historicalSqlBusinessKeyIsRejectedForEveryAffectedModule(String moduleType) {
        InputField field = field("field", "name");
        UserSearchConfig config = config(field, moduleType);
        config.setBusinessKey("if(1=1,sleep(2),0)");
        configure(field, config);

        assertThrows(GenericException.class, () -> service.getUserSearchConfigs("user", "org", "form", moduleType));
    }

    @ParameterizedTest
    @ValueSource(strings = { "name,(1+1)", "(1+1)", "name from opportunity", "name;select 1" })
    void sqlTextIsRejectedEvenIfOldFormMetadataContainsTheSameValue(String businessKey) {
        InputField field = field("field", businessKey);
        UserSearchConfig config = config(field, "searchAdvancedOpportunity");
        configure(field, config);

        assertThrows(GenericException.class,
                () -> service.getUserSearchConfigs("user", "org", "form", config.getModuleType()));
    }

    @Test
    void fieldFromAnotherFormIsRejected() {
        InputField field = field("current-field", "name");
        UserSearchConfig config = config(field("other-field", "name"), "searchAdvancedOpportunity");
        configure(field, config);

        assertThrows(GenericException.class,
                () -> service.getUserSearchConfigs("user", "org", "form", config.getModuleType()));
    }

    @Test
    void staleFieldTypeIsRejected() {
        InputField field = field("field", "name");
        UserSearchConfig config = config(field, "searchAdvancedOpportunity");
        config.setType("DATA_SOURCE");
        configure(field, config);

        assertThrows(GenericException.class,
                () -> service.getUserSearchConfigs("user", "org", "form", config.getModuleType()));
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
        configure(field, config);

        assertThrows(GenericException.class,
                () -> service.getUserSearchConfigs("user", "org", "form", config.getModuleType()));
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
        when(configs.selectListByLambda(any())).thenReturn(stored);

        assertSame(stored, service.getUserSearchConfigs("user", "org", "form", "searchAdvancedOpportunity"));
    }

    @Test
    void unconfiguredModuleDoesNotLoadItsFormOrResetOtherConfiguration() {
        List<UserSearchConfig> stored = List.of(config(field("field", "name"), "searchAdvancedClue"));
        when(configs.selectListByLambda(any())).thenReturn(stored);

        assertSame(stored, service.getUserSearchConfigs("user", "org", "form", "searchAdvancedOpportunity"));
        verifyNoInteractions(forms);
    }

    @Test
    void invalidConditionsCannotBecomeAnUnfilteredQuery() {
        var condition = service.getFilterCondition("if(1=1,sleep(2),0)", "keyword", "CONTAINS", "INPUT");

        assertThrows(GenericException.class,
                () -> service.buildCombineSearch(new ArrayList<>(List.of(condition)), new BasePageRequest()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "(1+1)", "name,(1+1)", "if(1=1,sleep(2),0)", "name from opportunity" })
    void sharedConditionBuilderAlsoRejectsSqlBusinessKeysForCountQueries(String businessKey) {
        UserSearchConfig config = config(field("field", businessKey), "searchAdvancedOpportunity");

        assertThrows(GenericException.class,
                () -> service.buildOtherFilterCondition("org", config, "keyword", new ArrayList<>()));
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
        configure(field, config);

        assertSame(config, service.getUserSearchConfigs("user", "org", "form", config.getModuleType()).getFirst());
    }

    private void configure(BaseField field, UserSearchConfig config) {
        ModuleFormConfigDTO form = new ModuleFormConfigDTO();
        form.setFields(List.of(field));
        when(forms.getBusinessFormConfig("form", "org")).thenReturn(form);
        when(configs.selectListByLambda(any())).thenReturn(List.of(config));
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
