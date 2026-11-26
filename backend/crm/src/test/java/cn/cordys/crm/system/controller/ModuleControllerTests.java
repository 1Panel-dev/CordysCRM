package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.domain.Module;
import cn.cordys.crm.system.dto.request.ModuleRequest;
import cn.cordys.crm.system.dto.request.ModuleSortRequest;
import cn.cordys.crm.system.dto.response.ModuleDTO;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleControllerTests extends BaseTest {

    private static final String LIST_ALL = "/module/list";
    private static final String SWITCH = "/module/switch/";
    private static final String SORT = "/module/sort";

    @Resource
    private BaseMapper<Module> moduleMapper;

    @Test
    @Order(1)
    void testInitModuleList() {
        List<Module> modules = moduleMapper.selectAll("pos").stream()
                .filter(module -> DEFAULT_ORGANIZATION_ID.equals(module.getOrganizationId()))
                .toList();
        assertEquals(List.of("home", "follow", "clue", "customer", "business", "contract", "order", "product",
                "customForm", "dashboard", "agent", "tender", "setting"),
                modules.stream().map(Module::getModuleKey).toList());
        assertEquals(List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L, 110L, 120L, 130L),
                modules.stream().map(Module::getPos).toList());
    }

    @Test
    @Order(2)
    void testGetModuleListAndSwitch() throws Exception {
        ModuleRequest request = new ModuleRequest();
        request.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(LIST_ALL, request);
        List<ModuleDTO> modules = getResultDataArray(mvcResult, ModuleDTO.class);
        assert !modules.isEmpty();
        String param = modules.getFirst().getId();
        this.requestGetWithOk(SWITCH + param);
        // permission check
        requestGetPermissionTest(PermissionConstants.MODULE_SETTING_UPDATE, SWITCH + param);
        // switch not exist module
        MvcResult mvcResult1 = this.requestGet(SWITCH + "none").andExpect(status().is5xxServerError()).andReturn();
        assert mvcResult1.getResponse().getContentAsString().contains(Translator.get("module.not_exist"));
        ModuleSortRequest sortRequest = new ModuleSortRequest();
        sortRequest.setDragModuleId(param);
        sortRequest.setStart(modules.getFirst().getPos());
        sortRequest.setEnd(modules.get(2).getPos());
        this.requestPostWithOk(SORT, sortRequest);
        sortRequest.setStart(modules.get(2).getPos());
        sortRequest.setEnd(modules.getFirst().getPos());
        this.requestPostWithOk(SORT, sortRequest);
        // permission check
        requestPostPermissionTest(PermissionConstants.MODULE_SETTING_UPDATE, SORT, sortRequest);
    }
}
