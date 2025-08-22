package io.cordys.crm.search;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.search.request.UserSearchConfigAddRequest;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserSearchConfigControllerTest extends BaseTest {
    private static final String BASE_PATH = "/search/config/";
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    public static String FIELD_ID = "wx_field-test-id";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(1)
    void initData() {
        ModuleField field = new ModuleField();
        field.setId(FIELD_ID);
        field.setFormId("form-test-1");
        field.setName("dep-test-1");
        field.setMobile(false);
        field.setType(FieldType.DEPARTMENT.name());
        field.setPos(1L);
        field.setCreateUser("admin");
        field.setCreateTime(System.currentTimeMillis());
        field.setUpdateUser("admin");
        field.setUpdateTime(System.currentTimeMillis());
        moduleFieldMapper.insert(field);

    }


    @Test
    @Order(2)
    void testAdd() throws Exception {
        UserSearchConfigAddRequest request = new UserSearchConfigAddRequest();
        request.setResultDisplay(false);
        request.setSortSetting(List.of("SEARCH_ADVANCED_CLUE", "SEARCH_ADVANCED_CUSTOMER", "SEARCH_ADVANCED_CONTACT", "SEARCH_ADVANCED_PUBLIC", "SEARCH_ADVANCED_CLUE_POOL", "SEARCH_ADVANCED_OPPORTUNITY"));
        request.setSearchFields(fieldMap());

        this.requestPost(DEFAULT_ADD, request);
    }

    private Map<String, List<String>> fieldMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("SEARCH_ADVANCED_CLUE", List.of(FIELD_ID));
        return map;
    }
}
