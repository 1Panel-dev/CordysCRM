package cn.cordys.crm.integration.qcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Paging {

    @JsonProperty("PageSize")
    private int pageSize;

    @JsonProperty("PageIndex")
    private int pageIndex;

    @JsonProperty("TotalRecords")
    private int totalRecords;
}
