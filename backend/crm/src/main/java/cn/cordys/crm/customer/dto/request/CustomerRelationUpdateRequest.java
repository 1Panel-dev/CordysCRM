package cn.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianxing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRelationUpdateRequest extends CustomerRelationSaveRequest {

    @NotBlank
    @Size(max = 32)
    @Schema(description = "ID")
    private String id;
}
