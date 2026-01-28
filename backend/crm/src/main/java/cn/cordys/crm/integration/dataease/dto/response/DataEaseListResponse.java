package cn.cordys.crm.integration.dataease.dto.response;

import java.util.List;
import lombok.Data;

/**
 * @Author: jianxing @CreateTime: 2025-08-15 15:48
 */
@Data
public class DataEaseListResponse<T> extends DataEaseBaseResponse {
  private List<T> data;
}
