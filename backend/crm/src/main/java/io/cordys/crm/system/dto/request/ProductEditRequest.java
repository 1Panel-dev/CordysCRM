package io.cordys.crm.system.dto.request;

import io.cordys.common.request.ModuleFieldValueDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class ProductEditRequest {

    @Schema(description = "id")
    @Size(max = 32)
    private String id;

    @Schema(description = "自定义字段")
    private List<ModuleFieldValueDTO> moduleFields;
}