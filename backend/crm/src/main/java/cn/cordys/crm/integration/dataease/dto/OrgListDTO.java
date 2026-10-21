package cn.cordys.crm.integration.dataease.dto;

import cn.cordys.common.dto.OptionDTO;
import lombok.Data;

import java.util.List;

/**
 * @author jianxing
 */
@Data
public class OrgListDTO {
    private List<OptionDTO> nodes;
}
