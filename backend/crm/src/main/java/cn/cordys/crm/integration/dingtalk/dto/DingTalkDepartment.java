package cn.cordys.crm.integration.dingtalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DingTalkDepartment {
    /**
     * 部门id
     */
    @JsonProperty("dept_id")
    private Long deptId;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门id。根部门为1
     */
    @JsonProperty("parent_id")
    private Long parentId;

    /**
     * 在父部门中的次序值。order值大的排序靠前。值范围是[0, 2^32)
     */
    private Long order;

    /**
     * 部门群ID
     */
    @JsonProperty("dept_group_chat_id")
    private String deptGroupChatId;

    /**
     * 是否隐藏本部门
     */
    @JsonProperty("hide_dept")
    private Boolean hideDept;

    /**
     * 部门的主管列表
     */
    @JsonProperty("dept_manager_userid_list")
    private List<String> deptManagerUseridList;

    /**
     * 部门的负责人
     */
    @JsonProperty("org_dept_owner")
    private String orgDeptOwner;

    /**
     * 当群已经创建后，是否有新人加入部门会自动加入该群
     */
    @JsonProperty("auto_add_user")
    private Boolean autoAddUser;

    /**
     * 是否开启部门群
     */
    @JsonProperty("create_dept_group")
    private Boolean createDeptGroup;

    /**
     * 是否是外部部门
     */
    @JsonProperty("outer_dept")
    private Boolean outerDept;

    /**
     * 部门是否包含子部门
     */
    @JsonProperty("group_contain_sub_dept")
    private Boolean groupContainSubDept;
}