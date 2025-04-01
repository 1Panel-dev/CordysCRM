package io.cordys.crm.system.service;

import io.cordys.aspectj.constants.LogModule;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.customer.service.CustomerContactLogService;
import io.cordys.crm.customer.service.CustomerLogService;

import java.util.HashMap;

public class ModuleLogServiceFactory {

    private static final HashMap<String, BaseModuleLogService> logServiceMap = new HashMap<>();

    static {
        logServiceMap.put(LogModule.CUSTOMER, CommonBeanFactory.getBean(CustomerLogService.class));
        logServiceMap.put(LogModule.CUSTOMER_CONTACT, CommonBeanFactory.getBean(CustomerContactLogService.class));
    }

    public static BaseModuleLogService getModuleLogService(String type) {
        return logServiceMap.get(type);
    }
}