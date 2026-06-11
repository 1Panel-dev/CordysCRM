package cn.cordys.crm.order.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.*;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.dto.stage.StageSortRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.dto.ResourceApprovalFieldUpdateParam;
import cn.cordys.crm.approval.dto.ResourceApprovalPostUpdateParam;
import cn.cordys.crm.approval.dto.ResourceSnapshotApprovalParam;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.order.domain.OrderField;
import cn.cordys.crm.order.domain.OrderFieldBlob;
import cn.cordys.crm.order.domain.OrderSnapshot;
import cn.cordys.crm.order.dto.request.OrderAddRequest;
import cn.cordys.crm.order.dto.request.OrderPageRequest;
import cn.cordys.crm.order.dto.request.OrderStageRequest;
import cn.cordys.crm.order.dto.request.OrderUpdateRequest;
import cn.cordys.crm.order.dto.response.OrderGetResponse;
import cn.cordys.crm.order.dto.response.OrderListResponse;
import cn.cordys.crm.order.dto.response.OrderStatisticResponse;
import cn.cordys.crm.order.mapper.ExtOrderMapper;
import cn.cordys.crm.order.mapper.ExtOrderStageConfigMapper;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class OrderService {

    @Resource
    private OrderFieldService orderFieldService;
    @Resource
    private BaseMapper<Order> orderMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<OrderSnapshot> snapshotBaseMapper;
    @Resource
    private ExtOrderMapper extOrderMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private LogService logService;
    @Resource
    private ApprovalResourceService approvalResourceService;
    @Resource
    private ExtOrderStageConfigMapper extOrderStageConfigMapper;
    @Resource
    private BaseMapper<Customer> customerBaseMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");
    public static final Long DEFAULT_POS = 1L;

    /**
     * 新建订单
     *
     * @param request
     * @param operatorId
     * @param orgId
     *
     * @return
     */
    @OperationLog(module = LogModule.ORDER_INDEX, type = LogType.ADD)
    @HitApproval(formKey = FormKey.ORDER, executeType = ExecuteTimingEnum.CREATE, operatorId = "{#operatorId}")
    public Order add(OrderAddRequest request, String operatorId, String orgId) {
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("order.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("order.form.config.required"));
        }
        List<StageConfigResponse> stageConfigList = extOrderStageConfigMapper.getStageConfigList(orgId);
        Long nextPos = getNextPos(orgId, stageConfigList.getFirst().getId());
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        Order order = new Order();
        BeanUtils.copyBean(order, request);
        order.setId(IDGenerator.nextStr());
        order.setStage(stageConfigList.getFirst().getId());
        order.setPos(nextPos);
        order.setApprovalStatus(ApprovalStatus.NONE.name());
        order.setOrganizationId(orgId);
        order.setCreateTime(System.currentTimeMillis());
        order.setCreateUser(operatorId);
        order.setUpdateTime(System.currentTimeMillis());
        order.setUpdateUser(operatorId);

        //判断总金额
        setAmount(request.getAmount(), order);

        //自定义字段
        orderFieldService.saveModuleField(order, orgId, operatorId, moduleFields, false);
        orderMapper.insert(order);

        baseService.handleAddLogWithSubTable(order, moduleFields, Translator.get("products_info"), moduleFormConfigDTO);

        // 保存表单配置快照
        List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, orderFieldService, order.getId());
        OrderGetResponse response = get(order, resolveFieldValues, moduleFormConfigDTO);
        saveSnapshot(order, saveModuleFormConfigDTO, response);

        return order;
    }


    private Long getNextPos(String orgId, String stage) {
        Long pos = extOrderMapper.selectNextPos(orgId, stage);
        return pos == null ? 1 : pos + 1;
    }

    /**
     * 保存订单快照
     *
     * @param order
     * @param moduleFormConfigDTO
     * @param response
     */
    private void saveSnapshot(Order order, ModuleFormConfigDTO moduleFormConfigDTO, OrderGetResponse response) {
        //移除response中moduleFields 集合里 的 BaseModuleFieldValue 的 fieldId="products"的数据，避免快照数据过大
        if (CollectionUtils.isNotEmpty(response.getModuleFields())) {
            response.setModuleFields(response.getModuleFields().stream()
                    .filter(field -> (field.getFieldValue() != null && StringUtils.isNotBlank(field.getFieldValue().toString()) && !"[]".equals(field.getFieldValue().toString()))).toList());
        }
        OrderSnapshot snapshot = new OrderSnapshot();
        snapshot.setId(IDGenerator.nextStr());
        snapshot.setOrderId(order.getId());
        snapshot.setOrderProp(JSON.toJSONString(moduleFormConfigDTO));
        snapshot.setOrderValue(JSON.toJSONString(response));
        snapshotBaseMapper.insert(snapshot);
    }

    private OrderGetResponse get(Order order, List<BaseModuleFieldValue> orderFields, ModuleFormConfigDTO orderFormConfig) {
        OrderGetResponse orderGetResponse = BeanUtils.copyBean(new OrderGetResponse(), order);
        orderGetResponse = baseService.setCreateUpdateOwnerUserName(orderGetResponse);

        String id = order.getId();
        // 获取模块字段
        moduleFormService.processBusinessFieldValues(orderGetResponse, orderFields, orderFormConfig);
        orderFields = orderFieldService.setBusinessRefFieldValue(List.of(orderGetResponse),
                moduleFormService.getFlattenFormFields(FormKey.ORDER.getKey(), order.getOrganizationId()), new HashMap<>(Map.of(id, orderFields))).get(id);

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(orderFormConfig, orderFields);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(orderGetResponse,
                OrderGetResponse::getOwner, OrderGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.ORDER_OWNER.getBusinessKey(), ownerFieldOption);

        Customer customer = customerMapper.selectByPrimaryKey(order.getCustomerId());
        Contract contract = contractMapper.selectByPrimaryKey(order.getContractId());

        Map<String, String> stageNameMap = extOrderStageConfigMapper.getStageConfigList(order.getOrganizationId()).stream()
                .collect(Collectors.toMap(StageConfigResponse::getId,
                        StageConfigResponse::getName));
        orderGetResponse.setStageName(stageNameMap.get(order.getStage()));

        if (customer != null) {
            orderGetResponse.setCustomerName(customer.getName());
            optionMap.put(BusinessModuleField.ORDER_CUSTOMER.getBusinessKey(), Collections.singletonList(new OptionDTO(customer.getId(), customer.getName())));
        }
        if (contract != null) {
            orderGetResponse.setContractName(contract.getName());
            optionMap.put(BusinessModuleField.ORDER_CONTRACT.getBusinessKey(), Collections.singletonList(new OptionDTO(contract.getId(), contract.getName())));
        }

        orderGetResponse.setOptionMap(optionMap);
        orderGetResponse.setModuleFields(orderFields);

        if (orderGetResponse.getOwner() != null) {
            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(orderGetResponse.getOwner(), order.getOrganizationId());
            if (userDeptDTO != null) {
                orderGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                orderGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        // 附件信息
        orderGetResponse.setAttachmentMap(moduleFormService.getAttachmentMap(orderFormConfig, orderFields));
        return orderGetResponse;
    }

    /**
     * 获取订单详情
     *
     * @param id
     *
     * @return
     */
    public OrderGetResponse get(String id, String orgId) {
        Order order = orderMapper.selectByPrimaryKey(id);
        // 获取模块字段
        ModuleFormConfigDTO orderFormConfig = getFormConfig(order.getOrganizationId());
        List<BaseModuleFieldValue> orderFields = orderFieldService.getModuleFieldValuesByResourceId(id);
        OrderGetResponse getResponse = get(order, orderFields, orderFormConfig);
        if (Strings.CI.equals(getResponse.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
            Map<String, Boolean> firstNodeApproved = baseService.getApprovingResourceFirstNodeApproved(List.of(getResponse.getId()), orgId);
            getResponse.setFirstApproved(firstNodeApproved.get(getResponse.getId()));
        }
        return getResponse;
    }

    /**
     * 获取订单详情（⚠️反射调用; 勿修改入参, 返回, 方法名!）
     *
     * @param id 订单ID
     *
     * @return 订单详情
     */
    public OrderGetResponse getSimple(String id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            return null;
        }
        OrderGetResponse response = BeanUtils.copyBean(new OrderGetResponse(), order);
        List<BaseModuleFieldValue> fvs = orderFieldService.getModuleFieldValuesByResourceId(id);
        ModuleFormConfigDTO orderFormConfig = getFormConfig(order.getOrganizationId());
        moduleFormService.processBusinessFieldValues(response, fvs, orderFormConfig);
        return response;
    }

    /**
     * 获取字段详情 (⚠️反射调用; 勿修改入参, 返回, 方法名!)
     * @param id 订单ID
     * @return 订单详情
     */
    public OrderGetResponse getFieldValues(String id) {
        OrderGetResponse response = new OrderGetResponse();
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            return null;
        }
        LambdaQueryWrapper<OrderSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSnapshot::getOrderId, id);
        OrderSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getOrderValue(), OrderGetResponse.class);
            if (StringUtils.isNotBlank(order.getCustomerId())) {
                Customer customer = customerBaseMapper.selectByPrimaryKey(order.getCustomerId());
                if (customer != null) {
                    response.setInCustomerPool(customer.getInSharedPool());
                    response.setPoolId(customer.getPoolId());
                }
            }
        }
        return response;
    }

    /**
     * 批量获取订单详情 (用于数据源批量查询优化)
     *
     * @param ids 订单ID集合
     *
     * @return 订单详情列表
     */
    public List<OrderGetResponse> batchGetSimpleByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<Order> orders = orderMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(orders)) {
            return Collections.emptyList();
        }
        Map<String, List<BaseModuleFieldValue>> fieldValueMap = orderFieldService.getResourceFieldMap(ids, true);

        return orders.stream().map(order -> {
            OrderGetResponse response = BeanUtils.copyBean(new OrderGetResponse(), order);
            response.setModuleFields(fieldValueMap.get(order.getId()));
            return response;
        }).toList();
    }

    /**
     * 编辑订单
     *
     * @param request
     * @param userId
     * @param orgId
     *
     * @return
     */
    @OperationLog(module = LogModule.ORDER_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    @HitApproval(formKey = FormKey.ORDER, executeType = ExecuteTimingEnum.EDIT, resourceId = "{#request.id}", updateType = "{#request.updateType}", operatorId = "{#userId}")
    public Order update(OrderUpdateRequest request, String userId, String orgId) {
        Order oldOrder = orderMapper.selectByPrimaryKey(request.getId());
        List<BaseModuleFieldValue> moduleFields = request.getModuleFields();
        ModuleFormConfigDTO moduleFormConfigDTO = request.getModuleFormConfigDTO();
        if (CollectionUtils.isEmpty(moduleFields)) {
            throw new GenericException(Translator.get("order.field.required"));
        }
        if (moduleFormConfigDTO == null) {
            throw new GenericException(Translator.get("order.form.config.required"));
        }
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);
        Optional.ofNullable(oldOrder).ifPresentOrElse(item -> {

            List<BaseModuleFieldValue> originFields = orderFieldService.getModuleFieldValuesByResourceId(request.getId());
            Order order = BeanUtils.copyBean(new Order(), request);
            order.setUpdateTime(System.currentTimeMillis());
            order.setUpdateUser(userId);
            // 保留不可更改的字段
            order.setNumber(oldOrder.getNumber());
            order.setCreateUser(oldOrder.getCreateUser());
            order.setCreateTime(oldOrder.getCreateTime());
            order.setStage(oldOrder.getStage());
            order.setApprovalStatus(oldOrder.getApprovalStatus());
            //判断总金额
            setAmount(request.getAmount(), order);
            updateFields(moduleFields, order, orgId, userId);
            orderMapper.update(order);
            //删除快照
            LambdaQueryWrapper<OrderSnapshot> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(OrderSnapshot::getOrderId, request.getId());
            List<OrderSnapshot> orderSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
            if (CollectionUtils.isNotEmpty(orderSnapshots)) {
                OrderSnapshot first = orderSnapshots.getFirst();
                if (first != null) {
                    OrderGetResponse response = JSON.parseObject(first.getOrderValue(), OrderGetResponse.class);
                    List<BaseModuleFieldValue> originModuleFields = response.getModuleFields();
                    originFields.addAll(originModuleFields);
                }
            }
            snapshotBaseMapper.deleteByLambda(delWrapper);
            //保存快照
            List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(moduleFields, moduleFormConfigDTO, orderFieldService, order.getId());
            // get 方法需要使用orgId
            order.setOrganizationId(orgId);
            OrderGetResponse response = get(order, resolveFieldValues, moduleFormConfigDTO);
            saveSnapshot(order, saveModuleFormConfigDTO, response);
            // 处理日志上下文
            baseService.handleUpdateLogWithSubTable(oldOrder, order, originFields, moduleFields, request.getId(), order.getName(), Translator.get("products_info"), moduleFormConfigDTO);
        }, () -> {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        });
        return orderMapper.selectByPrimaryKey(request.getId());
    }

    private void setAmount(String amount, Order order) {
        if (StringUtils.isNotBlank(amount)) {
            order.setAmount(new BigDecimal(amount));
            if (order.getAmount().compareTo(MAX_AMOUNT) > 0) {
                throw new GenericException(Translator.get("order.amount.exceed.max"));
            }
        } else {
            order.setAmount(BigDecimal.ZERO);
        }
    }


    /**
     * 更新自定义字段
     *
     * @param moduleFields
     * @param order
     * @param orgId
     * @param userId
     */
    private void updateFields(List<BaseModuleFieldValue> moduleFields, Order order, String orgId, String userId) {
        if (moduleFields == null) {
            return;
        }
        orderFieldService.deleteByResourceId(order.getId());
        orderFieldService.saveModuleField(order, orgId, userId, moduleFields, true);
    }


    /**
     * 删除订单
     *
     * @param id 订单ID
     */
    @OperationLog(module = LogModule.ORDER_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        orderFieldService.deleteByResourceId(id);
        orderMapper.deleteByPrimaryKey(id);

        //删除快照
        LambdaQueryWrapper<OrderSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSnapshot::getOrderId, id);
        snapshotBaseMapper.deleteByLambda(wrapper);
        // 添加日志上下文
        OperationLogContext.setResourceName(order.getName());
    }


    /**
     * ⚠️反射调用; 勿修改入参, 返回, 方法名!
     *
     * @param id 订单ID
     *
     * @return 订单详情
     */
    public OrderGetResponse getSnapshot(String id, String orgId) {
        OrderGetResponse response = new OrderGetResponse();
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            return null;
        }
        LambdaQueryWrapper<OrderSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSnapshot::getOrderId, id);
        OrderSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getOrderValue(), OrderGetResponse.class);
            if (StringUtils.isNotBlank(order.getCustomerId())) {
                Customer customer = customerBaseMapper.selectByPrimaryKey(order.getCustomerId());
                if (customer != null) {
                    response.setInCustomerPool(customer.getInSharedPool());
                    response.setPoolId(customer.getPoolId());
                }
            }
            if (Strings.CI.equals(response.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
                Map<String, Boolean> firstNodeApproved = baseService.getApprovingResourceFirstNodeApproved(List.of(response.getId()), orgId);
                response.setFirstApproved(firstNodeApproved.get(response.getId()));
            }
        }
        return response;
    }

    /**
     * ⚠️反射调用: 由审批执行操作统一调用, 勿修改
     *
     * @param param 参数
     */
    public void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param) {
        OrderSnapshot snapshotCriteria = new OrderSnapshot();
        snapshotCriteria.setOrderId(param.getResourceId());
        OrderSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
        if (snapshot != null) {
            OrderGetResponse response = JSON.parseObject(snapshot.getOrderValue(), OrderGetResponse.class);
            response.setApprovalStatus(param.getApprovalStatus());
            snapshot.setOrderValue(JSON.toJSONString(response));
            snapshotBaseMapper.update(snapshot);
        }
    }

    /**
     * ⚠️反射调用: 由审批执行后置操作统一调用, 勿修改
     *
     * @param postFieldParam 参数
     */
	@SuppressWarnings({"unchecked", "rawtypes"})
    public void updateApprovalPostField(ResourceApprovalPostUpdateParam postFieldParam) {
        ModuleFormConfigDTO formConfig = getFormConfig(OrganizationContext.getOrganizationId());
        List<BaseField> fields = formConfig.getFields();
        Map<String, BaseField> fieldConfigMap = fields.stream().collect(Collectors.toMap(BaseField::getId, f -> f));
        Order order = orderMapper.selectByPrimaryKey(postFieldParam.getResourceId());
        // 保存原始数据用于日志记录
        Order originOrder = BeanUtils.copyBean(new Order(), order);
        List<BaseModuleFieldValue> originFields = orderFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId());
        List<OrderField> orderFields = new ArrayList<>();
        List<OrderFieldBlob> orderFieldBlobs = new ArrayList<>();
        OrderSnapshot snapshotCriteria = new OrderSnapshot();
        snapshotCriteria.setOrderId(postFieldParam.getResourceId());
        OrderSnapshot snapshot = snapshotBaseMapper.selectOne(snapshotCriteria);
        OrderGetResponse response = new OrderGetResponse();
        if (snapshot != null) {
            response = JSON.parseObject(snapshot.getOrderValue(), OrderGetResponse.class);
        }
        for (ResourceApprovalFieldUpdateParam fieldUpdateParam : postFieldParam.getFields()) {
            if (!fieldConfigMap.containsKey(fieldUpdateParam.getFieldId()) || fieldUpdateParam.getFieldValue() == null) {
                return;
            }
            BaseField fieldConfig = fieldConfigMap.get(fieldUpdateParam.getFieldId());
			AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
			if (fieldConfig.hasBusinessKey()) {
                // 业务主表字段
                orderFieldService.setResourceFieldValue(order, fieldConfig.getBusinessKey(), fieldUpdateParam.getFieldValue());
            } else {
                // 自定义字段
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
                    orderFieldService.getResourceFieldBlobMapper().deleteByLambda(new LambdaQueryWrapper<OrderFieldBlob>()
                            .eq(OrderFieldBlob::getFieldId, fieldUpdateParam.getFieldId()).eq(OrderFieldBlob::getResourceId, postFieldParam.getResourceId()));
                    OrderFieldBlob field = new OrderFieldBlob();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
					field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    orderFieldBlobs.add(field);
                } else {
                    // 自定义表
                    orderFieldService.getResourceFieldMapper().deleteByLambda(new LambdaQueryWrapper<OrderField>()
                            .eq(OrderField::getFieldId, fieldUpdateParam.getFieldId()).eq(OrderField::getResourceId, postFieldParam.getResourceId()));
                    OrderField field = new OrderField();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
					field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    orderFields.add(field);
                }
            }
        }
        orderMapper.updateById(order);
        if (CollectionUtils.isNotEmpty(orderFields)) {
            orderFieldService.getResourceFieldMapper().batchInsert(orderFields);
        }
        if (CollectionUtils.isNotEmpty(orderFieldBlobs)) {
            orderFieldService.getResourceFieldBlobMapper().batchInsert(orderFieldBlobs);
        }
        // 更新快照
        if (snapshot != null) {
			OrderGetResponse snapshotRes = get(order, response.getModuleFields(), formConfig);
            snapshot.setOrderValue(JSON.toJSONString(snapshotRes));
            snapshotBaseMapper.update(snapshot);
        }
        // 记录审批后置字段更新日志
        baseService.handleUpdateLogWithSubTable(originOrder, order, originFields, orderFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId()),
                postFieldParam.getResourceId(), order.getName(), Translator.get("products_info"), formConfig);
        // 从 OperationLogContext 中获取日志信息并手动记录
        LogContextInfo contextInfo = OperationLogContext.getContext();
        if (contextInfo != null) {
            String orgId = OrganizationContext.getOrganizationId();
            LogDTO logDTO = new LogDTO(orgId, postFieldParam.getResourceId(), postFieldParam.getOperator(), LogType.UPDATE, LogModule.ORDER_INDEX, order.getName());
            logDTO.setOriginalValue(contextInfo.getOriginalValue());
            logDTO.setModifiedValue(contextInfo.getModifiedValue());
            logService.add(logDTO);
            OperationLogContext.clear();
        }
    }


    /**
     * 订单列表
     *
     * @param request
     * @param userId
     * @param orgId
     * @param deptDataPermission
     *
     * @return
     */
    public PagerWithOption<List<OrderListResponse>> list(OrderPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission, Boolean source) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<OrderListResponse> list = extOrderMapper.list(request, orgId, userId, deptDataPermission, source);
        List<OrderListResponse> results = buildList(list, orgId);
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(list, results, customerFormConfig);

        return PageUtils.setPageInfoWithOption(page, results, optionMap);
    }

    private Map<String, List<OptionDTO>> buildOptionMap(List<OrderListResponse> list, List<OrderListResponse> buildList,
                                                        ModuleFormConfigDTO formConfig) {
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, OrderListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFieldValues);
        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                OrderListResponse::getOwner, OrderListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.ORDER_OWNER.getBusinessKey(), ownerFieldOption);
        return optionMap;
    }

    private ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.ORDER.getKey(), orgId);
    }

    public List<OrderListResponse> buildList(List<OrderListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        List<String> orderIds = list.stream().map(OrderListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> orderFiledMap = orderFieldService.getResourceFieldMap(orderIds, true);
        Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = orderFieldService.setBusinessRefFieldValue(list, moduleFormService.getFlattenFormFields(FormKey.ORDER.getKey(), orgId), orderFiledMap);


        List<String> ownerIds = list.stream()
                .map(OrderListResponse::getOwner)
                .distinct()
                .toList();
        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        Map<String, String> stageNameMap = extOrderStageConfigMapper.getStageConfigList(orgId).stream()
                .collect(Collectors.toMap(StageConfigResponse::getId,
                        StageConfigResponse::getName));

        List<String> approvingResourceIds = list.stream().filter(item -> Strings.CI.contains(item.getApprovalStatus(), ApprovalStatus.APPROVING.name())).map(OrderListResponse::getId).toList();
        Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(approvingResourceIds, orgId);

        list.forEach(item -> {
            UserDeptDTO userDeptDTO = userDeptMap.get(item.getOwner());
            if (userDeptDTO != null) {
                item.setDepartmentId(userDeptDTO.getDeptId());
                item.setDepartmentName(userDeptDTO.getDeptName());
            }
            item.setStageName(stageNameMap.get(item.getStage()));
            // 获取自定义字段
            List<BaseModuleFieldValue> orderFields = resolvefieldValueMap.get(item.getId());
            item.setModuleFields(orderFields);
            item.setFirstApproved(firstNodeApprovedMap.get(item.getId()));
        });
        return baseService.setCreateUpdateOwnerUserName(list);
    }


    /**
     * 获取表单快照
     *
     * @param id
     * @param orgId
     *
     * @return
     */
    public ModuleFormConfigDTO getFormSnapshot(String id, String orgId) {
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }
        LambdaQueryWrapper<OrderSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSnapshot::getOrderId, id);
        OrderSnapshot snapshot = snapshotBaseMapper.selectListByLambda(wrapper).stream().findFirst().orElse(null);
        if (snapshot != null) {
            return JSON.parseObject(snapshot.getOrderProp(), ModuleFormConfigDTO.class);
        } else {
            return moduleFormCacheService.getBusinessFormConfig(FormKey.ORDER.getKey(), orgId);
        }
    }


    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.ORDER_READ, rolePermissions);
    }

    private void updateStageSnapshot(String id, String stage) {
        if (StringUtils.isBlank(stage)) {
            return;
        }
        LambdaQueryWrapper<OrderSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(OrderSnapshot::getOrderId, id);
        List<OrderSnapshot> orderSnapshots = snapshotBaseMapper.selectListByLambda(delWrapper);
        OrderSnapshot first = orderSnapshots.getFirst();
        if (first != null) {
            OrderGetResponse response = JSON.parseObject(first.getOrderValue(), OrderGetResponse.class);
            response.setStage(stage);
            first.setOrderValue(JSON.toJSONString(response));
            snapshotBaseMapper.update(first);
        }
    }

    public Order selectByPrimaryKey(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }


    @OperationLog(module = LogModule.ORDER_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void updateStage(OrderStageRequest request, String userId, String orgId) {
        Order order = orderMapper.selectByPrimaryKey(request.getId());
        if (order == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        List<StageConfigResponse> stageConfigList = extOrderStageConfigMapper.getStageConfigList(orgId);
        Map<String, String> stageMap = stageConfigList.stream()
                .collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName));

        final Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("orderStage", stageMap.get(order.getStage()));

        order.setStage(request.getStage());

        order.setUpdateTime(System.currentTimeMillis());
        order.setUpdateUser(userId);
        orderMapper.update(order);

        updateStageSnapshot(request.getId(), request.getStage());

        final Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("orderStage", stageMap.get(request.getStage()));
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(order.getName())
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
    }

    /**
     * 批量更新订单
     *
     * @param request 批量编辑参数
     * @param userId  当前用户ID
     * @param orgId   当前组织ID
     */
    public BatchAffectReasonResponse batchUpdate(ResourceBatchEditRequest request, String userId, String orgId) {
        BaseField field = orderFieldService.getAndCheckField(request.getFieldId(), orgId);
        moduleFormService.setFieldBusinessParam(field);
        List<Order> originOrders = orderMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(originOrders)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0).errorMessages(Translator.get("order_not_exist")).build();
        }

        // 校验状态权限，过滤出有权限操作的订单
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.ORDER.getValue(),
                originOrders,
                PermissionConstants.ORDER_UPDATE,
                orgId,
                Order::getId,
                Order::getApprovalStatus
        );

        if (CollectionUtils.isEmpty(permittedIds)) {
            return BatchAffectReasonResponse.builder().success(0).fail(originOrders.size()).skip(0).errorMessages(Translator.get("no.operation.permission")).build();
        }

            approvalResourceService.batchEditTriggerApproval(permittedIds, FormKey.ORDER, orgId, userId);

        List<Order> permittedOrders = originOrders.stream()
                .filter(o -> permittedIds.contains(o.getId()))
                .collect(Collectors.toList());

        ResourceBatchEditRequest filteredRequest = new ResourceBatchEditRequest();
        filteredRequest.setIds(permittedIds);
        filteredRequest.setFieldId(request.getFieldId());
        filteredRequest.setFieldValue(request.getFieldValue());

        orderFieldService.batchUpdate(filteredRequest, field, permittedOrders, Order.class, LogModule.ORDER_INDEX, extOrderMapper::batchUpdate, userId, orgId);

        ModuleFormConfigDTO moduleFormConfigDTO = getFormConfig(orgId);
        ModuleFormConfigDTO saveModuleFormConfigDTO = JSON.parseObject(JSON.toJSONString(moduleFormConfigDTO), ModuleFormConfigDTO.class);

        LambdaQueryWrapper<OrderSnapshot> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.in(OrderSnapshot::getOrderId, permittedIds);
        snapshotBaseMapper.deleteByLambda(delWrapper);

        List<Order> latestOrders = orderMapper.selectByIds(permittedIds);
        Map<String, Order> latestOrderMap = latestOrders.stream().collect(Collectors.toMap(Order::getId, item -> item));
        Map<String, List<BaseModuleFieldValue>> fieldMap = orderFieldService.getResourceFieldMap(permittedIds, true);

        List<OrderSnapshot> snapshots = new ArrayList<>();
        for (String id : permittedIds) {
            Order order = latestOrderMap.get(id);
            if (order == null) {
                continue;
            }
            List<BaseModuleFieldValue> orderFields = fieldMap.getOrDefault(id, Collections.emptyList());
            List<BaseModuleFieldValue> resolveFieldValues = moduleFormService.resolveSnapshotFields(orderFields, moduleFormConfigDTO, orderFieldService, id);
            OrderGetResponse response = get(order, resolveFieldValues, moduleFormConfigDTO);
            if (CollectionUtils.isNotEmpty(response.getModuleFields())) {
                response.setModuleFields(response.getModuleFields().stream()
                        .filter(f -> f.getFieldValue() != null
                                && StringUtils.isNotBlank(f.getFieldValue().toString())
                                && !"[]".equals(f.getFieldValue().toString()))
                        .toList());
            }
            OrderSnapshot snapshot = new OrderSnapshot();
            snapshot.setId(IDGenerator.nextStr());
            snapshot.setOrderId(id);
            snapshot.setOrderProp(JSON.toJSONString(saveModuleFormConfigDTO));
            snapshot.setOrderValue(JSON.toJSONString(response));
            snapshots.add(snapshot);
        }
        if (CollectionUtils.isNotEmpty(snapshots)) {
            snapshotBaseMapper.batchInsert(snapshots);
        }

        return BatchAffectReasonResponse.builder().success(permittedIds.size()).fail(originOrders.size() - permittedIds.size()).skip(0).errorMessages(Translator.get("batch.update.reason")).build();
    }

    public void download(String id, String userId, String organizationId) {
        OrderGetResponse getResponse = get(id, organizationId);
        if (getResponse == null) {
            throw new GenericException(Translator.get("order_not_exist"));
        }

        LogDTO logDTO = new LogDTO(organizationId, id, userId, LogType.DOWNLOAD, LogModule.ORDER_INDEX, getResponse.getName());
        logDTO.setOriginalValue(getResponse.getName());
        logService.add(logDTO);
    }


    /**
     * 统计
     *
     * @param request
     * @param userId
     * @param orgId
     * @param deptDataPermission
     *
     * @return
     */
    public OrderStatisticResponse searchStatistic(BaseCondition request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        OrderStatisticResponse response = extOrderMapper.searchStatistic(request, orgId, userId, deptDataPermission);
        return Optional.ofNullable(response).orElse(new OrderStatisticResponse());
    }


    /**
     * 通过ID集合获取订单名称
     *
     * @param ids id集合
     *
     * @return 工商表头名称
     */
    public Object getOrderNameByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return StringUtils.EMPTY;
        }
        List<Order> records = orderMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(records)) {
            List<String> names = records.stream().map(Order::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }


    /**
     * 通过名称获取订单集合
     *
     * @param names 名称
     *
     * @return 订单名称
     */
    public List<Order> getOrderListByNames(List<String> names) {
        LambdaQueryWrapper<Order> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Order::getName, names);
        return orderMapper.selectListByLambda(lambdaQueryWrapper);
    }

    public Object getOrderName(String id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order != null) {
            return order.getName();
        }
        return null;
    }


    /**
     * 阶段看板排序
     *
     * @param request
     * @param userId
     */
    public void sort(StageSortRequest request, String userId) {
        //拖拽节点
        Order order = orderMapper.selectByPrimaryKey(request.getDragNodeId());
        if (order == null) {
            throw new GenericException(Translator.get("order_not_exist"));
        }
        Long pos = DEFAULT_POS;
        if (StringUtils.isNotBlank(request.getDropNodeId())) {
            //放入节点
            Order dropNode = orderMapper.selectByPrimaryKey(request.getDropNodeId());
            pos = dropNode.getPos();
            if (request.getDropPosition() == -1) {

                extOrderMapper.moveUpStageOrder(pos, request.getStage(), DEFAULT_POS);
                pos = pos + 1;
            } else {
                extOrderMapper.moveDownStageOrder(pos, request.getStage(), DEFAULT_POS);
            }
        }
        Order dragOrder = new Order();
        dragOrder.setId(request.getDragNodeId());
        dragOrder.setPos(pos);
        dragOrder.setStage(request.getStage());
        dragOrder.setUpdateUser(userId);
        dragOrder.setUpdateTime(System.currentTimeMillis());
        orderMapper.updateById(dragOrder);

        updateStageSnapshot(request.getDragNodeId(), request.getStage());

    }

    /**
     * 处理旧版本审批状态 (APPROVING => NONE)
     */
    public void handleOldApprovalData() {
        List<Order> orders = orderMapper.selectAll(null);
        orders.forEach(order -> {
            ResourceSnapshotApprovalParam param = ResourceSnapshotApprovalParam
                    .builder()
                    .resourceId(order.getId())
                    .approvalStatus(ApprovalStatus.NONE.name())
                    .build();
            updateSnapshotApprovalStatus(param);
        });
        extOrderMapper.updateOldApprovalStatusNone();
    }
}
