package io.cordys.crm.customer.dto;

import io.cordys.crm.customer.domain.CustomerPool;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CustomerPoolDTO extends CustomerPool {

	@Schema(description = "成员范围")
	private List<String> scopeNames;
	@Schema(description = "管理员集合")
	private List<String> ownerNames;
}
