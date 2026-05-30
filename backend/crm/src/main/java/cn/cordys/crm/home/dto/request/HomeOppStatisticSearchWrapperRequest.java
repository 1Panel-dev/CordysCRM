package cn.cordys.crm.home.dto.request;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
@NoArgsConstructor
public class HomeOppStatisticSearchWrapperRequest extends HomeStatisticSearchWrapperRequest {

    private String stageScenario;

    public HomeOppStatisticSearchWrapperRequest(HomeStatisticSearchRequest staticRequest,
                                                DeptDataPermissionDTO dataPermission,
                                                String orgId, String userId) {
        super(staticRequest, dataPermission, orgId, userId);
    }
}
