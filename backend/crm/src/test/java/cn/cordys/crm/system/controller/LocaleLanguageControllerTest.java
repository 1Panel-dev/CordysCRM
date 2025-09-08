package cn.cordys.crm.system.controller;

import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.dto.request.LocaleLanguageRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LocaleLanguageControllerTest extends BaseTest {

    public static final String LANGUAGE_CHANGE = "/locale-language/change";

    @Test
    @Order(1)
    public void localeLanguageChange() throws Exception {
        LocaleLanguageRequest request = new LocaleLanguageRequest();
        request.setLanguage("en-US");
        this.requestPost(LANGUAGE_CHANGE, request).andExpect(status().isOk());
    }
}
