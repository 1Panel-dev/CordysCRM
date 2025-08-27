package cn.cordys.crm.clue.controller;

import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.domain.UserView;
import cn.cordys.crm.system.dto.request.UserViewAddRequest;
import cn.cordys.crm.system.dto.request.UserViewUpdateRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClueUserViewControllerTest extends BaseTest {

    private static final String BASE_PATH = "/clue/view/";

    protected static final String DETAIL = "detail/{0}";
    protected static final String LIST = "list";
    protected static final String FIXED = "fixed/{0}";
    protected static final String POS = "edit/pos";
    protected static final String ENABLE = "enable/{0}";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    private static String viewId;

    @Test
    @Order(1)
    void testAdd() throws Exception {

        UserViewAddRequest request = new UserViewAddRequest();
        request.setName("测试视图11");
        request.setSearchMode("AND");
        request.setConditions(buildConditions());
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        UserView resultData = getResultData(mvcResult, UserView.class);
        this.viewId = resultData.getId();

    }

    private List<FilterCondition> buildConditions() {
        List<FilterCondition> conditions = new ArrayList<>();

        FilterCondition condition1 = new FilterCondition();
        condition1.setName("123123312312");
        condition1.setValue(null);
        condition1.setOperator("EMPTY");
        condition1.setMultipleValue(false);
        condition1.setType("LOCATION");

        FilterCondition condition2 = new FilterCondition();
        condition2.setName("1231233333");
        condition2.setValue("[\n" +
                "                \"2\"\n" +
                "            ]");
        condition2.setOperator("IN");
        condition2.setMultipleValue(false);
        condition2.setType("1231");

        conditions.add(condition1);
        conditions.add(condition2);
        return conditions;
    }


    @Test
    @Order(2)
    void testUpdate() throws Exception {
        UserViewUpdateRequest request = new UserViewUpdateRequest();
        request.setId(this.viewId);
        request.setName("更新試圖22");
        request.setConditions(buildConditions());
        this.requestPost(DEFAULT_UPDATE, request);

        request.setId("123123123");
        this.requestPost(DEFAULT_UPDATE, request);
    }

    @Test
    @Order(3)
    void testGetDetail() throws Exception {
        this.requestGet(DETAIL, this.viewId);
    }

    @Test
    @Order(4)
    void testList() throws Exception {
        this.requestGet(LIST);
    }

    @Test
    @Order(5)
    void testFixed() throws Exception {
        this.requestGet(FIXED, this.viewId);
    }

    @Test
    @Order(5)
    void testSortPos() throws Exception {
        String moveId = this.viewId;
        testAdd();
        String targetId = this.viewId;
        PosRequest request = new PosRequest();
        request.setOrgId(DEFAULT_ORGANIZATION_ID);
        request.setMoveId(moveId);
        request.setTargetId(targetId);
        request.setMoveMode("after");
        this.requestGet(POS, request);
    }

    @Test
    @Order(6)
    void testEnable() throws Exception {
        this.requestGet(ENABLE, this.viewId);
    }

    @Test
    @Order(10)
    void testDelete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, this.viewId);
    }
}
