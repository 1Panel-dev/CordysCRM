package io.cordys.crm.system.domain;

import jakarta.persistence.Table;
import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 角色
 *
 * @author jianxing
 * @date 2025-01-02 15:30:44
 */
@Data
@Table(name = "sys_role")
public class Role extends BaseModel {

	@Schema(description = "id")
	private String id;

	@Schema(description = "角色名称")
	private String name;

	@Schema(description = "是否是内置角色")
	private Boolean internal;

	@Schema(description = "数据范围（全部数据权限/指定部门权限/本部门数据权限/本部门及以下数据权限/仅本人数据）")
	private String dataScope;

	@Schema(description = "创建时间")
	private Long createTime;

	@Schema(description = "更新时间")
	private Long updateTime;

	@Schema(description = "创建人")
	private String createUser;

	@Schema(description = "更新人")
	private String updateUser;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "组织id")
	private String organizationId;
}
