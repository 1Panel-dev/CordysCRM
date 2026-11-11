package cn.cordys.crm.form.service;

import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.service.DepartmentService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 自定义表单内置系统视图（全部/部门/我的）的服务端条件注入。
 * 自定义表单数据没有权限位式的数据权限，内置视图的范围由后端按当前用户身份决定。
 */
@Service
public class CustomFormViewScopeService {

    private static final String OWNER_FIELD = "owner";
    private static final String DEPARTMENT_ID_FIELD = "departmentId";

    @Resource
    private DepartmentService departmentService;
    @Resource
    private DataScopeService dataScopeService;

    /**
     * 解析内置视图对应的查询条件
     *
     * @param viewId 视图ID
     * @param userId 用户ID
     * @param orgId  组织ID
     * @return 内置视图条件, 非内置视图或全部视图返回 null
     */
    public CombineSearch resolveInternalView(String viewId, String userId, String orgId) {
        if (InternalUserView.isSelf(viewId)) {
            return buildCombineSearch(OWNER_FIELD, List.of(userId));
        }
        if (InternalUserView.isDepartment(viewId)) {
            List<String> deptIds = getCurrentUserDeptIds(userId, orgId);
            if (deptIds.isEmpty()) {
                // 空条件会被 FilterCondition.valid() 过滤成"不过滤", 这里回落到本人数据, 避免越权看到全部数据
                return buildCombineSearch(OWNER_FIELD, List.of(userId));
            }
            return buildCombineSearch(DEPARTMENT_ID_FIELD, deptIds);
        }
        // 全部视图不附加条件, 本人数据范围仍由 manageOwn 收敛
        return null;
    }

    private List<String> getCurrentUserDeptIds(String userId, String orgId) {
        OrganizationUser organizationUser = dataScopeService.getOrganizationUser(userId, orgId);
        if (organizationUser == null || StringUtils.isBlank(organizationUser.getDepartmentId())) {
            return List.of();
        }
        List<BaseTreeNode> tree = departmentService.getTree(orgId);
        return dataScopeService.getDeptIdsWithChild(tree, Set.of(organizationUser.getDepartmentId()));
    }

    private CombineSearch buildCombineSearch(String name, List<String> values) {
        FilterCondition condition = new FilterCondition();
        condition.setName(name);
        condition.setType(FieldType.MEMBER.name());
        condition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
        condition.setMultipleValue(true);
        condition.setValue(values);

        CombineSearch combineSearch = new CombineSearch();
        combineSearch.setSearchMode(CombineSearch.SearchMode.AND.name());
        combineSearch.setConditions(List.of(condition));
        return combineSearch;
    }
}
