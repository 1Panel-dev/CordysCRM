package cn.cordys.crm.integration.dingtalk.response;

import lombok.Data;

@Data
public class DingTalkUserResponse {

    /**
     * 用户的钉钉昵称。
     */
    private String nick;

    /**
     * 头像URL。。
     */
    private String avatarUrl;

    /**
     * 用户的手机号
     */
    private String mobile;


    /**
     * 用户的openId
     */
    private String openId;


    /**
     * 用户的unionId
     */
    private String unionId;

    /**
     * 用户的个人邮箱
     */
    private String email;


    /**
     * 手机号对应的国家号
     */
    private String stateCode;


}
