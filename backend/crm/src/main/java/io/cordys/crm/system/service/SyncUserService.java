package io.cordys.crm.system.service;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Objects;

public interface SyncUserService {
    void syncUser(String operatorId, String orgId);


    /**
     * url转换
     */
    default String urlTransfer(String urlPattern, Object... params) {
        Object[] vars = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (Objects.isNull(params[i])) {
                vars[i] = "";
                continue;
            }
            String var = StringUtils.stripToEmpty(params[i].toString());
            try {
                vars[i] = URLEncoder.encode(var, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                vars[i] = var;
            }
        }
        return MessageFormat.format(urlPattern, vars);
    }

}