package io.cordys.security;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.util.Set;

/**
 * <p>用户数据传输对象，用于传输用户信息，包含其他平台对接信息和头像。</p>
 */
@Data
public class UserDTO implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user.id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 50, message = "{user.id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user.name.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{user.name.length_range}", groups = {Created.class, Updated.class})
    private String name;

    @Schema(description = "用户邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user.email.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 64, message = "{user.email.length_range}", groups = {Created.class, Updated.class})
    private String email;

    @Schema(description = "用户密码")
    private String password;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "当前组织ID")
    private String lastOrganizationId;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "来源：LOCAL OIDC CAS OAUTH2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user.source.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{user.source.length_range}", groups = {Created.class, Updated.class})
    private String source;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "修改人")
    private String updateUser;

    @Schema(description = "是否删除", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{user.deleted.not_blank}", groups = {Created.class})
    private Boolean deleted;

    @Schema(description = "CFT Token")
    private String cftToken;

    @Schema(description = "其他平台对接信息", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private byte[] platformInfo;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "权限ID")
    private Set<String> permissionIds;
}
