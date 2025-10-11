package cn.cordys.crm.integration.sync.dto;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.Department;
import lombok.Data;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ThirdDepartment {
    /**
     * 创建的部门id
     */
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门id
     */
    private Long parentId;

    /**
     * 是否是根部门
     */
    private Boolean isRoot;

    /**
     * 在父部门中的次序值。order值大的排序靠前。值范围是[0, 2^32)
     */
    private Long order;


    /**
     * 子节点
     */
    private List<ThirdDepartment> children = new ArrayList<>();
    private String crmId;
    private String crmParentId;


    public static List<ThirdDepartment> buildDepartmentTree(String internalId, List<ThirdDepartment> departments) {
        Map<Long, ThirdDepartment> departmentMap = new HashMap<>();
        List<ThirdDepartment> rootDepartments = new ArrayList<>();
        // 1. 生成新的ID并存储到部门对象中
        for (ThirdDepartment department : departments) {
            departmentMap.put(department.getId(), department);
            department.setCrmId(department.getIsRoot() ? internalId : IDGenerator.nextStr());  // 生成新的 ID
            department.setCrmParentId(department.getIsRoot() ? internalId : department.getCrmParentId()); // 生成新的 ParentId
        }

        //1. 生成新的ID并存储到部门对象中
        for (ThirdDepartment department : departments) {
            if (department.getIsRoot()) {
                // 如果新的 parentId 是 0，表示根部门
                rootDepartments.add(department);
            } else {
                // 否则，将它添加到父部门的 children 中
                ThirdDepartment parentDepartment = departmentMap.get(department.getParentId());
                if (parentDepartment != null) {
                    department.setCrmParentId(parentDepartment.getCrmId());
                    parentDepartment.getChildren().add(department);
                }
            }
        }

        return rootDepartments;
    }


    public static List<ThirdDepartment> buildDepartmentTreeMultiple(String internalId, List<Department> currentDepartmentList, List<ThirdDepartment> departments) {
        Map<Long, ThirdDepartment> departmentMap = new HashMap<>();
        List<ThirdDepartment> rootDepartments = new ArrayList<>();
        // 1. 生成新的ID并存储到部门对象中
        for (ThirdDepartment department : departments) {
            departmentMap.put(department.getId(), department);

            currentDepartmentList.stream()
                    .filter(dept -> Strings.CI.equalsAny(dept.getResourceId(), department.getId().toString()))
                    .findFirst()
                    .ifPresentOrElse(dept -> {
                        department.setCrmId(dept.getId());
                        department.setCrmParentId(dept.getParentId());
                    }, () -> {
                        if (department.getIsRoot()) {
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
        for (ThirdDepartment department : departments) {
            if (department.getIsRoot()) {
                // 如果新的 parentId 是 0，表示根部门
                rootDepartments.add(department);
            } else {
                // 否则，将它添加到父部门的 children 中
                ThirdDepartment parentDepartment = departmentMap.get(department.getParentId());
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