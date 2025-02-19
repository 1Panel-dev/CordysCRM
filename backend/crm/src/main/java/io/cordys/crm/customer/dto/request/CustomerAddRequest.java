package io.cordys.crm.customer.dto.request;

import io.cordys.common.request.ModuleFieldValueDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerAddRequest {
    @Schema(description = "自定义字段")
    private List<ModuleFieldValueDTO> moduleFields;
}