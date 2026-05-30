package cn.cordys.crm.integration.dingtalk.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SubDeptIdListResult {
    @JsonProperty("dept_id_list")
    private List<Long> deptIdList;
}
