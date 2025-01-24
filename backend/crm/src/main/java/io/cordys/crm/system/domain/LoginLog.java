package io.cordys.crm.system.domain;

import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 登入日志
 * 
 * @author jianxing
 * @date 2025-01-24 16:12:06
 */
@Data
@Table(name = "sys_login_log")
public class LoginLog  {

	@Schema(description = "主键")
	private String id;

	@Schema(description = "操作时间")
	private Long createTime;

	@Schema(description = "操作人")
	private String operator;

	@Schema(description = "登录地")
	private String loginAddress;

	@Schema(description = "平台")
	private String platform;
}
