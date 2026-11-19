package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.follow.constants.FollowUpCommentTargetType;
import cn.cordys.crm.follow.controller.FollowUpPlanCommentController;
import cn.cordys.crm.follow.controller.FollowUpRecordCommentController;
import cn.cordys.crm.follow.domain.Comment;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpPlanComment;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.domain.FollowUpRecordComment;
import cn.cordys.crm.follow.dto.request.CommentAddRequest;
import cn.cordys.crm.follow.dto.request.CommentUpdateRequest;
import cn.cordys.crm.follow.mapper.ExtCommentMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.system.mapper.ExtStatisticMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FollowUpCommentPermissionTest {
    private static final String USER = "joint-member";
    private static final String ORG = "org-a";
    private static final String CUSTOMER = "customer-a";
    private static final String RESOURCE = "follow-a";
    private final RelatedResourcePermissionService relatedPermissions = new RelatedResourcePermissionService();
    private final CustomerCollaborationService collaborations = new CustomerCollaborationService();
    private final ResourcePermissionService resourcePermissions = mock(ResourcePermissionService.class);
    private final PermissionCache permissions = mock(PermissionCache.class);
    private final ExtStatisticMapper fields = mock(ExtStatisticMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> members = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<Comment> comments = mock(BaseMapper.class);
    private final List<CustomerCollaboration> membership = new ArrayList<>();
    private final Map<String, String> followValues = new HashMap<>(Map.of(
            "organization_id", ORG, "type", "CUSTOMER", "owner", "follow-owner", "customer_id", CUSTOMER));
    private final Map<String, String> customerValues = new HashMap<>(Map.of(
            "organization_id", ORG, "owner", "customer-owner", "in_shared_pool", "false"));
    private final CommentAddRequest addRequest = new CommentAddRequest();
    private BaseCommentService<? extends Comment> service;
    private MockedStatic<Translator> translator;
    private MockedStatic<IDGenerator> ids;

    @BeforeEach
    void configureRealCollaborationPermission() {
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ids = mockStatic(IDGenerator.class);
        AtomicInteger sequence = new AtomicInteger();
        ids.when(IDGenerator::nextStr).thenAnswer(invocation -> "generated-" + sequence.incrementAndGet());
        OperationLogContext.putEmptySpan();
        addRequest.setResourceId(RESOURCE);
        addRequest.setContent("共同跟进评论");
        membership.add(member(USER, "JOINT_FOLLOW", null));
        ReflectionTestUtils.setField(relatedPermissions, "customerCollaborationService", collaborations);
        ReflectionTestUtils.setField(relatedPermissions, "extStatisticMapper", fields);
        ReflectionTestUtils.setField(relatedPermissions, "permissionCache", permissions);
        ReflectionTestUtils.setField(relatedPermissions, "moduleFormCacheService", mock(ModuleFormCacheService.class));
        ReflectionTestUtils.setField(collaborations, "customerCollaborationMapper", members);
        when(members.select(any(CustomerCollaboration.class))).thenAnswer(invocation -> {
            CustomerCollaboration query = invocation.getArgument(0);
            return membership.stream().filter(item -> item.getCustomerId().equals(query.getCustomerId())
                    && (query.getUserId() == null || query.getUserId().equals(item.getUserId()))).toList();
        });
        when(fields.selectBusinessFieldValue(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String table = invocation.getArgument(0);
            String id = invocation.getArgument(2);
            if ("customer".equals(table) && CUSTOMER.equals(id)) {
                return customerValues.get(invocation.<String>getArgument(1));
            }
            return RESOURCE.equals(id) ? followValues.get(invocation.<String>getArgument(1)) : null;
        });
        when(permissions.getPermissionIds(USER, ORG))
                .thenReturn(Set.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ));
        doThrow(new GenericException(CrmHttpResultCode.FORBIDDEN)).when(resourcePermissions)
                .checkResourcePermission(anyString(), eq(RESOURCE), anyString(), eq(USER), eq(ORG));
    }

    @AfterEach
    void clearContext() {
        OperationLogContext.clear();
        translator.close();
        ids.close();
    }

    @ParameterizedTest
    @EnumSource(FollowUpCommentTargetType.class)
    void jointFollowCanCommentOnOtherOwnersWithoutTheirUpdateDataScope(FollowUpCommentTargetType target) {
        membership.getFirst().setExpireTime(Long.MAX_VALUE);
        configureTarget(target);

        Comment result = service.add(addRequest, USER, ORG);

        assertNotNull(result.getId());
        assertEquals(USER, result.getCreateUser());
        assertEquals(RESOURCE, result.getResourceId());
        verify(comments).insert(result);
        verifyNoInteractions(resourcePermissions);
    }

    static Stream<Arguments> rejectedScenarios() {
        return Stream.of(FollowUpCommentTargetType.values()).flatMap(target -> Stream.of(
                "READ_ONLY", "COLLABORATION", "EXPIRED", "REMOVED", "POOL", "MISSING_CUSTOMER",
                "OTHER_CUSTOMER_ORG", "OTHER_RESOURCE_ORG", "ISOLATED_OWNER", "EXPIRED_OWNER",
                "MISSING_RESOURCE", "CLUE", "NO_CUSTOMER_PERMISSION", "MISSING_CUSTOMER_LINK", "MISSING_OWNER",
                "OPPORTUNITY_ISOLATED")
                .map(scenario -> Arguments.of(target, scenario)));
    }

    @ParameterizedTest
    @MethodSource("rejectedScenarios")
    void invalidCollaborationCannotWriteComments(FollowUpCommentTargetType target, String scenario) {
        switch (scenario) {
        case "READ_ONLY", "COLLABORATION" -> membership.getFirst().setCollaborationType(scenario);
        case "EXPIRED" -> membership.getFirst().setExpireTime(1L);
        case "REMOVED" -> membership.clear();
        case "POOL" -> customerValues.put("in_shared_pool", "true");
        case "MISSING_CUSTOMER" -> customerValues.clear();
        case "OTHER_CUSTOMER_ORG" -> customerValues.put("organization_id", "other-org");
        case "OTHER_RESOURCE_ORG" -> followValues.put("organization_id", "other-org");
        case "ISOLATED_OWNER" -> membership.add(member("follow-owner", "COLLABORATION", null));
        case "EXPIRED_OWNER" -> membership.add(member("follow-owner", "JOINT_FOLLOW", 1L));
        case "MISSING_RESOURCE" -> followValues.clear();
        case "CLUE" -> followValues.put("type", "CLUE");
        case "NO_CUSTOMER_PERMISSION" -> when(permissions.getPermissionIds(USER, ORG)).thenReturn(Set.of());
        case "MISSING_CUSTOMER_LINK" -> followValues.remove("customer_id");
        case "MISSING_OWNER" -> followValues.remove("owner");
        case "OPPORTUNITY_ISOLATED" -> {
            followValues.put("opportunity_id", "opportunity-a");
            CustomerCollaboration privateMember = member(USER, "COLLABORATION", null);
            privateMember.setCustomerId("isolated-customer");
            membership.add(privateMember);
            when(fields.selectBusinessFieldValue(eq("opportunity"), anyString(), eq("opportunity-a")))
                    .thenAnswer(invocation -> switch (invocation.<String>getArgument(1)) {
                    case "organization_id" -> ORG;
                    case "customer_id" -> "isolated-customer";
                    default -> null;
                    });
            when(fields.selectBusinessFieldValue(eq("customer"), anyString(), eq("isolated-customer")))
                    .thenAnswer(invocation -> switch (invocation.<String>getArgument(1)) {
                    case "organization_id" -> ORG;
                    case "owner" -> "private-customer-owner";
                    case "in_shared_pool" -> "false";
                    default -> null;
                    });
        }
        default -> throw new AssertionError(scenario);
        }
        configureTarget(target);

        assertThrows(GenericException.class, () -> service.add(addRequest, USER, ORG));

        verifyNoInteractions(comments);
        if (Set.of("MISSING_RESOURCE", "OTHER_RESOURCE_ORG").contains(scenario)) {
            verifyNoInteractions(resourcePermissions);
        }
    }

    @ParameterizedTest
    @EnumSource(FollowUpCommentTargetType.class)
    void independentRoleUpdatePermissionStillAllowsComments(FollowUpCommentTargetType target) {
        membership.getFirst().setCollaborationType("READ_ONLY");
        configureTarget(target);
        String form = target == FollowUpCommentTargetType.RECORD ? FormKey.FOLLOW_RECORD.getKey()
                : FormKey.FOLLOW_PLAN.getKey();
        String permission = target == FollowUpCommentTargetType.RECORD ? PermissionConstants.FOLLOW_RECORD_UPDATE
                : PermissionConstants.FOLLOW_PLAN_UPDATE;
        doNothing().when(resourcePermissions).checkResourcePermission(permission, RESOURCE, form, USER, ORG);

        Comment result = service.add(addRequest, USER, ORG);

        verify(resourcePermissions).checkResourcePermission(permission, RESOURCE, form, USER, ORG);
        verify(comments).insert(result);
    }

    @ParameterizedTest
    @EnumSource(FollowUpCommentTargetType.class)
    void collaborationDoesNotChangeCommentCreatorAndOrganizationChecks(FollowUpCommentTargetType target) {
        configureTarget(target);
        Comment comment = target == FollowUpCommentTargetType.RECORD ? new FollowUpRecordComment()
                : new FollowUpPlanComment();
        comment.setId("comment-a");
        comment.setResourceId(RESOURCE);
        comment.setOrganizationId(ORG);
        comment.setCreateUser("other-user");
        comment.setContent("原评论");
        when(comments.selectByPrimaryKey("comment-a")).thenReturn(comment);
        CommentUpdateRequest update = new CommentUpdateRequest();
        update.setId("comment-a");
        update.setContent("修改评论");

        assertThrows(GenericException.class, () -> service.update(update, USER, ORG));
        assertThrows(GenericException.class, () -> service.delete("comment-a", USER, ORG));
        comment.setCreateUser(USER);
        assertThrows(GenericException.class, () -> service.update(update, USER, "other-org"));
        assertThrows(GenericException.class, () -> service.delete("comment-a", USER, "other-org"));
        service.update(update, USER, ORG);
        service.delete("comment-a", USER, ORG);
        verify(comments).update(comment);
        verify(comments).deleteByIds(List.of("comment-a"));
    }

    @ParameterizedTest
    @EnumSource(FollowUpCommentTargetType.class)
    void commentControllerUsesTheDedicatedServicePermission(FollowUpCommentTargetType target) throws Exception {
        Class<?> controller = target == FollowUpCommentTargetType.RECORD ? FollowUpRecordCommentController.class
                : FollowUpPlanCommentController.class;
        assertNull(controller.getMethod("add", CommentAddRequest.class).getAnnotation(CsPermission.class),
                "不能再由旧跟进编辑数据范围提前拦截共同跟进成员");
    }

    @SuppressWarnings("unchecked")
    private void configureTarget(FollowUpCommentTargetType target) {
        if (target == FollowUpCommentTargetType.PLAN) {
            service = new FollowUpPlanCommentService();
            BaseMapper<FollowUpPlan> mapper = mock(BaseMapper.class);
            FollowUpPlan plan = new FollowUpPlan();
            plan.setOrganizationId(followValues.get("organization_id"));
            plan.setOwner(followValues.get("owner"));
            plan.setType(followValues.get("type"));
            plan.setCustomerId(followValues.get("customer_id"));
            plan.setOpportunityId(followValues.get("opportunity_id"));
            when(mapper.selectByPrimaryKey(RESOURCE)).thenReturn(followValues.isEmpty() ? null : plan);
            ReflectionTestUtils.setField(service, "planMapper", mapper);
            ReflectionTestUtils.setField(service, "extPlanMapper", mock(ExtFollowUpPlanMapper.class));
        } else {
            service = new FollowUpRecordCommentService();
            BaseMapper<FollowUpRecord> mapper = mock(BaseMapper.class);
            FollowUpRecord record = new FollowUpRecord();
            record.setOrganizationId(followValues.get("organization_id"));
            record.setOwner(followValues.get("owner"));
            record.setType(followValues.get("type"));
            record.setCustomerId(followValues.get("customer_id"));
            record.setOpportunityId(followValues.get("opportunity_id"));
            when(mapper.selectByPrimaryKey(RESOURCE)).thenReturn(followValues.isEmpty() ? null : record);
            ReflectionTestUtils.setField(service, "recordMapper", mapper);
            ReflectionTestUtils.setField(service, "extRecordMapper", mock(ExtFollowUpRecordMapper.class));
        }
        BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
        Customer customer = new Customer();
        customer.setName("协作客户");
        when(customerMapper.selectByPrimaryKey(CUSTOMER)).thenReturn(customer);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "opportunityMapper", mock(BaseMapper.class));
        ReflectionTestUtils.setField(service, "commentMapper", comments);
        ReflectionTestUtils.setField(service, "extCommentMapper", mock(ExtCommentMapper.class));
        ReflectionTestUtils.setField(service, "commonNoticeSendService", mock(CommonNoticeSendService.class));
        ReflectionTestUtils.setField(service, "relatedResourcePermissionService", relatedPermissions);
        ReflectionTestUtils.setField(service, "resourcePermissionService", resourcePermissions);
    }

    private CustomerCollaboration member(String user, String type, Long expires) {
        CustomerCollaboration member = new CustomerCollaboration();
        member.setCustomerId(CUSTOMER);
        member.setUserId(user);
        member.setCollaborationType(type);
        member.setExpireTime(expires);
        return member;
    }
}
