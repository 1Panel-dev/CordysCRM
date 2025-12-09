package cn.cordys.crm.opportunity.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.dto.RolePermissionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.domain.ContractField;
import cn.cordys.crm.contract.domain.ContractFieldBlob;
import cn.cordys.crm.opportunity.constants.ApprovalState;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.domain.OpportunityQuotationApproval;
import cn.cordys.crm.opportunity.domain.OpportunityQuotationSnapshot;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationAddRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationBatchRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationEditRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationGetResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationListResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationSnapshotMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.BatchAffectSkipResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityQuotationService {

    @Resource
    private OpportunityQuotationFieldService opportunityQuotationFieldService;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private LogService logService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private ExtOpportunityQuotationMapper extOpportunityQuotationMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private BaseMapper<OpportunityQuotation> opportunityQuotationMapper;
    @Resource
    private BaseMapper<OpportunityQuotationSnapshot> snapshotBaseMapper;
    @Resource
    private BaseMapper<ContractField> contractFieldMapper;
    @Resource
    private BaseMapper<ContractFieldBlob> contractFieldBlobMapper;
    @Resource
    private BaseMapper<OpportunityQuotationApproval> approvalBaseMapper;
    @Resource
    private BaseMapper<Opportunity> opportunityBaseMapper;
    @Resource
    private ExtOpportunityQuotationSnapshotMapper extOpportunityQuotationSnapshotMapper;


    /**
     * 新增商机报价单
     * 新增报价单会自动将报价单状态设置为“提审”，此时需要保存报价单值快照，报价单表单设置快照
     *
     * @param request 新增请求参数
     * @return 商机报价单实体
     */
    @OperationLog(module = LogModule.OPPORTUNITY_QUOTATION, type = LogType.ADD, resourceName = "{#request.name}", operator = "{#userId}")
    public OpportunityQuotation add(OpportunityQuotationAddRequest request, String orgId, String userId) {
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("opportunity.quotation.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("opportunity.quotation.form.config.required"));
        }
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        OpportunityQuotation opportunityQuotation = new OpportunityQuotation();
        opportunityQuotation.setId(IDGenerator.nextStr());
        opportunityQuotation.setOrganizationId(orgId);
        opportunityQuotation.setName(request.getName());
        opportunityQuotation.setApprovalStatus(ApprovalState.APPROVING.toString());
        opportunityQuotation.setOpportunityId(request.getOpportunityId());
        opportunityQuotation.setCreateUser(userId);
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setCreateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        //计算子产品总金额
        setAmount(request.getProducts(), opportunityQuotation);
        // 设置子表格字段值
        moduleFields.add(new BaseModuleFieldValue("products", request.getProducts()));

        opportunityQuotationFieldService.saveModuleField(opportunityQuotation, orgId, userId, moduleFields, false);
        opportunityQuotationMapper.insert(opportunityQuotation);

        // 保存表单配置快照
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, opportunityQuotationFieldService);
        OpportunityQuotationGetResponse response = getOpportunityQuotationGetResponse(opportunityQuotation, resolveFieldValues, moduleFormConfigDTO);

        baseService.handleAddLogWithSubTable(opportunityQuotation, moduleFields, "products", Translator.get("products_info"), moduleFormConfigDTO);

        saveSnapshot(opportunityQuotation, saveModuleFormConfigDTO, response);

        //保存报价单审批表
        addQuotationApproval(userId, opportunityQuotation.getId());

        return opportunityQuotation;

    }

    /**
     * 计算子产品总金额
     *
     * @param products             子产品列表
     * @param opportunityQuotation 报价单实体
     */
    private void setAmount(List<Map<String, Object>> products, OpportunityQuotation opportunityQuotation) {
        BigDecimal totalAmount = products.stream()
                .map(product -> new BigDecimal(product.get("amount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        opportunityQuotation.setAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 新增报价单审批表
     *
     * @param userId      用户ID
     * @param quotationId 报价单ID
     */
    private void addQuotationApproval(String userId, String quotationId) {
        OpportunityQuotationApproval opportunityQuotationApproval = new OpportunityQuotationApproval();
        opportunityQuotationApproval.setId(IDGenerator.nextStr());
        opportunityQuotationApproval.setQuotationId(quotationId);
        opportunityQuotationApproval.setApprovalStatus(ApprovalState.APPROVING.toString());
        opportunityQuotationApproval.setCreateUser(userId);
        opportunityQuotationApproval.setUpdateUser(userId);
        opportunityQuotationApproval.setCreateTime(System.currentTimeMillis());
        opportunityQuotationApproval.setUpdateTime(System.currentTimeMillis());
        approvalBaseMapper.insert(opportunityQuotationApproval);
    }

    /**
     * 保存商机报价单快照
     *
     * @param opportunityQuotation 报价单实体
     * @param moduleFormConfigDTO  报价单表单配置
     * @param response             报价单详情响应类
     */
    private void saveSnapshot(OpportunityQuotation opportunityQuotation, ModuleFormConfigDTO moduleFormConfigDTO, OpportunityQuotationGetResponse response) {
        OpportunityQuotationSnapshot snapshot = new OpportunityQuotationSnapshot();
        response.setModuleFields(response.getModuleFields().stream()
                .filter(field -> (field.getFieldValue() != null && StringUtils.isNotBlank(field.getFieldValue().toString()) && !"[]".equals(field.getFieldValue().toString()))).toList());
        snapshot.setId(IDGenerator.nextStr());
        snapshot.setQuotationId(opportunityQuotation.getId());
        snapshot.setQuotationProp(JSON.toJSONString(moduleFormConfigDTO));
        snapshot.setQuotationValue(JSON.toJSONString(response));
        snapshotBaseMapper.insert(snapshot);
    }

    /**
     * 新增商机报价单详情
     *
     * @param opportunityQuotation 报价单实体
     * @param moduleFields         报价单字段值
     * @param moduleFormConfigDTO  报价单表单配置
     * @return 报价单详情
     */
    private OpportunityQuotationGetResponse getOpportunityQuotationGetResponse(OpportunityQuotation opportunityQuotation, List<BaseModuleFieldValue> moduleFields, ModuleFormConfigDTO moduleFormConfigDTO) {
        OpportunityQuotationGetResponse response = BeanUtils.copyBean(new OpportunityQuotationGetResponse(), opportunityQuotation);
        Opportunity opportunity = opportunityBaseMapper.selectByPrimaryKey(response.getOpportunityId());
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(moduleFormConfigDTO, moduleFields);
        optionMap.put("opportunityId", List.of(new OptionDTO(opportunity.getId(), opportunity.getName())));
        response.setOptionMap(optionMap);
        response.setOpportunityName(opportunity.getName());
        Map<String, List<Attachment>> attachmentMap = moduleFormService.getAttachmentMap(moduleFormConfigDTO, moduleFields);
        response.setAttachmentMap(attachmentMap);
        moduleFormService.processBusinessFieldValues(response, moduleFields, moduleFormConfigDTO);
        return baseService.setCreateAndUpdateUserName(response);
    }

    /**
     * 查询商机报价单快照详情
     *
     * @param id 报价单ID
     * @return 报价单详情
     */
    public OpportunityQuotationGetResponse getSnapshot(String id) {
        OpportunityQuotationGetResponse response = new OpportunityQuotationGetResponse();
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        // 已审核，查询最新快照
        LambdaQueryWrapper<OpportunityQuotationSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        OpportunityQuotationSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getQuotationValue(), OpportunityQuotationGetResponse.class);
        }
        return response;
    }

    /**
     * 查询商机报价单详情
     *
     * @param id 报价单ID
     * @return 报价单详情
     */
    public OpportunityQuotationGetResponse get(String id) {
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        // 未审核，查询当前值
        List<BaseModuleFieldValue> moduleFields = opportunityQuotationFieldService.getModuleFieldValuesByResourceId(id);
        ModuleFormConfigDTO moduleFormConfigDTO = moduleFormService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), opportunityQuotation.getOrganizationId());
        return getOpportunityQuotationGetResponse(opportunityQuotation, moduleFields, moduleFormConfigDTO);
    }

    /**
     * 撤销审批
     *
     * @param id     报价单ID
     * @param userId 用户ID
     */
    public String revoke(String id, String userId) {
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        if (!Strings.CI.equals(opportunityQuotation.getCreateUser(), userId) || !Strings.CI.equals(opportunityQuotation.getApprovalStatus(), ApprovalState.APPROVING.toString())) {
            return opportunityQuotation.getApprovalStatus();
        }
        opportunityQuotation.setApprovalStatus(ApprovalState.REVOKED.toString());
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotationMapper.update(opportunityQuotation);

        //更新报价单审批表
        updateQuotationApproval(userId, id, ApprovalState.REVOKED.toString());


        return opportunityQuotation.getApprovalStatus();
    }

    /**
     * 作废商机报价单
     *
     * @param id     报价单ID
     * @param userId 用户ID
     */
    public String voidQuotation(String id, String userId, String orgId) {
        OpportunityQuotation oldOpportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (oldOpportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        String oldApprovalStatus = oldOpportunityQuotation.getApprovalStatus();
        OpportunityQuotation opportunityQuotation = updateApprovalState(oldOpportunityQuotation, LogType.VOIDED, userId);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        checkQuotationLinked(id, "opportunity.quotation.no.voided");
        saveSateChangeLog(orgId, oldApprovalStatus, userId, LogType.VOIDED, opportunityQuotation);
        //增加通知
        sendNotice(Translator.get("opportunity.quotation.status.voided"), opportunityQuotation, userId, orgId, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL);
        return opportunityQuotation.getApprovalStatus();
    }

    /**
     * 检查报价单是否被合同关联
     *
     * @param id  id
     * @param key 提示词
     */
    private void checkQuotationLinked(String id, String key) {
        LambdaQueryWrapper<ContractField> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ContractField::getFieldValue, id);
        List<ContractField> contractFieldList = contractFieldMapper.selectListByLambda(wrapper);
        LambdaQueryWrapper<ContractFieldBlob> wrapperBlob = new LambdaQueryWrapper<>();
        wrapperBlob.like(ContractFieldBlob::getFieldValue, id);
        List<ContractFieldBlob> contractFieldBlobList = contractFieldBlobMapper.selectListByLambda(wrapperBlob);

        if (CollectionUtils.isNotEmpty(contractFieldList) || CollectionUtils.isNotEmpty(contractFieldBlobList)) {
            throw new GenericException(Translator.get(key));
        }
    }

    /**
     * 审批商机报价单
     *
     * @param request 新增请求参数
     * @param userId  用户ID
     */
    public String approve(OpportunityQuotationEditRequest request, String userId, String orgId) {
        //获取ApprovalState中APPROVED状态的id属性(以后改成获取自定义的审批状态)
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        List<String> approvalStatusList = Arrays.stream(ApprovalState.values()).map(ApprovalState::getId).filter(status -> ApprovalState.APPROVED.toString().equals(status)).toList();
        String noticeKey = approvalStatusList.contains(request.getApprovalStatus()) ?
                Translator.get("opportunity.quotation.status.approved") : Translator.get("opportunity.quotation.status.unapproved");
        OpportunityQuotation oldOpportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(request.getId());
        if (oldOpportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        String oldApprovalStatus = oldOpportunityQuotation.getApprovalStatus();
        OpportunityQuotation opportunityQuotation = updateApprovalState(oldOpportunityQuotation, request.getApprovalStatus(), userId);
        if (opportunityQuotation == null) {
            return request.getApprovalStatus();
        }
        //更新快照
        String id = request.getId();
        LambdaQueryWrapper<OpportunityQuotationSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        List<OpportunityQuotationSnapshot> opportunityQuotationSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
        OpportunityQuotationSnapshot first = opportunityQuotationSnapshots.getFirst();
        if (first != null) {
            OpportunityQuotationGetResponse response = JSON.parseObject(first.getQuotationValue(), OpportunityQuotationGetResponse.class);
            response.setApprovalStatus(request.getApprovalStatus());
            first.setQuotationProp(JSON.toJSONString(moduleFormConfigDTO));
            first.setQuotationValue(JSON.toJSONString(response));
            snapshotBaseMapper.update(first);
        }
        saveSateChangeLog(orgId, oldApprovalStatus, userId, LogType.APPROVAL, opportunityQuotation);
        sendNotice(noticeKey, opportunityQuotation, userId, orgId, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL);
        return opportunityQuotation.getApprovalStatus();
    }

    /**
     * 更新审批状态
     *
     * @param opportunityQuotation 报价单实体
     * @param approvalStatus       审批状态
     * @param userId               用户ID
     * @return 报价单
     */
    private OpportunityQuotation updateApprovalState(OpportunityQuotation opportunityQuotation, String approvalStatus, String userId) {
        if ((Strings.CI.equals(approvalStatus, ApprovalState.APPROVED.toString()) || Strings.CI.equals(approvalStatus, ApprovalState.UNAPPROVED.toString())) && !Strings.CI.equals(opportunityQuotation.getApprovalStatus(), ApprovalState.APPROVING.toString())) {
            return null;
        }
        opportunityQuotation.setApprovalStatus(approvalStatus);
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotationMapper.update(opportunityQuotation);
        return opportunityQuotation;
    }

    /**
     * 发送通知
     *
     * @param key                  通知内容key
     * @param opportunityQuotation 报价单实体
     * @param userId               用户ID
     * @param orgId                组织ID
     * @param event                事件类型
     */
    private void sendNotice(String key, OpportunityQuotation opportunityQuotation, String userId, String orgId, String event) {
        Map<String, Object> paramMap = new HashMap<>();
        if (StringUtils.isNotBlank(key)) {
            paramMap.put("state", key);
        }
        paramMap.put("name", opportunityQuotation.getName());
        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY, event,
                paramMap, userId, orgId, List.of(opportunityQuotation.getCreateUser()), true);
    }

    /**
     * 保存状态变更日志
     *
     * @param orgId                组织ID
     * @param state                审批状态
     * @param userId               用户ID
     * @param logType              日志类型
     * @param opportunityQuotation 报价单实体
     */
    private void saveSateChangeLog(String orgId, String state, String userId, String logType, OpportunityQuotation opportunityQuotation) {
        LogDTO logDTO = new LogDTO(orgId, opportunityQuotation.getId(), userId, logType, LogModule.OPPORTUNITY_QUOTATION, opportunityQuotation.getName());
        if (state == null) {
            logDTO.setOriginalValue(opportunityQuotation.getName());
        } else {
            Map<String, String> oldMap = new HashMap<>();
            oldMap.put("approvalStatus", state);
            logDTO.setOriginalValue(oldMap);
            Map<String, String> newMap = new HashMap<>();
            newMap.put("approvalStatus", opportunityQuotation.getApprovalStatus());
            logDTO.setModifiedValue(newMap);
        }
        logService.add(logDTO);
    }

    /**
     * 删除商机报价单
     *
     * @param id             报价单ID
     * @param userId         用户ID
     * @param organizationId 组织ID
     */
    public void delete(String id, String userId, String organizationId) {
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            return;
        }
        checkQuotationLinked(id, "opportunity.quotation.already.associated");
        opportunityQuotationFieldService.deleteByResourceId(id);
        opportunityQuotationMapper.deleteByPrimaryKey(id);

        //删除快照
        LambdaQueryWrapper<OpportunityQuotationSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        snapshotBaseMapper.deleteByLambda(wrapper);

        //删除审批记录
        LambdaQueryWrapper<OpportunityQuotationApproval> approvalWrapper = new LambdaQueryWrapper<>();
        approvalWrapper.eq(OpportunityQuotationApproval::getQuotationId, id);
        approvalBaseMapper.deleteByLambda(approvalWrapper);

        //记录日志
        saveSateChangeLog(organizationId, null, userId, LogType.DELETE, opportunityQuotation);

        //发送通知
        sendNotice(null, opportunityQuotation, userId, organizationId, NotificationConstants.Event.BUSINESS_QUOTATION_DELETED);
    }

    /**
     * 商机报价单列表
     *
     * @param request        列表请求参数
     * @param organizationId 组织ID
     * @return 商机报价单列表
     */
    public PagerWithOption<List<OpportunityQuotationListResponse>> list(OpportunityQuotationPageRequest request, String organizationId, String userId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<OpportunityQuotationListResponse> list = extOpportunityQuotationMapper.list(request, organizationId, userId, deptDataPermission);
        List<OpportunityQuotationListResponse> results = buildList(list);
        // 处理自定义字段选项
        ModuleFormConfigDTO moduleFormConfigDTO = moduleFormCacheService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), organizationId);
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(results, OpportunityQuotationListResponse::getModuleFields);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(moduleFormConfigDTO, moduleFieldValues);
        return PageUtils.setPageInfoWithOption(page, results, optionMap);
    }

    /**
     * 构建列表数据
     *
     * @param listData 列表数据
     * @return 列表数据
     */
    private List<OpportunityQuotationListResponse> buildList(List<OpportunityQuotationListResponse> listData) {
        // 查询列表数据的自定义字段
        Map<String, List<BaseModuleFieldValue>> dataFieldMap = opportunityQuotationFieldService.getResourceFieldMap(
                listData.stream().map(OpportunityQuotationListResponse::getId).toList(), true);
        // 列表项设置自定义字段&&用户名
        listData.forEach(item -> item.setModuleFields(dataFieldMap.get(item.getId())));
        return baseService.setCreateAndUpdateUserName(listData);
    }

    /**
     * 更新商机报价单
     *
     * @param request 更新请求参数
     * @param userId  更新用户ID
     * @param orgId   组织ID
     * @return 更新后的报价单实体
     */
    @OperationLog(module = LogModule.OPPORTUNITY_QUOTATION, type = LogType.UPDATE, resourceName = "{#request.name}", operator = "{#userId}")
    public OpportunityQuotation update(OpportunityQuotationEditRequest request, String userId, String orgId) {
        String id = request.getId();
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("opportunity.quotation.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("opportunity.quotation.form.config.required"));
        }
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);

        OpportunityQuotation oldOpportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (oldOpportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        List<BaseModuleFieldValue> originFields = new ArrayList<>();
        OpportunityQuotation opportunityQuotation = BeanUtils.copyBean(new OpportunityQuotation(), request);
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setCreateTime(oldOpportunityQuotation.getCreateTime());
        opportunityQuotation.setCreateUser(oldOpportunityQuotation.getCreateUser());
        opportunityQuotation.setApprovalStatus(ApprovalState.APPROVING.toString());
        setAmount(request.getProducts(), opportunityQuotation);
        // 设置子表格字段值
        moduleFields.add(new BaseModuleFieldValue("products", request.getProducts()));
        updateFields(moduleFields, opportunityQuotation, orgId, userId);
        opportunityQuotationMapper.update(opportunityQuotation);
        //更新报价单审批表
        updateQuotationApproval(userId, id, ApprovalState.APPROVING.toString());
        //删除快照
        LambdaQueryWrapper<OpportunityQuotationSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        List<OpportunityQuotationSnapshot> opportunityQuotationSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
        if (CollectionUtils.isNotEmpty(opportunityQuotationSnapshots)) {
            OpportunityQuotationSnapshot first = opportunityQuotationSnapshots.getFirst();
            if (first != null) {
                OpportunityQuotationGetResponse response = JSON.parseObject(first.getQuotationValue(), OpportunityQuotationGetResponse.class);
                List<BaseModuleFieldValue> moduleFields1 = response.getModuleFields();
                moduleFields1.add(new BaseModuleFieldValue("products", response.getProducts()));
                originFields.addAll(moduleFields1);
            }
        }
        snapshotBaseMapper.deleteByLambda(delWrapper);
        //保存快照
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, opportunityQuotationFieldService);
        OpportunityQuotationGetResponse response = getOpportunityQuotationGetResponse(opportunityQuotation, resolveFieldValues, moduleFormConfigDTO);
        saveSnapshot(opportunityQuotation, saveModuleFormConfigDTO, response);
        // 处理日志上下文
        baseService.handleUpdateLogWithSubTable(oldOpportunityQuotation, opportunityQuotation, originFields, moduleFields, id, opportunityQuotation.getName(), "products", Translator.get("products_info"), moduleFormConfigDTO);
        return opportunityQuotationMapper.selectByPrimaryKey(id);
    }

    /**
     * 更新报价单审批状态
     *
     * @param userId      更新用户ID
     * @param quotationId 报价单ID
     */
    private void updateQuotationApproval(String userId, String quotationId, String approvalStatus) {
        LambdaQueryWrapper<OpportunityQuotationApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpportunityQuotationApproval::getQuotationId, quotationId);
        List<OpportunityQuotationApproval> approvalList = approvalBaseMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(approvalList)) {
            addQuotationApproval(userId, quotationId);
        } else {
            updateQuotationApprovalState(userId, approvalList.getFirst(), approvalStatus);
        }
    }

    /**
     * 更新报价单审批状态
     *
     * @param userId            更新用户ID
     * @param quotationApproval 报价单审批实体
     * @param approvalStatus    审批状态
     */
    private void updateQuotationApprovalState(String userId, OpportunityQuotationApproval quotationApproval, String approvalStatus) {
        quotationApproval.setApprovalStatus(approvalStatus);
        quotationApproval.setUpdateTime(System.currentTimeMillis());
        quotationApproval.setUpdateUser(userId);
        approvalBaseMapper.update(quotationApproval);
    }

    /**
     * 更新自定义字段
     *
     * @param fields               自定义字段集合
     * @param opportunityQuotation 报价单实体
     * @param orgId                当前组织
     * @param userId               当前用户
     */
    private void updateFields(List<BaseModuleFieldValue> fields, OpportunityQuotation opportunityQuotation, String orgId, String userId) {
        if (fields == null) {
            return;
        }
        opportunityQuotationFieldService.deleteByResourceId(opportunityQuotation.getId());
        opportunityQuotationFieldService.saveModuleField(opportunityQuotation, orgId, userId, fields, true);
    }

    /**
     * 获取商机报价单模块标签页启用配置
     *
     * @param userId 用户ID
     * @param orgId  组织ID
     * @return 模块标签页启用配置
     */
    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.OPPORTUNITY_QUOTATION_READ, rolePermissions);
    }


    /**
     * 批量审批商机报价单
     *
     * @param request 批量审批请求参数
     * @param userId  用户ID
     * @param orgId   组织ID
     * @return 审批状态
     */
    public BatchAffectSkipResponse batchApprove(OpportunityQuotationBatchRequest request, String userId, String orgId) {
        List<String> ids = request.getIds();
        String approvalStatus = request.getApprovalStatus();
        LambdaQueryWrapper<OpportunityQuotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OpportunityQuotation::getId, ids);
        List<OpportunityQuotation> list = opportunityQuotationMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return BatchAffectSkipResponse.builder().success(0).fail(0).skip(0).build();
        }
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtOpportunityQuotationMapper batchUpdateMapper = sqlSession.getMapper(ExtOpportunityQuotationMapper.class);

        List<LogDTO> logs = new ArrayList<>();
        List<String> approvingIds = new ArrayList<>();
        AtomicInteger skipCount = new AtomicInteger(0);
        list.stream().filter(item -> {
            if (!Strings.CI.equals(item.getApprovalStatus(), ApprovalState.APPROVING.toString())) {
                skipCount.getAndIncrement();
                return false;
            }
            return true;
        }).forEach(item -> {
            approvingIds.add(item.getId());
            var log = new LogDTO(
                    orgId,
                    item.getId(),
                    userId,
                    LogType.APPROVAL,
                    LogModule.OPPORTUNITY_QUOTATION,
                    item.getName()
            );
            Map<String, String> oldMap = new HashMap<>();
            oldMap.put("approvalStatus", item.getApprovalStatus());
            log.setOriginalValue(oldMap);
            Map<String, String> newMap = new HashMap<>();
            newMap.put("approvalStatus", approvalStatus);
            log.setModifiedValue(newMap);
            logs.add(log);
            batchUpdateMapper.updateApprovalStatus(item.getId(), approvalStatus, userId, System.currentTimeMillis());
        });

        //批量修改报价单快照
        List<OpportunityQuotationSnapshot> opportunityQuotationSnapshots = snapshotBaseMapper.selectByIds(approvingIds);
        for (OpportunityQuotationSnapshot snapshot : opportunityQuotationSnapshots) {
            OpportunityQuotationGetResponse response = JSON.parseObject(snapshot.getQuotationValue(), OpportunityQuotationGetResponse.class);
            response.setApprovalStatus(approvalStatus);
            snapshot.setQuotationValue(JSON.toJSONString(response));
            extOpportunityQuotationSnapshotMapper.update(snapshot);

        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
        logService.batchAdd(logs);
        list.forEach(
                item -> sendNotice(Translator.get(Strings.CI.equals(approvalStatus, ApprovalState.APPROVED.toString()) ?
                        Translator.get("opportunity.quotation.status.approved") : Translator.get("opportunity.quotation.status.unapproved")), item, userId, orgId, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL)
        );
        return BatchAffectSkipResponse.builder().success(list.size() - skipCount.get()).fail(0).skip(skipCount.get()).build();
    }


    /**
     * 批量作废商机报价单
     *
     * @param request        批量作废请求参数
     * @param userId         用户ID
     * @param organizationId 组织ID
     * @return 批量作废响应参数
     */
    public BatchAffectReasonResponse batchVoidQuotation(OpportunityQuotationBatchRequest request, String userId, String organizationId) {
        List<String> ids = request.getIds();
        LambdaQueryWrapper<OpportunityQuotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OpportunityQuotation::getId, ids);
        List<OpportunityQuotation> list = opportunityQuotationMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0).errorMessages(Translator.get("opportunity.quotation.not.exist")).build();
        }


        // 校验商机报价单是否可以作废
        List<OpportunityQuotation> validateList = validateVoidQuotation(list);
        if (CollectionUtils.isEmpty(validateList)) {
            return BatchAffectReasonResponse.builder().success(0).fail(list.size()).skip(0).errorMessages(Translator.get("opportunity.quotation.batch.no.voided")).build();
        }

        List<LogDTO> logs = new ArrayList<>();
        List<String> successIds = new ArrayList<>();
        List<OpportunityQuotation> successList = new ArrayList<>();
        AtomicInteger skipCount = new AtomicInteger();
        validateList.stream().filter(
                item -> {
                    if (Strings.CI.equals(item.getApprovalStatus(), LogType.VOIDED)) {
                        skipCount.getAndIncrement();
                        return false;
                    }
                    return true;
                }
        ).forEach(item -> {
            successList.add(item);
            successIds.add(item.getId());
            var log = new LogDTO(
                    organizationId,
                    item.getId(),
                    userId,
                    LogType.VOIDED,
                    LogModule.OPPORTUNITY_QUOTATION,
                    item.getName()
            );
            log.setOriginalValue(item.getApprovalStatus());
            log.setModifiedValue(LogType.VOIDED);
            logs.add(log);

        });
        if (CollectionUtils.isNotEmpty(successIds)) {
            SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
            ExtOpportunityQuotationMapper batchUpdateMapper = sqlSession.getMapper(ExtOpportunityQuotationMapper.class);
            successIds.forEach(id -> batchUpdateMapper.updateApprovalStatus(id, LogType.VOIDED, userId, System.currentTimeMillis()));
            sqlSession.flushStatements();
            SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
            logService.batchAdd(logs);
            successList.forEach(
                    item -> sendNotice(Translator.get("opportunity.quotation.status.voided"), item, userId, organizationId, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL)
            );
        }

        return BatchAffectReasonResponse.builder().success(successIds.size()).fail(list.size() - validateList.size()).skip(skipCount.get()).errorMessages(Translator.get("opportunity.quotation.batch.no.voided")).build();
    }


    /**
     * 验证不可作废的报价单
     *
     * @param list 报价单列表
     * @return 不可作废的报价单列表
     */
    private List<OpportunityQuotation> validateVoidQuotation(List<OpportunityQuotation> list) {
        List<ContractField> contractFields = contractFieldMapper.selectAll(null);

        List<ContractFieldBlob> contractFieldBlobs = contractFieldBlobMapper.selectAll(null);
        Set<String> existingIdSet = new HashSet<>();
        contractFields.forEach(cf ->
                existingIdSet.addAll(extractIdsFromFieldValue(cf.getFieldValue()))
        );
        contractFieldBlobs.forEach(cfb ->
                existingIdSet.addAll(extractIdsFromFieldValue(cfb.getFieldValue()))
        );
        return list.stream()
                .filter(item -> !existingIdSet.contains(item.getId())).toList();

    }

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 从字段值中提取ID集合
     *
     * @param fieldValue 字段值
     * @return ID集合
     */
    private Set<String> extractIdsFromFieldValue(Object fieldValue) {
        if (fieldValue == null) {
            return Collections.emptySet();
        }
        String text = String.valueOf(fieldValue).trim();
        Set<String> result = new HashSet<>();
        // 尝试数组
        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                // 修复 ['123','456'] → ["123","456"]
                text = text.replace('\'', '"');

                JsonNode node = mapper.readTree(text);
                if (node.isArray()) {
                    for (JsonNode item : node) {
                        result.add(item.asText());
                    }
                    return result;
                }
            } catch (Exception ignore) {
            }
        }
        // 尝试 CSV，例如 "123,456"
        if (text.contains(",")) {
            for (String part : text.split(",")) {
                result.add(part.trim());
            }
            return result;
        }
        // 单值
        result.add(text);
        return result;
    }


    /**
     * 获取表单快照
     *
     * @param id    报价单ID
     * @param orgId 组织ID
     * @return 表单配置DTO
     */
    public ModuleFormConfigDTO getFormSnapshot(String id, String orgId) {
        ModuleFormConfigDTO moduleFormConfigDTO;
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        //获取ApprovalState中所有状态的id属性(以后改成获取自定义的审批状态)
        List<String> approvalStatusList = Arrays.stream(ApprovalState.values()).map(ApprovalState::getId).toList();
        if (approvalStatusList.contains(opportunityQuotation.getApprovalStatus()) && (Strings.CI.equals(opportunityQuotation.getApprovalStatus(), ApprovalState.APPROVED.toString()) || Strings.CI.equals(opportunityQuotation.getApprovalStatus(), ApprovalState.APPROVING.toString()))) {
            LambdaQueryWrapper<OpportunityQuotationSnapshot> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
            OpportunityQuotationSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
            if (snapshot != null) {
                moduleFormConfigDTO = JSON.parseObject(snapshot.getQuotationProp(), ModuleFormConfigDTO.class);
            } else {
                moduleFormConfigDTO = moduleFormCacheService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), orgId);
            }
        } else {
            moduleFormConfigDTO = moduleFormCacheService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), orgId);
        }
        return moduleFormConfigDTO;
    }

    public String getQuotationName(String id) {
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(opportunityQuotation).map(OpportunityQuotation::getName).orElse(null);
    }

    /**
     * 通过名称获取报价单集合
     *
     * @param names 名称集合
     * @return 报价单集合
     */
    public List<OpportunityQuotation> getQuotationListByNames(List<String> names) {
        LambdaQueryWrapper<OpportunityQuotation> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(OpportunityQuotation::getName, names);
        return opportunityQuotationMapper.selectListByLambda(lambdaQueryWrapper);
    }

    public String getQuotationNameByIds(List<String> ids) {
        List<OpportunityQuotation> opportunityQuotations = opportunityQuotationMapper.selectByIds(ids);
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(opportunityQuotations)) {
            List<String> names = opportunityQuotations.stream().map(OpportunityQuotation::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }

    public void download(String id, String userId, String organizationId) {
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        saveSateChangeLog(organizationId, null, userId, LogType.DOWNLOAD, opportunityQuotation);
    }
}
