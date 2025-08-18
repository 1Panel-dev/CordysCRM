package io.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Schema(description = "版本信息返回对象")
@Builder
public class VersionInfoDTO {

    @Schema(description = "当前系统正在运行的版本", example = "v1.0.0")
    private String currentVersion;

    @Schema(description = "最新可用版本（用于提示升级）", example = "v1.1.0")
    private String latestVersion;

    @Schema(description = "系统架构，例如 amd64, arm64", example = "amd64")
    private String architecture;

    @Schema(description = "版本发布时间（当前版本）", example = "2025-08-01T12:00:00")
    private String releaseDate;

    @Schema(description = "版权描述", example = "© 2025 YourCompany. All rights reserved.")
    private String copyright;

    @Schema(description = "是否有最新版本")
    private Boolean hasNewVersion;
}
