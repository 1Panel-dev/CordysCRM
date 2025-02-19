package io.cordys.crm.system.dto.request;

import io.cordys.common.request.ModuleFieldValueDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


/**
 * @author guoyuqi
 */
@Data
public class ProductEditRequest {

    @Schema(description = "id")
    @Size(max = 32)
    private String id;

    @Schema(description = "name")
    private String name;

    @Schema(description = "自定义字段")
    private List<ModuleFieldValueDTO> moduleFields;
}