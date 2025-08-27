package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_parameter")
public class Parameter {

	@Schema(description = "key")
	private String paramKey;

	@Schema(description = "value")
	private String paramValue;

	@Schema(description = "type")
	private String type;
}
