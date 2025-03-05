package io.cordys.crm.system.controller;

import io.cordys.crm.base.BaseTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SseControllerTests extends BaseTest {

    private static final String BASE_PATH = "/sse/";


    @Test
    @Order(0)
    void testConnect() throws Exception {
        mockMvc.perform(getRequestBuilder(BASE_PATH + "subscribe?clientId=fbf603acdc")).andExpect(status().isOk());

    }

    @Test
    @Order(1)
    void testClose() throws Exception {
        mockMvc.perform(getRequestBuilder(BASE_PATH + "close?clientId=fbf603acdc")).andExpect(status().isOk());

    }

}
