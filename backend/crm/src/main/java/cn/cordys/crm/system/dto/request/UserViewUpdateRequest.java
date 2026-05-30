package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserViewUpdateRequest extends UserViewAddRequest {

    @Schema(description = "id")
    @NotBlank
    private String id;
}
