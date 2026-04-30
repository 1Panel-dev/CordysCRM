package cn.cordys.crm.approval;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.approval.constants.*;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.domain.ApprovalFlowBlob;
import cn.cordys.crm.approval.domain.ApprovalNode;
import cn.cordys.crm.approval.domain.ApprovalNodeApprover;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import cn.cordys.crm.approval.dto.request.*;
import cn.cordys.crm.approval.dto.response.ApprovalFlowByFormTypeResponse;
import cn.cordys.crm.approval.dto.response.ApprovalFlowDetailResponse;
import cn.cordys.crm.approval.dto.response.ApprovalFlowListResponse;
import cn.cordys.crm.approval.dto.response.StatusPermissionSettingResponse;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApprovalFlowControllerTests extends BaseTest {
    private static final String BASE_PATH = "/approval-flow/";
    private static final String ENABLE = "enable/{0}";
    private static final String STATUS_PERMISSION_SETTING = "status-permission/setting/{0}";
    private static final String GET_BY_FORM_TYPE = "get-by-form-type/{0}";

    /**
     * 记录创建的审批流
     */
    private static ApprovalFlow addApprovalFlow;
    private static ApprovalFlow anotherApprovalFlow;

    @Resource
    private BaseMapper<ApprovalFlow> approvalFlowMapper;
    @Resource
    private BaseMapper<ApprovalFlowBlob> approvalFlowBlobMapper;
    @Resource
    private BaseMapper<ApprovalNode> approvalNodeMapper;
    @Resource
    private BaseMapper<ApprovalNodeApprover> approvalNodeApproverMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    /**
     * 构建简单的审批人节点请求
     */
    private ApprovalNodeApproverRequest buildApproverNodeRequest(String name, int sort) {
        ApprovalNodeApproverRequest node = new ApprovalNodeApproverRequest();
        node.setName(name);
        node.setNodeType(ApprovalNodeTypeEnum.APPROVER.name());
        node.setSort(sort);
        node.setApprovalType(ApprovalTypeEnum.MANUAL.name());
        node.setMultiApproverMode(MultiApproverModeEnum.ALL.name());

        // 配置审批人
        node.setApproverType(ApproverTypeEnum.ROLE.name());
        node.setApproverList(List.of("sales_manager"));

        // 配置抄送
        node.setCcType(ApproverTypeEnum.ROLE.name());
        node.setCcList(List.of("org_admin"));

        return node;
    }

    /**
     * 构建开始节点请求
     */
    private ApprovalNodeRequest buildStartNodeRequest() {
        ApprovalNodeRequest node = new ApprovalNodeRequest();
        node.setName("开始");
        node.setNodeType(ApprovalNodeTypeEnum.START.name());
        node.setSort(0);
        return node;
    }

    /**
     * 构建结束节点请求
     */
    private ApprovalNodeRequest buildEndNodeRequest() {
        ApprovalNodeRequest node = new ApprovalNodeRequest();
        node.setName("结束");
        node.setNodeType(ApprovalNodeTypeEnum.END.name());
        node.setSort(999);
        return node;
    }

    /**
     * 构建状态权限配置
     */
    private List<StatusPermissionDTO> buildStatusPermissions() {
        List<StatusPermissionDTO> statusPermissions = new ArrayList<>();
        StatusPermissionDTO p1 = new StatusPermissionDTO();
        p1.setApprovalStatus(ApprovalState.APPROVED.getId());
        p1.setPermission("view");
        p1.setEnabled(true);
        statusPermissions.add(p1);

        StatusPermissionDTO p2 = new StatusPermissionDTO();
        p2.setApprovalStatus(ApprovalState.UNAPPROVED.getId());
        p2.setPermission("edit");
        p2.setEnabled(false);
        statusPermissions.add(p2);

        return statusPermissions;
    }

    /**
     * 构建新增请求
     */
    private ApprovalFlowAddRequest buildAddRequest(String name, ApprovalFormTypeEnum formType, boolean enable) {
        ApprovalFlowAddRequest request = new ApprovalFlowAddRequest();
        request.setName(name);
        request.setFormType(formType.getValue());
        request.setEnable(enable);
        request.setDescription("测试审批流描述");
        request.setSubmitterCanRevoke(true);
        request.setAllowBatchProcess(false);
        request.setAllowWithdraw(true);
        request.setAllowAddSign(false);
        request.setDuplicateApproverRule(DuplicateApproverRuleEnum.EACH.name());
        request.setRequireComment(false);
        request.setExecuteTiming(List.of(ExecuteTimingEnum.CREATE.name(), ExecuteTimingEnum.EDIT.name()));
        request.setStatusPermissions(buildStatusPermissions());

        // 构建节点配置: 开始 -> 审批人 -> 结束
        List<ApprovalNodeRequest> nodes = new ArrayList<>();
        nodes.add(buildStartNodeRequest());
        nodes.add(buildApproverNodeRequest("主管审批", 1));
        nodes.add(buildEndNodeRequest());
        request.setNodes(nodes);

        return request;
    }

    @Test
    @Order(0)
    void testPageEmpty() throws Exception {
        ApprovalFlowPageRequest request = new ApprovalFlowPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.APPROVAL_FLOW_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        // 请求成功 - 创建启用的审批流
        ApprovalFlowAddRequest request = buildAddRequest("报价审批流", ApprovalFormTypeEnum.QUOTATION, true);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        ApprovalFlowDetailResponse resultData = getResultData(mvcResult, ApprovalFlowDetailResponse.class);
        ApprovalFlow flow = approvalFlowMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        addApprovalFlow = flow;
        Assertions.assertEquals(request.getName(), flow.getName());
        Assertions.assertEquals(request.getFormType(), flow.getFormType());
        Assertions.assertEquals(request.getEnable(), flow.getEnable());
        Assertions.assertEquals(flow.getOrganizationId(), DEFAULT_ORGANIZATION_ID);
        Assertions.assertNotNull(flow.getNumber());

        // 校验大字段
        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(flow.getId());
        Assertions.assertEquals(request.getDescription(), blob.getDescription());

        // 校验节点配置
        List<ApprovalNode> nodes = getNodesByFlowId(flow.getId());
        Assertions.assertEquals(3, nodes.size());

        // 校验审批人节点配置
        ApprovalNode approverNode = nodes.stream()
                .filter(n -> ApprovalNodeTypeEnum.APPROVER.name().equals(n.getNodeType()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(approverNode);
        ApprovalNodeApprover approverConfig = approvalNodeApproverMapper.selectByPrimaryKey(approverNode.getId());
        Assertions.assertNotNull(approverConfig);

        // 添加另一条数据，不同表单类型
        ApprovalFlowAddRequest anotherRequest = buildAddRequest("合同审批流", ApprovalFormTypeEnum.CONTRACT, true);
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, anotherRequest);
        anotherApprovalFlow = approvalFlowMapper.selectByPrimaryKey(getResultData(mvcResult, ApprovalFlowDetailResponse.class).getId());

        // 校验创建禁用的审批流
        ApprovalFlowAddRequest disabledRequest = buildAddRequest("禁用的发票审批流", ApprovalFormTypeEnum.INVOICE, false);
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, disabledRequest);
        ApprovalFlow disabledFlow = approvalFlowMapper.selectByPrimaryKey(getResultData(mvcResult, ApprovalFlowDetailResponse.class).getId());
        Assertions.assertFalse(disabledFlow.getEnable());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.APPROVAL_FLOW_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        ApprovalFlowUpdateRequest request = new ApprovalFlowUpdateRequest();
        request.setId(addApprovalFlow.getId());
        request.setName("更新后的报价审批流");
        request.setDescription("更新后的描述");
        request.setSubmitterCanRevoke(false);
        request.setAllowBatchProcess(true);
        request.setAllowWithdraw(false);
        request.setAllowAddSign(true);
        request.setDuplicateApproverRule(DuplicateApproverRuleEnum.FIRST_ONLY.name());
        request.setRequireComment(true);
        request.setExecuteTiming(List.of(ExecuteTimingEnum.CREATE.name()));
        request.setStatusPermissions(buildStatusPermissions());

        // 更新节点配置
        List<ApprovalNodeRequest> nodes = new ArrayList<>();
        nodes.add(buildStartNodeRequest());
        ApprovalNodeApproverRequest approverNode = buildApproverNodeRequest("经理审批", 1);
        approverNode.setApprovalType(ApprovalTypeEnum.AUTO_PASS.name());
        nodes.add(approverNode);
        nodes.add(buildEndNodeRequest());
        request.setNodes(nodes);

        this.requestPostWithOk(DEFAULT_UPDATE, request);

        // 校验请求成功数据
        ApprovalFlow updatedFlow = approvalFlowMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), updatedFlow.getName());

        // 校验大字段更新
        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getDescription(), blob.getDescription());

        // 校验节点配置已更新（删除旧节点，插入新节点）
        List<ApprovalNode> updatedNodes = getNodesByFlowId(request.getId());
        Assertions.assertEquals(3, updatedNodes.size());

        // 不修改信息
        ApprovalFlowUpdateRequest emptyRequest = new ApprovalFlowUpdateRequest();
        emptyRequest.setId(addApprovalFlow.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.APPROVAL_FLOW_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(4)
    void testPage() throws Exception {
        ApprovalFlowPageRequest request = new ApprovalFlowPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        // 请求成功
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        List<ApprovalFlowListResponse> pageResult = getPageResult(mvcResult, ApprovalFlowListResponse.class).getList();

        // 校验数据
        Assertions.assertFalse(pageResult.isEmpty());

        // 按名称筛选
        ApprovalFlowPageRequest nameFilterRequest = new ApprovalFlowPageRequest();
        nameFilterRequest.setCurrent(1);
        nameFilterRequest.setPageSize(10);
        nameFilterRequest.setName("更新后的报价审批流");
        MvcResult nameMvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, nameFilterRequest);
        List<ApprovalFlowListResponse> namePageResult = getPageResult(nameMvcResult, ApprovalFlowListResponse.class).getList();

        // 校验筛选结果
        Assertions.assertEquals(1, namePageResult.size());
        Assertions.assertEquals("更新后的报价审批流", namePageResult.get(0).getName());

        // 按表单类型筛选
        ApprovalFlowPageRequest formTypeFilterRequest = new ApprovalFlowPageRequest();
        formTypeFilterRequest.setCurrent(1);
        formTypeFilterRequest.setPageSize(10);
        formTypeFilterRequest.setFormType(ApprovalFormTypeEnum.QUOTATION.getValue());
        MvcResult formTypeMvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, formTypeFilterRequest);
        List<ApprovalFlowListResponse> formTypePageResult = getPageResult(formTypeMvcResult, ApprovalFlowListResponse.class).getList();

        // 校验筛选结果
        Assertions.assertFalse(formTypePageResult.isEmpty());
        formTypePageResult.forEach(flow -> Assertions.assertEquals(ApprovalFormTypeEnum.QUOTATION.getValue(), flow.getFormType()));

        // 校验权限
        requestPostPermissionTest(PermissionConstants.APPROVAL_FLOW_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(5)
    void testGet() throws Exception {
        // 请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addApprovalFlow.getId());
        ApprovalFlowDetailResponse response = getResultData(mvcResult, ApprovalFlowDetailResponse.class);

        ApprovalFlow approvalFlow = approvalFlowMapper.selectByPrimaryKey(addApprovalFlow.getId());

        // 校验基本信息
        Assertions.assertEquals(approvalFlow.getId(), response.getId());
        Assertions.assertEquals(approvalFlow.getName(), response.getName());
        Assertions.assertEquals(approvalFlow.getFormType(), response.getFormType());
        Assertions.assertEquals(approvalFlow.getNumber(), response.getNumber());
        Assertions.assertEquals(approvalFlow.getEnable(), response.getEnable());

        // 校验大字段
        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(addApprovalFlow.getId());
        Assertions.assertEquals(blob.getDescription(), response.getDescription());

        // 校验节点配置
        Assertions.assertFalse(CollectionUtils.isEmpty(response.getNodes()));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.APPROVAL_FLOW_READ, DEFAULT_GET, addApprovalFlow.getId());
    }

    @Test
    @Order(6)
    void testEnable() throws Exception {
        // 启用之前创建的禁用审批流
        ApprovalFlow disabledFlow = getDisabledFlow();
        Assertions.assertNotNull(disabledFlow);
        Assertions.assertFalse(disabledFlow.getEnable());

        // 启用
        String enableUrl = ENABLE + "?enable=true";
        this.requestGetWithOk(enableUrl, disabledFlow.getId());
        ApprovalFlow enabledFlow = approvalFlowMapper.selectByPrimaryKey(disabledFlow.getId());
        Assertions.assertTrue(enabledFlow.getEnable());

        // 禁用
        String disableUrl = ENABLE + "?enable=false";
        this.requestGetWithOk(disableUrl, enabledFlow.getId());
        ApprovalFlow disabledAgainFlow = approvalFlowMapper.selectByPrimaryKey(disabledFlow.getId());
        Assertions.assertFalse(disabledAgainFlow.getEnable());

        // 校验权限
        requestGetPermissionTest(PermissionConstants.APPROVAL_FLOW_UPDATE, enableUrl, disabledFlow.getId());
    }

    @Test
    @Order(7)
    void testPageWithEnableFilter() throws Exception {
        // 筛选启用的审批流
        ApprovalFlowPageRequest request = new ApprovalFlowPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setEnable(true);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        List<ApprovalFlowListResponse> pageResult = getPageResult(mvcResult, ApprovalFlowListResponse.class).getList();

        // 校验结果都是启用的
        pageResult.forEach(flow -> Assertions.assertTrue(flow.getEnable()));

        // 筛选禁用的审批流
        ApprovalFlowPageRequest disabledRequest = new ApprovalFlowPageRequest();
        disabledRequest.setCurrent(1);
        disabledRequest.setPageSize(10);
        disabledRequest.setEnable(false);

        MvcResult disabledMvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, disabledRequest);
        List<ApprovalFlowListResponse> disabledPageResult = getPageResult(disabledMvcResult, ApprovalFlowListResponse.class).getList();

        // 校验结果都是禁用的
        disabledPageResult.forEach(flow -> Assertions.assertFalse(flow.getEnable()));
    }

    @Test
    @Order(20)
    void testDelete() throws Exception {
        // 删除第一个创建的审批流
        this.requestGetWithOk(DEFAULT_DELETE, addApprovalFlow.getId());

        // 校验请求成功数据
        Assertions.assertNull(approvalFlowMapper.selectByPrimaryKey(addApprovalFlow.getId()));

        // 校验大字段也被删除
        Assertions.assertNull(approvalFlowBlobMapper.selectByPrimaryKey(addApprovalFlow.getId()));

        // 校验节点配置也被删除
        Assertions.assertTrue(CollectionUtils.isEmpty(getNodesByFlowId(addApprovalFlow.getId())));

        // 删除另一条创建的审批流
        this.requestGetWithOk(DEFAULT_DELETE, anotherApprovalFlow.getId());
        Assertions.assertNull(approvalFlowMapper.selectByPrimaryKey(anotherApprovalFlow.getId()));

        // 删除禁用的审批流
        ApprovalFlow disabledFlow = getDisabledFlow();
        if (disabledFlow != null) {
            this.requestGetWithOk(DEFAULT_DELETE, disabledFlow.getId());
            Assertions.assertNull(approvalFlowMapper.selectByPrimaryKey(disabledFlow.getId()));
        }

        // 校验权限
        requestGetPermissionTest(PermissionConstants.APPROVAL_FLOW_DELETE, DEFAULT_DELETE, addApprovalFlow.getId());
    }

    /**
     * 获取流程对应的节点列表
     */
    private List<ApprovalNode> getNodesByFlowId(String flowId) {
        ApprovalNode criteria = new ApprovalNode();
        criteria.setFlowId(flowId);
        return approvalNodeMapper.select(criteria);
    }

    /**
     * 获取之前创建的禁用审批流
     */
    private ApprovalFlow getDisabledFlow() {
        ApprovalFlow criteria = new ApprovalFlow();
        criteria.setFormType(ApprovalFormTypeEnum.INVOICE.getValue());
        criteria.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        criteria.setEnable(false);
        List<ApprovalFlow> flows = approvalFlowMapper.select(criteria);
        return flows.isEmpty() ? null : flows.get(0);
    }

    @Test
    @Order(8)
    void testGetStatusPermissionSetting() throws Exception {
        // 请求成功 - 获取报价审批流的状态权限配置
        MvcResult mvcResult = this.requestGetWithOkAndReturn(STATUS_PERMISSION_SETTING, ApprovalFormTypeEnum.QUOTATION.getValue());
        StatusPermissionSettingResponse response = getResultData(mvcResult, StatusPermissionSettingResponse.class);

        // 校验返回数据不为空
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getPermissions());
        Assertions.assertNotNull(response.getStatusPermissions());

        // 校验权限列表不为空
        Assertions.assertFalse(response.getPermissions().isEmpty());

        // 校验权限数据结构正确
        response.getPermissions().forEach(p -> {
            Assertions.assertNotNull(p.getId());
            Assertions.assertNotNull(p.getName());
        });

        // 校验状态权限数据结构正确
        response.getStatusPermissions().forEach(p -> {
            Assertions.assertNotNull(p.getApprovalStatus());
            Assertions.assertNotNull(p.getPermission());
            Assertions.assertNotNull(p.getEnabled());
        });
    }

    @Test
    @Order(9)
    void testGetByFormType() throws Exception {
        // 请求成功 - 根据表单类型获取审批流信息
        MvcResult mvcResult = this.requestGetWithOkAndReturn(GET_BY_FORM_TYPE, ApprovalFormTypeEnum.QUOTATION.getValue());
        ApprovalFlowByFormTypeResponse response = getResultData(mvcResult, ApprovalFlowByFormTypeResponse.class);

        // 校验基本信息
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getId());
        Assertions.assertNotNull(response.getNumber());
        Assertions.assertNotNull(response.getName());
        Assertions.assertEquals(ApprovalFormTypeEnum.QUOTATION.getValue(), response.getFormType());
        Assertions.assertTrue(response.getEnable());

        // 校验大字段
        ApprovalFlowBlob blob = approvalFlowBlobMapper.selectByPrimaryKey(response.getId());
        Assertions.assertNotNull(blob);
        Assertions.assertEquals(blob.getDescription(), response.getDescription());

        // 校验权限列表
        Assertions.assertNotNull(response.getPermissions());
        Assertions.assertFalse(response.getPermissions().isEmpty());

        // 校验状态权限配置
        Assertions.assertNotNull(response.getStatusPermissions());

        // 校验不包含节点配置（ApprovalFlowByFormTypeResponse没有nodes字段）
        Assertions.assertThrows(NoSuchMethodException.class,
                () -> response.getClass().getMethod("getNodes"),
                "Response should not have nodes field");

        // 请求不存在的表单类型，应返回 null
        mvcResult = this.requestGetWithOkAndReturn(GET_BY_FORM_TYPE, "non_existent_form_type");
        ApprovalFlowByFormTypeResponse response2 = getResultData(mvcResult, ApprovalFlowByFormTypeResponse.class);

        Assertions.assertNull(response2);
    }
}