package cn.cordys.crm.integration.lark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author guoyuqi
 * @date 2024-07-26
 */
@Data
public class LarkTenant {

    @JsonProperty("name")
    private String name;

    @JsonProperty("display_id")
    private String displayId;

    @JsonProperty("tenant_key")
    private String tenantKey;

    @JsonProperty("avatar")
    private LarkTenantAvatar avatar;

    @Data
    public static class LarkTenantAvatar {
        @JsonProperty("avatar_origin")
        private String avatarOrigin;
        @JsonProperty("avatar_72")
        private String avatar72;
        @JsonProperty("avatar_240")
        private String avatar240;
        @JsonProperty("avatar_640")
        private String avatar640;
    }
}