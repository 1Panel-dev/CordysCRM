package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.PoolClueService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("unchecked")
class AttachmentPermissionServiceTest {
    private final AttachmentPermissionService service = new AttachmentPermissionService();
    private final BaseMapper<ApprovalComment> comments = mock(BaseMapper.class);
    private final ExtApprovalTaskMapper tasks = mock(ExtApprovalTaskMapper.class);
    private final ExtApprovalCommentMapper mentions = mock(ExtApprovalCommentMapper.class);
    private final ResourceAccessContextProvider provider = mock(ResourceAccessContextProvider.class);
    private final ResourcePermissionService permissions = mock(ResourcePermissionService.class);
    private final BaseMapper<Customer> customers = mock(BaseMapper.class);
    private final BaseMapper<Clue> clues = mock(BaseMapper.class);
    private final BaseMapper<FollowUpRecord> records = mock(BaseMapper.class);
    private final BaseMapper<FollowUpPlan> plans = mock(BaseMapper.class);
    private final BaseMapper<ProductPrice> prices = mock(BaseMapper.class);
    private final BaseMapper<CustomFormData> customData = mock(BaseMapper.class);
    private final PoolCustomerService customerPools = mock(PoolCustomerService.class);
    private final PoolClueService cluePools = mock(PoolClueService.class);
    private final FollowUpRecordService followRecords = mock(FollowUpRecordService.class);
    private final FollowUpPlanService followPlans = mock(FollowUpPlanService.class);
    private final CustomFormDataService customForms = mock(CustomFormDataService.class);
    private MockedStatic<SessionUtils> session;
    private MockedStatic<OrganizationContext> organization;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "approvalCommentMapper", comments);
        ReflectionTestUtils.setField(service, "extApprovalTaskMapper", tasks);
        ReflectionTestUtils.setField(service, "extApprovalCommentMapper", mentions);
        ReflectionTestUtils.setField(service, "contextProviders", List.of(provider));
        ReflectionTestUtils.setField(service, "resourcePermissionService", permissions);
        ReflectionTestUtils.setField(service, "customerMapper", customers);
        ReflectionTestUtils.setField(service, "clueMapper", clues);
        ReflectionTestUtils.setField(service, "followUpRecordMapper", records);
        ReflectionTestUtils.setField(service, "followUpPlanMapper", plans);
        ReflectionTestUtils.setField(service, "productPriceMapper", prices);
        ReflectionTestUtils.setField(service, "customFormDataMapper", customData);
        ReflectionTestUtils.setField(service, "poolCustomerService", customerPools);
        ReflectionTestUtils.setField(service, "poolClueService", cluePools);
        ReflectionTestUtils.setField(service, "followUpRecordService", followRecords);
        ReflectionTestUtils.setField(service, "followUpPlanService", followPlans);
        ReflectionTestUtils.setField(service, "customFormDataService", customForms);
        session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
        organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE));
        session.when(SessionUtils::getUserId).thenReturn("user");
        organization.when(OrganizationContext::getOrganizationId).thenReturn("org");
        attachment = new Attachment();
        attachment.setId("attachment");
        attachment.setResourceId("resource");
        attachment.setOrganizationId("org");
    }

    @AfterEach
    void tearDown() {
        session.close();
        organization.close();
    }

    @ParameterizedTest
    @CsvSource({
            "contact,CUSTOMER_MANAGEMENT_CONTACT:READ",
            "opportunity,OPPORTUNITY_MANAGEMENT:READ",
            "product,PRODUCT_MANAGEMENT:READ",
            "quotation,OPPORTUNITY_QUOTATION:READ",
            "contract,CONTRACT:READ",
            "invoice,CONTRACT_INVOICE:READ",
            "contractPaymentPlan,CONTRACT_PAYMENT_PLAN:READ",
            "contractPaymentRecord,CONTRACT_PAYMENT_RECORD:READ",
            "order,ORDER:READ"
    })
    void ordinaryAttachmentsRequireTheActualModuleReadPermission(String formType, String permission) {
        locate(formType);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(permissions).checkResourcePermissionWithoutApprovalTask(permission, "resource", formType, "user", "org");
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(permissions)
                .checkResourcePermissionWithoutApprovalTask(permission, "resource", formType, "user", "org");
        assertForbidden();
    }

    @Test
    void missingIdentityOrganizationAndResourceAreRejectedBeforeBusinessQueries() {
        session.when(SessionUtils::getUserId).thenReturn(null);
        assertForbidden();
        session.when(SessionUtils::getUserId).thenReturn("user");
        organization.when(OrganizationContext::getOrganizationId).thenReturn("");
        assertForbidden();
        organization.when(OrganizationContext::getOrganizationId).thenReturn("org");
        attachment.setOrganizationId("other-org");
        assertForbidden();
        attachment.setOrganizationId("org");
        attachment.setResourceId("");
        assertForbidden();
        verifyNoInteractions(comments, tasks, provider, permissions);
    }

    @Test
    void commentAttachmentsAllowHistoricalParticipantsInTheCurrentOrganization() {
        ApprovalComment comment = comment("business");
        when(comments.selectByPrimaryKey("resource")).thenReturn(comment);
        when(tasks.existsResourceParticipant("business", "user", "org")).thenReturn(true);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(tasks, never()).existsAttachmentParticipant(anyString(), anyString(), anyString(), anyString());
        verifyNoInteractions(provider, permissions);
        comment.setOrganizationId("other-org");
        assertForbidden();
    }

    @Test
    void commentAttachmentsAlsoAllowBusinessViewersWhoNeverParticipated() {
        when(comments.selectByPrimaryKey("resource")).thenReturn(comment("business"));
        when(provider.getAccessContext("business", "org")).thenReturn(new ResourceAccessContext());
        when(provider.getFormType()).thenReturn(FormKey.CONTRACT.getKey());
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(permissions).checkResourcePermissionWithoutApprovalTask(
                PermissionConstants.CONTRACT_READ, "business", "contract", "user", "org");
    }

    @Test
    void mentionedUserCanReadCommentAttachmentsWithoutGainingWritePermission() {
        when(comments.selectByPrimaryKey("resource")).thenReturn(comment("business"));
        when(mentions.existsMention("business", "user", "org")).thenReturn(true);

        assertDoesNotThrow(() -> service.checkReadPermission(attachment));
        assertForbidden();

        verify(mentions).existsMention("business", "user", "org");
    }

    @Test
    void removedMentionRevokesCommentAttachmentRead() {
        when(comments.selectByPrimaryKey("resource")).thenReturn(comment("business"));
        when(mentions.existsMention("business", "user", "org")).thenReturn(true, false);
        assertDoesNotThrow(() -> service.checkReadPermission(attachment));

        assertThrows(GenericException.class, () -> service.checkReadPermission(attachment));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mentionReadStillRejectsForeignAttachmentOrComment(boolean foreignComment) {
        ApprovalComment comment = comment("business");
        when(comments.selectByPrimaryKey("resource")).thenReturn(comment);
        when(mentions.existsMention("business", "user", "org")).thenReturn(true);
        if (foreignComment) {
            comment.setOrganizationId("other-org");
        } else {
            attachment.setOrganizationId("other-org");
        }

        assertThrows(GenericException.class, () -> service.checkReadPermission(attachment));

        verifyNoInteractions(mentions);
    }

    @Test
    void mentionOnAnotherBusinessCannotReadThisCommentAttachment() {
        when(comments.selectByPrimaryKey("resource")).thenReturn(comment("business"));
        when(mentions.existsMention("another-business", "user", "org")).thenReturn(true);

        assertThrows(GenericException.class, () -> service.checkReadPermission(attachment));

        verify(mentions).existsMention("business", "user", "org");
    }

    @Test
    void ordinaryBusinessAttachmentsDoNotInheritCommentMentionPermission() {
        when(mentions.existsMention("resource", "user", "org")).thenReturn(true);

        assertThrows(GenericException.class, () -> service.checkReadPermission(attachment));

        verifyNoInteractions(mentions);
    }

    @Test
    void ordinaryAttachmentsDoNotInheritHistoricalApprovalParticipation() {
        locate(FormKey.CONTRACT.getKey());
        when(tasks.existsResourceParticipant("resource", "user", "org")).thenReturn(true);
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(permissions)
                .checkResourcePermissionWithoutApprovalTask(anyString(), anyString(), anyString(), anyString(),
                        anyString());
        assertForbidden();
        verify(tasks, never()).existsResourceParticipant(anyString(), anyString(), anyString());
    }

    @Test
    void approvalOperationAttachmentsAllowParticipantsOfTheReferencedInstance() {
        when(tasks.existsAttachmentParticipant("attachment", "resource", "user", "org")).thenReturn(true);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(tasks).existsAttachmentParticipant("attachment", "resource", "user", "org");
        verify(tasks, never()).existsResourceParticipant(anyString(), anyString(), anyString());
        verifyNoInteractions(provider, permissions);
    }

    @Test
    void unknownResourceAndUnknownProviderCannotGrantAccess() {
        assertForbidden();
        locate("unrecognized");
        assertForbidden();
        verifyNoInteractions(permissions);
    }

    @Test
    void customerAttachmentsFollowPrivateAndSharedPoolReadPermissions() {
        locate(FormKey.CUSTOMER.getKey());
        Customer customer = new Customer();
        customer.setOrganizationId("org");
        customer.setPoolId("pool");
        when(customers.selectByPrimaryKey("resource")).thenReturn(customer);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(permissions).checkResourcePermissionWithoutApprovalTask(
                PermissionConstants.CUSTOMER_MANAGEMENT_READ, "resource", "customer", "user", "org");
        customer.setInSharedPool(true);
        assertForbidden();
        when(customerPools.hasPoolMember("pool", "user", "org")).thenReturn(true);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(permissions, times(2)).checkPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ);
        customer.setOrganizationId("other-org");
        assertForbidden();
    }

    @Test
    void cluePoolAttachmentsRequirePoolReadPermissionAndMembership() {
        locate(FormKey.CLUE.getKey());
        Clue clue = new Clue();
        clue.setOrganizationId("org");
        clue.setPoolId("pool");
        clue.setInSharedPool(true);
        when(clues.selectByPrimaryKey("resource")).thenReturn(clue);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(permissions).checkPermission(PermissionConstants.CLUE_MANAGEMENT_POOL_READ);
        verify(cluePools).checkPoolMember("pool", "user", "org");
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(cluePools)
                .checkPoolMember("pool", "user", "org");
        assertForbidden();
    }

    @Test
    void followAttachmentsReuseParentBusinessPermissionsAndValidateOrganization() {
        locate(FormKey.FOLLOW_RECORD.getKey());
        FollowUpRecord record = new FollowUpRecord();
        record.setOrganizationId("org");
        when(records.selectByPrimaryKey("resource")).thenReturn(record);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(followRecords).checkRecordPermission("resource", "org", "user", true);
        record.setOrganizationId("other-org");
        assertForbidden();
        locate(FormKey.FOLLOW_PLAN.getKey());
        FollowUpPlan plan = new FollowUpPlan();
        plan.setOrganizationId("org");
        when(plans.selectByPrimaryKey("resource")).thenReturn(plan);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(followPlans).checkPlanPermission("resource", "org", "user", true);
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(followPlans)
                .checkPlanPermission("resource", "org", "user", true);
        assertForbidden();
    }

    @Test
    void priceAttachmentsRequireTheSameOrganizationAndPriceReadPermission() {
        ProductPrice price = new ProductPrice();
        price.setOrganizationId("org");
        when(prices.selectByPrimaryKey("resource")).thenReturn(price);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(permissions).checkPermission(PermissionConstants.PRICE_READ);
        price.setOrganizationId("other-org");
        assertForbidden();
    }

    @Test
    void customFormAttachmentsRequireModuleAndFormDataPermissions() {
        CustomFormData data = new CustomFormData();
        data.setOrganizationId("org");
        when(customData.selectByPrimaryKey("resource")).thenReturn(data);
        assertForbidden();
        when(customForms.hasViewPermission("resource", "user", "org")).thenReturn(true);
        assertDoesNotThrow(() -> service.checkPermission(attachment));
        verify(permissions, times(2)).checkPermission(PermissionConstants.CUSTOM_FORM_READ);
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(permissions)
                .checkPermission(PermissionConstants.CUSTOM_FORM_READ);
        assertForbidden();
    }

    private void locate(String formType) {
        when(provider.getFormType()).thenReturn(formType);
        when(provider.getAccessContext("resource", "org")).thenReturn(new ResourceAccessContext());
    }

    private ApprovalComment comment(String resourceId) {
        ApprovalComment comment = new ApprovalComment();
        comment.setOrganizationId("org");
        comment.setResourceId(resourceId);
        return comment;
    }

    private void assertForbidden() {
        assertEquals(CrmHttpResultCode.FORBIDDEN,
                assertThrows(GenericException.class, () -> service.checkPermission(attachment)).getErrorCode());
    }
}
