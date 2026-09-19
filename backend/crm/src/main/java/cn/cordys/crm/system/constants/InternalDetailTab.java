package cn.cordys.crm.system.constants;

import cn.cordys.common.constants.FormKey;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 已有详情页中由系统提供的内置标签。
 *
 * <p>内置标签只允许改名，不能关闭或删除。直接关联字段使用 internalKey 定位，避免不同组织初始化后
 * 字段 ID 不同；间接关联标签的字段为空，独立业务标签的关联表单和字段均为空。运行时通过枚举名称生成的
 * internalKey 稳定识别各内置标签。</p>
 */
@Getter
public enum InternalDetailTab {

    CUSTOMER_FOLLOW_RECORD(FormKey.CUSTOMER.getKey(), FormKey.FOLLOW_RECORD.getKey(), FormKey.FOLLOW_RECORD.getKey(), "recordCustomer"),
    CUSTOMER_CONTACT(FormKey.CUSTOMER.getKey(), "module.form.detail_tab.contact_info", FormKey.CONTACT.getKey(), "contactCustomer"),
    CUSTOMER_FOLLOW_PLAN(FormKey.CUSTOMER.getKey(), FormKey.FOLLOW_PLAN.getKey(), FormKey.FOLLOW_PLAN.getKey(), "planCustomer"),
    CUSTOMER_OWNER_RECORD(FormKey.CUSTOMER.getKey(), "module.form.detail_tab.owner_record", null, null),
    CUSTOMER_RELATION(FormKey.CUSTOMER.getKey(), "module.form.detail_tab.customer_relation", null, null),
    CUSTOMER_OPPORTUNITY(FormKey.CUSTOMER.getKey(), "module.form.detail_tab.opportunity_info", FormKey.OPPORTUNITY.getKey(), "opportunityCustomer"),
    CUSTOMER_COLLABORATION(FormKey.CUSTOMER.getKey(), "module.form.detail_tab.collaboration", null, null),
    CUSTOMER_CONTRACT(FormKey.CUSTOMER.getKey(), FormKey.CONTRACT.getKey(), FormKey.CONTRACT.getKey(), "contractCustomer"),
    CUSTOMER_PAYMENT_PLAN(FormKey.CUSTOMER.getKey(), FormKey.CONTRACT_PAYMENT_PLAN.getKey(), FormKey.CONTRACT_PAYMENT_PLAN.getKey(), null),
    CUSTOMER_PAYMENT_RECORD(FormKey.CUSTOMER.getKey(), FormKey.CONTRACT_PAYMENT_RECORD.getKey(), FormKey.CONTRACT_PAYMENT_RECORD.getKey(), null),
    CUSTOMER_INVOICE(FormKey.CUSTOMER.getKey(), FormKey.INVOICE.getKey(), FormKey.INVOICE.getKey(), null),
    CUSTOMER_ORDER(FormKey.CUSTOMER.getKey(), FormKey.ORDER.getKey(), FormKey.ORDER.getKey(), "orderCustomer"),

    CLUE_FOLLOW_RECORD(FormKey.CLUE.getKey(), FormKey.FOLLOW_RECORD.getKey(), FormKey.FOLLOW_RECORD.getKey(), "recordClue"),
    CLUE_FOLLOW_PLAN(FormKey.CLUE.getKey(), FormKey.FOLLOW_PLAN.getKey(), FormKey.FOLLOW_PLAN.getKey(), "planClue"),
    CLUE_OWNER_RECORD(FormKey.CLUE.getKey(), "module.form.detail_tab.owner_record", null, null),

    OPPORTUNITY_FOLLOW_RECORD(FormKey.OPPORTUNITY.getKey(), FormKey.FOLLOW_RECORD.getKey(), FormKey.FOLLOW_RECORD.getKey(), "recordOpportunity"),
    OPPORTUNITY_FOLLOW_PLAN(FormKey.OPPORTUNITY.getKey(), FormKey.FOLLOW_PLAN.getKey(), FormKey.FOLLOW_PLAN.getKey(), "planOpportunity"),
    OPPORTUNITY_CONTACT(FormKey.OPPORTUNITY.getKey(), "module.form.detail_tab.contact_info", FormKey.CONTACT.getKey(), null),
    OPPORTUNITY_QUOTATION(FormKey.OPPORTUNITY.getKey(), FormKey.QUOTATION.getKey(), FormKey.QUOTATION.getKey(), "quotationOpportunity"),

    CONTRACT_PAYMENT_PLAN(FormKey.CONTRACT.getKey(), FormKey.CONTRACT_PAYMENT_PLAN.getKey(), FormKey.CONTRACT_PAYMENT_PLAN.getKey(), "contractPaymentPlanContract"),
    CONTRACT_PAYMENT_RECORD(FormKey.CONTRACT.getKey(), FormKey.CONTRACT_PAYMENT_RECORD.getKey(), FormKey.CONTRACT_PAYMENT_RECORD.getKey(), "contractPaymentRecordContract"),
    CONTRACT_INVOICE(FormKey.CONTRACT.getKey(), FormKey.INVOICE.getKey(), FormKey.INVOICE.getKey(), "invoiceContract"),
    CONTRACT_ORDER(FormKey.CONTRACT.getKey(), FormKey.ORDER.getKey(), FormKey.ORDER.getKey(), "orderContract");

    /** 标签所属的详情表单。 */
    private final String formKey;
    /** 内置标签的默认名称国际化键。 */
    private final String labelKey;
    /** 标签展示数据对应的关联表单。 */
    private final String relatedFormKey;
    /** 关联表单中指向当前表单的数据源字段；间接关联或独立标签时为空。 */
    private final String relatedFieldInternalKey;

    InternalDetailTab(String formKey, String labelKey, String relatedFormKey, String relatedFieldInternalKey) {
        this.formKey = formKey;
        this.labelKey = labelKey;
        this.relatedFormKey = relatedFormKey;
        this.relatedFieldInternalKey = relatedFieldInternalKey;
    }

    /**
     * 根据稳定业务标识查找内置标签，支持改名后继续识别无关联标签。
     */
    public static Optional<InternalDetailTab> findByInternalKey(String formKey, String internalKey) {
        if (internalKey == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(tab -> tab.formKey.equals(formKey))
                .filter(tab -> tab.name().equals(internalKey))
                .findFirst();
    }
}
