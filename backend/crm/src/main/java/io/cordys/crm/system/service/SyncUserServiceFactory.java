package io.cordys.crm.system.service;

import io.cordys.wecom.service.WeComDepartMentService;

public class SyncUserServiceFactory {

    public static SyncUserService getSyncUserService(String type) {
        switch (type) {
            case "WECOM":
                return new WeComDepartMentService();
            default:
                return null;
        }
    }
}