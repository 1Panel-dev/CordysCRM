package cn.cordys.crm.system.dto.form.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 联动场景
 * @author song-cc-rock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkScenario {

    @NotEmpty
    @Schema(description = "场景业务KEY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String key;
    @Schema(description = "联动字段集合")
    private List<LinkField> linkFields;
}
