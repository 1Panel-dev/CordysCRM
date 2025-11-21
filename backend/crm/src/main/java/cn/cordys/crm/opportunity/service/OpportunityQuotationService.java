package cn.cordys.crm.opportunity.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.domain.ContractField;
import cn.cordys.crm.opportunity.constants.ApprovalState;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.domain.OpportunityQuotationSnapshot;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationAddRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationEditRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityQuotationPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationGetResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationListResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ExtOpportunityQuotationMapper extOpportunityQuotationMapper;
    @Resource
    private BaseMapper<OpportunityQuotation> opportunityQuotationMapper;
    @Resource
    private BaseMapper<OpportunityQuotationSnapshot> snapshotBaseMapper;
    @Resource
    private BaseMapper<ContractField> contractFieldMapper;


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
        OpportunityQuotation opportunityQuotation = new OpportunityQuotation();
        opportunityQuotation.setId(IDGenerator.nextStr());
        opportunityQuotation.setOrganizationId(orgId);
        opportunityQuotation.setName(request.getName());
        opportunityQuotation.setApprovalStatus(ApprovalState.APPROVING.toString());
        opportunityQuotation.setAmount(request.getAmount());
        opportunityQuotation.setOpportunityId(request.getOpportunityId());
        opportunityQuotation.setCreateUser(userId);
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setCreateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotationFieldService.saveModuleField(opportunityQuotation, orgId, userId, moduleFields, false);
        opportunityQuotationMapper.insert(opportunityQuotation);
        baseService.handleAddLog(opportunityQuotation, moduleFields);

        // 保存表单配置快照
        OpportunityQuotationGetResponse response = getOpportunityQuotationGetResponse(opportunityQuotation, moduleFields, moduleFormConfigDTO);
        saveSnapshot(opportunityQuotation, moduleFormConfigDTO, response);

        return opportunityQuotation;

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
        response.setModuleFields(moduleFields);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(moduleFormConfigDTO, moduleFields);
        response.setOptionMap(optionMap);
        Map<String, List<Attachment>> attachmentMap = moduleFormService.getAttachmentMap(moduleFormConfigDTO, moduleFields);
        response.setAttachmentMap(attachmentMap);
        return baseService.setCreateAndUpdateUserName(response);
    }

    /**
     * 查询商机报价单详情
     *
     * @param id 报价单ID
     * @return 报价单详情
     */
    public OpportunityQuotationGetResponse get(String id) {
        OpportunityQuotationGetResponse response = new OpportunityQuotationGetResponse();
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        if (Strings.CI.equals(opportunityQuotation.getApprovalStatus(), ApprovalState.APPROVED.toString()) || Strings.CI.equals(opportunityQuotation.getApprovalStatus(), ApprovalState.APPROVING.toString())) {
            // 已审核，查询最新快照
            LambdaQueryWrapper<OpportunityQuotationSnapshot> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
            OpportunityQuotationSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
            if (snapshot != null) {
                response = JSON.parseObject(snapshot.getQuotationValue(), OpportunityQuotationGetResponse.class);
            }
        } else {
            // 未审核，查询当前值
            List<BaseModuleFieldValue> moduleFields = opportunityQuotationFieldService.getModuleFieldValuesByResourceId(id);
            ModuleFormConfigDTO moduleFormConfigDTO = moduleFormService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), opportunityQuotation.getOrganizationId());
            response = getOpportunityQuotationGetResponse(opportunityQuotation, moduleFields, moduleFormConfigDTO);
        }

        return response;
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
        opportunityQuotationMapper.updateById(opportunityQuotation);
        return opportunityQuotation.getApprovalStatus();
    }

    /**
     * 作废商机报价单
     *
     * @param id     报价单ID
     * @param userId 用户ID
     */
    public String voidQuotation(String id, String userId, String orgId) {
        OpportunityQuotation opportunityQuotation = updateApprovalState(id, ApprovalState.VOIDED.toString(), userId, "opportunity.quotation.status.voided", orgId);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        checkQuotationLinked(id, "opportunity.quotation.no.voided");
        saveSateChangeLog(orgId, id, userId, LogType.VOIDED, opportunityQuotation);
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
        if (CollectionUtils.isNotEmpty(contractFieldList)) {
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
        String noticeKey = Strings.CI.equals(request.getApprovalStatus(), ApprovalState.APPROVED.toString()) ?
                "opportunity.quotation.status.approved" : "opportunity.quotation.status.unapproved";
        OpportunityQuotation opportunityQuotation = updateApprovalState(request.getId(), request.getApprovalStatus(), userId, noticeKey, orgId);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }

        //删除快照
        String id = request.getId();
        LambdaQueryWrapper<OpportunityQuotationSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        snapshotBaseMapper.deleteByLambda(delWrapper);

        //保存快照
        OpportunityQuotationGetResponse response = getOpportunityQuotationGetResponse(opportunityQuotation, request.getModuleFields(), request.getModuleFormConfigDTO());
        saveSnapshot(opportunityQuotation, request.getModuleFormConfigDTO(), response);

        saveSateChangeLog(orgId, id, userId, LogType.APPROVAL, opportunityQuotation);
        return opportunityQuotation.getApprovalStatus();
    }

    /**
     * 更新审批状态
     *
     * @param id             报价单ID
     * @param approvalStatus 审批状态
     * @param userId         用户ID
     * @param key            审批状态key
     * @param orgId          组织ID
     * @return 报价单
     */
    private OpportunityQuotation updateApprovalState(String id, String approvalStatus, String userId, String key, String orgId) {
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            return null;
        }
        if ((Strings.CI.equals(approvalStatus, ApprovalState.APPROVED.toString()) || Strings.CI.equals(approvalStatus, ApprovalState.UNAPPROVED.toString())) && !Strings.CI.equals(opportunityQuotation.getApprovalStatus(), ApprovalState.APPROVING.toString())) {
            return null;
        }
        opportunityQuotation.setApprovalStatus(approvalStatus);
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotationMapper.updateById(opportunityQuotation);
        //增加通知
        Map<String, Object> paramMap = new HashMap<>(8);
        paramMap.put("state", Translator.get(key));
        paramMap.put("name", opportunityQuotation.getName());
        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL,
                paramMap, userId, orgId, List.of(opportunityQuotation.getCreateUser()), true);

        return opportunityQuotation;
    }

    /**
     * 保存状态变更日志
     *
     * @param orgId                组织ID
     * @param id                   报价单ID
     * @param userId               用户ID
     * @param logType              日志类型
     * @param opportunityQuotation 报价单实体
     */
    private void saveSateChangeLog(String orgId, String id, String userId, String logType, OpportunityQuotation opportunityQuotation) {
        LogDTO logDTO = new LogDTO(orgId, id, userId, logType, LogModule.OPPORTUNITY_QUOTATION, opportunityQuotation.getName());
        logDTO.setOriginalValue(opportunityQuotation.getName());
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

        //记录日志
        saveSateChangeLog(organizationId, id, userId, LogType.DELETE, opportunityQuotation);
    }

    /**
     * 商机报价单列表
     *
     * @param request        列表请求参数
     * @param organizationId 组织ID
     * @return 商机报价单列表
     */
    public PagerWithOption<List<OpportunityQuotationListResponse>> list(OpportunityQuotationPageRequest request, String organizationId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<OpportunityQuotationListResponse> list = extOpportunityQuotationMapper.list(request, organizationId);
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
        OpportunityQuotation oldOpportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (oldOpportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        List<BaseModuleFieldValue> originFields = opportunityQuotationFieldService.getModuleFieldValuesByResourceId(id);
        OpportunityQuotation opportunityQuotation = BeanUtils.copyBean(new OpportunityQuotation(), request);
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setApprovalStatus(ApprovalState.APPROVING.toString());
        updateFields(moduleFields, opportunityQuotation, orgId, userId);
        opportunityQuotationMapper.update(opportunityQuotation);
        // 处理日志上下文
        baseService.handleUpdateLog(oldOpportunityQuotation, opportunityQuotation, originFields, moduleFields, id, opportunityQuotation.getName());

        //删除快照
        LambdaQueryWrapper<OpportunityQuotationSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        snapshotBaseMapper.deleteByLambda(delWrapper);
        //保存快照
        OpportunityQuotationGetResponse response = getOpportunityQuotationGetResponse(opportunityQuotation, moduleFields, request.getModuleFormConfigDTO());
        saveSnapshot(opportunityQuotation, request.getModuleFormConfigDTO(), response);

        return opportunityQuotationMapper.selectByPrimaryKey(id);
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
}
