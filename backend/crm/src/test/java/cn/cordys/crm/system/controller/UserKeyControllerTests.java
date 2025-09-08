package cn.cordys.crm.system.controller;


import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.domain.UserKey;
import cn.cordys.crm.system.dto.request.UserKeyUpdateRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserKeyControllerTests extends BaseTest {

    private static final String BASE_PATH = "/user/api/key/";
    private static final String ENABLE = "/enable/{0}";
    private static final String DISABLE = "/disable/{0}";

    private static String APIKEY_ID;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }


    @Test
    @Order(1)
    public void addApiKey() throws Exception {
        this.requestGet(DEFAULT_ADD);
    }

    @Test
    @Order(2)
    public void list() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_LIST);
        List<UserKey> result = getResultDataArray(mvcResult, UserKey.class);
        APIKEY_ID = result.getFirst().getId();
    }


    @Test
    @Order(3)
    public void updateApiKey() throws Exception {
        UserKeyUpdateRequest request = new UserKeyUpdateRequest();
        request.setId(APIKEY_ID);
        request.setForever(true);
        request.setDescription("描述");
        this.requestPost(DEFAULT_UPDATE, request);
    }

    @Test
    @Order(4)
    public void enable() throws Exception {
        this.requestGet(ENABLE, APIKEY_ID);
    }

    @Test
    @Order(5)
    public void disable() throws Exception {
        this.requestGet(DISABLE, APIKEY_ID);
    }

    @Test
    @Order(6)
    public void delete() throws Exception {
        this.requestGet(DEFAULT_DELETE, APIKEY_ID);
    }
}
