package cn.cordys.crm.integration.wecom.dto;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.Department;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class WeComDepartment {
    /**
     * 创建的部门id
     */
    private Long id;

    /**
     * 部门名称，代开发自建应用需要管理员授权才返回；此字段从2019年12月30日起，对新创建第三方应用不再返回，2020年6月30日起，对所有历史第三方应用不再返回name，返回的name字段使用id代替，后续第三方仅通讯录应用可获取，未返回名称的情况需要通过通讯录展示组件来展示部门名称
     */
    private String name;

    /**
     * 英文名称，此字段从2019年12月30日起，对新创建第三方应用不再返回，2020年6月30日起，对所有历史第三方应用不再返回该字段
     */
    @JsonProperty("name_en")
    private String nameEn;

    /**
     * 部门负责人的UserID；第三方仅通讯录应用可获取
     */
    @JsonProperty("department_leader")
    private List<String> departmentLeader;

    /**
     * 父部门id。根部门为1
     */
    @JsonProperty("parentid")
    private Long parentId;

    /**
     * 在父部门中的次序值。order值大的排序靠前。值范围是[0, 2^32)
     */
    private Integer order;


    /**
     * 子节点
     */
    private List<WeComDepartment> children = new ArrayList<>();
    private String crmId;
    private String crmParentId;


    public static List<WeComDepartment> buildDepartmentTree(String internalId, List<WeComDepartment> departments) {
        Map<Long, WeComDepartment> departmentMap = new HashMap<>();
        List<WeComDepartment> rootDepartments = new ArrayList<>();
        // 1. 生成新的ID并存储到部门对象中
        for (WeComDepartment department : departments) {
            departmentMap.put(department.getId(), department);
            department.setCrmId(department.getParentId() == 0 ? internalId : IDGenerator.nextStr());  // 生成新的 ID
            department.setCrmParentId(department.getParentId() == 0 ? internalId : department.getCrmParentId()); // 生成新的 ParentId
        }

        //1. 生成新的ID并存储到部门对象中
        for (WeComDepartment department : departments) {
            if (department.getParentId() == 0) {
                // 如果新的 parentId 是 0，表示根部门
                rootDepartments.add(department);
            } else {
                // 否则，将它添加到父部门的 children 中
                WeComDepartment parentDepartment = departmentMap.get(department.getParentId());
                if (parentDepartment != null) {
                    department.setCrmParentId(parentDepartment.getCrmId());
                    parentDepartment.getChildren().add(department);
                }
            }
        }

        return rootDepartments;
    }


    public static List<WeComDepartment> buildDepartmentTreeMultiple(String internalId, List<Department> currentDepartmentList, List<WeComDepartment> departments) {
        Map<Long, WeComDepartment> departmentMap = new HashMap<>();
        List<WeComDepartment> rootDepartments = new ArrayList<>();
        // 1. 生成新的ID并存储到部门对象中
        for (WeComDepartment department : departments) {
            departmentMap.put(department.getId(), department);

            currentDepartmentList.stream()
                    .filter(dept -> Strings.CI.equalsAny(dept.getResourceId(), department.getId().toString()))
                    .findFirst()
                    .ifPresentOrElse(dept -> {
                        department.setCrmId(dept.getId());
                        department.setCrmParentId(dept.getParentId());
                    }, () -> {
                        if (department.getParentId() == 0) {
                            department.setCrmId(internalId);
                            department.setCrmParentId(internalId);
                        } else {
                            currentDepartmentList.stream()
                                    .filter(dept -> Strings.CI.equalsAny(dept.getResourceId(), department.getParentId().toString()))
                                    .findFirst()
                                    .ifPresentOrElse(parent -> {
                                        department.setCrmId(IDGenerator.nextStr());
                                        department.setCrmParentId(parent.getId());
                                    }, () -> {
                                        department.setCrmId(IDGenerator.nextStr());
                                        department.setCrmParentId(department.getCrmParentId());
                                    });
                        }
                    });
        }

        // 2. 按照 parentId 构建树形结构
        for (WeComDepartment department : departments) {
            if (department.getParentId() == 0) {
                // 如果新的 parentId 是 0，表示根部门
                rootDepartments.add(department);
            } else {
                // 否则，将它添加到父部门的 children 中
                WeComDepartment parentDepartment = departmentMap.get(department.getParentId());
                if (parentDepartment != null) {
                    if (!parentDepartment.getChildren().contains(department)) {
                        department.setCrmParentId(parentDepartment.getCrmId());
                        parentDepartment.getChildren().add(department);
                    }
                }
            }
        }

        return rootDepartments;
    }
}
