package cn.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author jianxing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDataScopeDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description =  "选项ID")
    private String id;
    @Schema(description =  "选项名称")
    private String name;
    @Schema(description =  "数据权限")
    private String dataScope;
    @Schema(description =  "组织ID")
    private String organizationId;
}
