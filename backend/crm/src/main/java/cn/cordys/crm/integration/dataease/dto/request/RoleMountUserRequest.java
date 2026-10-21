package cn.cordys.crm.integration.dataease.dto.request;

import lombok.Data;

import java.util.List;

/**
 * @author jianxing
 */
@Data
public class RoleMountUserRequest {
    private String roleId;
    private List<String> userIds;
}
