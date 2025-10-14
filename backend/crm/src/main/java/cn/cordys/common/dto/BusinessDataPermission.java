package cn.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BusinessDataPermission extends DeptDataPermissionDTO{

	@Schema(description = "数据来源表")
	private String sourceTable;
}
