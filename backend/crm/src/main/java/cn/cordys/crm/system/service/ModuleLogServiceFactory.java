package cn.cordys.crm.system.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.clue.service.ClueLogService;
import cn.cordys.crm.customer.service.CustomerContactLogService;
import cn.cordys.crm.customer.service.CustomerLogService;
import cn.cordys.crm.customer.service.RoleLogService;
import cn.cordys.crm.follow.service.FollowUpPlanLogService;
import cn.cordys.crm.follow.service.FollowUpRecordLogService;
import cn.cordys.crm.opportunity.service.OpportunityLogService;
import cn.cordys.crm.product.service.ProductLogService;

import java.util.HashMap;

public class ModuleLogServiceFactory {

    private static final HashMap<String, BaseModuleLogService> logServiceMap = new HashMap<>();

    static {
        logServiceMap.put(LogModule.CUSTOMER_INDEX, CommonBeanFactory.getBean(CustomerLogService.class));
        logServiceMap.put(LogModule.CUSTOMER_POOL, CommonBeanFactory.getBean(CustomerLogService.class));
        logServiceMap.put(LogModule.CUSTOMER_CONTACT, CommonBeanFactory.getBean(CustomerContactLogService.class));
        logServiceMap.put(LogModule.OPPORTUNITY_INDEX, CommonBeanFactory.getBean(OpportunityLogService.class));
        logServiceMap.put(LogModule.OPPORTUNITY_QUOTATION, CommonBeanFactory.getBean(OpportunityLogService.class));
        logServiceMap.put(LogModule.SYSTEM_ORGANIZATION, CommonBeanFactory.getBean(OrganizationLogService.class));
        logServiceMap.put(LogModule.PRODUCT_MANAGEMENT, CommonBeanFactory.getBean(ProductLogService.class));
        logServiceMap.put(LogModule.CLUE_INDEX, CommonBeanFactory.getBean(ClueLogService.class));
        logServiceMap.put(LogModule.CLUE_POOL_INDEX, CommonBeanFactory.getBean(ClueLogService.class));
        logServiceMap.put(LogModule.FOLLOW_UP_RECORD, CommonBeanFactory.getBean(FollowUpRecordLogService.class));
        logServiceMap.put(LogModule.FOLLOW_UP_PLAN, CommonBeanFactory.getBean(FollowUpPlanLogService.class));
        logServiceMap.put(LogModule.SYSTEM_ROLE, CommonBeanFactory.getBean(RoleLogService.class));
        logServiceMap.put(LogModule.SYSTEM_MODULE, CommonBeanFactory.getBean(SystemModuleLogService.class));

    }

    public static BaseModuleLogService getModuleLogService(String type) {
        return logServiceMap.get(type);
    }
}