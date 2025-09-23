package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.ClueOwner;
import cn.cordys.crm.clue.dto.request.ClueAddRequest;
import cn.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import cn.cordys.crm.clue.dto.response.ClueOwnerListResponse;
import cn.cordys.crm.clue.service.ClueOwnerHistoryService;
import cn.cordys.crm.clue.service.ClueService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClueOwnerHistoryControllerTests extends BaseTest {
    private static final String BASE_PATH = "/lead/owner/history/";
    protected static final String LIST = "list/{0}";
    private static Clue addClue;

    @Resource
    private ClueOwnerHistoryService clueOwnerHistoryService;

    @Resource
    private ClueService clueService;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testListEmpty() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, "111");
        List<ClueOwnerListResponse> list = getResultDataArray(mvcResult, ClueOwnerListResponse.class);
        Assertions.assertTrue(CollectionUtils.isEmpty(list));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, LIST, "111");
    }

    @Test
    @Order(1)
    void testAdd() {
        ClueAddRequest clueAddRequest = new ClueAddRequest();
        clueAddRequest.setName(UUID.randomUUID().toString());
        clueAddRequest.setOwner(InternalUser.ADMIN.getValue());
        clueAddRequest.setProducts(List.of("cc"));
        addClue = clueService.add(clueAddRequest, InternalUser.ADMIN.getValue(), DEFAULT_ORGANIZATION_ID);

        ClueOwner clueOwner = clueOwnerHistoryService.add(addClue, InternalUser.ADMIN.getValue(), false);
        // 校验成功数据
        Assertions.assertEquals(clueOwner.getClueId(), addClue.getId());
        Assertions.assertEquals(clueOwner.getOperator(), InternalUser.ADMIN.getValue());
        Assertions.assertEquals(clueOwner.getCollectionTime(), addClue.getCollectionTime());
        Assertions.assertEquals(clueOwner.getOwner(), addClue.getOwner());
    }

    @Test
    @Order(2)
    void testBatchAdd() {
        ClueBatchTransferRequest transferRequest = new ClueBatchTransferRequest();
        transferRequest.setIds(List.of(addClue.getId()));
        transferRequest.setOwner(PERMISSION_USER_NAME);
        clueOwnerHistoryService.batchAdd(transferRequest, InternalUser.ADMIN.getValue());

        List<ClueOwnerListResponse> list = clueOwnerHistoryService.list(addClue.getId(), DEFAULT_ORGANIZATION_ID);
        for (ClueOwnerListResponse clueOwner : list) {
            // 校验成功数据
            Assertions.assertEquals(clueOwner.getClueId(), addClue.getId());
            Assertions.assertEquals(clueOwner.getOperator(), InternalUser.ADMIN.getValue());
            Assertions.assertEquals(clueOwner.getCollectionTime(), addClue.getCollectionTime());
            Assertions.assertEquals(clueOwner.getOwner(), addClue.getOwner());
        }
    }

    @Test
    @Order(3)
    void testList() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, addClue.getId());
        List<ClueOwnerListResponse> list = getResultDataArray(mvcResult, ClueOwnerListResponse.class);
        list.forEach(clueOwner -> {
            // 校验成功数据
            Assertions.assertEquals(clueOwner.getClueId(), addClue.getId());
            Assertions.assertEquals(clueOwner.getOperator(), InternalUser.ADMIN.getValue());
            Assertions.assertEquals(clueOwner.getCollectionTime(), addClue.getCollectionTime());
            Assertions.assertEquals(clueOwner.getOwner(), addClue.getOwner());
            Assertions.assertNotNull(clueOwner.getOwnerName());
            Assertions.assertNotNull(clueOwner.getOwnerName());
        });

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, LIST, "111");
    }

    @Test
    @Order(10)
    void batchDelete() {
        clueOwnerHistoryService.deleteByClueIds(List.of(addClue.getId()));
        List<ClueOwnerListResponse> list = clueOwnerHistoryService.list(addClue.getId(), DEFAULT_ORGANIZATION_ID);
        Assertions.assertTrue(list.isEmpty());
    }
}