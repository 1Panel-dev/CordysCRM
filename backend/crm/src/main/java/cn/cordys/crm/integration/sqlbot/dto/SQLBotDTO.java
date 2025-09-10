package cn.cordys.crm.integration.sqlbot.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class SQLBotDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private List<DataSourceDTO> data;
}
