package cn.cordys.crm.search.response.global;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class GlobalCluePoolResponse {

  @Schema(description = "ID")
  private String id;

  @Schema(description = "线索名称")
  private String name;

  @Schema(description = "线索池id")
  private String poolId;

  @Schema(description = "线索池名称")
  private String poolName;

  @Schema(description = "电话")
  private String phone;

  @Schema(description = "意向产品")
  private List<String> products;

  @Schema(description = "是否有当前数据的权限")
  private boolean hasPermission;

  @Schema(description = "自定义字段集合")
  private List<BaseModuleFieldValue> moduleFields;

  @Schema(description = "最新跟进日期")
  private Long followTime;
}
