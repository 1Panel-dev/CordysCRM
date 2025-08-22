package io.cordys.crm.integration.dataease.dto;

import io.cordys.common.dto.OptionDTO;
import lombok.Data;

import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-15  15:54
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
