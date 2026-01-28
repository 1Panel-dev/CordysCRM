package cn.cordys.crm.search.service.advanced;

import cn.cordys.aspectj.constants.GlobalSearchModule;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.search.service.BaseSearchService;
import java.util.HashMap;

public class AdvancedSearchServiceFactory {

  private static final HashMap<String, BaseSearchService<?, ?>> searchServiceMap = new HashMap<>();

  static {
    searchServiceMap.put(
        GlobalSearchModule.OPPORTUNITY,
        CommonBeanFactory.getBean(AdvancedOpportunitySearchService.class));
    searchServiceMap.put(
        GlobalSearchModule.CUSTOMER_POOL,
        CommonBeanFactory.getBean(AdvancedCustomerPoolSearchService.class));
    searchServiceMap.put(
        GlobalSearchModule.CUSTOMER,
        CommonBeanFactory.getBean(AdvancedCustomerSearchService.class));
    searchServiceMap.put(
        GlobalSearchModule.CUSTOMER_CONTACT,
        CommonBeanFactory.getBean(AdvancedCustomerContactSearchService.class));
    searchServiceMap.put(
        GlobalSearchModule.CLUE, CommonBeanFactory.getBean(AdvancedClueSearchService.class));
    searchServiceMap.put(
        GlobalSearchModule.CLUE_POOL,
        CommonBeanFactory.getBean(AdvancedCluePoolSearchService.class));
  }

  @SuppressWarnings("unchecked")
  public static <REQ extends BasePageRequest, RES> BaseSearchService<REQ, RES> getSearchService(
      String type) {
    return (BaseSearchService<REQ, RES>) searchServiceMap.get(type);
  }
}
