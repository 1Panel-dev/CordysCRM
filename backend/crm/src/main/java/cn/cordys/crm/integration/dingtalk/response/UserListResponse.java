package cn.cordys.crm.integration.dingtalk.response;

import lombok.Data;

/**
 * 钉钉用户列表响应
 */
@Data
public class UserListResponse {
    private Integer errcode;
    private String errmsg;
    private UserListPage result;
}