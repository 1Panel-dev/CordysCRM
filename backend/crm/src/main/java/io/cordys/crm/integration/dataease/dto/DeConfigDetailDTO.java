package io.cordys.crm.integration.dataease.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DeConfigDetailDTO {

	@Schema(description = "应用ID")
	private String agentId;
	@Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
	private String appSecret;
	@Schema(description = "DE账号")
	private String deAccount;
	@Schema(description = "回调地址")
	private String redirectUrl;
	@Schema(description = "是否验证通过")
	private Boolean verify;
	@Schema(description = "DE模块嵌入")
	private Boolean deModuleEmbedding;
	@Schema(description = "DE外链集成")
	private Boolean deLinkIntegration;
	@Schema(description = "DE自动同步")
	private Boolean deAutoSync;
	@Schema(description = "DEAccessKey")
	private String deAccessKey;
	@Schema(description = "DESecretKey")
	private String deSecretKey;
	@Schema(description = "DE组织id")
	private String deOrgID;
}
