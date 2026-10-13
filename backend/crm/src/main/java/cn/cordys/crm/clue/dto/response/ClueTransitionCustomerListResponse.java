package cn.cordys.crm.clue.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ClueTransitionCustomerListResponse {

    @Schema(description = "转换关联的客户列表，当前最多一条")
    private List<CustomerListResponse> list = List.of();

    @Schema(description = "客户字段选项集合，无客户数据权限时为空")
    private Map<String, List<OptionDTO>> optionMap = Map.of();

    @Schema(description = "是否有客户数据权限，为 false 时仅展示基础字段且不可打开客户详情")
    private boolean hasPermission;
}
