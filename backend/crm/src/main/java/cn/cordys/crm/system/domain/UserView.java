package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "sys_user_view")
public class UserView extends BaseModel {

	@Schema(description = "用户id")
	private String userId;

	@Schema(description = "视图名称")
	private String name;

	@Schema(description = "是否固定视图")
	private Boolean fixed;

	@Schema(description = "状态")
	private Boolean enable;

	@Schema(description = "资源类型(客户/线索/商机)")
	private String resourceType;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "排序")
	private Long pos;

	@Schema(description = "匹配模式(AND/OR)")
	private String searchMode;
}
