package cn.cordys.crm.opportunity.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 商机报价单列表响应类;
 *
 * @author guoyuqi
 */
@Data
public class OpportunityQuotationListResponse {

  @Schema(description = "ID")
  private String id;

  @Schema(description = "名称")
  private String name;

  @Schema(description = "商机id")
  private String opportunityId;

  @Schema(description = "商机名称")
  private String opportunityName;

  @Schema(description = "部门id")
  private String departmentId;

  @Schema(description = "部门名称")
  private String departmentName;

  @Schema(description = "累计金额")
  private BigDecimal amount;

  @Schema(description = "审核状态")
  private String approvalStatus;

  @Schema(description = "有效期至")
  private Long untilTime;

  @Schema(description = "创建人")
  private String createUser;

  @Schema(description = "修改人")
  private String updateUser;

  @Schema(description = "创建时间")
  private Long createTime;

  @Schema(description = "更新时间")
  private Long updateTime;

  @Schema(description = "创建人名称")
  private String createUserName;

  @Schema(description = "更新人名称")
  private String updateUserName;

  @Schema(description = "自定义字段")
  private List<BaseModuleFieldValue> moduleFields;
}
