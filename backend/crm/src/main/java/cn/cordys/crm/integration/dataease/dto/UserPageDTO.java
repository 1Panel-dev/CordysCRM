package cn.cordys.crm.integration.dataease.dto;

import cn.cordys.common.dto.OptionDTO;
import lombok.Data;

import java.util.List;

/**
 * @author jianxing
 */
@Data
public class UserPageDTO {

    private String id;

    private String account;

    private String name;

    private String email;

    private Boolean enable = true;

    private List<OptionDTO> roleItems;

    private String sysVariable;
}
