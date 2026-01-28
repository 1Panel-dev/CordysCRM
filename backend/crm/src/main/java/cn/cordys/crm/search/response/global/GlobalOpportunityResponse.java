package cn.cordys.crm.search.response.global;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class GlobalOpportunityResponse {

  @Schema(description = "ID")
  private String id;

  @Schema(description = "商机名称")
  private String name;

  @Schema(description = "客户id")
  private String customerId;

  @Schema(description = "客户名称")
  private String customerName;

  @Schema(description = "关联的客户是否在公海")
  private Boolean inCustomerPool;

  @Schema(description = "客户公海id")
  private String poolId;

  @Schema(description = "联系人Id")
  private String contactId;

  @Schema(description = "联系人名称")
  private String contactName;

  @Schema(description = "负责人")
  private String owner;

  @Schema(description = "负责人名称")
  private String ownerName;

  @Schema(description = "意向产品")
  private List<String> products;

  @Schema(description = "商机阶段")
  private String stage;

  @Schema(description = "商机阶段")
  private String stageName;

  @Schema(description = "负责人部门名称")
  private String departmentName;

  @Schema(description = "是否有当前数据的权限")
  private boolean hasPermission;

  @Schema(description = "创建时间")
  private Long createTime;

  @Schema(description = "自定义字段集合")
  private List<BaseModuleFieldValue> moduleFields;

  @Schema(description = "最新跟进日期")
  private Long followTime;
}
