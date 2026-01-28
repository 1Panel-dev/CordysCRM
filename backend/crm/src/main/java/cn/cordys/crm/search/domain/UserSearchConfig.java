package cn.cordys.crm.search.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/** 用户全局搜索范围 */
@Data
@Table(name = "sys_user_search_config")
public class UserSearchConfig extends BaseModel {

  @Schema(description = "搜索字段id")
  private String fieldId;

  @Schema(description = "类型")
  private String type;

  @Schema(description = "业务字段key")
  private String businessKey;

  @Schema(description = "数据源对应类型")
  private String dataSourceType;

  @Schema(description = "用户id")
  private String userId;

  @Schema(description = "搜索模块(customer/lead/等)")
  private String moduleType;

  @Schema(description = "模块顺序设置['customer','clue']")
  private String sortSetting;

  @Schema(description = "是否展示有搜索结果的列表")
  private Boolean resultDisplay;

  @Schema(description = "组织id")
  private String organizationId;
}
