package cn.cordys.common.permission;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import cn.cordys.crm.approval.dto.response.StatusPermissionSettingResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.request.ClueTransitionCustomerRequest;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.service.ClueResourceAccessContextProvider;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.customer.controller.CustomerContactController;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.service.CustomerContactResourceAccessContextProvider;
import cn.cordys.crm.customer.service.CustomerResourceAccessContextProvider;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.service.OpportunityQuotationResourceAccessContextProvider;
import cn.cordys.crm.opportunity.service.OpportunityResourceAccessContextProvider;
import cn.cordys.crm.product.controller.ProductController;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.product.service.ProductResourceAccessContextProvider;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ResourcePermissionServiceTest {
    private final ResourcePermissionService service = new ResourcePermissionService();
    private final DataScopeService dataScope = spy(new DataScopeService());
    private final OpportunityResourceAccessContextProvider opportunities =
            new OpportunityResourceAccessContextProvider();
    private final OpportunityQuotationResourceAccessContextProvider quotations =
            new OpportunityQuotationResourceAccessContextProvider();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Opportunity> opportunityMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<OpportunityQuotation> quotationMapper = mock(BaseMapper.class);
    private final Opportunity opportunity = new Opportunity();
    private final OpportunityQuotation quotation = new OpportunityQuotation();
    private final ClueResourceAccessContextProvider clues = new ClueResourceAccessContextProvider();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Clue> clueMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerContact> contactMapper = mock(BaseMapper.class);
    private final Clue clue = new Clue();
    private final ClueService conversion = new ClueService();
    private final CustomerService customers = mock(CustomerService.class);
    private final CommonNoticeSendService notices = mock(CommonNoticeSendService.class);
    private final ClueTransitionCustomerRequest conversionRequest = new ClueTransitionCustomerRequest();
    private final ExtClueMapper extClueMapper = mock(ExtClueMapper.class);
    private final ExtApprovalCommentMapper commentMentions = mock(ExtApprovalCommentMapper.class);
    private MessageSource previousMessageSource;
    private LocaleContext previousLocaleContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "dataScopeService", dataScope);
        ReflectionTestUtils.setField(service, "extApprovalCommentMapper", commentMentions);
        ReflectionTestUtils.setField(opportunities, "opportunityMapper", opportunityMapper);
        ReflectionTestUtils.setField(quotations, "quotationMapper", quotationMapper);
        ReflectionTestUtils.setField(clues, "clueMapper", clueMapper);
        register(List.of(opportunities, quotations, clues));
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
        doReturn(self).when(dataScope).getDeptDataPermission("sales", "org",
                PermissionConstants.CLUE_MANAGEMENT_UPDATE);
        clue.setId("clue-id");
        clue.setOrganizationId("org");
        clue.setOwner("sales");
        clue.setName("示例线索");
        clue.setPhone("00000000000");
        clue.setApprovalStatus("NONE");
        when(clueMapper.selectByPrimaryKey(clue.getId())).thenReturn(clue);
        when(clueMapper.selectByIds(List.of(clue.getId()))).thenReturn(List.of(clue));
        ReflectionTestUtils.setField(conversion, "clueMapper", clueMapper);
        ReflectionTestUtils.setField(conversion, "extClueMapper", extClueMapper);
        ReflectionTestUtils.setField(conversion, "resourcePermissionService", service);
        ReflectionTestUtils.setField(conversion, "customerService", customers);
        ReflectionTestUtils.setField(conversion, "customerContactMapper", contactMapper);
        ReflectionTestUtils.setField(conversion, "commonNoticeSendService", notices);
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        conversionRequest.setClueId(clue.getId());
        conversionRequest.setName("示例客户");
        Customer customer = new Customer();
        customer.setId("customer-id");
        customer.setOwner("sales");
        when(customers.add(conversionRequest, "sales", "org")).thenReturn(customer);
        when(contactMapper.selectListByLambda(any())).thenReturn(List.of(new CustomerContact()));
        // 条件更新命中未转化线索时返回 1, 返回 0 表示已被并发请求抢先转化
        when(extClueMapper.updateTransitionIfAbsent(eq(clue.getId()), eq("org"), eq(FormKey.CUSTOMER.name()),
                eq(customer.getId()), eq("sales"), anyLong())).thenReturn(1);

        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        previousLocaleContext = LocaleContextHolder.getLocaleContext();
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/cordys-crm");
        messages.setDefaultEncoding("UTF-8");
        new Translator().setMessageSource(messages);
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    @AfterEach
    void restoreMessages() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        new Translator().setMessageSource(previousMessageSource);
        LocaleContextHolder.setLocaleContext(previousLocaleContext);
        RequestContextHolder.resetRequestAttributes();
    }

    private void register(List<ResourceAccessContextProvider> providers) {
        ReflectionTestUtils.setField(service, "contextProviders", providers);
        service.initializeProviders();
    }

    private MockedStatic<PermissionUtils> mockPermissions() {
        // 全局 subclass 引擎不支持静态方法，仅权限测试显式使用 inline 引擎。
        return mockStatic(PermissionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
    }

    private void withRolePermission(Runnable action) {
        try (MockedStatic<PermissionUtils> roles = mockPermissions()) {
            roles.when(() -> PermissionUtils.hasPermission("permission")).thenReturn(true);
            roles.when(() -> PermissionUtils.hasPermission(PermissionConstants.PRODUCT_MANAGEMENT_EXPORT))
                    .thenReturn(true);
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
    @ValueSource(strings = { "opportunity", "quotation" })
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
    @ValueSource(strings = { "opportunity", "quotation" })
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
        scanner.addIncludeFilter(
                new org.springframework.core.type.filter.AssignableTypeFilter(ResourceAccessContextProvider.class));
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
        try (MockedStatic<PermissionUtils> ignored = mockPermissions()) {
            assertThrows(GenericException.class, () -> single("opportunity"));
            assertThrows(GenericException.class, () -> batch("quotation"));
        }
    }

    @Test
    void explicitRoleOnlyChecksRemainSupported() {
        withRolePermission(() -> {
            assertDoesNotThrow(() -> service.checkResourcePermission("permission", "id", "", "sales", "org"));
            assertDoesNotThrow(
                    () -> service.checkBatchResourcePermission("permission", List.of("id"), "", "sales", "org"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "customerContactExportSelect", "batchUpdate" })
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
        try (MockedStatic<PermissionUtils> roles = mockPermissions()) {
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
        try (MockedStatic<PermissionUtils> ignored = mockPermissions()) {
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

    @ParameterizedTest
    @ValueSource(strings = { "customer-role", "clue-role", "other-owner", "other-org", "missing", "no-owner",
            "blank-id" })
    void clueConversionRejectsUnauthorizedSourcesBeforeWriting(String deniedReason) {
        // 客户新增的全部范围不能扩大线索编辑的本人范围。
        DeptDataPermissionDTO all = new DeptDataPermissionDTO();
        all.setAll(true);
        doReturn(all).when(dataScope).getDeptDataPermission("sales", "org",
                PermissionConstants.CUSTOMER_MANAGEMENT_ADD);
        switch (deniedReason) {
        case "other-owner" -> clue.setOwner("other-sales");
        case "other-org" -> clue.setOrganizationId("other-org");
        case "missing" -> when(clueMapper.selectByPrimaryKey(clue.getId())).thenReturn(null);
        case "no-owner" -> clue.setOwner(null);
        case "blank-id" -> conversionRequest.setClueId(" ");
        }
        try (MockedStatic<PermissionUtils> roles = mockPermissions()) {
            roles.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_ADD))
                    .thenReturn(!"customer-role".equals(deniedReason));
            roles.when(() -> PermissionUtils.hasPermission(PermissionConstants.CLUE_MANAGEMENT_UPDATE))
                    .thenReturn(!"clue-role".equals(deniedReason));
            assertConversionDenied();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "sales", "other-sales" })
    void authorizedClueConversionChecksSourceBeforeCreatingCustomer(String owner) {
        clue.setOwner(owner);
        if (!"sales".equals(owner)) {
            DeptDataPermissionDTO all = new DeptDataPermissionDTO();
            all.setAll(true);
            doReturn(all).when(dataScope).getDeptDataPermission("sales", "org",
                    PermissionConstants.CLUE_MANAGEMENT_UPDATE);
        }
        try (MockedStatic<PermissionUtils> roles = mockPermissions()) {
            roles.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_ADD))
                    .thenReturn(true);
            roles.when(() -> PermissionUtils.hasPermission(PermissionConstants.CLUE_MANAGEMENT_UPDATE))
                    .thenReturn(true);
            assertDoesNotThrow(() -> conversion.transitionCustomer(conversionRequest, "sales", "org"));
        }
        var order = inOrder(dataScope, customers, extClueMapper);
        order.verify(dataScope).hasDataPermission("sales", "org", owner, PermissionConstants.CLUE_MANAGEMENT_UPDATE);
        order.verify(customers).add(conversionRequest, "sales", "org");
        // 转化关系由条件更新写入, 不再直接改内存对象后调用 clueMapper.update
        order.verify(extClueMapper).updateTransitionIfAbsent(eq(clue.getId()), eq("org"), eq(FormKey.CUSTOMER.name()),
                eq("customer-id"), eq("sales"), anyLong());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<CustomerContact>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(contactMapper).selectListByLambda(query.capture());
        assertEquals("org", query.getValue().getParams().get("organization_id_eq"));
        assertEquals(clue.getPhone(), query.getValue().getParams().get("phone_eq"));
    }

    @Test
    void clueAllScopeStillRejectsForeignAndIncompleteSelections() {
        DeptDataPermissionDTO all = new DeptDataPermissionDTO();
        all.setAll(true);
        doReturn(all).when(dataScope).getDeptDataPermission("sales", "org", "permission");
        withRolePermission(() -> {
            assertDoesNotThrow(() -> single("clue"));
            assertDoesNotThrow(() -> batch("clue"));
            clue.setOrganizationId("other-org");
            assertThrows(GenericException.class, () -> single("clue"));
            assertThrows(GenericException.class, () -> batch("clue"));
            clue.setOrganizationId("org");
            clue.setOwner(null);
            assertThrows(GenericException.class, () -> single("clue"));
            assertThrows(GenericException.class, () -> batch("clue"));
            clue.setOwner("sales");
            when(clueMapper.selectByIds(List.of(clue.getId(), "missing"))).thenReturn(List.of(clue));
            assertThrows(GenericException.class, () -> service.checkBatchResourcePermission("permission",
                    List.of(clue.getId(), "missing"), FormKeyConstants.CLUE, "sales", "org"));
            assertThrows(GenericException.class, () -> service.checkResourcePermission("permission",
                    clue.getId(), FormKeyConstants.CLUE, "sales", null));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "role", "scope", "status" })
    void approvalTaskCannotReplaceClueConversionPermission(String deniedReason) {
        @SuppressWarnings("unchecked")
        BaseMapper<ApprovalTask> taskMapper = mock(BaseMapper.class);
        @SuppressWarnings("unchecked")
        BaseMapper<ApprovalInstance> instanceMapper = mock(BaseMapper.class);
        ApprovalTask task = new ApprovalTask();
        task.setApproverId("sales");
        task.setInstanceId("instance-id");
        task.setNodeId("node-id");
        task.setStatus(ApprovalStatus.APPROVING.name());
        task.setType(ApprovalTaskType.NL.name());
        ApprovalInstance instance = new ApprovalInstance();
        instance.setResourceId(clue.getId());
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        instance.setCurrentNodeId("node-id");
        when(taskMapper.selectByPrimaryKey("task-id")).thenReturn(task);
        when(instanceMapper.selectByPrimaryKey("instance-id")).thenReturn(instance);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
        if ("scope".equals(deniedReason)) {
            clue.setOwner("other-sales");
        } else if ("status".equals(deniedReason)) {
            clue.setApprovalStatus("APPROVING");
            StatusPermissionDTO status = new StatusPermissionDTO();
            status.setApprovalStatus(clue.getApprovalStatus());
            status.setPermission(PermissionConstants.CLUE_MANAGEMENT_UPDATE);
            status.setEnabled(false);
            StatusPermissionSettingResponse setting = new StatusPermissionSettingResponse();
            setting.setStatusPermissions(List.of(status));
            ApprovalFlowService approvals = mock(ApprovalFlowService.class);
            when(approvals.getStatusPermissionsByFormType(FormKeyConstants.CLUE, "org")).thenReturn(setting);
            ReflectionTestUtils.setField(service, "approvalFlowService", approvals);
        }
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Approval-Task-Id", "task-id");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try (MockedStatic<PermissionUtils> roles = mockPermissions()) {
            roles.when(() -> PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_ADD))
                    .thenReturn(true);
            roles.when(() -> PermissionUtils.hasPermission(PermissionConstants.CLUE_MANAGEMENT_UPDATE))
                    .thenReturn(!"role".equals(deniedReason));
            // 审批接口保留待办授权，转换接口必须继续检查普通业务权限。
            assertDoesNotThrow(() -> service.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_UPDATE,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));
            assertConversionDenied();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void completedApprovalTaskCannotGrantResourcePermission() {
        ApprovalTask task = pendingTask("task-id");
        ApprovalInstance instance = pendingInstance(task);
        bindApprovalTask(task, instance);
        // 负责人不是本人且无角色权限位, 只能依赖处理中的待办授权
        clue.setOwner("other-sales");
        withApprovalTaskHeader(task.getId());

        try (MockedStatic<PermissionUtils> ignored = mockPermissions()) {
            Runnable check = () -> service.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_READ,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org");

            assertTrue(service.hasApprovalTaskPermission(clue.getId(), "sales"));
            assertDoesNotThrow(check::run);

            // 已处理的待办 ID 不能再用于放权
            task.setStatus(ApprovalStatus.APPROVED.name());
            assertTrue(service.hasApprovalTaskPermission(clue.getId(), "sales"));
            assertDoesNotThrow(check::run);

            // 审批实例已结束
            task.setStatus(ApprovalStatus.APPROVING.name());
            instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
            assertDoesNotThrow(check::run);

            // 任务不在实例当前审批节点
            instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
            instance.setCurrentNodeId("next-node");
            assertDoesNotThrow(check::run);

            // 抄送任务不代表审批权限
            instance.setCurrentNodeId(task.getNodeId());
            task.setType(ApprovalTaskType.CC.name());
            assertDoesNotThrow(check::run);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { PermissionConstants.CLUE_MANAGEMENT_DELETE, PermissionConstants.CLUE_MANAGEMENT_TRANSFER,
            PermissionConstants.CLUE_MANAGEMENT_RECYCLE })
    void approvalTaskCannotAuthorizeDestructiveOperations(String permission) {
        ApprovalTask task = pendingTask("task-id");
        ApprovalInstance instance = pendingInstance(task);
        bindApprovalTask(task, instance);
        clue.setOwner("other-sales");
        withApprovalTaskHeader(task.getId());

        try (MockedStatic<PermissionUtils> roles = mockPermissions()) {
            // 待办仍可用于审批过程中的查看
            assertDoesNotThrow(() -> service.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_READ,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));
            // 删除/转移/回收等操作不能凭待办 ID 跳过权限位与数据权限
            assertThrows(GenericException.class, () -> service.checkResourcePermission(permission,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));

            // 真正拥有权限的用户不受影响
            clue.setOwner("sales");
            roles.when(() -> PermissionUtils.hasPermission(permission)).thenReturn(true);
            DeptDataPermissionDTO self = new DeptDataPermissionDTO();
            self.setSelf(true);
            doReturn(self).when(dataScope).getDeptDataPermission("sales", "org", permission);
            assertDoesNotThrow(() -> service.checkResourcePermission(permission,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void effectiveCommentMentionGrantsOnlyTheMentionedResourceReadWithoutRolePermission(boolean header) {
        bindCommentMention("GET", header);
        try (MockedStatic<PermissionUtils> roles = mockPermissions()) {
            assertTrue(service.hasApprovalCommentMentionPermission(clue.getId(), "sales", "org"));
            assertTrue(service.hasApprovalViewPermission(clue.getId(), "sales", "org"));
            assertDoesNotThrow(() -> service.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_READ,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));
            roles.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {PermissionConstants.CLUE_MANAGEMENT_UPDATE, PermissionConstants.CLUE_MANAGEMENT_DELETE,
            PermissionConstants.CLUE_MANAGEMENT_TRANSFER})
    void commentMentionCannotAuthorizeWritesEvenOnGetRequests(String permission) {
        bindCommentMention("GET", true);
        try (MockedStatic<PermissionUtils> ignored = mockPermissions()) {
            assertThrows(GenericException.class, () -> service.checkResourcePermission(permission,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));
        }
        verifyNoInteractions(commentMentions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "other-user", "other-org", "other-resource", "no-context", "POST"})
    void rejectsCommentMentionOutsideItsUserOrganizationResourceOrGetContext(String reason) {
        bindCommentMention("POST".equals(reason) ? "POST" : "GET", true);
        String resourceId = "other-resource".equals(reason) ? "another-resource" : clue.getId();
        String userId = "other-user".equals(reason) ? "other-user" : "sales";
        String orgId = "other-org".equals(reason) ? "other-org" : "org";
        if ("unknown".equals(reason)) {
            when(commentMentions.selectMentionInstance("mention-id", "sales", "org")).thenReturn(null);
        } else if ("no-context".equals(reason)) {
            RequestContextHolder.resetRequestAttributes();
        }
        try (MockedStatic<PermissionUtils> ignored = mockPermissions()) {
            assertFalse(service.hasApprovalCommentMentionPermission(resourceId, userId, orgId));
            assertThrows(GenericException.class, () -> service.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_READ,
                    resourceId, FormKeyConstants.CLUE, userId, orgId));
        }
    }

    @Test
    void removingMentionImmediatelyRevokesItsReadGrantAndDoesNotAuthorizeBatchReads() {
        bindCommentMention("GET", true);
        try (MockedStatic<PermissionUtils> ignored = mockPermissions()) {
            assertDoesNotThrow(() -> service.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_READ,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));
            assertThrows(GenericException.class, () -> service.checkBatchResourcePermission(PermissionConstants.CLUE_MANAGEMENT_READ,
                    List.of(clue.getId()), FormKeyConstants.CLUE, "sales", "org"));

            when(commentMentions.selectMentionInstance("mention-id", "sales", "org")).thenReturn(null);
            assertFalse(service.hasApprovalViewPermission(clue.getId(), "sales", "org"));
            assertThrows(GenericException.class, () -> service.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_READ,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));
        }
    }

    @Test
    void commentMentionFormPermissionIsLimitedToTheInstancesCustomForm() {
        ApprovalInstance instance = bindCommentMention("GET", false);
        instance.setType("custom-form-a");

        assertTrue(service.hasApprovalCommentMentionFormPermission("custom-form-a", "sales", "org"));
        assertFalse(service.hasApprovalCommentMentionFormPermission("custom-form-b", "sales", "org"));
        assertFalse(service.hasApprovalCommentMentionFormPermission("custom-form-a", "sales", "other-org"));

        instance.setType(FormKeyConstants.CONTRACT);
        assertFalse(service.hasApprovalCommentMentionFormPermission(FormKeyConstants.CONTRACT, "sales", "org"));
    }

    @Test
    void commentMentionCannotReadAnotherResourceTypeWithTheSameResourceId() {
        ApprovalInstance instance = bindCommentMention("GET", true);
        instance.setType(FormKeyConstants.CUSTOMER);
        try (MockedStatic<PermissionUtils> ignored = mockPermissions()) {
            assertFalse(service.hasApprovalViewPermission(clue.getId(), "sales", "org", FormKeyConstants.CLUE));
            assertThrows(GenericException.class, () -> service.checkResourcePermission(PermissionConstants.CLUE_MANAGEMENT_READ,
                    clue.getId(), FormKeyConstants.CLUE, "sales", "org"));
        }
    }

    private ApprovalInstance bindCommentMention(String method, boolean header) {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("mentioned-instance");
        instance.setResourceId(clue.getId());
        instance.setType(FormKeyConstants.CLUE);
        when(commentMentions.selectMentionInstance("mention-id", "sales", "org")).thenReturn(instance);
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/clue/get/" + clue.getId());
        if (header) {
            request.addHeader("Approval-Comment-Mention-Id", "mention-id");
        } else {
            request.addParameter("commentMentionId", "mention-id");
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return instance;
    }

    private ApprovalTask pendingTask(String taskId) {
        ApprovalTask task = new ApprovalTask();
        task.setId(taskId);
        task.setApproverId("sales");
        task.setInstanceId("instance-id");
        task.setNodeId("node-id");
        task.setStatus(ApprovalStatus.APPROVING.name());
        task.setType(ApprovalTaskType.NL.name());
        return task;
    }

    private ApprovalInstance pendingInstance(ApprovalTask task) {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(task.getInstanceId());
        instance.setResourceId(clue.getId());
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        instance.setCurrentNodeId(task.getNodeId());
        return instance;
    }

    private void bindApprovalTask(ApprovalTask task, ApprovalInstance instance) {
        @SuppressWarnings("unchecked")
        BaseMapper<ApprovalTask> taskMapper = mock(BaseMapper.class);
        @SuppressWarnings("unchecked")
        BaseMapper<ApprovalInstance> instanceMapper = mock(BaseMapper.class);
        when(taskMapper.selectByPrimaryKey(task.getId())).thenReturn(task);
        when(instanceMapper.selectByPrimaryKey(instance.getId())).thenReturn(instance);
        ReflectionTestUtils.setField(service, "approvalTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "approvalInstanceMapper", instanceMapper);
    }

    private void withApprovalTaskHeader(String taskId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Approval-Task-Id", taskId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void assertConversionDenied() {
        GenericException error = assertThrows(GenericException.class,
                () -> conversion.transitionCustomer(conversionRequest, "sales", "org"));
        assertEquals(CrmHttpResultCode.FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(customers, contactMapper, notices, extClueMapper);
        assertNull(clue.getTransitionId());
    }
}
