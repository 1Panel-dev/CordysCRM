package io.cordys.integration.wecom.response;

import io.cordys.integration.wecom.dto.WeComDepartment;
import lombok.Data;

import java.util.List;

/**
 * 企业微信部门 响应对象
 */

@Data
public class DepartmentListResponse extends WeComResponseEntity {
    /**
     * 部门列表数据。
     */
    private List<WeComDepartment> department;

}