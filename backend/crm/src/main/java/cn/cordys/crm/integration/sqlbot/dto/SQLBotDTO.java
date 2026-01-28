package cn.cordys.crm.integration.sqlbot.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SQLBotDTO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private int code;
  private String message;
  private List<DataSourceDTO> data;
}
