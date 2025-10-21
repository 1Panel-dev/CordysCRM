package cn.cordys.crm.integration.lark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class LarkDepartmentData {

    @JsonProperty("items")
    private List<LarkDepartment> items;

    @JsonProperty("has_more")
    private boolean hasMore;

    @JsonProperty("page_token")
    private String pageToken;
}
