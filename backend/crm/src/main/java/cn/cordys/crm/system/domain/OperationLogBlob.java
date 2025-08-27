package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
@Schema(description = "操作日志Blob")
@Table(name = "sys_operation_log_blob")
public class OperationLogBlob implements Serializable {
    @Schema(description = "主键,与operation_log表id一致", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "变更前内容")
    private byte[] originalValue;

    @Schema(description = "变更后内容")
    private byte[] modifiedValue;

    @Serial
    private static final long serialVersionUID = 1L;
}