package cn.cordys.crm.approval.service;

import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.request.ApprovalTodoPageRequest;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockMakers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ApprovalTodoServiceTest {

    private ApprovalTodoService service;
    private ExtApprovalCommentMapper mapper;
    private BaseMapper<Customer> customerMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ApprovalTodoService();
        mapper = mock(ExtApprovalCommentMapper.class);
        customerMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "extApprovalCommentMapper", mapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
    }

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    @ParameterizedTest
    @CsvSource(value = {"ALL,NULL", "CONTRACT,contract", "custom-form-a,custom-form-a"}, nullValues = "NULL")
    void mentionPagePreservesDifferentCommentsOnSameInstanceAndPassesFilters(String resourceType, String expectedType) {
        ApprovalTodoPageRequest request = new ApprovalTodoPageRequest();
        request.setCurrent(3);
        request.setPageSize(5);
        request.setResourceType(resourceType);
        request.setKeyword(" 合同 ");
        ApprovalTodoItemResponse first = mention("mention-a", "comment-a");
        ApprovalTodoItemResponse second = mention("mention-b", "comment-b");
        when(mapper.selectMentionTodos("user-a", "org-a", expectedType, "合同")).thenAnswer(invocation -> {
            Page<?> page = PageHelper.getLocalPage();
            assertEquals(3, page.getPageNum());
            assertEquals(5, page.getPageSize());
            page.setTotal(12);
            return List.of(first, second);
        });
        try (var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-a");

            Pager<List<ApprovalTodoItemResponse>> result = service.getMentionPage(request, "user-a");

            assertEquals(List.of(first, second), result.getList());
            assertEquals(12, result.getTotal());
            assertEquals(3, result.getCurrent());
            assertEquals(5, result.getPageSize());
            assertEquals(first.getApprovalInstanceId(), second.getApprovalInstanceId());
            assertNull(first.getApprovalTaskId());
            assertNull(second.getApprovalTaskId());
            verify(mapper).selectMentionTodos("user-a", "org-a", expectedType, "合同");
        }
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,org-a", "user-a,NULL"}, nullValues = "NULL")
    void mentionPageWithoutCurrentUserOrOrganizationIsEmpty(String userId, String organizationId) {
        try (var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            organization.when(OrganizationContext::getOrganizationId).thenReturn(organizationId);

            Pager<List<ApprovalTodoItemResponse>> result = service.getMentionPage(new ApprovalTodoPageRequest(), userId);

            assertTrue(result.getList().isEmpty());
            assertEquals(0, result.getTotal());
            verifyNoInteractions(mapper, customerMapper);
        }
    }

    @Test
    void mentionPageKeepsSharedPoolInformationForCustomerEntries() {
        ApprovalTodoItemResponse item = mention("mention-a", "comment-a");
        item.setResourceType("CUSTOMER");
        Customer customer = new Customer();
        customer.setId(item.getResourceId());
        customer.setInSharedPool(true);
        customer.setPoolId("pool-a");
        when(mapper.selectMentionTodos("user-a", "org-a", null, null)).thenReturn(List.of(item));
        when(customerMapper.selectByIds(List.of(item.getResourceId()))).thenReturn(List.of(customer));
        try (var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-a");

            ApprovalTodoItemResponse result = service.getMentionPage(new ApprovalTodoPageRequest(), "user-a").getList().getFirst();

            assertTrue(result.getInSharedPool());
            assertEquals("pool-a", result.getPoolId());
        }
    }

    private ApprovalTodoItemResponse mention(String mentionId, String commentId) {
        ApprovalTodoItemResponse item = new ApprovalTodoItemResponse();
        item.setCommentMentionId(mentionId);
        item.setCommentId(commentId);
        item.setMentionTime(100L);
        item.setApprovalInstanceId("instance-a");
        item.setResourceId("resource-a");
        item.setResourceType("CONTRACT");
        return item;
    }
}
