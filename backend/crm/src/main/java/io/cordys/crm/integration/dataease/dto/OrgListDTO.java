package io.cordys.crm.integration.dataease.dto;

import io.cordys.common.dto.OptionDTO;
import lombok.Data;

import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-22  18:55
 */
@Data
public class OrgListDTO {
    private List<OptionDTO> nodes;
}
