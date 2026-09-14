package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationGetResponse;
import cn.cordys.crm.opportunity.service.OpportunityQuotationFieldService;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FormulaRecordSnapshotServiceTest {
    @Test
    void 使用原始动态值并拒绝跨组织快照() {
        ApplicationContext beans = mock(ApplicationContext.class);
        OpportunityQuotationService records = mock(OpportunityQuotationService.class);
        OpportunityQuotationFieldService fields = mock(OpportunityQuotationFieldService.class);
        when(beans.getBean(OpportunityQuotationService.class)).thenReturn(records);
        when(beans.getBean(OpportunityQuotationFieldService.class)).thenReturn(fields);
        OpportunityQuotationGetResponse record = new OpportunityQuotationGetResponse();
        record.setId("record-1");
        when(records.getSimple("record-1")).thenReturn(record);
        List<BaseModuleFieldValue> raw = List.of(new BaseModuleFieldValue("amount", "1234.567"));
        when(fields.getModuleFieldValuesByResourceId("record-1")).thenReturn(raw);
        FormulaRecordSnapshotService service = spy(new FormulaRecordSnapshotService(beans));
        cn.cordys.crm.opportunity.domain.OpportunityQuotation entity =
                new cn.cordys.crm.opportunity.domain.OpportunityQuotation();
        entity.setId("record-1");
        entity.setOrganizationId("org-1");
        doReturn(entity).when(service).readEntity("quotation", "record-1");
        assertEquals(raw, service.load("quotation", "record-1", "org-1").get("moduleFields"));
        assertThrows(GenericException.class, () -> service.load("quotation", "record-1", "org-2"));
        when(fields.getModuleFieldValuesByResourceId("record-1")).thenThrow(new IllegalStateException("read failed"));
        assertThrows(IllegalStateException.class, () -> service.load("quotation", "record-1", "org-1"));
    }

    @Test
    void 无权限时不得读取计算基线() throws Exception {
        ApplicationContext beans = mock(ApplicationContext.class);
        ResourcePermissionService permissions = mock(ResourcePermissionService.class);
        when(beans.getBean(ResourcePermissionService.class)).thenReturn(permissions);
        doThrow(new IllegalStateException("denied")).when(permissions)
                .checkResourcePermission("QUOTATION:UPDATE", "record-1", "quotation", "user-1", "org-1");
        try (var session = mockStatic(SessionUtils.class); var organization = mockStatic(OrganizationContext.class)) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");
            MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("endpoint", Request.class), 0);
            assertThrows(IllegalStateException.class,
                    () -> new FormulaRecordSnapshotService(beans).loadForUpdate("quotation", new Request(), parameter));
            verify(beans, never()).getBean(OpportunityQuotationService.class);
            verify(beans, never()).getBean(OpportunityQuotationFieldService.class);
        }
    }

    @CsPermission(value = "QUOTATION:UPDATE", formType = "quotation")
    private void endpoint(Request request) {}

    public static class Request {
        public String getId() { return "record-1"; }
    }
}
