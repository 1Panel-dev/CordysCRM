package cn.cordys.crm.customer.dto.request;

import cn.cordys.crm.customer.dto.CustomerPoolPickRuleDTO;
import cn.cordys.crm.customer.dto.CustomerPoolRecycleRuleDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CustomerPoolAddRequest {

	@NotBlank
	@Size(max = 255)
	@Schema(description = "公海池名称")
	private String name;

	@NotNull
	@Schema(description = "范围ID集合")
	private List<String> scopeIds;

	@NotNull
	@Schema(description = "管理员ID集合")
	private List<String> ownerIds;

	@NotNull
	@Schema(description = "启用/禁用")
	private Boolean enable;

	@NotNull
	@Schema(description = "是否自动回收")
	private Boolean auto;

	@Schema(description = "领取规则")
	private CustomerPoolPickRuleDTO pickRule;

	@Schema(description = "回收规则")
	private CustomerPoolRecycleRuleDTO recycleRule;

	@Schema(description = "隐藏字段ID集合")
	private Set<@NotBlank String> hiddenFieldIds;
}
