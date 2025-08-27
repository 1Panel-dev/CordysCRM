package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "版本信息返回对象")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionInfoDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前系统正在运行的版本", example = "v1.0.0")
    private String currentVersion;

    @Schema(description = "最新可用版本（用于提示升级）", example = "v1.1.0")
    private String latestVersion;

    @Schema(description = "系统架构，例如 amd64, arm64", example = "amd64")
    private String architecture;

    @Schema(description = "版权描述", example = "© 2025 YourCompany. All rights reserved.")
    private String copyright;

    @Schema(description = "是否有最新版本")
    private Boolean hasNewVersion;
}
