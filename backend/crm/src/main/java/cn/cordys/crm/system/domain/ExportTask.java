package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "export_task")
public class ExportTask extends BaseModel {

	@Schema(description = "文件名")
	private String fileName;

	@Schema(description = "资源类型")
	private String resourceType;

	@Schema(description = "文件id")
	private String fileId;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "组织id")
	private String organizationId;
}
