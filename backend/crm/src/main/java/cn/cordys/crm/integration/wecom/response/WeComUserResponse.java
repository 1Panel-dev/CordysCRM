package cn.cordys.crm.integration.wecom.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class WeComUserResponse extends WeComResponseEntity {

    /**
     * 成员UserID。对应管理端的帐号，企业内必须唯一。不区分大小写，长度为1~64个字节；第三方应用返回的值为open_userid
     */
    @JsonProperty("userid")
    private String userId;

    /**
     * 成员名称；第三方不可获取，调用时返回userid以代替name；代开发自建应用需要管理员授权才返回；对于非第三方创建的成员，第三方通讯录应用也不可获取；未返回name的情况需要通过通讯录展示组件来展示名字
     */
    private String name;

    /**
     * 性别。0表示未定义，1表示男性，2表示女性。代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段。注：不可获取指返回值0
     */
    private Integer gender;

    /**
     * 头像url。 代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段
     */
    private String avatar;

    /**
     * 员工个人二维码，扫描可添加为外部联系人(注意返回的是一个url，可在浏览器上打开该url以展示二维码)；代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段
     */
    @JsonProperty("qr_code")
    private String qrCode;

    /**
     * 手机号码，代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段
     */
    private String mobile;

    /**
     * 邮箱，代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段
     */
    private String email;

    /**
     * 企业邮箱，代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段
     */
    @JsonProperty("biz_mail")
    private String bizMail;

    /**
     * 地址。代开发自建应用需要管理员授权且成员oauth2授权获取；第三方仅通讯录应用可获取；对于非第三方创建的成员，第三方通讯录应用也不可获取；上游企业不可获取下游企业成员该字段
     */
    private String address;

}
