package io.cordys.crm.search.service.global;

import io.cordys.aspectj.constants.GlobalSearchModule;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.search.service.BaseSearchService;

import java.util.HashMap;

public class GlobalSearchServiceFactory {

    private static final HashMap<String, BaseSearchService<?,?>> searchServiceMap = new HashMap<>();

    static {
        searchServiceMap.put(GlobalSearchModule.OPPORTUNITY, CommonBeanFactory.getBean(GlobalOpportunitySearchService.class));
     /*   searchServiceMap.put(GlobalSearchModule.CUSTOMER_POOL, CommonBeanFactory.getBean(AdvancedCustomerPoolSearchService.class));
        searchServiceMap.put(GlobalSearchModule.CUSTOMER, CommonBeanFactory.getBean(AdvancedCustomerSearchService.class));
        searchServiceMap.put(GlobalSearchModule.CUSTOMER_CONTACT, CommonBeanFactory.getBean(AdvancedCustomerContactSearchService.class));
        searchServiceMap.put(GlobalSearchModule.CLUE, CommonBeanFactory.getBean(AdvancedClueSearchService.class));
        searchServiceMap.put(GlobalSearchModule.CLUE_POOL, CommonBeanFactory.getBean(AdvancedCluePoolSearchService.class));*/
    }

    @SuppressWarnings("unchecked")
    public static <REQ extends BasePageRequest, RES> BaseSearchService<REQ, RES> getSearchService(String type) {
        return (BaseSearchService<REQ, RES>) searchServiceMap.get(type);
    }
}