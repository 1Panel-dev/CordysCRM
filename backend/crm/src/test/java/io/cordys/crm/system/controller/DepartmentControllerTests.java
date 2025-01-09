package io.cordys.crm.system.controller;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.dto.request.DepartmentAddRequest;
import io.cordys.crm.system.dto.request.DepartmentCommanderRequest;
import io.cordys.crm.system.dto.request.DepartmentRenameRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DepartmentControllerTests extends BaseTest {

    public static final String DEPARTMENT_TREE = "/department/tree";
    public static final String ADD_DEPARTMENT = "/department/add";
    public static final String RENAME_DEPARTMENT = "/department/rename";
    public static final String DEPARTMENT_SET_COMMANDER = "/department/set-commander";


    @Sql(scripts = {"/dml/init_department_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @Order(1)
    public void departmentTree() throws Exception {
        this.requestGet(DEPARTMENT_TREE).andExpect(status().isOk());
    }


    @Test
    @Order(2)
    public void addDepartment() throws Exception {
        DepartmentAddRequest request = new DepartmentAddRequest();
        request.setName("测试部门");
        request.setParentId("NONE");
        this.requestPost(ADD_DEPARTMENT, request).andExpect(status().isOk());
    }


    @Test
    @Order(3)
    public void renameDepartment() throws Exception {
        DepartmentRenameRequest request = new DepartmentRenameRequest();
        request.setName("测试部门");
        request.setId("1");
        this.requestPost(RENAME_DEPARTMENT, request).andExpect(status().isOk());

        request.setId("12363435234");
        this.requestPost(RENAME_DEPARTMENT, request).andExpect(status().is5xxServerError());
    }

    @Test
    @Order(4)
    public void departmentCommander() throws Exception {
        DepartmentCommanderRequest request = new DepartmentCommanderRequest();
        request.setCommanderId("admin");
        request.setDepartmentId("1");
        this.requestPost(DEPARTMENT_SET_COMMANDER, request).andExpect(status().isOk());

    }

}
