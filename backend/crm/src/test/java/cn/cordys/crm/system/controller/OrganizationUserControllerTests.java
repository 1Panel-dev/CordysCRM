package cn.cordys.crm.system.controller;

import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.handler.ResultHolder;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.dto.request.*;
import cn.cordys.crm.system.dto.response.UserPageResponse;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.OrganizationUserService;
import cn.cordys.crm.utils.FileBaseUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrganizationUserControllerTests extends BaseTest {

    public static final String USER_LIST = "/user/list";
    public static final String USER_ADD = "/user/add";
    public static final String USER_DETAIL = "/user/detail/";
    public static final String USER_UPDATE = "/user/update";
    public static final String USER_RESET_PASSWORD = "/user/reset-password/";
    public static final String USER_BATCH_ENABLE = "/user/batch-enable";
    public static final String USER_BATCH_RESET_PASSWORD = "/user/batch/reset-password";
    public static final String USER_BATCH_EDIT = "/user/batch/edit";
    public static final String USER_DOWNLOAD_TEMPLATE = "/user/download/template";
    public static final String USER_IMPORT_PRE_CHECK = "/user/import/pre-check";
    public static final String USER_IMPORT = "/user/import";
    public static final String USER_OPTION = "/user/option";
    public static final String USER_ROLE_OPTION = "/user/role/option";
    public static final String USER_SYNC_CHECK = "/user/sync-check";
    public static final String USER_DELETE = "/user/delete/";
    public static final String USER_DELETE_CHECK = "/user/delete/check/";
    public static final String USER_UPDATE_NAME = "/user/update/name";
    public static final String USER_GET = "/user/get/";


    @Sql(scripts = {"/dml/init_user_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @Order(1)
    public void userList() throws Exception {
        UserPageRequest request = new UserPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setDepartmentIds(List.of("8"));
        this.requestPost(USER_LIST, request).andExpect(status().isOk());

    }

    @Test
    @Order(2)
    public void userAdd() throws Exception {
        UserAddRequest request = new UserAddRequest();
        request.setName("test");
        request.setPhone("12345678901");
        request.setGender(true);
        request.setEnable(true);
        request.setEmail("1@Cordys.com");
        request.setDepartmentId("1");
        request.setRoleIds(List.of("1", "2"));
        this.requestPost(USER_ADD, request).andExpect(status().isOk());

        //格式错误
        request.setEmail("1234");
        this.requestPost(USER_ADD, request).andExpect(status().is4xxClientError());
        //邮箱重复
        request.setEmail("1@Cordys.com");
        this.requestPost(USER_ADD, request).andExpect(status().is5xxServerError());
        //电话重复
        request.setEmail("2@Cordys.com");
        this.requestPost(USER_ADD, request).andExpect(status().is5xxServerError());
    }


    @Test
    @Order(3)
    public void userDetail() throws Exception {
        this.requestGet(USER_DETAIL + "u_1").andExpect(status().isOk());
    }

    @Test
    @Order(4)
    public void userUpdate() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("test111");
        request.setPhone("12345633342");
        request.setGender(true);
        request.setEnable(true);
        request.setEmail("221@Cordys.com");
        request.setDepartmentId("9");
        request.setRoleIds(List.of("1", "2", "3"));
        request.setId("u_1");
        this.requestPost(USER_UPDATE, request).andExpect(status().isOk());
    }

    @Test
    @Order(4)
    public void userUpdateName() throws Exception {
        UserUpdateName request = new UserUpdateName();
        request.setName("更新名称");
        request.setUserId("5");
        this.requestPost(USER_UPDATE_NAME, request).andExpect(status().isOk());
    }


    @Test
    @Order(5)
    public void resetPassword() throws Exception {
        // TODO 密码重置踢出用户导致后续测试失败
        // this.requestGet(USER_RESET_PASSWORD + "5").andExpect(status().isOk());
    }

    @Test
    @Order(6)
    public void batchEnable() throws Exception {
        UserBatchEnableRequest request = new UserBatchEnableRequest();
        request.setEnable(true);
        request.setIds(List.of("u_1", "u_2"));
        // TODO 密码重置踢出用户导致后续测试失败,重新登录
        // this.requestPost(USER_BATCH_ENABLE, request).andExpect(status().isOk());
    }

    @Test
    @Order(7)
    public void batchResetPassword() throws Exception {
        UserBatchRequest request = new UserBatchRequest();
        request.setIds(List.of("u_1", "u_2"));

        // TODO 密码重置踢出用户导致后续测试失败,重新登录
        // this.requestPost(USER_BATCH_RESET_PASSWORD, request).andExpect(status().isOk());
    }

    @Test
    @Order(9)
    public void batchEdit() throws Exception {
        UserBatchEditRequest request = new UserBatchEditRequest();
        request.setIds(List.of("u_1", "u_2"));
        request.setWorkCity("深圳");
        // TODO 密码重置踢出用户导致后续测试失败,重新登录
        //   this.requestPost(USER_BATCH_EDIT, request).andExpect(status().isOk());
    }


    @Test
    @Order(10)
    public void downloadTemplate() throws Exception {
        // this.requestGetExcel(USER_DOWNLOAD_TEMPLATE);
    }

    private MvcResult requestGetExcel(String url) throws Exception {
        return mockMvc.perform(getRequestBuilder(url))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @Order(11)
    public void testImportCheckExcel() throws Exception {
        String filePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("file/user.xlsx")).getPath();
        MockMultipartFile file = new MockMultipartFile("file", "11.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, FileBaseUtils.getFileBytes(filePath));
        LinkedMultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("file", file);
        this.requestMultipart(USER_IMPORT_PRE_CHECK, paramMap);

    }

    @Test
    @Order(12)
    public void testImportExcel() throws Exception {
        String filePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("file/user1.xlsx")).getPath();
        MockMultipartFile file = new MockMultipartFile("file", "1111.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, FileBaseUtils.getFileBytes(filePath));
        LinkedMultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("file", file);
        this.requestMultipart(USER_IMPORT, paramMap);

    }

    @Test
    @Order(13)
    public void testUserOption() throws Exception {
        this.requestGet(USER_OPTION).andExpect(status().isOk());
    }

    @Test
    @Order(14)
    public void testUserRoleOption() throws Exception {
        this.requestGet(USER_ROLE_OPTION).andExpect(status().isOk());
    }

    @Test
    @Order(15)
    public void testSyncCheck() throws Exception {
        this.requestGet(USER_SYNC_CHECK).andExpect(status().isOk());
    }

    @Test
    @Order(16)
    public void testUserDelete() throws Exception {
        this.requestGet(USER_DELETE + "u_5").andExpect(status().isOk());
    }

    @Test
    @Order(15)
    public void testUserDeleteCheck() throws Exception {
        this.requestGet(USER_DELETE_CHECK + "u_5").andExpect(status().isOk());
    }

    @Test
    @Order(15)
    public void testUserGet() throws Exception {
        this.requestGet(USER_GET + "9").andExpect(status().isOk());
    }

    //测试高级搜索
    @Test
    @Order(16)
    public void testUserListForCombineSearch() throws Exception {

        List<String> allDeptIdList = new ArrayList<>();
        String superiorDeptId;
        AtomicReference<String> deptId1 = new AtomicReference<>(), deptId2 = new AtomicReference<>();

        long startTestTime = System.currentTimeMillis();

        // 准备数据
        {
            DepartmentService departmentService = CommonBeanFactory.getBean(DepartmentService.class);
            OrganizationUserService organizationUserService = CommonBeanFactory.getBean(OrganizationUserService.class);
            Assertions.assertNotNull(departmentService);
            Assertions.assertNotNull(organizationUserService);

            DepartmentAddRequest deptAddRequest = new DepartmentAddRequest();
            deptAddRequest.setName("高级搜索测试节点1");
            deptAddRequest.setParentId("NONE");
            this.requestPost(DepartmentControllerTests.ADD_DEPARTMENT, deptAddRequest).andExpect(status().isOk());
            deptAddRequest.setName("高级搜索测试节点2");
            this.requestPost(DepartmentControllerTests.ADD_DEPARTMENT, deptAddRequest).andExpect(status().isOk());

            MvcResult orgTreeResult = this.requestGetWithOkAndReturn(DepartmentControllerTests.DEPARTMENT_TREE);
            List<BaseTreeNode> departmentTree = JSON.parseArray(
                    JSON.toJSONString(
                            JSON.parseObject(
                                    orgTreeResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                                    ResultHolder.class
                            ).getData()),
                    BaseTreeNode.class);

            departmentTree.forEach(node -> {
                allDeptIdList.add(node.getId());
                if (node.getName().equals("高级搜索测试节点1")) {
                    deptId1.set(node.getId());
                } else if (node.getName().equals("高级搜索测试节点2")) {
                    deptId2.set(node.getId());
                }
            });


            MvcResult userPageResult = this.requestPostWithOkAndReturn(USER_LIST, new UserPageRequest() {{
                setCurrent(1);
                setPageSize(10);
                setDepartmentIds(allDeptIdList);
            }});

            Pager<List<UserPageResponse>> userResponse = JSON.parseObject(
                    JSON.toJSONString(
                            JSON.parseObject(
                                    userPageResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                                    ResultHolder.class).getData()),
                    Pager.class);
            UserPageResponse lastUser = JSON.parseObject(
                    JSON.toJSONString(userResponse.getList().getLast()),
                    UserPageResponse.class);
            superiorDeptId = lastUser.getId();

            UserAddRequest userAddRequest;
            for (int i = 0; i < 20; i++) {
                userAddRequest = new UserAddRequest();
                userAddRequest.setName("combineTest_" + i);
                userAddRequest.setEnable(true);
                userAddRequest.setEmail("combineTest_" + i + "@Cordys.com");
                userAddRequest.setRoleIds(List.of("1", "2"));
                userAddRequest.setEmployeeId("1000" + i);

                if (i < 10) {
                    userAddRequest.setPhone("1858888000" + i);
                    userAddRequest.setDepartmentId(deptId1.get());
                    userAddRequest.setGender(true);
                    userAddRequest.setEmployeeType("formal");
                    userAddRequest.setWorkCity("110101");
                    userAddRequest.setOnboardingDate(System.currentTimeMillis());
                } else {
                    userAddRequest.setPhone("183888800" + i);
                    userAddRequest.setDepartmentId(deptId2.get());
                    userAddRequest.setGender(false);
                    userAddRequest.setSupervisorId(superiorDeptId);
                    userAddRequest.setEmployeeType("internship");
                }

                if (i % 5 == 0) {
                    userAddRequest.setEnable(false);
                }
                userAddRequest.setPosition("测试职位");
                this.requestPost(USER_ADD, userAddRequest).andExpect(status().isOk());
            }
        }

        /*
        姓名  userName
        是否启用  enable
        性别  gender
        手机号 phone
        邮箱  email
        部门  departmentName
        直属上级    supervisorId
        工号  employeeId
        职位  position
        员工类型    employeeType
        创建人     createUser
        更新人     updateUser
        入职日期    onboardingDate  （动态范围查询）
        创建时间    createTime      （动态范围查询）
        更新时间    updateTime      （动态范围查询）
        工作城市    workCity        (查询方式待商榷，暂不查询)
        入职日期    onboardingDate  (查询方式待商榷，暂不查询)
         */

        UserPageRequest request = new UserPageRequest();
        request.setCurrent(1);
        request.setPageSize(100);
        request.setDepartmentIds(allDeptIdList);

        CombineSearch combineSearch = new CombineSearch();
        List<FilterCondition> filterConditionList = new ArrayList<>();

        //全量检查
        MvcResult pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        Pager<List<UserPageResponse>> result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertTrue(result.getTotal() > 20);

        //        姓名  userName    like查询
        FilterCondition likeCondition = new FilterCondition();
        likeCondition.setOperator(FilterCondition.CombineConditionOperator.CONTAINS.name());
        likeCondition.setName("name");
        likeCondition.setValue("combineTest_");
        filterConditionList.add(likeCondition);
        combineSearch.setConditions(filterConditionList);
        request.setCombineSearch(combineSearch);

        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(20, result.getTotal());

        //        性别  gender
        filterConditionList.clear();
        FilterCondition booleanCondition = new FilterCondition();
        booleanCondition.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        booleanCondition.setName("gender");
        booleanCondition.setValue(false);
        filterConditionList.add(booleanCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(10, result.getTotal());

        //        是否启用  enable
        filterConditionList.clear();
        booleanCondition = new FilterCondition();
        booleanCondition.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        booleanCondition.setName("enable");
        booleanCondition.setValue(false);
        filterConditionList.add(booleanCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(4, result.getTotal());

        //      手机号 phone
        filterConditionList.clear();
        likeCondition = new FilterCondition();
        likeCondition.setOperator(FilterCondition.CombineConditionOperator.CONTAINS.name());
        likeCondition.setName("phone");
        likeCondition.setValue("1858888000");
        filterConditionList.add(likeCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(10, result.getTotal());

        //      邮箱  email
        filterConditionList.clear();
        likeCondition = new FilterCondition();
        likeCondition.setOperator(FilterCondition.CombineConditionOperator.CONTAINS.name());
        likeCondition.setName("email");
        likeCondition.setValue("combineTest_");
        filterConditionList.add(likeCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(20, result.getTotal());

        //      直属上级    supervisorId
        filterConditionList.clear();
        FilterCondition inCondition = new FilterCondition();
        inCondition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
        inCondition.setName("supervisorId");
        inCondition.setValue(List.of(superiorDeptId));
        filterConditionList.add(inCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(10, result.getTotal());

        //      部门  departmentId
        filterConditionList.clear();
        inCondition = new FilterCondition();
        inCondition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
        inCondition.setName("departmentId");
        inCondition.setValue(List.of(deptId1, deptId2));
        filterConditionList.add(inCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(20, result.getTotal());

        filterConditionList.clear();
        inCondition = new FilterCondition();
        inCondition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
        inCondition.setName("departmentId");
        inCondition.setValue(List.of(deptId1));
        filterConditionList.add(inCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(10, result.getTotal());

        //        工号  employeeId
        filterConditionList.clear();
        likeCondition = new FilterCondition();
        likeCondition.setOperator(FilterCondition.CombineConditionOperator.CONTAINS.name());
        likeCondition.setName("employeeId");
        likeCondition.setValue("1000");
        filterConditionList.add(likeCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(20, result.getTotal());

        //        职位  position
        filterConditionList.clear();
        likeCondition = new FilterCondition();
        likeCondition.setOperator(FilterCondition.CombineConditionOperator.CONTAINS.name());
        likeCondition.setName("position");
        likeCondition.setValue("测试");
        filterConditionList.add(likeCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(20, result.getTotal());

        //        员工类型        IN查询  formal  internship outsourcing
        filterConditionList.clear();
        inCondition = new FilterCondition();
        inCondition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
        inCondition.setName("employeeType");
        inCondition.setValue(List.of("formal"));
        filterConditionList.add(inCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(10, result.getTotal());

        //        创建人     createUser
        filterConditionList.clear();
        inCondition = new FilterCondition();
        inCondition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
        inCondition.setName("createUser");
        inCondition.setValue(List.of("admin"));
        filterConditionList.add(inCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertTrue(result.getTotal() > 20);

        //        更新人     updateUser
        filterConditionList.clear();
        inCondition = new FilterCondition();
        inCondition.setOperator(FilterCondition.CombineConditionOperator.IN.name());
        inCondition.setName("updateUser");
        inCondition.setValue(List.of("admin"));
        filterConditionList.add(inCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertTrue(result.getTotal() > 20);

        //        入职日期    onboardingDate
        filterConditionList.clear();
        FilterCondition betweenCondition = new FilterCondition();
        betweenCondition.setOperator(FilterCondition.CombineConditionOperator.BETWEEN.name());
        betweenCondition.setName("onboardingDate");
        betweenCondition.setValue(List.of(startTestTime, System.currentTimeMillis()));
        filterConditionList.add(betweenCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(10, result.getTotal());

        //        创建时间    createTime
        filterConditionList.clear();
        betweenCondition = new FilterCondition();
        betweenCondition.setOperator(FilterCondition.CombineConditionOperator.BETWEEN.name());
        betweenCondition.setName("createTime");
        betweenCondition.setValue(List.of(startTestTime, System.currentTimeMillis()));
        filterConditionList.add(betweenCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertTrue(result.getTotal() >= 20);

        //        更新时间    updateTime
        filterConditionList.clear();
        betweenCondition = new FilterCondition();
        betweenCondition.setOperator(FilterCondition.CombineConditionOperator.BETWEEN.name());
        betweenCondition.setName("updateTime");
        betweenCondition.setValue(List.of(startTestTime, System.currentTimeMillis()));
        filterConditionList.add(betweenCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertTrue(result.getTotal() >= 20);

        //        工作城市    workCity
        filterConditionList.clear();
        betweenCondition = new FilterCondition();
        betweenCondition.setOperator(FilterCondition.CombineConditionOperator.EQUALS.name());
        betweenCondition.setName("workCity");
        betweenCondition.setValue("110101");
        filterConditionList.add(betweenCondition);
        request.getCombineSearch().setConditions(filterConditionList);
        pageResult = this.requestPostWithOkAndReturn(USER_LIST, request);
        result = JSON.parseObject(
                JSON.toJSONString(
                        JSON.parseObject(pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        Assertions.assertEquals(10, result.getTotal());
    }

}
