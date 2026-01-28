package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerAddRequest {

  @NotBlank
  @Size(max = 255)
  @Schema(description = "客户名称")
  private String name;

  @Size(max = 32)
  @Schema(description = "负责人")
  private String owner;

  @Schema(description = "模块字段值")
  private List<BaseModuleFieldValue> moduleFields;

  @Schema(description = "最新跟进人(转客户时需录入)")
  private String follower;

  @Schema(description = "最新跟进时间(转客户时需录入)")
  private Long followTime;
}
