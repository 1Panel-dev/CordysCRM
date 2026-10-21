package cn.cordys.crm.integration.lark.response;

import cn.cordys.crm.integration.lark.dto.LarkTenant;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author guoyuqi
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
