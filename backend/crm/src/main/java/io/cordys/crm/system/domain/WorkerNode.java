package io.cordys.crm.system.domain;

import io.cordys.common.groups.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public class WorkerNode implements Serializable {
    @Schema(description = "auto increment id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{worker_node.id.not_blank}", groups = {Updated.class})
    private Long id;

    @Schema(description = "host name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{worker_node.host_name.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 64, message = "{worker_node.host_name.length_range}", groups = {Created.class, Updated.class})
    private String hostName;

    @Schema(description = "port", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{worker_node.port.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 64, message = "{worker_node.port.length_range}", groups = {Created.class, Updated.class})
    private String port;

    @Schema(description = "node type: ACTUAL or CONTAINER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{worker_node.type.not_blank}", groups = {Created.class})
    private Integer type;

    @Schema(description = "launch date", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{worker_node.launch_date.not_blank}", groups = {Created.class})
    private Long launchDate;

    @Schema(description = "modified time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{worker_node.modified.not_blank}", groups = {Created.class})
    private Long modified;

    @Schema(description = "created time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{worker_node.created.not_blank}", groups = {Created.class})
    private Long created;

    @Serial
    private static final long serialVersionUID = 1L;
}