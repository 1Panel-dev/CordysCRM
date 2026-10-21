package cn.cordys.crm.integration.dataease.dto.request;

import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class UserUpdateRequest extends UserCreateRequest {
    private String id;
}
