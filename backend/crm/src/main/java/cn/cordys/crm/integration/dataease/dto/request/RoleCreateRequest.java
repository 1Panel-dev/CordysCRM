package cn.cordys.crm.integration.dataease.dto.request;

import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class RoleCreateRequest {
    private String name;
    /**
     * 角色类型
     * 0: 普通用户
     * 1: 组织管理员
     */
    private Integer typeCode = 0;
    private String desc;
}
