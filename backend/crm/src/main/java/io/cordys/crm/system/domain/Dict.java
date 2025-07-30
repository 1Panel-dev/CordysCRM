package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.persistence.Table;

/**
 * 客户字典表;
 */
@Data
@Table(name = "sys_dict")
public class Dict extends BaseModel {
	@Schema(description = "字典值")
	private String name;

	@Schema(description = "字典模块")
	private String module;

	@Schema(description = "字典值类型")
	private String type;

	@Schema(description = "自定义排序")
	private Long pos;

	@Schema(description = "组织ID")
	private String organizationId;
}