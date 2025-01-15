package io.cordys.crm.system.dto.request;

import io.cordys.common.pager.condition.BasePageRequest;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-01-13 17:33:23
 */
@Data
public class RoleUserPageRequest extends BasePageRequest {
    private String roleId;
}
