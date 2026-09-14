package cn.cordys.common.formula;

import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.security.SessionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 公式只解析当前用户可读的真实来源记录，不使用无权限边界的名称辅助查询。 */
@Service
public class FormulaReferenceValueService {
    private final FormulaRecordSnapshotService snapshots;
    private final ApplicationContext beans;

    public FormulaReferenceValueService(FormulaRecordSnapshotService snapshots, ApplicationContext beans) {
        this.snapshots = snapshots;
        this.beans = beans;
    }

    public Object resolve(DatasourceField field, Object value) {
        if (value == null || "".equals(value)) return "";
        List<?> ids;
        if ("DATA_SOURCE_MULTIPLE".equals(field.getType())) {
            if (!(value instanceof List<?> values)) throw new FormulaEvaluationException("INVALID_SOURCE_VALUE");
            ids = values;
        } else {
            ids = List.of(value);
        }
        List<String> names = new ArrayList<>();
        for (Object id : ids) {
            if (!(id instanceof String text) || text.isBlank()) throw new FormulaEvaluationException("INVALID_SOURCE_ID");
            names.add(resolveName(field.getDataSourceType(), text));
        }
        return String.join(",", names);
    }

    private String resolveName(String source, String id) {
        if (source == null || source.isBlank()) throw new FormulaEvaluationException("INVALID_SOURCE_TYPE");
        String org = OrganizationContext.getOrganizationId();
        String user = SessionUtils.getUserId();
        Source binding = switch (source) {
            case "CUSTOMER" -> new Source("customer", "CUSTOMER_MANAGEMENT:READ");
            case "CLUE" -> new Source("clue", "CLUE_MANAGEMENT:READ");
            case "CONTACT" -> new Source("contact", "CUSTOMER_MANAGEMENT_CONTACT:READ");
            case "OPPORTUNITY" -> new Source("opportunity", "OPPORTUNITY_MANAGEMENT:READ");
            case "PRODUCT" -> new Source("product", "PRODUCT_MANAGEMENT:READ");
            case "PRICE" -> new Source("price", "PRICE:READ");
            case "QUOTATION" -> new Source("quotation", "OPPORTUNITY_QUOTATION:READ");
            case "CONTRACT" -> new Source("contract", "CONTRACT:READ");
            case "PAYMENT_PLAN" -> new Source("contractPaymentPlan", "CONTRACT_PAYMENT_PLAN:READ");
            case "CONTRACT_PAYMENT_RECORD" -> new Source("contractPaymentRecord", "CONTRACT_PAYMENT_RECORD:READ");
            case "INVOICE" -> new Source("invoice", "CONTRACT_INVOICE:READ");
            case "ORDER" -> new Source("order", "ORDER:READ");
            case "BUSINESS_TITLE" -> new Source("businessTitle", "CONTRACT_BUSINESS_TITLE:READ");
            default -> null;
        };
        if (binding == null) {
            var record = beans.getBean(CustomFormDataService.class).get(id, user, org);
            if (record == null || !Objects.equals(source, record.getCustomFormId()) || record.getName() == null) {
                throw new FormulaEvaluationException("INVALID_SOURCE_RECORD");
            }
            return record.getName();
        }
        Object entity = snapshots.readEntity(binding.formKey(), id);
        if (entity == null) throw new FormulaEvaluationException("SOURCE_NOT_FOUND");
        Map<String, Object> record = JSON.parseToMap(JSON.toJSONString(entity));
        if (org == null || !Objects.equals(org, record.get("organizationId"))) {
            throw new FormulaEvaluationException("SOURCE_ORGANIZATION_MISMATCH");
        }
        beans.getBean(ResourcePermissionService.class)
                .checkResourcePermission(binding.permission(), id, binding.formKey(), user, org);
        if (record.get("owner") instanceof String owner
                && !beans.getBean(DataScopeService.class).hasDataPermission(user, org, owner, binding.permission())) {
            throw new FormulaEvaluationException("SOURCE_FORBIDDEN");
        }
        if (!(record.get("name") instanceof String name)) throw new FormulaEvaluationException("SOURCE_NAME_MISSING");
        return name;
    }

    private record Source(String formKey, String permission) {}
}
