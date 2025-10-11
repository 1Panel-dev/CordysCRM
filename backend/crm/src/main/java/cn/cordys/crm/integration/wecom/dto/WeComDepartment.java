package cn.cordys.crm.integration.wecom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WeComDepartment {
    /**
     * 创建的部门id
     */
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门id。根部门为1
     */
    @JsonProperty("parentid")
    private Long parentId;

    /**
     * 在父部门中的次序值。order值大的排序靠前。值范围是[0, 2^32)
     */
    private Long order;

}