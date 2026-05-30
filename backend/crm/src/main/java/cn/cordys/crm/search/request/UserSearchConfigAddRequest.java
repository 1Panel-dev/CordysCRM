package cn.cordys.crm.search.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserSearchConfigAddRequest {

    @Schema(description = "搜索字段集合")
    private Map<String, List<String>> searchFields;

    @Schema(description = "是否展示有搜索结果的列表")
    private Boolean resultDisplay = false;

    @Schema(description = "排序列表")
    private List<String> sortSetting;

}
