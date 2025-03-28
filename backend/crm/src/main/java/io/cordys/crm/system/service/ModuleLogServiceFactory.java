package io.cordys.crm.system.service;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.customer.service.CustomerLogService;

public class ModuleLogServiceFactory {

    public static BaseModuleLogService getModuleLogService(String type) {
        switch (type) {
            case "CUSTOMER":
                return CommonBeanFactory.getBean(CustomerLogService.class);
            default:
                return null;
        }
    }
}