package cn.cordys.common.service;


import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.service.BusinessTitleService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.FieldSourceType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Service
@Slf4j
public class FieldSourceServiceProvider {

    private static final Map<FieldSourceType, Object> SERVICE_MAP = new HashMap<>();

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

    @PostConstruct
    public void init() {
        SERVICE_MAP.put(FieldSourceType.CLUE, clueService);
        SERVICE_MAP.put(FieldSourceType.CUSTOMER, customerService);
        SERVICE_MAP.put(FieldSourceType.OPPORTUNITY, opportunityService);
        SERVICE_MAP.put(FieldSourceType.CONTACT, customerContactService);
        SERVICE_MAP.put(FieldSourceType.PRODUCT, productService);
        SERVICE_MAP.put(FieldSourceType.PRICE, productPriceService);
        SERVICE_MAP.put(FieldSourceType.QUOTATION, opportunityQuotationService);
        SERVICE_MAP.put(FieldSourceType.CONTRACT, contractService);
        SERVICE_MAP.put(FieldSourceType.BUSINESS_TITLE, businessTitleService);
    }

    /**
     * 根据来源类型获取对应的 Service
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(FieldSourceType type) {
        return (T) SERVICE_MAP.get(type);
    }

    /**
     * 根据数据源类型和入参获取数据对象
     *
     * @param type 数据源类型
     * @param id   主键ID
     *
     * @return 数据对象
     */
    public Object getById(FieldSourceType type, Object id) {
        Object service = getService(type);
        if (service == null || !(id instanceof String) || ((String) id).isEmpty()) {
            log.error("数据源类型{}有误, 或参数值{}传递有误", new Object[]{type.name(), id});
            return null;
        }
        try {
            return service.getClass().getMethod("get", String.class).invoke(service, id.toString());
        } catch (Exception e) {
            log.error("获取数据源详情异常：" + id, e);
            return null;
        }
    }

    /**
     * 挂起事务, 防止下游方法异常回滚当前事务
     *
     * @param type 数据源类型
     * @param id   主键ID
     *
     * @return 数据对象
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Object safeGetById(FieldSourceType type, String id) {
        return getById(type, id);
    }
}