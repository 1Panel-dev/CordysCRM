package cn.cordys.crm.opportunity.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.CommonResultCode;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.*;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalState;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.dto.ResourceApprovalFieldUpdateParam;
import cn.cordys.crm.approval.dto.ResourceApprovalPostUpdateParam;
import cn.cordys.crm.approval.dto.ResourceSnapshotApprovalParam;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.contract.domain.ContractField;
import cn.cordys.crm.contract.domain.ContractFieldBlob;
import cn.cordys.crm.opportunity.domain.*;
import cn.cordys.crm.opportunity.dto.request.*;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationGetResponse;
import cn.cordys.crm.opportunity.dto.response.OpportunityQuotationListResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationSnapshotMapper;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.BatchAffectSkipResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.DictService;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
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
    private BaseMapper<Opportunity> opportunityBaseMapper;
    @Resource
    private DictService dictService;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ApprovalResourceService approvalResourceService;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");


    /**
     * 新增商机报价单
     * 新增报价单会自动将报价单状态设置为“提审”，此时需要保存报价单值快照，报价单表单设置快照
     *
     * @param request 新增请求参数
     * @return 商机报价单实体
     */
    @OperationLog(module = LogModule.OPPORTUNITY_QUOTATION, type = LogType.ADD, resourceName = "{#request.name}", operator = "{#userId}")
	@HitApproval(formKey = FormKey.QUOTATION, executeType = ExecuteTimingEnum.CREATE, operatorId = "{#userId}")
    public OpportunityQuotation add(OpportunityQuotationAddRequest request, String orgId, String userId) {
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        checkQuotationInfo(moduleFields, moduleFormConfigDTO);

        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        OpportunityQuotation opportunityQuotation = new OpportunityQuotation();
        opportunityQuotation.setId(IDGenerator.nextStr());
        opportunityQuotation.setOrganizationId(orgId);
        opportunityQuotation.setName(request.getName());
		opportunityQuotation.setInvalid(false);
        opportunityQuotation.setApprovalStatus(ApprovalStatus.NONE.name());
        opportunityQuotation.setOpportunityId(request.getOpportunityId());
        opportunityQuotation.setUntilTime(request.getUntilTime());
        opportunityQuotation.setCreateUser(userId);
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setCreateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());

        //判断总金额
        setAmount(request.getAmount(), opportunityQuotation);
        // 设置子表格字段值
        moduleFields.add(new BaseModuleFieldValue("products", request.getProducts()));

        opportunityQuotationFieldService.saveModuleField(opportunityQuotation, orgId, userId, moduleFields, false);
        opportunityQuotationMapper.insert(opportunityQuotation);

        // 保存表单配置快照
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, opportunityQuotationFieldService, opportunityQuotation.getId());
        OpportunityQuotationGetResponse response = getOpportunityQuotationGetResponse(opportunityQuotation, resolveFieldValues, moduleFormConfigDTO);

        baseService.handleAddLogWithSubTable(opportunityQuotation, moduleFields, Translator.get("products_info"), moduleFormConfigDTO);

        saveSnapshot(opportunityQuotation, saveModuleFormConfigDTO, response);

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
        List<BaseModuleFieldValue> fvs = opportunityQuotationFieldService.setBusinessRefFieldValue(List.of(response), moduleFormService.getFlattenFormFields(FormKey.QUOTATION.getKey(), opportunityQuotation.getOrganizationId()),
                new HashMap<>(Map.of(response.getId(), moduleFields))).get(response.getId());
        response.setModuleFields(fvs);
        Opportunity opportunity = opportunityBaseMapper.selectByPrimaryKey(response.getOpportunityId());
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(moduleFormConfigDTO, fvs);
        if (opportunity != null) {
            optionMap.put("opportunityId", List.of(new OptionDTO(opportunity.getId(), opportunity.getName())));
            response.setOpportunityName(opportunity.getName());
        }
        response.setOptionMap(optionMap);
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
    public OpportunityQuotationGetResponse getSnapshot(String id, String orgId) {
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
		if (Strings.CI.equals(response.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
			Map<String, Boolean> firstNodeApproved = baseService.getApprovingResourceFirstNodeApproved(List.of(response.getId()), orgId);
			response.setFirstApproved(firstNodeApproved.get(response.getId()));
		}
        return response;
    }

    /**
     * @param id 报价单ID
     * @return 报价单详情
     */
    public OpportunityQuotationGetResponse get(String id, String orgId) {
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
        response.setApprovalStatus(opportunityQuotation.getApprovalStatus());
        ModuleFormConfigDTO moduleFormConfigDTO = moduleFormCacheService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), opportunityQuotation.getOrganizationId());
        List<BaseModuleFieldValue> moduleFieldValues = opportunityQuotationFieldService.getModuleFieldValuesByResourceId(id);
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFieldValues, moduleFormConfigDTO, opportunityQuotationFieldService, opportunityQuotation.getId());
        List<BaseModuleFieldValue> fvs = opportunityQuotationFieldService.setBusinessRefFieldValue(List.of(response), moduleFormService.getFlattenFormFields(FormKey.QUOTATION.getKey(), opportunityQuotation.getOrganizationId()),
                new HashMap<>(Map.of(response.getId(), resolveFieldValues))).get(response.getId());
        response.setModuleFields(fvs);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(moduleFormConfigDTO, fvs);
        Opportunity opportunity = opportunityBaseMapper.selectByPrimaryKey(response.getOpportunityId());
        if (opportunity != null) {
            optionMap.put("opportunityId", List.of(new OptionDTO(opportunity.getId(), opportunity.getName())));
            response.setOpportunityName(opportunity.getName());
        }
        response.setOptionMap(optionMap);
        Map<String, List<Attachment>> attachmentMap = moduleFormService.getAttachmentMap(moduleFormConfigDTO, response.getModuleFields());
        response.setAttachmentMap(attachmentMap);
        baseService.setCreateAndUpdateUserName(response);
        UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(opportunityQuotation.getCreateUser(), opportunityQuotation.getOrganizationId());
        if (userDeptDTO != null) {
            response.setDepartmentId(userDeptDTO.getDeptId());
            response.setDepartmentName(userDeptDTO.getDeptName());
        }
		if (Strings.CI.equals(response.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
			Map<String, Boolean> firstNodeApproved = baseService.getApprovingResourceFirstNodeApproved(List.of(response.getId()), orgId);
			response.setFirstApproved(firstNodeApproved.get(response.getId()));
		}
        return response;
    }

	/**
	 * 获取报价单详情 (⚠️反射调用; 勿修改入参, 返回, 方法名!)
	 * @param id 报价单ID
	 * @return 报价单详情
	 */
	public OpportunityQuotationGetResponse getSimple(String id) {
		OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
		if (opportunityQuotation == null) {
			return null;
		}
		OpportunityQuotationGetResponse response = BeanUtils.copyBean(new OpportunityQuotationGetResponse(), opportunityQuotation);
		ModuleFormConfigDTO quotationFormConf = moduleFormCacheService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), opportunityQuotation.getOrganizationId());
		List<BaseModuleFieldValue> fvs = opportunityQuotationFieldService.getModuleFieldValuesByResourceId(id);
		moduleFormService.processBusinessFieldValues(response, fvs, quotationFormConf);
		return response;
	}


    /**
     * 获取字段详情 (⚠️反射调用; 勿修改入参, 返回, 方法名!)
     * @param id 报价单ID
     * @return 报价单详情
     */
    public OpportunityQuotationGetResponse getFieldValues(String id) {
        OpportunityQuotationGetResponse response = new OpportunityQuotationGetResponse();
        LambdaQueryWrapper<OpportunityQuotationSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        OpportunityQuotationSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getQuotationValue(), OpportunityQuotationGetResponse.class);
        }
        return response;
    }



	/**
	 * 批量获取报价单详情 (用于数据源批量查询优化)
	 * @param ids 报价单ID集合
	 * @return 报价单详情列表
	 */
	public List<OpportunityQuotationGetResponse> batchGetSimpleByIds(List<String> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}
		List<OpportunityQuotation> quotations = opportunityQuotationMapper.selectByIds(ids);
		if (CollectionUtils.isEmpty(quotations)) {
			return Collections.emptyList();
		}
		Map<String, List<BaseModuleFieldValue>> fieldValueMap = opportunityQuotationFieldService.getResourceFieldMap(ids, true);

		return quotations.stream().map(quotation -> {
			OpportunityQuotationGetResponse response = BeanUtils.copyBean(new OpportunityQuotationGetResponse(), quotation);
			response.setModuleFields(fieldValueMap.get(quotation.getId()));
			return response;
		}).toList();
	}

    /**
     * 撤销审批
     *
     * @param id     报价单ID
     * @param userId 用户ID
     */
    public String revoke(String id, String userId, String orgId) {
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        checkApprovalConfig(orgId);

        if (!Strings.CI.equals(opportunityQuotation.getCreateUser(), userId) || !Strings.CI.equals(opportunityQuotation.getApprovalStatus(), ApprovalState.APPROVING.toString())) {
            return opportunityQuotation.getApprovalStatus();
        }
        opportunityQuotation.setApprovalStatus(ApprovalState.REVOKED.toString());
        opportunityQuotation.setUpdateUser(userId);
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotationMapper.update(opportunityQuotation);



        //更新快照
        updateSnapshot(id, ApprovalState.REVOKED.toString(), null);
        return opportunityQuotation.getApprovalStatus();
    }

    private void checkApprovalConfig(String orgId) {
        if (!dictService.isDictConfigEnable(DictModule.QUOTATION_APPROVAL.name(), orgId)) {
            // 未开启审批
            throw new GenericException(CommonResultCode.APPROVAL_NOT_ENABLED_ERROR);
        }
    }

    /**
     * 作废商机报价单
     *
     * @param id     报价单ID
     * @param userId 用户ID
     */
    public void voidQuotation(String id, String userId, String orgId) {
        OpportunityQuotation opportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (opportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
		checkQuotationLinked(id, "opportunity.quotation.no.voided");
		opportunityQuotation.setInvalid(true);
		opportunityQuotation.setUpdateUser(userId);
		opportunityQuotation.setUpdateTime(System.currentTimeMillis());
		opportunityQuotationMapper.update(opportunityQuotation);

        // 修改快照中的作废状态
		voidedSnapshot(id);

		// 作废日志
		LogDTO logDTO = new LogDTO(orgId, opportunityQuotation.getId(), userId, LogType.VOIDED, LogModule.OPPORTUNITY_QUOTATION, opportunityQuotation.getName());
		logService.add(logDTO);
        // 通知
        sendNotice(Translator.get("opportunity.quotation.status.voided"), opportunityQuotation, userId, orgId, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL);
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
        checkApprovalConfig(orgId);

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
        //更新快照状态
        updateSnapshot(request.getId(), request.getApprovalStatus(), moduleFormConfigDTO);
        saveSateChangeLog(orgId, oldApprovalStatus, userId, LogType.APPROVAL, opportunityQuotation);
        sendNotice(noticeKey, opportunityQuotation, userId, orgId, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL);
        return opportunityQuotation.getApprovalStatus();
    }

    private void updateSnapshot(String id, String approvalStatus, ModuleFormConfigDTO moduleFormConfigDTO) {
        LambdaQueryWrapper<OpportunityQuotationSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        List<OpportunityQuotationSnapshot> opportunityQuotationSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
        OpportunityQuotationSnapshot first = opportunityQuotationSnapshots.getFirst();
        if (first != null) {
            OpportunityQuotationGetResponse response = JSON.parseObject(first.getQuotationValue(), OpportunityQuotationGetResponse.class);
            response.setApprovalStatus(approvalStatus);
            if (moduleFormConfigDTO != null) {
                first.setQuotationProp(JSON.toJSONString(moduleFormConfigDTO));
            }
            first.setQuotationValue(JSON.toJSONString(response));
            snapshotBaseMapper.update(first);
        }
    }

	/**
	 * ⚠️反射调用: 由审批执行操作统一调用, 勿修改
	 * @param param 参数
	 */
	public void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param) {
		OpportunityQuotationSnapshot snapshotCriteria = new OpportunityQuotationSnapshot();
		snapshotCriteria.setQuotationId(param.getResourceId());
		OpportunityQuotationSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
		if (snapshot != null) {
			OpportunityQuotationGetResponse response = JSON.parseObject(snapshot.getQuotationValue(), OpportunityQuotationGetResponse.class);
			response.setApprovalStatus(param.getApprovalStatus());
			snapshot.setQuotationValue(JSON.toJSONString(response));
			snapshotBaseMapper.update(snapshot);
		}
	}

	/**
	 * ⚠️反射调用: 由审批执行后置操作统一调用, 勿修改
	 * @param postFieldParam 参数
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void updateApprovalPostField(ResourceApprovalPostUpdateParam postFieldParam) {
		ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), OrganizationContext.getOrganizationId());
		List<BaseField> fields = formConfig.getFields();
		Map<String, BaseField> fieldConfigMap = fields.stream().collect(Collectors.toMap(BaseField::getId, f -> f));
		OpportunityQuotation quotation = opportunityQuotationMapper.selectByPrimaryKey(postFieldParam.getResourceId());
		// 保存原始数据用于日志记录
		OpportunityQuotation originQuotation = BeanUtils.copyBean(new OpportunityQuotation(), quotation);
		List<BaseModuleFieldValue> originFields = opportunityQuotationFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId());
		List<OpportunityQuotationField> quotationFields = new ArrayList<>();
		List<OpportunityQuotationFieldBlob> quotationFieldBlobs = new ArrayList<>();
		OpportunityQuotationSnapshot snapshotCriteria = new OpportunityQuotationSnapshot();
		snapshotCriteria.setQuotationId(postFieldParam.getResourceId());
		OpportunityQuotationSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
		OpportunityQuotationGetResponse response = new OpportunityQuotationGetResponse();
		if (snapshot != null) {
			response = JSON.parseObject(snapshot.getQuotationValue(), OpportunityQuotationGetResponse.class);
		}
		for (ResourceApprovalFieldUpdateParam fieldUpdateParam : postFieldParam.getFields()) {
			if (!fieldConfigMap.containsKey(fieldUpdateParam.getFieldId()) || fieldUpdateParam.getFieldValue() == null) {
				return;
			}
			BaseField fieldConfig = fieldConfigMap.get(fieldUpdateParam.getFieldId());
			AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
			if (fieldConfig.hasBusinessKey()) {
				// 业务主表字段
				opportunityQuotationFieldService.setResourceFieldValue(quotation, fieldConfig.getBusinessKey(), fieldUpdateParam.getFieldValue());
			} else {
				// 快照自定义字段
				Optional<BaseModuleFieldValue> findField = response.getModuleFields().stream().filter(fieldValue -> Strings.CI.equals(fieldValue.getFieldId(), fieldUpdateParam.getFieldId())).findAny();
				if (findField.isPresent()) {
					findField.get().setFieldValue(fieldUpdateParam.getFieldValue());
				} else {
					BaseModuleFieldValue fv = new BaseModuleFieldValue();
					fv.setFieldId(fieldUpdateParam.getFieldId());
					fv.setFieldValue(fieldUpdateParam.getFieldValue());
					response.getModuleFields().add(fv);
				}
				if (fieldConfig.isBlob()) {
					// 自定义大表
					opportunityQuotationFieldService.getResourceFieldBlobMapper().deleteByLambda(new LambdaQueryWrapper<OpportunityQuotationFieldBlob>()
							.eq(OpportunityQuotationFieldBlob::getFieldId, fieldUpdateParam.getFieldId()).eq(OpportunityQuotationFieldBlob::getResourceId, postFieldParam.getResourceId()));
					OpportunityQuotationFieldBlob field = new OpportunityQuotationFieldBlob();
					field.setId(IDGenerator.nextStr());
					field.setResourceId(postFieldParam.getResourceId());
					field.setFieldId(fieldUpdateParam.getFieldId());
					field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
					quotationFieldBlobs.add(field);
				} else {
					// 自定义表
					opportunityQuotationFieldService.getResourceFieldMapper().deleteByLambda(new LambdaQueryWrapper<OpportunityQuotationField>()
							.eq(OpportunityQuotationField::getFieldId, fieldUpdateParam.getFieldId()).eq(OpportunityQuotationField::getResourceId, postFieldParam.getResourceId()));
					OpportunityQuotationField field = new OpportunityQuotationField();
					field.setId(IDGenerator.nextStr());
					field.setResourceId(postFieldParam.getResourceId());
					field.setFieldId(fieldUpdateParam.getFieldId());
					field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
					quotationFields.add(field);
				}
			}
		}
		opportunityQuotationMapper.updateById(quotation);
		if (CollectionUtils.isNotEmpty(quotationFields)) {
			opportunityQuotationFieldService.getResourceFieldMapper().batchInsert(quotationFields);
		}
		if (CollectionUtils.isNotEmpty(quotationFields)) {
			opportunityQuotationFieldService.getResourceFieldBlobMapper().batchInsert(quotationFieldBlobs);
		}
		// 更新快照
		if (snapshot != null) {
			OpportunityQuotationGetResponse snapshotRes = getOpportunityQuotationGetResponse(quotation, response.getModuleFields(), formConfig);
			snapshot.setQuotationValue(JSON.toJSONString(snapshotRes));
			snapshotBaseMapper.update(snapshot);
		}
		// 记录审批后置字段更新日志
		baseService.handleUpdateLogWithSubTable(originQuotation, quotation, originFields, opportunityQuotationFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId()),
				postFieldParam.getResourceId(), quotation.getName(), Translator.get("products_info"), formConfig);
		// 从 OperationLogContext 中获取日志信息并手动记录
		LogContextInfo contextInfo = OperationLogContext.getContext();
		if (contextInfo != null) {
			String orgId = OrganizationContext.getOrganizationId();
			LogDTO logDTO = new LogDTO(orgId, postFieldParam.getResourceId(), postFieldParam.getOperator(), LogType.UPDATE, LogModule.OPPORTUNITY_QUOTATION, quotation.getName());
			logDTO.setOriginalValue(contextInfo.getOriginalValue());
			logDTO.setModifiedValue(contextInfo.getModifiedValue());
			logService.add(logDTO);
			OperationLogContext.clear();
		}
	}

	/**
	 * 作废报价快照
	 * @param id
	 */
	private void voidedSnapshot(String id) {
		LambdaQueryWrapper<OpportunityQuotationSnapshot> delWrapper = new LambdaQueryWrapper<>();
		delWrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
		List<OpportunityQuotationSnapshot> opportunityQuotationSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
		OpportunityQuotationSnapshot first = opportunityQuotationSnapshots.getFirst();
		if (first != null) {
			OpportunityQuotationGetResponse response = JSON.parseObject(first.getQuotationValue(), OpportunityQuotationGetResponse.class);
			response.setInvalid(true);
			first.setQuotationValue(JSON.toJSONString(response));
			snapshotBaseMapper.update(first);
		}
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
    public PagerWithOption<List<OpportunityQuotationListResponse>> list(OpportunityQuotationPageRequest request, String organizationId, String userId, DeptDataPermissionDTO deptDataPermission, Boolean source) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<OpportunityQuotationListResponse> list = extOpportunityQuotationMapper.list(request, organizationId, userId, deptDataPermission, source);
        List<OpportunityQuotationListResponse> results = buildList(list, organizationId);
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
    private List<OpportunityQuotationListResponse> buildList(List<OpportunityQuotationListResponse> listData, String organizationId) {
        // 查询列表数据的自定义字段
        Map<String, List<BaseModuleFieldValue>> dataFieldMap = opportunityQuotationFieldService.getResourceFieldMap(
                listData.stream().map(OpportunityQuotationListResponse::getId).collect(Collectors.toList()), true);
        Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = opportunityQuotationFieldService.setBusinessRefFieldValue(listData,
                moduleFormService.getFlattenFormFields(FormKey.QUOTATION.getKey(), organizationId), dataFieldMap);

        // 列表项设置自定义字段&&用户名
        List<String> createUserIds = listData.stream().map(OpportunityQuotationListResponse::getCreateUser).toList();
        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(createUserIds, organizationId);
		// 审批相关
		List<String> approvingResourceIds = listData.stream().filter(item -> Strings.CI.contains(item.getApprovalStatus(), ApprovalStatus.APPROVING.name())).map(OpportunityQuotationListResponse::getId).toList();
		Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(approvingResourceIds, organizationId);
        listData.forEach(item -> {
            item.setModuleFields(resolvefieldValueMap.get(item.getId()));
            UserDeptDTO userDeptDTO = userDeptMap.get(item.getCreateUser());
            if (userDeptDTO != null) {
                item.setDepartmentId(userDeptDTO.getDeptId());
                item.setDepartmentName(userDeptDTO.getDeptName());
            }
			item.setFirstApproved(firstNodeApprovedMap.get(item.getId()));
        });
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
	@HitApproval(formKey = FormKey.QUOTATION, executeType = ExecuteTimingEnum.EDIT, resourceId = "{#request.id}", updateType = "{#request.updateType}", operatorId = "{#userId}")
    public OpportunityQuotation update(OpportunityQuotationEditRequest request, String userId, String orgId) {
        String id = request.getId();
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        checkQuotationInfo(moduleFields, moduleFormConfigDTO);
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);

        OpportunityQuotation oldOpportunityQuotation = opportunityQuotationMapper.selectByPrimaryKey(id);
        if (oldOpportunityQuotation == null) {
            throw new GenericException(Translator.get("opportunity.quotation.not.exist"));
        }
        List<BaseModuleFieldValue> originFields = new ArrayList<>();
        OpportunityQuotation opportunityQuotation = BeanUtils.copyBean(new OpportunityQuotation(), request);
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateUser(userId);
		opportunityQuotation.setInvalid(oldOpportunityQuotation.getInvalid());
        opportunityQuotation.setCreateTime(oldOpportunityQuotation.getCreateTime());
        opportunityQuotation.setCreateUser(oldOpportunityQuotation.getCreateUser());
		opportunityQuotation.setApprovalStatus(oldOpportunityQuotation.getApprovalStatus());
        //判断总金额
        setAmount(request.getAmount(), opportunityQuotation);
        // 设置子表格字段值
        moduleFields.add(new BaseModuleFieldValue("products", request.getProducts()));
        updateFields(moduleFields, opportunityQuotation, orgId, userId);
        opportunityQuotationMapper.update(opportunityQuotation);

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
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, opportunityQuotationFieldService, opportunityQuotation.getId());
        OpportunityQuotationGetResponse response = getOpportunityQuotationGetResponse(opportunityQuotation, resolveFieldValues, moduleFormConfigDTO);
        saveSnapshot(opportunityQuotation, saveModuleFormConfigDTO, response);
        // 处理日志上下文
        baseService.handleUpdateLogWithSubTable(oldOpportunityQuotation, opportunityQuotation, originFields, moduleFields, id, opportunityQuotation.getName(), Translator.get("products_info"), moduleFormConfigDTO);
        return opportunityQuotationMapper.selectByPrimaryKey(id);
    }

    private void setAmount(String amount, OpportunityQuotation opportunityQuotation) {
        if (StringUtils.isNotBlank(amount)) {
            opportunityQuotation.setAmount(new BigDecimal(amount));
            if (opportunityQuotation.getAmount().compareTo(MAX_AMOUNT) > 0) {
                throw new GenericException(Translator.get("opportunity.quotation.amount.exceed.max"));
            }
        } else {
            opportunityQuotation.setAmount(BigDecimal.ZERO);
        }
    }

    /**
     * 检查报价单信息
     *
     * @param moduleFields        报价单字段值
     * @param moduleFormConfigDTO 报价单表单配置
     */
    private void checkQuotationInfo(List<BaseModuleFieldValue> moduleFields, ModuleFormConfigDTO moduleFormConfigDTO) {
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("opportunity.quotation.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("opportunity.quotation.form.config.required"));
        }
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
        checkApprovalConfig(orgId);

        List<String> ids = request.getIds();
        String approvalStatus = request.getApprovalStatus();
        LambdaQueryWrapper<OpportunityQuotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OpportunityQuotation::getId, ids);
        List<OpportunityQuotation> list = opportunityQuotationMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return BatchAffectSkipResponse.builder().success(0).fail(0).skip(0).build();
        }

        // 校验状态权限，过滤出有权限操作的报价单
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.QUOTATION.getValue(),
                list,
                PermissionConstants.OPPORTUNITY_QUOTATION_APPROVAL,
				orgId,
                OpportunityQuotation::getId,
                OpportunityQuotation::getApprovalStatus
        );

        Set<String> permittedIdSet = new HashSet<>(permittedIds);
        AtomicInteger permissionDeniedCount = new AtomicInteger(0);

        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtOpportunityQuotationMapper batchUpdateMapper = sqlSession.getMapper(ExtOpportunityQuotationMapper.class);
        ExtOpportunityQuotationSnapshotMapper extOpportunityQuotationSnapshotMapper = sqlSession.getMapper(ExtOpportunityQuotationSnapshotMapper.class);

        List<LogDTO> logs = new ArrayList<>();
        List<String> approvingIds = new ArrayList<>();
        AtomicInteger skipCount = new AtomicInteger(0);
        list.stream().filter(item -> {
            // 检查状态权限
            if (!permittedIdSet.contains(item.getId())) {
                permissionDeniedCount.getAndIncrement();
                return false;
            }
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
        LambdaQueryWrapper<OpportunityQuotationSnapshot> snapshotWrapper = new LambdaQueryWrapper<>();
        snapshotWrapper.in(OpportunityQuotationSnapshot::getQuotationId, approvingIds);
        List<OpportunityQuotationSnapshot> opportunityQuotationSnapshots = snapshotBaseMapper.selectListByLambda(snapshotWrapper);
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
                item -> sendNotice((Strings.CI.equals(approvalStatus, ApprovalState.APPROVED.toString()) ?
                        Translator.get("opportunity.quotation.status.approved") : Translator.get("opportunity.quotation.status.unapproved")), item, userId, orgId, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL)
        );
        return BatchAffectSkipResponse.builder().success(approvingIds.size()).fail(permissionDeniedCount.get()).skip(skipCount.get()).build();
    }

    /**
     * 批量更新报价单
     *
     * @param request        批量编辑参数
     * @param userId         当前用户ID
     * @param organizationId 当前组织ID
     */
    public BatchAffectReasonResponse batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = opportunityQuotationFieldService.getAndCheckField(request.getFieldId(), organizationId);
        moduleFormService.setFieldBusinessParam(field);
        List<OpportunityQuotation> originQuotations = opportunityQuotationMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(originQuotations)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0).errorMessages(Translator.get("opportunity.quotation.not.exist")).build();
        }

        // 校验状态权限，过滤出有权限操作的报价单
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.QUOTATION.getValue(),
                originQuotations,
                PermissionConstants.OPPORTUNITY_QUOTATION_UPDATE,
				organizationId,
                OpportunityQuotation::getId,
                OpportunityQuotation::getApprovalStatus
        );

        if (CollectionUtils.isEmpty(permittedIds)) {
            return BatchAffectReasonResponse.builder().success(0).fail(originQuotations.size()).skip(0).errorMessages(Translator.get("no.operation.permission")).build();
        }

            approvalResourceService.batchEditTriggerApproval(permittedIds, FormKey.QUOTATION, organizationId, userId);

        // 只对有权限的报价单进行操作
        List<OpportunityQuotation> permittedQuotations = originQuotations.stream()
                .filter(q -> permittedIds.contains(q.getId()))
                .collect(Collectors.toList());

        ResourceBatchEditRequest filteredRequest = new ResourceBatchEditRequest();
        filteredRequest.setIds(permittedIds);
        filteredRequest.setFieldId(request.getFieldId());
        filteredRequest.setFieldValue(request.getFieldValue());

        opportunityQuotationFieldService.batchUpdate(
                filteredRequest,
                field,
                permittedQuotations,
                OpportunityQuotation.class,
                LogModule.OPPORTUNITY_QUOTATION,
                extOpportunityQuotationMapper::batchUpdate,
                userId,
                organizationId
        );

        ModuleFormConfigDTO moduleFormConfigDTO = moduleFormCacheService.getBusinessFormConfig(FormKey.QUOTATION.getKey(), organizationId);
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);

        LambdaQueryWrapper<OpportunityQuotationSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.in(OpportunityQuotationSnapshot::getQuotationId, permittedIds);
        snapshotBaseMapper.deleteByLambda(delWrapper);

        List<OpportunityQuotation> latestQuotations = opportunityQuotationMapper.selectByIds(permittedIds);
        Map<String, OpportunityQuotation> latestQuotationMap = latestQuotations.stream()
                .collect(Collectors.toMap(OpportunityQuotation::getId, item -> item));
        Map<String, List<BaseModuleFieldValue>> fieldMap = opportunityQuotationFieldService.getResourceFieldMap(permittedIds, true);

        List<OpportunityQuotationSnapshot> snapshots = new ArrayList<>();
        for (String id : permittedIds) {
            OpportunityQuotation opportunityQuotation = latestQuotationMap.get(id);
            if (opportunityQuotation == null) {
                continue;
            }
            List<BaseModuleFieldValue> quotationFields = fieldMap.getOrDefault(id, Collections.emptyList());
            List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(
                    quotationFields,
                    moduleFormConfigDTO,
                    opportunityQuotationFieldService,
                    id
            );
            OpportunityQuotationGetResponse response = getOpportunityQuotationGetResponse(opportunityQuotation, resolveFieldValues, moduleFormConfigDTO);
            if (CollectionUtils.isNotEmpty(response.getModuleFields())) {
                response.setModuleFields(response.getModuleFields().stream()
                        .filter(f -> f.getFieldValue() != null
                                && StringUtils.isNotBlank(f.getFieldValue().toString())
                                && !"[]".equals(f.getFieldValue().toString()))
                        .toList());
            }
            OpportunityQuotationSnapshot snapshot = new OpportunityQuotationSnapshot();
            snapshot.setId(IDGenerator.nextStr());
            snapshot.setQuotationId(id);
            snapshot.setQuotationProp(JSON.toJSONString(saveModuleFormConfigDTO));
            snapshot.setQuotationValue(JSON.toJSONString(response));
            snapshots.add(snapshot);
        }
        if (CollectionUtils.isNotEmpty(snapshots)) {
            snapshotBaseMapper.batchInsert(snapshots);
        }

        return BatchAffectReasonResponse.builder().success(permittedIds.size()).fail(originQuotations.size() - permittedIds.size()).skip(0).errorMessages(Translator.get("batch.update.reason")).build();
    }


    /**
     * 批量作废商机报价单
     *
     * @param request        批量作废请求参数
     * @param userId         用户ID
     * @param organizationId 组织ID
     * @return 批量作废响应参数
     */
    public BatchAffectReasonResponse batchVoidQuotation(OpportunityQuotationBatchVoidedRequest request, String userId, String organizationId) {
        List<String> ids = request.getIds();
        LambdaQueryWrapper<OpportunityQuotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OpportunityQuotation::getId, ids);
        List<OpportunityQuotation> list = opportunityQuotationMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0).errorMessages(Translator.get("opportunity.quotation.not.exist")).build();
        }

        // 校验状态权限，过滤出有权限操作的报价单
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.QUOTATION.getValue(),
                list,
                PermissionConstants.OPPORTUNITY_QUOTATION_VOIDED,
				organizationId,
                OpportunityQuotation::getId,
                OpportunityQuotation::getApprovalStatus
        );

        Set<String> permittedIdSet = new HashSet<>(permittedIds);
        AtomicInteger permissionDeniedCount = new AtomicInteger(0);

        // 过滤出有权限的报价单
        List<OpportunityQuotation> permittedList = list.stream()
                .filter(item -> {
                    if (!permittedIdSet.contains(item.getId())) {
                        permissionDeniedCount.getAndIncrement();
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 校验商机报价单是否可以作废
        List<OpportunityQuotation> validateList = validateVoidQuotation(permittedList);
        if (CollectionUtils.isEmpty(validateList)) {
            return BatchAffectReasonResponse.builder().success(0).fail(permissionDeniedCount.get() + (permittedList.size())).skip(0).errorMessages(Translator.get("opportunity.quotation.batch.no.voided")).build();
        }

        List<LogDTO> logs = new ArrayList<>();
        List<String> successIds = new ArrayList<>();
        List<OpportunityQuotation> successList = new ArrayList<>();
        AtomicInteger skipCount = new AtomicInteger();
        validateList.stream().filter(
                item -> {
                    if (BooleanUtils.isTrue(item.getInvalid())) {
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
            logs.add(log);

        });
        if (CollectionUtils.isNotEmpty(successIds)) {
            LambdaQueryWrapper<OpportunityQuotationSnapshot> snapshotWrapper = new LambdaQueryWrapper<>();
            snapshotWrapper.in(OpportunityQuotationSnapshot::getQuotationId, successIds);
            List<OpportunityQuotationSnapshot> opportunityQuotationSnapshots = snapshotBaseMapper.selectListByLambda(snapshotWrapper);
            SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
            ExtOpportunityQuotationMapper batchUpdateMapper = sqlSession.getMapper(ExtOpportunityQuotationMapper.class);
            ExtOpportunityQuotationSnapshotMapper extOpportunityQuotationSnapshotMapper = sqlSession.getMapper(ExtOpportunityQuotationSnapshotMapper.class);
            successIds.forEach(id -> batchUpdateMapper.voided(id, userId, System.currentTimeMillis()));
            //批量修改报价单快照
            for (OpportunityQuotationSnapshot snapshot : opportunityQuotationSnapshots) {
                OpportunityQuotationGetResponse response = JSON.parseObject(snapshot.getQuotationValue(), OpportunityQuotationGetResponse.class);
                response.setInvalid(true);
                snapshot.setQuotationValue(JSON.toJSONString(response));
                extOpportunityQuotationSnapshotMapper.update(snapshot);
            }
            sqlSession.flushStatements();
            SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
            logService.batchAdd(logs);
            successList.forEach(
                    item -> sendNotice(Translator.get("opportunity.quotation.status.voided"), item, userId, organizationId, NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL)
            );
        }

        return BatchAffectReasonResponse.builder().success(successIds.size()).fail(permissionDeniedCount.get() + (permittedList.size() - validateList.size())).skip(skipCount.get()).errorMessages(Translator.get("opportunity.quotation.batch.no.voided")).build();
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
        LambdaQueryWrapper<OpportunityQuotationSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpportunityQuotationSnapshot::getQuotationId, id);
        OpportunityQuotationSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            moduleFormConfigDTO = JSON.parseObject(snapshot.getQuotationProp(), ModuleFormConfigDTO.class);
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

	/**
	 * 处理旧版本审批状态 (APPROVING, VOIDED => NONE), 作废字段单独处理
	 */
	public void handleOldApprovalData() {
		List<OpportunityQuotation> quotations = opportunityQuotationMapper.selectAll(null);
		quotations.forEach(quotation -> {
			quotation.setInvalid(Strings.CI.equals(quotation.getApprovalStatus(), "VOIDED"));
			if (Strings.CI.equalsAny(quotation.getApprovalStatus(), "VOIDED", "APPROVING")) {
				quotation.setApprovalStatus(ApprovalStatus.NONE.name());
			}
			opportunityQuotationMapper.updateById(quotation);
			OpportunityQuotationSnapshot snapshotCriteria = new OpportunityQuotationSnapshot();
			snapshotCriteria.setQuotationId(quotation.getId());
			OpportunityQuotationSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
			if (snapshot != null) {
				OpportunityQuotationGetResponse response = JSON.parseObject(snapshot.getQuotationValue(), OpportunityQuotationGetResponse.class);
				response.setApprovalStatus(quotation.getApprovalStatus());
				response.setInvalid(quotation.getInvalid());
				snapshot.setQuotationValue(JSON.toJSONString(response));
				snapshotBaseMapper.update(snapshot);
			}
		});
	}
}
