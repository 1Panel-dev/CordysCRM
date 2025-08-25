package io.cordys.crm.search;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.search.request.FieldMaskConfigDTO;
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
public class SearchFieldMaskConfigControllerTest extends BaseTest {

    private static final String BASE_PATH = "/mask/config/";

    protected static final String SAVE = "save";
    protected static final String GET = "get";

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    public static String FIELD_ID = "wx_mask_field-test-id";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(1)
    void initData() {
        ModuleField field = new ModuleField();
        field.setId(FIELD_ID);
        field.setFormId("mask_form-test-1");
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
    void testSave() throws Exception {
        FieldMaskConfigDTO request = new FieldMaskConfigDTO();
        request.setSearchFields(fieldMap());

        this.requestPost(SAVE, request);
    }

    private Map<String, List<String>> fieldMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("SEARCH_ADVANCED_CLUE", List.of(FIELD_ID));
        return map;
    }


    @Test
    @Order(3)
    void testGet() throws Exception {
        this.requestGet(GET);
    }
}
