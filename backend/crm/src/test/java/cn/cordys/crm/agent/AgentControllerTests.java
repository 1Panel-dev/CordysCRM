package cn.cordys.crm.agent;

import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.crm.integration.agent.domain.Agent;
import cn.cordys.crm.integration.agent.dto.request.AgentAddRequest;
import cn.cordys.crm.integration.agent.dto.request.AgentPageRequest;
import cn.cordys.crm.integration.agent.dto.request.AgentRenameRequest;
import cn.cordys.crm.integration.agent.dto.request.AgentUpdateRequest;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AgentControllerTests extends BaseTest {

    private static final String BASE_PATH = "/agent/";

    protected static final String DETAIL = "detail/{0}";
    protected static final String RENAME = "rename";
    protected static final String COLLECT = "collect/{0}";
    protected static final String UN_COLLECT = "un-collect/{0}";
    protected static final String COLLECT_PAGE = "collect/page";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    private static Agent addAgent;
    @Resource
    private BaseMapper<Agent> agentMapper;

    @Sql(scripts = {"/dml/init_agent_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @Order(1)
    void testAdd() throws Exception {
        AgentAddRequest request = new AgentAddRequest();
        request.setName("wx_智能体");
        request.setAgentModuleId("wx_123");
        request.setScopeIds(List.of("1", "2", "3"));
        request.setScript("<script src='xxxxx'></script>");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Opportunity resultData = getResultData(mvcResult, Opportunity.class);
        Agent agent = agentMapper.selectByPrimaryKey(resultData.getId());
        addAgent = agent;
    }


    @Test
    @Order(2)
    void testGetDetail() throws Exception {
        this.requestGet(DETAIL, addAgent.getId());
    }

    @Test
    @Order(3)
    void testUpdate() throws Exception {
        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setId(addAgent.getId());
        request.setName("智能体更新");
        this.requestPost(DEFAULT_UPDATE, request);
    }

    @Test
    @Order(4)
    void testRename() throws Exception {
        AgentRenameRequest request = new AgentRenameRequest();
        request.setId(addAgent.getId());
        request.setName("重命名");
        request.setAgentModuleId("wx_123");
        this.requestPost(RENAME, request);
    }


    @Test
    @Order(5)
    void testList() throws Exception {
        AgentPageRequest request = new AgentPageRequest();
        request.setAgentModuleIds(List.of("wx_123"));
        request.setCurrent(1);
        request.setPageSize(30);
        this.requestPost(DEFAULT_PAGE, request);
    }


    @Test
    @Order(6)
    void testCollect() throws Exception {
        this.requestGet(COLLECT, addAgent.getId());
    }

    @Test
    @Order(7)
    void testUnCollect() throws Exception {
        this.requestGet(UN_COLLECT, addAgent.getId());
    }

    @Test
    @Order(8)
    void testCollectPage() throws Exception {
        BasePageRequest request = new BasePageRequest();
        request.setCurrent(1);
        request.setPageSize(30);
        this.requestPost(COLLECT_PAGE, addAgent.getId());
    }



    @Test
    @Order(12)
    void testDelete() throws Exception {
        this.requestGet(DEFAULT_DELETE, addAgent.getId());
    }
}
