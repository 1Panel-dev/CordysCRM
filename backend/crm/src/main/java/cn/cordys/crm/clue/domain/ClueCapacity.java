package cn.cordys.crm.clue.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "clue_capacity")
public class ClueCapacity extends BaseModel {

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "范围ID")
    private String scopeId;

    @Schema(description = "库容")
    private Integer capacity;
}
