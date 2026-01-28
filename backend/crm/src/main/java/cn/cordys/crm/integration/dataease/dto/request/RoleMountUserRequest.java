package cn.cordys.crm.integration.dataease.dto.request;

import java.util.List;
import lombok.Data;

/**
 * @Author: jianxing @CreateTime: 2025-08-15 16:30
 */
@Data
public class RoleMountUserRequest {
  private String roleId;
  private List<String> userIds;
}
