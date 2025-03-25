package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author song-cc-rock
 */
@Data
@AllArgsConstructor
public class UploadTransferRequest {

	@Schema(description = "组织ID")
	private String organizationId;

	@Schema(description = "转存文件来源ID")
	private String resourceId;

	@Schema(description = "操作人")
	private String operatorUserId;

	@Schema(description = "转存文件临时ID")
	private List<String> tempFileIds;
}
