package cn.cordys.crm.approval.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.request.ApprovalCommentPageRequest;
import cn.cordys.crm.approval.dto.response.ApprovalCommentResponse;
import cn.cordys.crm.approval.service.ApprovalCommentService;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApprovalCommentControllerTest {

    private ApprovalCommentService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ApprovalCommentService.class);
        ApprovalCommentController controller = new ApprovalCommentController();
        ReflectionTestUtils.setField(controller, "approvalCommentService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void pageBindsPaginationAndSortAndUsesCurrentUserAndOrganization() throws Exception {
        ApprovalCommentResponse root = new ApprovalCommentResponse();
        root.setId("comment-a");
        when(service.page(any(), eq("user-a"), eq("org-a")))
                .thenReturn(new Pager<>(List.of(root), 8, 2, 3));
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
                var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-a");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-a");

            mvc.perform(post("/approval-comment/page").contentType(MediaType.APPLICATION_JSON)
                            .content(JSON.toJSONString(Map.of("instanceId", "instance-a", "current", 3,
                                    "pageSize", 2, "sort", Map.of("name", "createTime", "type", "asc")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(8))
                    .andExpect(jsonPath("$.current").value(3))
                    .andExpect(jsonPath("$.pageSize").value(2))
                    .andExpect(jsonPath("$.list[0].id").value("comment-a"));

            ArgumentCaptor<ApprovalCommentPageRequest> request =
                    ArgumentCaptor.forClass(ApprovalCommentPageRequest.class);
            verify(service).page(request.capture(), eq("user-a"), eq("org-a"));
            assertEquals("instance-a", request.getValue().getInstanceId());
            assertEquals(3, request.getValue().getCurrent());
            assertEquals(2, request.getValue().getPageSize());
            assertEquals("asc", request.getValue().getSort().getType());
        }
        mvc.perform(get("/approval-comment/list/instance-a")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({ "'',1,20", "instance-a,0,20", "instance-a,1,0", "instance-a,1,501" })
    void pageRejectsMissingInstanceAndInvalidPagination(String instanceId, int current, int pageSize) throws Exception {
        mvc.perform(post("/approval-comment/page").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(Map.of("instanceId", instanceId,
                                "current", current, "pageSize", pageSize))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
