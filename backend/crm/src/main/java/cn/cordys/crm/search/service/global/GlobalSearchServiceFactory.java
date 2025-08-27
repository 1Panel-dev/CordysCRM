package cn.cordys.crm.search.service.global;

import cn.cordys.aspectj.constants.GlobalSearchModule;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.search.service.BaseSearchService;

import java.util.HashMap;

public class GlobalSearchServiceFactory {

    private static final HashMap<String, BaseSearchService<?, ?>> searchServiceMap = new HashMap<>();

    static {
        searchServiceMap.put(GlobalSearchModule.OPPORTUNITY, CommonBeanFactory.getBean(GlobalOpportunitySearchService.class));
        searchServiceMap.put(GlobalSearchModule.CUSTOMER_POOL, CommonBeanFactory.getBean(GlobalCustomerPoolSearchService.class));
        searchServiceMap.put(GlobalSearchModule.CLUE, CommonBeanFactory.getBean(GlobalClueSearchService.class));
        searchServiceMap.put(GlobalSearchModule.CLUE_POOL, CommonBeanFactory.getBean(GlobalCluePoolSearchService.class));
        searchServiceMap.put(GlobalSearchModule.CUSTOMER, CommonBeanFactory.getBean(GlobalCustomerSearchService.class));
        searchServiceMap.put(GlobalSearchModule.CUSTOMER_CONTACT, CommonBeanFactory.getBean(GlobalCustomerContactSearchService.class));
    }

    @SuppressWarnings("unchecked")
    public static <REQ extends BasePageRequest, RES> BaseSearchService<REQ, RES> getSearchService(String type) {
        return (BaseSearchService<REQ, RES>) searchServiceMap.get(type);
    }
}