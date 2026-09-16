package cn.cordys.common.permission;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.crm.customer.controller.CustomerContactController;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.service.CustomerContactResourceAccessContextProvider;
import cn.cordys.crm.customer.service.CustomerResourceAccessContextProvider;
import cn.cordys.crm.product.controller.ProductController;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.product.service.ProductResourceAccessContextProvider;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.service.OpportunityResourceAccessContextProvider;
import cn.cordys.crm.opportunity.service.OpportunityQuotationResourceAccessContextProvider;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourcePermissionServiceTest {
    private final ResourcePermissionService service = new ResourcePermissionService();
    private final DataScopeService dataScope = spy(new DataScopeService());
    private final OpportunityResourceAccessContextProvider opportunities = new OpportunityResourceAccessContextProvider();
    private final OpportunityQuotationResourceAccessContextProvider quotations = new OpportunityQuotationResourceAccessContextProvider();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Opportunity> opportunityMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<OpportunityQuotation> quotationMapper = mock(BaseMapper.class);
    private final Opportunity opportunity = new Opportunity();
    private final OpportunityQuotation quotation = new OpportunityQuotation();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(opportunities, "opportunityMapper", opportunityMapper);
        ReflectionTestUtils.setField(quotations, "quotationMapper", quotationMapper);
        register(List.of(opportunities, quotations));
        opportunity.setId("opportunity-id");
        opportunity.setOrganizationId("org");
        opportunity.setOwner("sales");
        quotation.setId("quotation-id");
        quotation.setOpportunityId(opportunity.getId());
        quotation.setOrganizationId("org");
        quotation.setCreateUser("sales");
        when(opportunityMapper.selectByPrimaryKey(opportunity.getId())).thenReturn(opportunity);
        when(quotationMapper.selectByPrimaryKey(quotation.getId())).thenReturn(quotation);
        when(opportunityMapper.selectByIds(List.of(opportunity.getId()))).thenReturn(List.of(opportunity));
        when(quotationMapper.selectByIds(List.of(quotation.getId()))).thenReturn(List.of(quotation));
        DeptDataPermissionDTO self = new DeptDataPermissionDTO();
        self.setSelf(true);
        doReturn(self).when(dataScope).getDeptDataPermission("sales", "org", "permission");
    }

    private void register(List<ResourceAccessContextProvider> providers) {
        ReflectionTestUtils.setField(service, "contextProviders", providers);
        service.initializeProviders();
    }

    private void withRolePermission(Runnable action) {
        try (MockedStatic<PermissionUtils> roles = mockStatic(PermissionUtils.class)) {
            roles.when(() -> PermissionUtils.hasPermission("permission")).thenReturn(true);
            roles.when(() -> PermissionUtils.hasPermission(PermissionConstants.PRODUCT_MANAGEMENT_EXPORT)).thenReturn(true);
            action.run();
        }
    }

    private void single(String form) {
        service.checkResourcePermission("permission", form + "-id", form, "sales", "org");
    }

    private void batch(String form) {
        service.checkBatchResourcePermission("permission", List.of(form + "-id"), form, "sales", "org");
    }

    @ParameterizedTest
    @ValueSource(strings = {"opportunity", "quotation"})
    void selfScopeChecksSingleResourcesAndOpportunityBatches(String form) {
        withRolePermission(() -> {
            assertDoesNotThrow(() -> single(form));
            assertDoesNotThrow(() -> batch(form));
            opportunity.setOwner("other-sales");
            quotation.setCreateUser("other-sales");
            assertThrows(GenericException.class, () -> single(form));
            if ("quotation".equals(form)) {
                assertDoesNotThrow(() -> batch(form));
            } else {
                assertThrows(GenericException.class, () -> batch(form));
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"opportunity", "quotation"})
    void singleResourcesAndOpportunityBatchesRequireOrganizationAndOwner(String form) {
        withRolePermission(() -> {
            opportunity.setOrganizationId("other-org");
            quotation.setOrganizationId("other-org");
            assertThrows(GenericException.class, () -> single(form));
            assertThrows(GenericException.class, () -> batch(form));
            opportunity.setOrganizationId("org");
            quotation.setOrganizationId("org");
            opportunity.setOwner(null);
            quotation.setCreateUser(null);
            assertThrows(GenericException.class, () -> single(form));
            if ("quotation".equals(form)) {
                assertDoesNotThrow(() -> batch(form));
            } else {
                assertThrows(GenericException.class, () -> batch(form));
            }
        });
    }

    @Test
    void quotationSelfScopeUsesCreatorInsteadOfOpportunityOwner() {
        withRolePermission(() -> {
            opportunity.setOwner("other-sales");
            assertDoesNotThrow(() -> single("quotation"));
            opportunity.setOwner("sales");
            quotation.setCreateUser("other-sales");
            assertThrows(GenericException.class, () -> single("quotation"));
            verifyNoInteractions(opportunityMapper);
        });
    }

    @Test
    void quotationBatchSkipsOwnerDataScopeButRejectsMissingResources() {
        assertEquals(Map.of(), quotations.batchGetOwnerIds(List.of(quotation.getId()), "org"));
        withRolePermission(() -> {
            opportunity.setOwner("other-sales");
            quotation.setCreateUser("other-sales");
            assertDoesNotThrow(() -> batch("quotation"));
            verifyNoInteractions(dataScope);
            when(quotationMapper.selectByIds(List.of(quotation.getId()))).thenReturn(List.of());
            assertThrows(GenericException.class, () -> batch("quotation"));
        });
    }

    @Test
    void missingProviderContextAndIncompleteOwnerMapAreDenied() {
        ResourceAccessContextProvider provider = mock(ResourceAccessContextProvider.class);
        when(provider.getFormType()).thenReturn("test");
        register(List.of(provider));
        withRolePermission(() -> {
            assertThrows(GenericException.class, () -> single("unknown"));
            assertThrows(GenericException.class, () -> batch("unknown"));
            assertThrows(GenericException.class, () -> single("test"));
            assertThrows(GenericException.class, () -> batch("test"));
            when(provider.batchGetOwnerIds(List.of("one", "missing"), "org"))
                    .thenReturn(Map.of("one", "sales"));
            assertThrows(GenericException.class, () -> service.checkBatchResourcePermission(
                    "permission", List.of("one", "missing"), "test", "sales", "org"));
        });
    }

    @Test
    void duplicateAndBlankRegistrationFailInitialization() {
        assertThrows(IllegalStateException.class, () -> register(List.of(opportunities, opportunities)));
        ResourceAccessContextProvider invalid = mock(ResourceAccessContextProvider.class);
        when(invalid.getFormType()).thenReturn(" ");
        assertThrows(IllegalStateException.class, () -> register(List.of(invalid)));
    }

    @Test
    void allProductionProvidersHaveUniqueFormTypes() throws Exception {
        var scanner = new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new org.springframework.core.type.filter.AssignableTypeFilter(ResourceAccessContextProvider.class));
        List<ResourceAccessContextProvider> providers = new ArrayList<>();
        for (var definition : scanner.findCandidateComponents("cn.cordys.crm")) {
            providers.add((ResourceAccessContextProvider) Class.forName(definition.getBeanClassName())
                    .getDeclaredConstructor().newInstance());
        }
        assertFalse(providers.isEmpty());
        assertDoesNotThrow(() -> register(providers));
        assertEquals("quotation", quotations.getFormType());
        assertEquals("opportunity", opportunities.getFormType());
        assertTrue(providers.stream().anyMatch(provider -> "contact".equals(provider.getFormType())));
    }

    @Test
    void quotationOrganizationAndApprovalStatusArePreserved() {
        quotation.setApprovalStatus("NONE");
        assertEquals("NONE", quotations.getAccessContext(quotation.getId(), "org").getApprovalStatus());
        quotation.setOrganizationId("other-org");
        withRolePermission(() -> {
            assertThrows(GenericException.class, () -> single("quotation"));
            assertThrows(GenericException.class, () -> batch("quotation"));
        });
    }

    @Test
    void springLifecycleRejectsDuplicateProviders() {
        ReflectionTestUtils.setField(service, "contextProviders", List.of(opportunities, opportunities));
        var processor = new org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor();
        processor.setInitAnnotationType(jakarta.annotation.PostConstruct.class);
        assertThrows(org.springframework.beans.factory.BeanCreationException.class,
                () -> processor.postProcessBeforeInitialization(service, "resourcePermissionService"));
    }

    @Test
    void allScopeCanAccessAnotherOwnersResources() {
        DeptDataPermissionDTO all = new DeptDataPermissionDTO();
        all.setAll(true);
        doReturn(all).when(dataScope).getDeptDataPermission("sales", "org", "permission");
        opportunity.setOwner("other-sales");
        quotation.setCreateUser("other-sales");
        withRolePermission(() -> {
            assertDoesNotThrow(() -> single("opportunity"));
            assertDoesNotThrow(() -> single("quotation"));
            assertDoesNotThrow(() -> batch("opportunity"));
            assertDoesNotThrow(() -> batch("quotation"));
        });
    }

    @Test
    void rolePermissionIsStillRequired() {
        try (MockedStatic<PermissionUtils> ignored = mockStatic(PermissionUtils.class)) {
            assertThrows(GenericException.class, () -> single("opportunity"));
            assertThrows(GenericException.class, () -> batch("quotation"));
        }
    }

    @Test
    void explicitRoleOnlyChecksRemainSupported() {
        withRolePermission(() -> {
            assertDoesNotThrow(() -> service.checkResourcePermission("permission", "id", "", "sales", "org"));
            assertDoesNotThrow(() -> service.checkBatchResourcePermission("permission", List.of("id"), "", "sales", "org"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"customerContactExportSelect", "batchUpdate"})
    void contactBatchEndpointsUseContactOwnership(String methodName) {
        CustomerContactResourceAccessContextProvider contacts = new CustomerContactResourceAccessContextProvider();
        CustomerResourceAccessContextProvider customers = new CustomerResourceAccessContextProvider();
        @SuppressWarnings("unchecked")
        BaseMapper<CustomerContact> contactMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(contacts, "contactMapper", contactMapper);
        register(List.of(customers, contacts));
        var method = java.util.Arrays.stream(CustomerContactController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        var annotation = method.getAnnotation(CsBatchPermission.class);
        assertNotNull(annotation);
        assertEquals(FormKeyConstants.CONTACT, annotation.formType());
        CustomerContact contact = new CustomerContact();
        contact.setId("contact-id");
        contact.setOwner("sales");
        when(contactMapper.selectByIds(List.of(contact.getId()))).thenReturn(List.of(contact));
        DeptDataPermissionDTO self = new DeptDataPermissionDTO();
        self.setSelf(true);
        doReturn(self).when(dataScope).getDeptDataPermission("sales", "org", annotation.value());
        Runnable check = () -> service.checkBatchResourcePermission(annotation.value(), List.of(contact.getId()),
                annotation.formType(), "sales", "org");
        try (MockedStatic<PermissionUtils> roles = mockStatic(PermissionUtils.class)) {
            roles.when(() -> PermissionUtils.hasPermission(annotation.value())).thenReturn(true);
            assertDoesNotThrow(check::run);
            contact.setOwner("other-sales");
            assertThrows(GenericException.class, check::run);
            when(contactMapper.selectByIds(List.of(contact.getId()))).thenReturn(List.of());
            assertThrows(GenericException.class, check::run);
        }
    }

    @Test
    void productExportChecksOrganizationWithoutRequiringAnOwner() throws Exception {
        ProductResourceAccessContextProvider products = new ProductResourceAccessContextProvider();
        @SuppressWarnings("unchecked")
        BaseMapper<Product> productMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(products, "productMapper", productMapper);
        register(List.of(opportunities, quotations, products));
        Product product = new Product();
        product.setId("product-id");
        product.setOrganizationId("org");
        when(productMapper.selectByPrimaryKey(product.getId())).thenReturn(product);
        when(productMapper.selectByIds(List.of(product.getId()))).thenReturn(List.of(product));

        var annotation = ProductController.class.getDeclaredMethod("exportSelect", ExportSelectRequest.class)
                .getAnnotation(CsBatchPermission.class);
        assertNotNull(annotation);
        assertEquals(FormKeyConstants.PRODUCT, annotation.formType());
        assertEquals(PermissionConstants.PRODUCT_MANAGEMENT_EXPORT, annotation.value());
        Runnable single = () -> service.checkResourcePermission(annotation.value(), product.getId(),
                annotation.formType(), "sales", "org");
        Runnable batch = () -> service.checkBatchResourcePermission(annotation.value(), List.of(product.getId()),
                annotation.formType(), "sales", "org");
        withRolePermission(() -> {
            assertDoesNotThrow(single::run);
            assertDoesNotThrow(batch::run);
            product.setOrganizationId("other-org");
            assertThrows(GenericException.class, single::run);
            assertThrows(GenericException.class, batch::run);
            product.setOrganizationId("org");
            when(productMapper.selectByPrimaryKey(product.getId())).thenReturn(null);
            when(productMapper.selectByIds(List.of(product.getId()))).thenReturn(List.of());
            assertThrows(GenericException.class, single::run);
            assertThrows(GenericException.class, batch::run);
        });
        verify(dataScope, never()).checkDataPermission(anyString(), anyString(), anyList(), anyString());
        verify(dataScope, never()).hasDataPermission(anyString(), anyString(), anyString(), anyString());
        try (MockedStatic<PermissionUtils> ignored = mockStatic(PermissionUtils.class)) {
            assertThrows(GenericException.class, batch::run);
        }
    }

    @Test
    void mixedProductSelectionCannotHideForeignOrMissingIds() {
        ProductResourceAccessContextProvider products = new ProductResourceAccessContextProvider();
        @SuppressWarnings("unchecked")
        BaseMapper<Product> productMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(products, "productMapper", productMapper);
        register(List.of(products));
        Product own = new Product();
        own.setId("own-product");
        own.setOrganizationId("org");
        Product foreign = new Product();
        foreign.setId("foreign-product");
        foreign.setOrganizationId("other-org");
        when(productMapper.selectByIds(List.of(own.getId(), foreign.getId())))
                .thenReturn(List.of(own, foreign));
        when(productMapper.selectByIds(List.of(own.getId(), "missing"))).thenReturn(List.of(own));
        withRolePermission(() -> {
            assertThrows(GenericException.class, () -> service.checkBatchResourcePermission(
                    PermissionConstants.PRODUCT_MANAGEMENT_EXPORT, List.of(own.getId(), foreign.getId()),
                    FormKeyConstants.PRODUCT, "sales", "org"));
            assertThrows(GenericException.class, () -> service.checkBatchResourcePermission(
                    PermissionConstants.PRODUCT_MANAGEMENT_EXPORT, List.of(own.getId(), "missing"),
                    FormKeyConstants.PRODUCT, "sales", "org"));
            assertThrows(GenericException.class, () -> service.checkBatchResourcePermission(
                    PermissionConstants.PRODUCT_MANAGEMENT_EXPORT, List.of(own.getId()),
                    FormKeyConstants.PRODUCT, "sales", null));
        });
    }

}
