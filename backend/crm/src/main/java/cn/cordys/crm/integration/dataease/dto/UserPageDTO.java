package cn.cordys.crm.integration.dataease.dto;

import cn.cordys.common.dto.OptionDTO;
import java.util.List;
import lombok.Data;

/**
 * @Author: jianxing @CreateTime: 2025-08-15 15:54
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
