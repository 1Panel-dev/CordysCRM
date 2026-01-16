package cn.cordys.common.constants;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 业务模块字段（定义在主表中，有特定业务含义）(标准字段)
 *
 * @Author: jianxing
 * @CreateTime: 2025-02-18  17:27
 */
@Getter
public enum BusinessModuleField {

    /*------ start: CUSTOMER ------*/
    /**
     * 客户名称
     */
    CUSTOMER_NAME("customerName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.CUSTOMER.getKey()),
    /**
     * 负责人
     */
    CUSTOMER_OWNER("customerOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.CUSTOMER.getKey()),
    /*------ end: CUSTOMER ------*/

    /*------ start: CLUE ------*/
    /**
     * 线索名称
     */
    CLUE_NAME("clueName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.CLUE.getKey()),
    /**
     * 负责人
     */
    CLUE_OWNER("clueOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.CLUE.getKey()),
    /**
     * 联系人
     */
    CLUE_CONTACT("clueContactName", "contact", Set.of(), FormKey.CLUE.getKey()),
    /**
     * 联系人电话
     */
    CLUE_CONTACT_PHONE("clueContactPhone", "phone", Set.of(), FormKey.CLUE.getKey()),
    /**
     * 意向产品
     */
    CLUE_PRODUCTS("clueProduct", "products", Set.of(), FormKey.CLUE.getKey()),
    /*------ end: CUSTOMER ------*/

    /*------ start: CUSTOMER_MANAGEMENT_CONTACT ------*/
    /**
     * 联系人客户id
     */
    CUSTOMER_CONTACT_CUSTOMER("contactCustomer", "customerId", Set.of(), FormKey.CONTACT.getKey()),
    /**
     * 联系人责任人
     */
    CUSTOMER_CONTACT_OWNER("contactOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.CONTACT.getKey()),
    /**
     * 联系人名称
     */
    CUSTOMER_CONTACT_NAME("contactName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.CONTACT.getKey()),
    /**
     * 联系人电话
     */
    CUSTOMER_CONTACT_PHONE("contactPhone", "phone", Set.of(), FormKey.CONTACT.getKey()),
    /*------ end: CUSTOMER_MANAGEMENT_CONTACT ------*/


    /*------ start: OPPORTUNITY ------*/
    /**
     * 商机名称
     */
    OPPORTUNITY_NAME("opportunityName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.OPPORTUNITY.getKey()),
    /**
     * 客户名称
     */
    OPPORTUNITY_CUSTOMER_NAME("opportunityCustomer", "customerId", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 商机金额
     */
    OPPORTUNITY_AMOUNT("opportunityPrice", "amount", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 可能性
     */
    OPPORTUNITY_POSSIBLE("opportunityWinRate", "possible", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 结束时间
     */
    OPPORTUNITY_END_TIME("opportunityEndTime", "expectedEndTime", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 意向产品
     */
    OPPORTUNITY_PRODUCTS("opportunityProduct", "products", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 联系人
     */
    OPPORTUNITY_CONTACT("opportunityContact", "contactId", Set.of(), FormKey.OPPORTUNITY.getKey()),

    /**
     * 负责人
     */
    OPPORTUNITY_OWNER("opportunityOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.OPPORTUNITY.getKey()),
    /*------ end: OPPORTUNITY ------*/



    /*------ start: FOLLOW_UP_RECORD ------*/
    /**
     * 跟进类型
     */
    FOLLOW_RECORD_TYPE("recordType", "type", Set.of("options", "rules.required", "mobile", "readable"), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 客户id
     */
    FOLLOW_RECORD_CUSTOMER("recordCustomer", "customerId", Set.of("rules.required", "mobile", "readable"), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 商机id
     */
    FOLLOW_RECORD_OPPORTUNITY("recordOpportunity", "opportunityId", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 线索id
     */
    FOLLOW_RECORD_CLUE("recordClue", "clueId", Set.of("rules.required", "mobile", "readable"), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 责任人id
     */
    FOLLOW_RECORD_OWNER("recordOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 联系人id
     */
    FOLLOW_RECORD_CONTACT("recordContact", "contactId", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 跟进内容
     */
    FOLLOW_RECORD_CONTENT("recordDescription", "content", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 跟进时间
     */
    FOLLOW_RECORD_TIME("recordTime", "followTime", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 跟进方式
     */
    FOLLOW_METHOD("recordMethod", "followMethod", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /*------ end: FOLLOW_UP_RECORD ------*/


    /*------ start: FOLLOW_UP_PLAN ------*/
    /**
     * 跟进类型
     */
    FOLLOW_PLAN_TYPE("planType", "type", Set.of("options", "rules.required", "mobile", "readable"), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 客户id
     */
    FOLLOW_PLAN_CUSTOMER("planCustomer", "customerId", Set.of("rules.required", "mobile", "readable"), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 商机id
     */
    FOLLOW_PLAN_OPPORTUNITY("planOpportunity", "opportunityId", Set.of(), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 线索id
     */
    FOLLOW_PLAN_CLUE("planClue", "clueId", Set.of("rules.required", "mobile", "readable"), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 责任人id
     */
    FOLLOW_PLAN_OWNER("planOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 联系人id
     */
    FOLLOW_PLAN_CONTACT("planContact", "contactId", Set.of(), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 预计开始时间
     */
    FOLLOW_PLAN_ESTIMATED_TIME("planStartTime", "estimatedTime", Set.of(), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 预计沟通内容
     */
    FOLLOW_PLAN_CONTENT("planContent", "content", Set.of(), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 跟进方式
     */
    FOLLOW_PLAN_METHOD("planMethod", "method", Set.of(), FormKey.FOLLOW_PLAN.getKey()),


    /*------ end: FOLLOW_UP_PLAN ------*/

    /*------ start: PRODUCT ------*/
    PRODUCT_NAME("productName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.PRODUCT.getKey()),
    PRODUCT_PRICE("productPrice", "price", Set.of(), FormKey.PRODUCT.getKey()),
    PRODUCT_STATUS("productStatus", "status", Set.of("rules.required", "mobile", "readable"), FormKey.PRODUCT.getKey()),
    /*------ end: PRODUCT ------*/

    /**
     * 价格表单
     */
    PRICE_NAME("priceName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.PRICE.getKey()),
    PRICE_STATUS("priceStatus", "status", Set.of("rules.required", "mobile", "readable"), FormKey.PRICE.getKey()),
    PRICE_PRODUCT_TABLE("priceProducts", "products", Set.of("mobile", "readable"), FormKey.PRICE.getKey()),
    PRICE_PRODUCT("priceProduct", "product", Set.of("rules.required", "mobile", "dataSourceType", "readable"), FormKey.PRICE.getKey()),
    PRICE_PRODUCT_AMOUNT("priceProductAmount", "amount", Set.of("rules.required", "mobile", "readable"), FormKey.PRICE.getKey()),

    /**
     * 报价单表单
     */
    QUOTATION_NAME("quotationName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.QUOTATION.getKey()),
    QUOTATION_OPPORTUNITY("quotationOpportunity", "opportunityId", Set.of("rules.required", "mobile", "dataSourceType", "readable"), FormKey.QUOTATION.getKey()),
    QUOTATION_PRODUCT_TABLE("quotationProducts", "products", Set.of("mobile", "readable"), FormKey.QUOTATION.getKey()),
    QUOTATION_PRODUCT_AMOUNT("quotationAmount", "amount", Set.of("rules.required", "mobile", "readable"), FormKey.QUOTATION.getKey()),
    QUOTATION_UNTIL_TIME("quotationUntilTime", "untilTime", Set.of("rules.required", "mobile", "dateType", "readable"), FormKey.QUOTATION.getKey()),

    /**
     * 合同回款计划
     */
    /*------ start: CONTRACT_PAYMENT_PLAN ------*/
    /**
     * 负责人
     */
    CONTRACT_PAYMENT_PLAN_OWNER("contractPaymentPlanOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_PLAN.getKey()),
    /**
     * 合同
     */
    CONTRACT_PAYMENT_PLAN_CONTRACT("contractPaymentPlanContract", "contractId", Set.of("rules.required", "dataSourceType", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_PLAN.getKey()),
    /**
     * 计划回款金额
     */
    CONTRACT_PAYMENT_PLAN_PLAN_AMOUNT("contractPaymentPlanPlanAmount", "planAmount", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_PLAN.getKey()),
    /**
     * 计划回款时间
     */
    CONTRACT_PAYMENT_PLAN_PLAN_END_TIME("contractPaymentPlanPlanEndTime", "planEndTime", Set.of("rules.required", "dateType", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_PLAN.getKey()),
    /**
     * 回款计划名称
     */
    CONTRACT_PAYMENT_PLAN_NAME("contractPaymentPlanName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_PLAN.getKey()),
    /*------ end: CONTRACT_PAYMENT_PLAN ------*/


    /**
     * 合同
     */
    /*------ start: CONTRACT ------*/
    /**
     * 合同名稱
     */
    CONTRACT_NAME("contractName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT.getKey()),

    CONTRACT_CUSTOMER_NAME("contractCustomer", "customerId", Set.of("rules.required", "mobile", "readable", "dataSourceType"), FormKey.CONTRACT.getKey()),
    CONTRACT_PRODUCT_TABLE("contractProducts", "products", Set.of("mobile", "readable"), FormKey.CONTRACT.getKey()),
    CONTRACT_PRODUCT("contractProduct", "product", Set.of("rules.required", "mobile", "dataSourceType", "readable"), FormKey.CONTRACT.getKey()),
    CONTRACT_PRODUCT_AMOUNT("contractProductAmount", "price", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT.getKey()),
    CONTRACT_PRODUCT_SUM_AMOUNT("contractProductSumAmount", "amount", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT.getKey()),
    CONTRACT_OWNER("contractOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT.getKey()),
    CONTRACT_NO("contractNo", "number", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT.getKey()),
    CONTRACT_START_TIME("contractStartTime", "startTime", Set.of("rules.required", "mobile", "dateType", "readable"), FormKey.CONTRACT.getKey()),
    CONTRACT_END_TIME("contractEndTime", "endTime", Set.of("rules.required", "mobile", "dateType", "readable"), FormKey.CONTRACT.getKey()),

    /*------ end: CONTRACT ------*/

    /**
     * 发票
     */
    /*------ start: CONTRACT_INVOICE ------*/
    INVOICE_NAME("invoiceName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.INVOICE.getKey()),
    INVOICE_OWNER("invoiceOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.INVOICE.getKey()),
    INVOICE_AMOUNT("invoiceAmount", "amount", Set.of("rules.required", "mobile"), FormKey.INVOICE.getKey()),
    INVOICE_CONTRACT_ID("invoiceContract", "contractId", Set.of("rules.required", "mobile", "readable", "dataSourceType"), FormKey.INVOICE.getKey()),
    INVOICE_INVOICE_TYPE("invoiceType", "invoiceType", Set.of("rules.required", "mobile"), FormKey.INVOICE.getKey()),
    INVOICE_TAX_RATE("invoiceTaxRate", "taxRate", Set.of("rules.required", "mobile"), FormKey.INVOICE.getKey()),
    INVOICE_BUSINESS_TITLE_ID("invoiceBusinessTitle", "businessTitleId", Set.of("rules.required", "mobile", "dataSourceType"), FormKey.INVOICE.getKey()),
    /*------ end: CONTRACT_INVOICE ------*/


    /*------ start: CONTRACT_PAYMENT_RECORD 合同回款记录  ------*/
    CONTRACT_PAYMENT_RECORD_NO("contractPaymentRecordNo", "no", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey()),
    CONTRACT_PAYMENT_RECORD_NAME("contractPaymentRecordName", "name", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey()),
    CONTRACT_PAYMENT_RECORD_OWNER("contractPaymentRecordOwner", "owner", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey()),
    CONTRACT_PAYMENT_RECORD_CONTRACT("contractPaymentRecordContract", "contractId", Set.of("rules.required", "dataSourceType", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey()),
    CONTRACT_PAYMENT_RECORD_PLAN("contractPaymentRecordPlan", "paymentPlanId", Set.of("dataSourceType", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey()),
    CONTRACT_PAYMENT_RECORD_AMOUNT("contractPaymentRecordAmount", "recordAmount", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey()),
    CONTRACT_PAYMENT_RECORD_END_TIME("contractPaymentRecordEndTime", "recordEndTime", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey()),
    CONTRACT_PAYMENT_RECORD_BANK("contractPaymentRecordBank", "recordBank", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey()),
    CONTRACT_PAYMENT_RECORD_BANK_NO("contractPaymentRecordBankNo", "recordBankNo", Set.of("rules.required", "mobile", "readable"), FormKey.CONTRACT_PAYMENT_RECORD.getKey());
    /*------ end: CONTRACT_PAYMENT_RECORD 合同回款记录  ------*/


    /**
     * 业务字段缓存
     */
    private static final Map<String, BusinessModuleField> INTERNAL_CACHE = new HashMap<>();

    static {
        for (BusinessModuleField field : values()) {
            // 防止ofKey方法频繁调用
            INTERNAL_CACHE.put(field.key, field);
        }
    }

    /**
     * 字段 key，field.json 中的 internalKey
     */
    private final String key;
    /**
     * 业务字段 key
     */
    private final String businessKey;
    /**
     * 禁止修改的参数列表
     */
    private final Set<String> disabledProps;
    /**
     * 表单 key
     */
    private final String formKey;

    BusinessModuleField(String key, String businessKey, Set<String> disabledProps, String formKey) {
        this.key = key;
        this.businessKey = businessKey;
        this.disabledProps = disabledProps;
        this.formKey = formKey;
    }

    /**
     * 判断业务字段是否被删除
     *
     * @param formKey 表单 key
     * @param fields  字段集合
     * @return 是否被删除
     */
    public static boolean isBusinessDeleted(String formKey, List<BaseField> fields) {
        List<BusinessModuleField> formBusinessFields = Arrays.stream(BusinessModuleField.values()).filter(field -> Strings.CS.equals(formKey, field.getFormKey())).toList();
        if (CollectionUtils.isEmpty(formBusinessFields)) {
            return false;
        }
        return formBusinessFields.stream()
                .anyMatch(businessField ->
                        fields.stream().noneMatch(field -> Strings.CS.equals(businessField.getKey(), field.getInternalKey()))
                                && businessField.noneMatchOfSubFields(fields)
                );
    }

    /**
     * 判断子表字段中是否存在业务字段
     *
     * @param fields 字段集合
     * @return 是否存在业务字段
     */
    private boolean noneMatchOfSubFields(List<BaseField> fields) {
        boolean noneMatch = true;
        for (BaseField field : fields) {
            if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
                noneMatch = subField.getSubFields().stream().noneMatch(sub -> Strings.CS.equals(this.getKey(), sub.getInternalKey()));
                if (!noneMatch) {
                    break;
                }
            }
        }
        return noneMatch;
    }

    /**
     * 判断是否有重复的字段
     *
     * @param fields 字段集合
     * @return 是否有重复的字段
     */
    public static boolean hasRepeatName(List<BaseField> fields) {
        return fields.stream()
                .collect(Collectors.groupingBy(BaseField::getName, Collectors.counting()))
                .values().stream()
                .anyMatch(count -> count > 1);
    }

    /**
     * 通过Key查询业务字段
     *
     * @param internalKey 业务key
     * @return 业务字段
     */
    public static BusinessModuleField ofKey(String internalKey) {
        return INTERNAL_CACHE.get(internalKey);
    }
}
