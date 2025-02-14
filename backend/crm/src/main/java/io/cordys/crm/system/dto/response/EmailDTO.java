package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailDTO {
    //SMTP 主机
    @Schema(description = "SMTP 主机")
    private String host;
    //SMTP 端口
    @Schema(description = "SMTP 端口")
    private String port;
    //SMTP账号
    @Schema(description = "SMTP账号")
    private String account;
    //SMTP密码
    @Schema(description = "SMTP密码")
    private String password;
    //指定发件人
    @Schema(description = "指定发件人")
    private String from;
    //指定收件人
    @Schema(description = "指定收件人")
    private String recipient;
    //ssl开关
    @Schema(description = "ssl开关")
    private String ssl;
    //tsl开关
    @Schema(description = "tsl开关")
    private String tsl;

}
