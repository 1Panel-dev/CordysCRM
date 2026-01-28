package cn.cordys.crm.search.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/** 全局脱敏搜索配置 */
@Data
@Table(name = "sys_search_field_mask_config")
public class SearchFieldMaskConfig extends BaseModel {
  @Schema(description = "搜索字段id")
  private String fieldId;

  @Schema(description = "类型")
  private String type;

  @Schema(description = "业务字段key")
  private String businessKey;

  @Schema(description = "数据源对应类型")
  private String dataSourceType;

  @Schema(description = "搜索模块(customer/lead/等)")
  private String moduleType;

  @Schema(description = "组织id")
  private String organizationId;
}
