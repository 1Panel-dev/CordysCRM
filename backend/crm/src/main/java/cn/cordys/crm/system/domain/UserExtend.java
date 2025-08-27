package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
@Schema(description = "用户扩展信息")
@Table(name = "sys_user_extend")
public class UserExtend implements Serializable {
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "其他平台对接信息")
    private byte[] platformInfo;

    @Serial
    private static final long serialVersionUID = 1L;
}