package io.cordys.crm.search.service;

import io.cordys.aspectj.constants.GlobalSearchModule;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.util.CommonBeanFactory;

import java.util.HashMap;

public class GlobalSearchServiceFactory {

    private static final HashMap<String, GlobalSearchBaseService<?,?>> searchServiceMap = new HashMap<>();

    static {
        searchServiceMap.put(GlobalSearchModule.OPPORTUNITY, CommonBeanFactory.getBean(GlobalSearchOpportunityService.class));
        searchServiceMap.put(GlobalSearchModule.CUSTOMER_POOL, CommonBeanFactory.getBean(GlobalSearchCustomerPoolService.class));
        searchServiceMap.put(GlobalSearchModule.CUSTOMER, CommonBeanFactory.getBean(GlobalSearchCustomerService.class));
        searchServiceMap.put(GlobalSearchModule.CUSTOMER_CONTACT, CommonBeanFactory.getBean(GlobalSearchCustomerContactService.class));
        searchServiceMap.put(GlobalSearchModule.CLUE, CommonBeanFactory.getBean(GlobalSearchClueService.class));
        searchServiceMap.put(GlobalSearchModule.CLUE_POOL, CommonBeanFactory.getBean(GlobalSearchCluePoolService.class));
    }

    @SuppressWarnings("unchecked")
    public static <REQ extends BasePageRequest, RES> GlobalSearchBaseService<REQ, RES> getSearchService(String type) {
        return (GlobalSearchBaseService<REQ, RES>) searchServiceMap.get(type);
    }
}