package cn.cordys.common.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.service.BusinessTitleService;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.contract.service.ContractPaymentPlanService;
import cn.cordys.crm.contract.service.ContractPaymentRecordService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.FieldSourceType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 按数据源分发详情查询，查询失败时保留引用字段的空值降级行为。
 */
@Service
@Slf4j
public class FieldSourceServiceProvider {

    private final Map<FieldSourceType, SourceReader> sources = new EnumMap<>(FieldSourceType.class);
    private final Map<String, SourceReader> approvalSources = new HashMap<>();
    private final Map<String, Function<String, Object>> approvalFields = new HashMap<>();
    private SourceReader customSource;

    @Resource
    private ClueService clueService;
    @Resource
    private CustomerService customerService;
    @Resource
    private OpportunityService opportunityService;
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private ProductService productService;
    @Resource
    private ProductPriceService productPriceService;
    @Resource
    private OpportunityQuotationService opportunityQuotationService;
    @Resource
    private ContractService contractService;
    @Resource
    private BusinessTitleService businessTitleService;
    @Resource
    private OrderService orderService;
    @Resource
    private ContractPaymentRecordService paymentRecordService;
    @Resource
    private ContractPaymentPlanService paymentPlanService;
    @Resource
    private ContractInvoiceService invoiceService;

    @Lazy
    @Resource
    private CustomFormDataService customFormDataService;

    @PostConstruct
    public void init() {
        sources.put(FieldSourceType.CLUE,
                new SourceReader(clueService::getSimple,
                        clueService::batchGetSimpleByIds));
        sources.put(FieldSourceType.CUSTOMER,
                new SourceReader(customerService::getSimple,
                        customerService::batchGetSimpleByIds));
        sources.put(FieldSourceType.OPPORTUNITY,
                new SourceReader(opportunityService::getSimple,
                        opportunityService::batchGetSimpleByIds));
        sources.put(FieldSourceType.CONTACT,
                new SourceReader(customerContactService::getSimple,
                        customerContactService::batchGetSimpleByIds));
        sources.put(FieldSourceType.PRODUCT,
                new SourceReader(productService::getSimple,
                        productService::batchGetSimpleByIds));
        sources.put(FieldSourceType.PRICE,
                new SourceReader(productPriceService::getSimple,
                        productPriceService::batchGetSimpleByIds));
        sources.put(FieldSourceType.QUOTATION,
                new SourceReader(opportunityQuotationService::getSimple,
                        opportunityQuotationService::batchGetSimpleByIds));
        sources.put(FieldSourceType.CONTRACT,
                new SourceReader(contractService::getSimple,
                        contractService::batchGetSimpleByIds));
        sources.put(FieldSourceType.BUSINESS_TITLE,
                new SourceReader(businessTitleService::getSimple,
                        businessTitleService::batchGetSimpleByIds));
        sources.put(FieldSourceType.ORDER,
                new SourceReader(orderService::getSimple,
                        orderService::batchGetSimpleByIds));
        sources.put(FieldSourceType.CONTRACT_PAYMENT_RECORD,
                new SourceReader(paymentRecordService::getSimple,
                        paymentRecordService::batchGetSimpleByIds));
        sources.put(FieldSourceType.PAYMENT_PLAN,
                new SourceReader(paymentPlanService::getSimple,
                        paymentPlanService::batchGetSimpleByIds));
        sources.put(FieldSourceType.INVOICE,
                new SourceReader(invoiceService::getSimple,
                        invoiceService::batchGetSimpleByIds));
        customSource = new SourceReader(customFormDataService::getSimple, customFormDataService::batchGetSimpleByIds);

        approvalSources.put(FormKey.CLUE.getKey(), sources.get(FieldSourceType.CLUE));
        approvalSources.put(FormKey.CUSTOMER.getKey(), sources.get(FieldSourceType.CUSTOMER));
        approvalSources.put(FormKey.OPPORTUNITY.getKey(), sources.get(FieldSourceType.OPPORTUNITY));
        approvalSources.put(FormKey.QUOTATION.getKey(), sources.get(FieldSourceType.QUOTATION));
        approvalSources.put(FormKey.CONTRACT.getKey(), sources.get(FieldSourceType.CONTRACT));
        approvalSources.put(FormKey.INVOICE.getKey(), sources.get(FieldSourceType.INVOICE));
        approvalSources.put(FormKey.ORDER.getKey(), sources.get(FieldSourceType.ORDER));

        approvalFields.put(FormKey.QUOTATION.getKey(), opportunityQuotationService::getFieldValues);
        approvalFields.put(FormKey.CONTRACT.getKey(), contractService::getFieldValues);
        approvalFields.put(FormKey.INVOICE.getKey(), invoiceService::getFieldValues);
        approvalFields.put(FormKey.ORDER.getKey(), orderService::getFieldValues);
    }

    /**
     * 挂起调用方事务，避免详情查询失败将业务操作标记为回滚。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Object safeGetSimpleById(FieldSourceType type, String id) {
        return readSingle(sources.getOrDefault(type, customSource).single(), id, type);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Object safeGetSimpleById(String formKey, String id) {
        SourceReader source = approvalSources.get(formKey);
        if (source == null && FormKey.ofKey(formKey) == null) {
            source = customSource;
        }
        return readSingle(source == null ? null : source.single(), id, formKey);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @SuppressWarnings("unchecked")
    public List<Object> batchGetSimpleByIds(FieldSourceType type, List<String> ids) {
        if (ids == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        try {
            return (List<Object>) sources.getOrDefault(type, customSource).batch()
                    .apply(ids.stream().map(Object::toString).toList());
        } catch (Exception e) {
            log.error("获取数据源详情异常：type={}, ids={}, error={}", type, ids, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Object safeGetFieldsById(String formKey, String id) {
        Function<String, Object> reader = approvalFields.get(formKey);
        if (reader == null && FormKey.ofKey(formKey) == null) {
            reader = customFormDataService::getFieldValues;
        }
        return readSingle(reader, id, formKey);
    }

    private Object readSingle(Function<String, Object> reader, String id, Object source) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (reader == null) {
            log.error("数据源查询不支持该表单：{}", source);
            return null;
        }
        try {
            return reader.apply(id);
        } catch (Exception e) {
            log.error("获取数据源详情异常：type={}, id={}, error={}", source, id, e.getMessage(), e);
            return null;
        }
    }

    private record SourceReader(Function<String, Object> single, Function<List<String>, List<?>> batch) {
    }
}
