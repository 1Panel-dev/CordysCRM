package cn.cordys.crm.integration.sync.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ThirdUser {

    /**
     * 成员UserID。对应管理端的帐号，企业内必须唯一。
     */
    private String userId;

    /**
     * 成员名称；
     */
    private String name;

    /**
     * 手机号码。，代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段
     */
    private String mobile;

    /**
     * 职务信息；
     */
    private String position;

    /**
     * 性别。0表示未定义，1表示男性，2表示女性。
     */
    private Integer gender;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 表示在所在的部门内是否为部门负责人，数量与department一致；第三方通讯录应用或者授权了“组织架构信息-应用可获取企业的部门组织架构信息-部门负责人”权限的第三方应用可获取；对于非第三方创建的成员，第三方通讯录应用不可获取；上游企业不可获取下游企业成员该字段
     */
    private Boolean isLeaderInDept;

    /**
     * 头像url。 代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段
     */
    private String avatar;

}