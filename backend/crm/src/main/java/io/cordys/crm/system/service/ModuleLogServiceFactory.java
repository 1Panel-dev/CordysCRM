package io.cordys.crm.system.service;

import io.cordys.aspectj.constants.LogModule;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.clue.service.ClueLogService;
import io.cordys.crm.customer.service.CustomerContactLogService;
import io.cordys.crm.customer.service.CustomerLogService;
import io.cordys.crm.follow.service.FollowUpPlanLogService;
import io.cordys.crm.follow.service.FollowUpRecordLogService;
import io.cordys.crm.opportunity.service.OpportunityLogService;

import java.util.HashMap;

public class ModuleLogServiceFactory {

    private static final HashMap<String, BaseModuleLogService> logServiceMap = new HashMap<>();

    static {
        logServiceMap.put(LogModule.CUSTOMER_INDEX, CommonBeanFactory.getBean(CustomerLogService.class));
        logServiceMap.put(LogModule.CUSTOMER_CONTACT, CommonBeanFactory.getBean(CustomerContactLogService.class));
        logServiceMap.put(LogModule.OPPORTUNITY, CommonBeanFactory.getBean(OpportunityLogService.class));
        logServiceMap.put(LogModule.SYSTEM_ORGANIZATION, CommonBeanFactory.getBean(OrganizationLogService.class));
        logServiceMap.put(LogModule.PRODUCT_MANAGEMENT, CommonBeanFactory.getBean(ProductLogService.class));
        logServiceMap.put(LogModule.CLUE_INDEX, CommonBeanFactory.getBean(ClueLogService.class));
        logServiceMap.put(LogModule.FOLLOW_UP_RECORD, CommonBeanFactory.getBean(FollowUpRecordLogService.class));
        logServiceMap.put(LogModule.FOLLOW_UP_PLAN, CommonBeanFactory.getBean(FollowUpPlanLogService.class));

    }

    public static BaseModuleLogService getModuleLogService(String type) {
        return logServiceMap.get(type);
    }
}