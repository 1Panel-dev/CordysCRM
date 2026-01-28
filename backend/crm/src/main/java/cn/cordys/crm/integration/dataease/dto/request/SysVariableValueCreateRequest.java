package cn.cordys.crm.integration.dataease.dto.request;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author: jianxing @CreateTime: 2025-08-15 16:30
 */
@Data
public class SysVariableValueCreateRequest {
  private String sysVariableId;
  private String value;
  private String end = StringUtils.EMPTY;
  private String begin = StringUtils.EMPTY;
}
