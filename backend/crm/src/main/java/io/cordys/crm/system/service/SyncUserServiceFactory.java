package io.cordys.crm.system.service;

import io.cordys.integration.wecom.service.WeComDepartmentService;

public class SyncUserServiceFactory {

    public static SyncUserService getSyncUserService(String type) {
        switch (type) {
            case "WECOM":
                return new WeComDepartmentService();
            default:
                return null;
        }
    }
}