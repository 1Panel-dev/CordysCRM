package cn.cordys.crm.approval;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModel;
import cn.cordys.crm.approval.constants.*;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.StatusPermissionDTO;
import cn.cordys.crm.approval.dto.request.*;
import cn.cordys.crm.approval.dto.response.ApprovalFlowByFormTypeResponse;
import cn.cordys.crm.approval.dto.response.ApprovalFlowDetailResponse;
import cn.cordys.crm.approval.dto.response.ApprovalFlowListResponse;
import cn.cordys.crm.approval.dto.response.StatusPermissionSettingResponse;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.domain.Department;
import cn.cordys.crm.system.domain.DepartmentCommander;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
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
    private BaseMapper<ApprovalFlowVersion> approvalFlowVersionMapper;
    @Resource
    private BaseMapper<ApprovalNode> approvalNodeMapper;
    @Resource
    private BaseMapper<ApprovalNodeApprover> approvalNodeApproverMapper;
    @Resource
    private BaseMapper<ApprovalNodeLink> approvalNodeLinkMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private BaseMapper<Department> departmentMapper;
    @Resource
    private BaseMapper<DepartmentCommander> departmentCommanderMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private BaseMapper<User> userMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    /**
     * 构建简单的审批人节点请求
     */
    private ApprovalNodeApproverRequest buildApproverNodeRequest(String id, String name) {
        ApprovalNodeApproverRequest node = new ApprovalNodeApproverRequest();
        node.setId(id);
        node.setName(name);
        node.setNodeType(ApprovalNodeTypeEnum.APPROVER.name());
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
    private ApprovalNodeRequest buildStartNodeRequest(String id) {
        ApprovalNodeRequest node = new ApprovalNodeRequest();
        node.setId(id);
        node.setName("开始");
        node.setNodeType(ApprovalNodeTypeEnum.START.name());
        return node;
    }

    /**
     * 构建结束节点请求
     */
    private ApprovalNodeRequest buildEndNodeRequest(String id) {
        ApprovalNodeRequest node = new ApprovalNodeRequest();
        node.setId(id);
        node.setName("结束");
        node.setNodeType(ApprovalNodeTypeEnum.END.name());
        return node;
    }

    /**
     * 构建节点连接请求
     */
    private ApprovalNodeLinkRequest buildLinkRequest(String fromNodeId, String toNodeId) {
        ApprovalNodeLinkRequest link = new ApprovalNodeLinkRequest();
        link.setFromNodeId(fromNodeId);
        link.setToNodeId(toNodeId);
        return link;
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
        request.setCreateExecute(false);
        request.setUpdateExecute(true);
        request.setStatusPermissions(buildStatusPermissions());

        // 构建节点配置: 开始 -> 审批人 -> 结束
        String startNodeId = "start_" + System.currentTimeMillis();
        String approverNodeId = "approver_" + System.currentTimeMillis();
        String endNodeId = "end_" + System.currentTimeMillis();

        List<ApprovalNodeRequest> nodes = new ArrayList<>();
        nodes.add(buildStartNodeRequest(startNodeId));
        nodes.add(buildApproverNodeRequest(approverNodeId, "主管审批"));
        nodes.add(buildEndNodeRequest(endNodeId));
        request.setUpdateNodeConfig(new ApprovalFlowNodeConfigRequest());
        request.getUpdateNodeConfig().setNodes(nodes);

        // 构建连接配置: 开始 -> 审批人 -> 结束
        List<ApprovalNodeLinkRequest> links = new ArrayList<>();
        links.add(buildLinkRequest(startNodeId, approverNodeId));
        links.add(buildLinkRequest(approverNodeId, endNodeId));
        request.getUpdateNodeConfig().setLinks(links);

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
        requestPostPermissionTest(PermissionConstants.PROCESS_SETTING_READ, DEFAULT_PAGE, request);
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
        Assertions.assertEquals(request.getDescription(), flow.getDescription());

        // 校验配置字段存储在主表
        Assertions.assertEquals(request.getSubmitterCanRevoke(), flow.getSubmitterCanRevoke());
        Assertions.assertEquals(request.getAllowBatchProcess(), flow.getAllowBatchProcess());
        Assertions.assertEquals(request.getAllowWithdraw(), flow.getAllowWithdraw());
        Assertions.assertEquals(request.getAllowAddSign(), flow.getAllowAddSign());
        Assertions.assertEquals(request.getDuplicateApproverRule(), flow.getDuplicateApproverRule());
        Assertions.assertEquals(request.getRequireComment(), flow.getRequireComment());
        Assertions.assertEquals(request.getCreateExecute(), flow.getCreateExecute());
        Assertions.assertEquals(request.getUpdateExecute(), flow.getUpdateExecute());

        // 校验版本表存在
        ApprovalFlowVersion version = approvalFlowVersionMapper.selectByPrimaryKey(flow.getCurrentVersionId());
        Assertions.assertNotNull(version);

        // 校验节点配置
        List<ApprovalNode> nodes = getNodesByFlowVersionId(flow.getCurrentVersionId());
        Assertions.assertEquals(3, nodes.size());

        // 校验审批人节点配置
        ApprovalNode approverNode = nodes.stream()
                .filter(n -> ApprovalNodeTypeEnum.APPROVER.name().equals(n.getNodeType()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(approverNode);
        ApprovalNodeApprover approverConfig = approvalNodeApproverMapper.selectByPrimaryKey(approverNode.getId());
        Assertions.assertNotNull(approverConfig);

        // 校验节点连接配置
        List<ApprovalNodeLink> links = getLinksByFlowVersionId(flow.getCurrentVersionId());
        Assertions.assertEquals(2, links.size());

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
        requestPostPermissionTest(PermissionConstants.PROCESS_SETTING_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testAddDuplicateType() throws Exception {
        // 测试重复创建同一表单类型的审批流应该失败
        ApprovalFlowAddRequest duplicateRequest = buildAddRequest("重复的报价审批流", ApprovalFormTypeEnum.QUOTATION, true);
        MvcResult result = this.requestPost(DEFAULT_ADD, duplicateRequest).andReturn();
        // 应该返回错误状态码而不是成功
        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(response.contains("该表单类型的审批流已存在") || response.contains("already exists"));
    }

    @Test
    @Order(3)
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
        request.setCreateExecute(true);
        request.setUpdateExecute(false);
        request.setStatusPermissions(buildStatusPermissions());

        // 更新节点配置
        String startNodeId = "start_update_" + System.currentTimeMillis();
        String approverNodeId = "approver_update_" + System.currentTimeMillis();
        String endNodeId = "end_update_" + System.currentTimeMillis();

        List<ApprovalNodeRequest> nodes = new ArrayList<>();
        nodes.add(buildStartNodeRequest(startNodeId));
        ApprovalNodeApproverRequest approverNode = buildApproverNodeRequest(approverNodeId, "经理审批");
        approverNode.setApprovalType(ApprovalTypeEnum.AUTO_PASS.name());
        nodes.add(approverNode);
        nodes.add(buildEndNodeRequest(endNodeId));
        request.setCreateNodeConfig(new ApprovalFlowNodeConfigRequest());
        request.getCreateNodeConfig().setNodes(nodes);

        // 构建连接配置
        List<ApprovalNodeLinkRequest> links = new ArrayList<>();
        links.add(buildLinkRequest(startNodeId, approverNodeId));
        links.add(buildLinkRequest(approverNodeId, endNodeId));
        request.getCreateNodeConfig().setLinks(links);

        this.requestPostWithOk(DEFAULT_UPDATE, request);

        // 校验请求成功数据
        ApprovalFlow updatedFlow = approvalFlowMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), updatedFlow.getName());
        Assertions.assertEquals(request.getDescription(), updatedFlow.getDescription());

        // 校验配置字段存储在主表
        Assertions.assertEquals(request.getSubmitterCanRevoke(), updatedFlow.getSubmitterCanRevoke());
        Assertions.assertEquals(request.getAllowBatchProcess(), updatedFlow.getAllowBatchProcess());
        Assertions.assertEquals(request.getAllowWithdraw(), updatedFlow.getAllowWithdraw());
        Assertions.assertEquals(request.getAllowAddSign(), updatedFlow.getAllowAddSign());
        Assertions.assertEquals(request.getDuplicateApproverRule(), updatedFlow.getDuplicateApproverRule());
        Assertions.assertEquals(request.getRequireComment(), updatedFlow.getRequireComment());

        // 校验更新后产生了新版本
        Assertions.assertNotEquals(addApprovalFlow.getCurrentVersionId(), updatedFlow.getCurrentVersionId());

        // 校验节点配置已更新（删除旧节点，插入新节点）
        List<ApprovalNode> updatedNodes = getNodesByFlowVersionId(updatedFlow.getCurrentVersionId());
        Assertions.assertEquals(3, updatedNodes.size());

        // 不修改信息
        ApprovalFlowUpdateRequest emptyRequest = new ApprovalFlowUpdateRequest();
        emptyRequest.setId(addApprovalFlow.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.PROCESS_SETTING_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(5)
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
        requestPostPermissionTest(PermissionConstants.PROCESS_SETTING_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(6)
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
        Assertions.assertEquals(approvalFlow.getDescription(), response.getDescription());

        // 校验配置字段从主表获取
        Assertions.assertEquals(approvalFlow.getSubmitterCanRevoke(), response.getSubmitterCanRevoke());
        Assertions.assertEquals(approvalFlow.getAllowBatchProcess(), response.getAllowBatchProcess());
        Assertions.assertEquals(approvalFlow.getAllowWithdraw(), response.getAllowWithdraw());
        Assertions.assertEquals(approvalFlow.getAllowAddSign(), response.getAllowAddSign());
        Assertions.assertEquals(approvalFlow.getDuplicateApproverRule(), response.getDuplicateApproverRule());
        Assertions.assertEquals(approvalFlow.getRequireComment(), response.getRequireComment());

        // 校验节点配置
        Assertions.assertFalse(CollectionUtils.isEmpty(response.getCreateNodeConfig().getNodes()));

        // 校验连接配置
        Assertions.assertFalse(CollectionUtils.isEmpty(response.getCreateNodeConfig().getLinks()));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.PROCESS_SETTING_READ, DEFAULT_GET, addApprovalFlow.getId());
    }

    @Test
    @Order(7)
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
        requestGetPermissionTest(PermissionConstants.PROCESS_SETTING_UPDATE, enableUrl, disabledFlow.getId());
    }

    @Test
    @Order(8)
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

        // 校验软删除：记录仍存在但 deleted = true
        ApprovalFlow deletedFlow = approvalFlowMapper.selectByPrimaryKey(addApprovalFlow.getId());
        Assertions.assertNotNull(deletedFlow);
        Assertions.assertTrue(deletedFlow.getDeleted());

        // 删除另一条创建的审批流
        this.requestGetWithOk(DEFAULT_DELETE, anotherApprovalFlow.getId());
        ApprovalFlow deletedAnother = approvalFlowMapper.selectByPrimaryKey(anotherApprovalFlow.getId());
        Assertions.assertNotNull(deletedAnother);
        Assertions.assertTrue(deletedAnother.getDeleted());

        // 删除禁用的审批流
        ApprovalFlow disabledFlow = getDisabledFlow();
        if (disabledFlow != null) {
            this.requestGetWithOk(DEFAULT_DELETE, disabledFlow.getId());
            ApprovalFlow deletedDisabled = approvalFlowMapper.selectByPrimaryKey(disabledFlow.getId());
            Assertions.assertNotNull(deletedDisabled);
            Assertions.assertTrue(deletedDisabled.getDeleted());
        }

        // 校验权限
        requestGetPermissionTest(PermissionConstants.PROCESS_SETTING_DELETE, DEFAULT_DELETE, addApprovalFlow.getId());
    }

    /**
     * 获取版本对应的节点列表
     */
    private List<ApprovalNode> getNodesByFlowVersionId(String flowVersionId) {
        ApprovalNode criteria = new ApprovalNode();
        criteria.setFlowVersionId(flowVersionId);
        return approvalNodeMapper.select(criteria);
    }

    /**
     * 获取版本对应的节点连接列表
     */
    private List<ApprovalNodeLink> getLinksByFlowVersionId(String flowVersionId) {
        ApprovalNodeLink criteria = new ApprovalNodeLink();
        criteria.setFlowVersionId(flowVersionId);
        return approvalNodeLinkMapper.select(criteria);
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
    @Order(9)
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
    @Order(10)
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
        Assertions.assertNotNull(response.getDescription());

        // 校验配置字段从主表获取
        ApprovalFlow flow = approvalFlowMapper.selectByPrimaryKey(response.getId());
        Assertions.assertNotNull(flow);
        Assertions.assertEquals(flow.getSubmitterCanRevoke(), response.getSubmitterCanRevoke());
        Assertions.assertEquals(flow.getAllowBatchProcess(), response.getAllowBatchProcess());
        Assertions.assertEquals(flow.getAllowWithdraw(), response.getAllowWithdraw());
        Assertions.assertEquals(flow.getAllowAddSign(), response.getAllowAddSign());
        Assertions.assertEquals(flow.getDuplicateApproverRule(), response.getDuplicateApproverRule());
        Assertions.assertEquals(flow.getRequireComment(), response.getRequireComment());

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

    @Test
    @Order(11)
    void testResolveMultipleDeptHeadApproversWithDirection() {
        prepareMultipleDeptHeadData();

        List<User> bottomUpApprovers = approvalFlowService.resolveApprovers(
                "amd_submit_user",
                DEFAULT_ORGANIZATION_ID,
                ApproverTypeEnum.MULTIPLE_DEPT_HEAD,
                List.of("2"),
                ApproverDirectionEnum.BOTTOM_UP
        );
        Assertions.assertEquals(List.of("amd_child_head", "amd_parent_head"),
                bottomUpApprovers.stream().map(User::getId).toList());

        List<User> topDownApprovers = approvalFlowService.resolveApprovers(
                "amd_submit_user",
                DEFAULT_ORGANIZATION_ID,
                ApproverTypeEnum.MULTIPLE_DEPT_HEAD,
                List.of("2"),
                ApproverDirectionEnum.TOP_DOWN
        );
        Assertions.assertEquals(List.of("amd_root_head", "amd_parent_head"),
                topDownApprovers.stream().map(User::getId).toList());
    }

    @Test
    @Order(12)
    void testResolveMultipleDeptHeadApproversShouldKeepEmptyDepartmentLevel() {
        prepareMultipleDeptHeadDataWithoutChildCommander();

        List<User> approvers = approvalFlowService.resolveApprovers(
                "amd2_submit_user",
                DEFAULT_ORGANIZATION_ID,
                ApproverTypeEnum.MULTIPLE_DEPT_HEAD,
                List.of("2"),
                ApproverDirectionEnum.BOTTOM_UP
        );

        Assertions.assertEquals(List.of("amd2_parent_head"), approvers.stream().map(User::getId).toList());
    }

    private void prepareMultipleDeptHeadData() {
        insertDepartment("amd_root", "审批测试根部门", "0");
        insertDepartment("amd_parent", "审批测试父部门", "amd_root");
        insertDepartment("amd_child", "审批测试子部门", "amd_parent");

        insertUser("amd_child_head", "子部门负责人");
        insertUser("amd_parent_head", "父部门负责人");
        insertUser("amd_root_head", "根部门负责人");
        insertUser("amd_submit_user", "部门负责人提交人");

        insertOrganizationUser("amd_child_head-org", "amd_child_head", "amd_child");
        insertOrganizationUser("amd_parent_head-org", "amd_parent_head", "amd_parent");
        insertOrganizationUser("amd_root_head-org", "amd_root_head", "amd_root");
        insertOrganizationUser("amd_submit_org", "amd_submit_user", "amd_child");

        insertDepartmentCommander("amd_child-commander", "amd_child", "amd_child_head");
        insertDepartmentCommander("amd_parent-commander", "amd_parent", "amd_parent_head");
        insertDepartmentCommander("amd_root-commander", "amd_root", "amd_root_head");
    }

    private void prepareMultipleDeptHeadDataWithoutChildCommander() {
        insertDepartment("amd2_root", "审批测试根部门2", "0");
        insertDepartment("amd2_parent", "审批测试父部门2", "amd2_root");
        insertDepartment("amd2_child", "审批测试子部门2", "amd2_parent");

        insertUser("amd2_parent_head", "父部门负责人2");
        insertUser("amd2_root_head", "根部门负责人2");
        insertUser("amd2_submit_user", "部门负责人提交人2");

        insertOrganizationUser("amd2_parent_head-org", "amd2_parent_head", "amd2_parent");
        insertOrganizationUser("amd2_root_head-org", "amd2_root_head", "amd2_root");
        insertOrganizationUser("amd2_submit_org", "amd2_submit_user", "amd2_child");

        insertDepartmentCommander("amd2_parent-cmd", "amd2_parent", "amd2_parent_head");
        insertDepartmentCommander("amd2_root-cmd", "amd2_root", "amd2_root_head");
    }

    private void insertDepartment(String id, String name, String parentId) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        department.setParentId(parentId);
        department.setPos(1L);
        department.setResource("TEST");
        department.setResourceId(id + "-resource");
        setAuditFields(department);
        departmentMapper.insert(department);
    }

    private void insertDepartmentCommander(String id, String departmentId, String userId) {
        DepartmentCommander commander = new DepartmentCommander();
        commander.setId(id);
        commander.setDepartmentId(departmentId);
        commander.setUserId(userId);
        setAuditFields(commander);
        departmentCommanderMapper.insert(commander);
    }

    private void insertOrganizationUser(String id, String userId, String departmentId) {
        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setId(id);
        organizationUser.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        organizationUser.setDepartmentId(departmentId);
        organizationUser.setUserId(userId);
        organizationUser.setEnable(true);
        setAuditFields(organizationUser);
        organizationUserMapper.insert(organizationUser);
    }

    private void insertUser(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword("123456");
        user.setGender(false);
        setAuditFields(user);
        userMapper.insert(user);
    }

    private void setAuditFields(BaseModel model) {
        long now = System.currentTimeMillis();
        model.setCreateUser("admin");
        model.setUpdateUser("admin");
        model.setCreateTime(now);
        model.setUpdateTime(now);
    }
}
