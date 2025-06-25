package io.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class ExportTaskCenterQueryRequest {

	@Schema(description = "下载名称")
	private String keyword;
	@Schema(description = "导出类型")
	private String exportType;
	@Schema(description = "导出状态")
	private String exportStatus;
}
