package cn.cordys.crm.contract.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Table(name = "business_title_config")
public class BusinessTitleConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "字段")
    private String field;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "组织id")
    private String organizationId;

}
