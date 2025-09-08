package cn.cordys.crm.system.domain;

import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Table(name = "user_key")
public class UserKey implements Serializable {
    @Schema(description = "user_key ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user_key.id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 32, message = "{user_key.id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @Schema(description = "用户ID")
    private String createUser;

    @Schema(description = "access_key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user_key.access_key.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{user_key.access_key.length_range}", groups = {Created.class, Updated.class})
    private String accessKey;

    @Schema(description = "secret key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{user_key.secret_key.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{user_key.secret_key.length_range}", groups = {Created.class, Updated.class})
    private String secretKey;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "状态")
    private Boolean enable;

    @Schema(description = "是否永久有效", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{user_key.forever.not_blank}", groups = {Created.class})
    private Boolean forever;

    @Schema(description = "到期时间")
    private Long expireTime;

    private String description;

    @Serial
    private static final long serialVersionUID = 1L;
}