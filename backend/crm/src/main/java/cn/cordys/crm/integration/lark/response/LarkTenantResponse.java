package cn.cordys.crm.integration.lark.response;

import cn.cordys.crm.integration.lark.dto.LarkTenant;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author guoyuqi
 * @date 2024-07-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LarkTenantResponse extends LarkResponseEntity {

    @JsonProperty("data")
    private LarkTenantData data;

    @Data
    public static class LarkTenantData {
        @JsonProperty("tenant")
        private LarkTenant tenant;
    }
}