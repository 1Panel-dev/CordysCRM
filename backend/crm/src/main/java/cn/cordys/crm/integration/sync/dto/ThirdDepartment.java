package cn.cordys.crm.integration.sync.dto;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.Department;
import lombok.Data;
import org.apache.commons.lang3.Strings;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class ThirdDepartment {
    /**
     * 创建的部门id
     */
    private String id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门id
     */
    private String parentId;

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

    public static List<ThirdDepartment> buildDepartmentTree(Department internalDepartment, List<ThirdDepartment> departments, String type) {

        Map<String, ThirdDepartment> departmentMap = new HashMap<>();
        List<ThirdDepartment> rootDepartments = new ArrayList<>();

        // 是否存在第三方根部门
        boolean hasRootDepartment = departments.stream()
                .anyMatch(department -> Boolean.TRUE.equals(department.getIsRoot()));

        // 生成 CRM ID，同时建立 ID 映射
        for (ThirdDepartment department : departments) {
            departmentMap.put(department.getId(), department);

            if (Boolean.TRUE.equals(department.getIsRoot())) {
                // 第三方根部门直接使用内部根部门 ID
                department.setCrmId(internalDepartment.getId());
                department.setCrmParentId(internalDepartment.getId());
                rootDepartments.add(department);
            } else {
                // 普通部门生成新的 CRM ID
                department.setCrmId(IDGenerator.nextStr());
            }
        }

        // 如果第三方数据没有根部门 则使用 internalDepartment 作为根节点
        if (!hasRootDepartment) {
            // 这里需要把 Department 转成 ThirdDepartment
            ThirdDepartment rootDepartment = new ThirdDepartment();
            if (Strings.CI.equals(type, "WECOM") || Strings.CI.equals(type, "DINGTALK")) {
                rootDepartment.setId("1");
            } else if (Strings.CI.equals(type, "LARK")) {
                rootDepartment.setId("0");
            }
            rootDepartment.setName(internalDepartment.getName());
            rootDepartment.setCrmId(internalDepartment.getId());
            rootDepartment.setIsRoot(true);
            rootDepartment.setChildren(new ArrayList<>());

            rootDepartments.add(rootDepartment);

            // 所有第三方部门挂到 internalDepartment 下面
            for (ThirdDepartment department : departments) {
                ThirdDepartment parentDepartment = departmentMap.get(department.getParentId());
                if (parentDepartment != null) {
                    department.setCrmParentId(parentDepartment.getCrmId());

                    if (parentDepartment.getChildren() == null) {
                        parentDepartment.setChildren(new ArrayList<>());
                    }

                    parentDepartment.getChildren().add(department);
                } else {
                    // 找不到父部门，直接挂到 internalDepartment
                    department.setCrmParentId(internalDepartment.getId());
                    rootDepartment.getChildren().add(department);
                }
            }
            return rootDepartments;
        }

        // 存在第三方根部门 按 parentId 组装树
        for (ThirdDepartment department : departments) {
            if (Boolean.TRUE.equals(department.getIsRoot())) {
                continue;
            }

            ThirdDepartment parentDepartment = departmentMap.get(department.getParentId());

            if (parentDepartment != null) {
                department.setCrmParentId(parentDepartment.getCrmId());

                if (parentDepartment.getChildren() == null) {
                    parentDepartment.setChildren(new ArrayList<>());
                }

                parentDepartment.getChildren().add(department);
            }
        }
        return rootDepartments;
    }


    public static List<ThirdDepartment> buildDepartmentTreeMultiple(String internalId, List<Department> currentDepartmentList, List<ThirdDepartment> departments) {
        // 当前部门集合
        Map<String, Department> currentDepartmentMap = currentDepartmentList.stream()
                .filter(dept -> dept.getResourceId() != null)
                .collect(Collectors.toMap(
                        Department::getResourceId,
                        Function.identity(),
                        (a, b) -> a
                ));
        // 企业微信部门集合
        Map<String, ThirdDepartment> wechatDepartmentMap = departments.stream()
                .collect(Collectors.toMap(
                        ThirdDepartment::getId,
                        Function.identity(),
                        (a, b) -> a
                ));


        Map<String, ThirdDepartment> finalDepartmentMap = new LinkedHashMap<>();
        // currentDepartmentList 转成 ThirdDepartment
        for (Department current : currentDepartmentList) {
            ThirdDepartment department = new ThirdDepartment();
            // 企业微信部门 ID
            department.setId(current.getResourceId());
            department.setName(current.getName());
            // CRM ID
            department.setCrmId(current.getId());
            // CRM 原来的父部门
            department.setCrmParentId(current.getParentId());
            // CRM 根部门
            department.setIsRoot(Objects.equals(current.getId(), internalId));
            department.setChildren(new ArrayList<>());
            finalDepartmentMap.put(current.getResourceId(), department);
        }


        // 设置crmId
        for (ThirdDepartment department : departments) {
            Department currentDepartment = currentDepartmentMap.get(department.getId());
            if (currentDepartment != null) {
                // 已经存在于 CRM
                department.setCrmId(currentDepartment.getId());
            } else if (Boolean.TRUE.equals(department.getIsRoot())) {
                // 企业微信根部门
                department.setCrmId(internalId);
            } else {
                // 新部门
                department.setCrmId(IDGenerator.nextStr());
            }
            department.setChildren(new ArrayList<>());
        }

        // 计算 crmParentId
        for (ThirdDepartment department : departments) {
            String parentId = department.getParentId();
            // 根部门
            if (Boolean.TRUE.equals(department.getIsRoot())) {
                department.setCrmId(internalId);
                department.setCrmParentId(internalId);
                continue;
            }

            // 当前 CRM 中的父部门
            Department currentParent = currentDepartmentMap.get(parentId);
            if (currentParent != null) {
                department.setCrmParentId(currentParent.getId());
                continue;
            }

            // 企业微信中的父部门
            ThirdDepartment wechatParent = wechatDepartmentMap.get(parentId);
            if (wechatParent != null) {
                department.setCrmParentId(wechatParent.getCrmId());
                continue;
            }
            // 父部门完全不存在 直接挂到根部门
            department.setCrmParentId(internalId);
        }

        // departments 覆盖 currentDepartmentList 中的旧数据
        for (ThirdDepartment department : departments) {
            finalDepartmentMap.put(department.getId(), department);
        }

        // 清空 children
        for (ThirdDepartment department : finalDepartmentMap.values()) {
            department.setChildren(new ArrayList<>());
        }

        // CRM ID -> ThirdDepartment
        Map<String, ThirdDepartment> crmDepartmentMap = finalDepartmentMap.values()
                .stream()
                .filter(department ->
                        department.getCrmId() != null)
                .collect(Collectors.toMap(
                        ThirdDepartment::getCrmId,
                        Function.identity(),
                        (a, b) -> a
                ));

        // 找根部门
        ThirdDepartment rootDepartment = crmDepartmentMap.get(internalId);
        if (rootDepartment == null) {
            return new ArrayList<>();
        }

        // 根据 crmParentId 重新组装整棵树
        for (ThirdDepartment department : finalDepartmentMap.values()) {
            // 根节点跳过
            if (Objects.equals(department.getCrmId(), internalId)) {
                continue;
            }
            // 根据 crmParentId 找父节点
            ThirdDepartment parentDepartment = crmDepartmentMap.get(department.getCrmParentId());
            if (parentDepartment != null) {
                parentDepartment.getChildren().add(department);
            } else {
                // 父节点不存在 直接挂根节点
                department.setCrmParentId(internalId);
                rootDepartment.getChildren().add(department);
            }
        }
        return Collections.singletonList(rootDepartment);
    }


    public static List<ThirdDepartment> buildThirdDepartmentTree(List<ThirdDepartment> departments) {
        if (departments == null || departments.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, ThirdDepartment> departmentMap = departments.stream()
                .collect(Collectors.toMap(
                        ThirdDepartment::getId,
                        Function.identity(),
                        (a, b) -> a
                ));

        List<ThirdDepartment> roots = new ArrayList<>();
        for (ThirdDepartment department : departments) {
            String parentId = department.getParentId();
            if (parentId == null || parentId.isEmpty() || !departmentMap.containsKey(parentId)) {
                roots.add(department);
                continue;
            }
            ThirdDepartment parent = departmentMap.get(parentId);
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(department);
        }
        sortDepartmentTree(roots);

        return roots;
    }

    private static void sortDepartmentTree(List<ThirdDepartment> departments) {
        if (departments == null || departments.isEmpty()) {
            return;
        }

        departments.sort(Comparator.comparing(ThirdDepartment::getOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        for (ThirdDepartment department : departments) {
            sortDepartmentTree(department.getChildren());
        }
    }

}