package io.cordys.wecom.utils;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.system.domain.*;
import io.cordys.crm.system.service.DepartmentService;
import io.cordys.crm.system.service.OrganizationUserService;
import io.cordys.wecom.dto.WeComDepartment;
import io.cordys.wecom.dto.WeComUser;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepartmentDataHandle {

    private String orgId;
    private DepartmentService departmentService;
    private OrganizationUserService organizationUserService;
    private List<Department> departments = new ArrayList<>();
    private List<WeComDepartment> weComDepartmentTree = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private List<UserExtend> userExtends = new ArrayList<>();
    private List<OrganizationUser> organizationUsers = new ArrayList<>();
    private Map<String, List<WeComUser>> departmentUserMap = new HashMap<>();
    private List<DepartmentCommander> departmentCommanders = new ArrayList<>();


    public DepartmentDataHandle(List<WeComDepartment> weComDepartments, String orgId, Map<String, List<WeComUser>> departmentUserMap) {
        this.departmentService = CommonBeanFactory.getBean(DepartmentService.class);
        this.organizationUserService = CommonBeanFactory.getBean(OrganizationUserService.class);
        organizationUserService.deleteUser(orgId);
        departmentService.deleteByOrgId(orgId);
        this.weComDepartmentTree = WeComDepartment.buildDepartmentTree(weComDepartments);
        this.departmentUserMap = departmentUserMap;
        this.orgId = orgId;
    }

    /**
     * 处理数据
     *
     * @param operatorId
     */
    public void handleData(String operatorId) {
        weComDepartmentTree.forEach(weComDepartment -> {
            handleTreeData(weComDepartment, operatorId);

        });
        departmentService.save(departments);
        organizationUserService.save(users, userExtends, organizationUsers, departmentCommanders);

    }

    private void handleTreeData(WeComDepartment weComDepartment, String operatorId) {
        buildData(weComDepartment, operatorId, departmentUserMap);
        if (CollectionUtils.isNotEmpty(weComDepartment.getChildren())) {
            weComDepartment.getChildren().forEach(department -> {
                handleTreeData(department, operatorId);
            });
        }
    }


    public void buildData(WeComDepartment weComDepartment, String operatorId, Map<String, List<WeComUser>> departmentUserMap) {
        List<WeComUser> weComUsers = new ArrayList<>();
        if (departmentUserMap.containsKey(weComDepartment.getId().toString())) {
            weComUsers = departmentUserMap.get(weComDepartment.getId().toString());
        }
        //构建部门参数
        departmentService.buildDepartment(weComDepartment, operatorId, orgId, departments);
        //构建用户参数
        organizationUserService.buildUser(weComDepartment, weComUsers, operatorId, orgId, users, userExtends, organizationUsers, departmentCommanders);

    }
}
