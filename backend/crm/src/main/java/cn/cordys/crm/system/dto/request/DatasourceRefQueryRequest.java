package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 数据源引用请求对象
 * @author song-cc-rock
 */
@Data
public class DatasourceRefQueryRequest {

	@NotNull
	@Schema(description = "数据源ID集合")
	private List<String> sourceIds;
	@NotEmpty
	@Schema(description = "数据源类型")
	private String dataSourceType;
}
