package cn.cordys.common.dto;

import cn.cordys.common.constants.InternalUserView;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 部门的数据权限
 *
 * @author jianxing
 */
@Data
public class DeptDataPermissionDTO {

    /**
     * 搜索类型(ALL/SELF/DEPARTMENT/VISIBLE)
     * {@link InternalUserView}
     */
    private String viewId;
    /**
     * 是否可查看全部数据
     */
    private Boolean all = false;
    /**
     * 是否可查看自己的数据
     */
    private Boolean self = false;
    /**
     * 是否被设置为可见
     */
    private Boolean visible = false;
    /**
     * 可查看的部门Id
     */
    private Set<String> deptIds = new HashSet<>();
}
