package cn.cordys.crm.dashboard.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DashboardPageRequest extends BasePageRequest {

    @Schema(description = "文件ids")
    private List<String> dashboardModuleIds;
}
