package cn.cordys.common.formula;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.clue.service.ClueFieldService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.customer.service.CustomerFieldService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerContactFieldService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.opportunity.service.OpportunityFieldService;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityQuotationFieldService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.contract.service.ContractFieldService;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.contract.service.ContractInvoiceFieldService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractPaymentPlanFieldService;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.contract.service.ContractPaymentRecordFieldService;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.crm.order.service.OrderFieldService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.product.service.ProductFieldService;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductPriceFieldService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.follow.service.FollowUpRecordFieldService;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpPlanFieldService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 仅供已通过权限检查的更新入口读取持久化基线；延迟取 Bean 避免业务服务循环依赖。 */
@Service
public class FormulaRecordSnapshotService {
    private final ApplicationContext beans;

    public FormulaRecordSnapshotService(ApplicationContext beans) {
        this.beans = beans;
    }

    public Map<String, Object> loadForUpdate(String formKey, Object request,
            org.springframework.core.MethodParameter parameter) {
        org.springframework.beans.BeanWrapper wrapper = new org.springframework.beans.BeanWrapperImpl(request);
        Object value = wrapper.isReadableProperty("id") ? wrapper.getPropertyValue("id") : null;
        if (!(value instanceof String id) || id.isBlank() || parameter == null) {
            throw new GenericException(CrmHttpResultCode.VALIDATE_FAILED);
        }
        String userId = cn.cordys.security.SessionUtils.getUserId();
        String orgId = cn.cordys.context.OrganizationContext.getOrganizationId();
        var permission = parameter.getMethodAnnotation(cn.cordys.common.permission.CsPermission.class);
        var shiro = parameter.getMethodAnnotation(org.apache.shiro.authz.annotation.RequiresPermissions.class);
        if (permission != null) {
            beans.getBean(cn.cordys.common.permission.ResourcePermissionService.class)
                    .checkResourcePermission(permission.value(), id, permission.formType(), userId, orgId);
        } else if (shiro != null) {
            boolean allowed = shiro.logical() == org.apache.shiro.authz.annotation.Logical.OR
                    ? java.util.Arrays.stream(shiro.value()).anyMatch(cn.cordys.common.permission.PermissionUtils::hasPermission)
                    : java.util.Arrays.stream(shiro.value()).allMatch(cn.cordys.common.permission.PermissionUtils::hasPermission);
            if (!allowed) throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        } else if ("record".equals(formKey)) {
            beans.getBean(FollowUpRecordService.class).checkUpdatePermission(id, userId);
        } else if ("plan".equals(formKey)) {
            beans.getBean(FollowUpPlanService.class).checkUpdatePermission(id, userId);
        } else {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        return load(formKey, id, orgId);
    }

    public Map<String, Object> load(String formKey, String id, String orgId) {
        // 展示 DTO（例如报价）不暴露 organizationId；先用原始实体校验归属，不能信任 DTO。
        Object entity = readEntity(formKey, id);
        if (entity == null) throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        Map<String, Object> raw = JSON.parseToMap(JSON.toJSONString(entity));
        if (orgId == null || !Objects.equals(orgId, raw.get("organizationId"))) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        Snapshot snapshot = switch (formKey) {
            case "clue" -> new Snapshot(beans.getBean(ClueService.class).getSimple(id),
                    beans.getBean(ClueFieldService.class).getModuleFieldValuesByResourceId(id));
            case "customer" -> new Snapshot(beans.getBean(CustomerService.class).getSimple(id),
                    beans.getBean(CustomerFieldService.class).getModuleFieldValuesByResourceId(id));
            case "contact" -> new Snapshot(beans.getBean(CustomerContactService.class).getSimple(id),
                    beans.getBean(CustomerContactFieldService.class).getModuleFieldValuesByResourceId(id));
            case "opportunity" -> new Snapshot(beans.getBean(OpportunityService.class).getSimple(id),
                    beans.getBean(OpportunityFieldService.class).getModuleFieldValuesByResourceId(id));
            case "quotation" -> new Snapshot(beans.getBean(OpportunityQuotationService.class).getSimple(id),
                    beans.getBean(OpportunityQuotationFieldService.class).getModuleFieldValuesByResourceId(id));
            case "contract" -> new Snapshot(beans.getBean(ContractService.class).getSimple(id),
                    beans.getBean(ContractFieldService.class).getModuleFieldValuesByResourceId(id));
            case "invoice" -> new Snapshot(beans.getBean(ContractInvoiceService.class).getSimple(id),
                    beans.getBean(ContractInvoiceFieldService.class).getModuleFieldValuesByResourceId(id));
            case "contractPaymentPlan" -> new Snapshot(beans.getBean(ContractPaymentPlanService.class).getSimple(id),
                    beans.getBean(ContractPaymentPlanFieldService.class).getModuleFieldValuesByResourceId(id));
            case "contractPaymentRecord" -> new Snapshot(beans.getBean(ContractPaymentRecordService.class).getSimple(id),
                    beans.getBean(ContractPaymentRecordFieldService.class).getModuleFieldValuesByResourceId(id));
            case "order" -> new Snapshot(beans.getBean(OrderService.class).getSimple(id),
                    beans.getBean(OrderFieldService.class).getModuleFieldValuesByResourceId(id));
            case "product" -> new Snapshot(beans.getBean(ProductService.class).getSimple(id),
                    beans.getBean(ProductFieldService.class).getModuleFieldValuesByResourceId(id));
            case "price" -> new Snapshot(beans.getBean(ProductPriceService.class).getSimple(id),
                    beans.getBean(ProductPriceFieldService.class).getModuleFieldValuesByResourceId(id));
            case "record" -> new Snapshot(beans.getBean(FollowUpRecordService.class).get(id, orgId),
                    beans.getBean(FollowUpRecordFieldService.class).getModuleFieldValuesByResourceId(id));
            case "plan" -> new Snapshot(beans.getBean(FollowUpPlanService.class).get(id, orgId),
                    beans.getBean(FollowUpPlanFieldService.class).getModuleFieldValuesByResourceId(id));
            default -> throw new GenericException(CrmHttpResultCode.VALIDATE_FAILED);
        };
        if (snapshot.record() == null) throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        Map<String, Object> record = new LinkedHashMap<>(JSON.parseToMap(JSON.toJSONString(snapshot.record())));
        record.putAll(raw);
        record.put("moduleFields", snapshot.fields());
        return record;
    }

    Object readEntity(String formKey, String id) {
        Class<?> type = switch (formKey) {
            case "clue" -> cn.cordys.crm.clue.domain.Clue.class;
            case "customer" -> cn.cordys.crm.customer.domain.Customer.class;
            case "contact" -> cn.cordys.crm.customer.domain.CustomerContact.class;
            case "opportunity" -> cn.cordys.crm.opportunity.domain.Opportunity.class;
            case "quotation" -> cn.cordys.crm.opportunity.domain.OpportunityQuotation.class;
            case "contract" -> cn.cordys.crm.contract.domain.Contract.class;
            case "invoice" -> cn.cordys.crm.contract.domain.ContractInvoice.class;
            case "contractPaymentPlan" -> cn.cordys.crm.contract.domain.ContractPaymentPlan.class;
            case "contractPaymentRecord" -> cn.cordys.crm.contract.domain.ContractPaymentRecord.class;
            case "order" -> cn.cordys.crm.order.domain.Order.class;
            case "product" -> cn.cordys.crm.product.domain.Product.class;
            case "price" -> cn.cordys.crm.product.domain.ProductPrice.class;
            case "record" -> cn.cordys.crm.follow.domain.FollowUpRecord.class;
            case "plan" -> cn.cordys.crm.follow.domain.FollowUpPlan.class;
            case "businessTitle" -> cn.cordys.crm.contract.domain.BusinessTitle.class;
            default -> throw new GenericException(CrmHttpResultCode.VALIDATE_FAILED);
        };
        return cn.cordys.mybatis.DataAccessLayer.with(type,
                beans.getBean(org.apache.ibatis.session.SqlSession.class)).selectByPrimaryKey(id);
    }

    private record Snapshot(Object record, List<BaseModuleFieldValue> fields) {}
}
