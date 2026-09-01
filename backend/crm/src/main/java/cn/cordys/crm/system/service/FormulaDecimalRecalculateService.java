package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractField;
import cn.cordys.crm.contract.domain.ContractSnapshot;
import cn.cordys.crm.contract.dto.response.ContractGetResponse;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.order.domain.OrderField;
import cn.cordys.crm.order.domain.OrderSnapshot;
import cn.cordys.crm.order.dto.response.OrderGetResponse;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.FormulaField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 已有订单/合同公式字段展示精度重算。
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FormulaDecimalRecalculateService {

    private static final int BATCH_SIZE = 200;
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_ERROR = "ERROR";
    private static final String CONTRACT_TOTAL_AMOUNT_KEY = "contractTotalAmount";
    private static final String CONTRACT_PRODUCTS_KEY = "contractProducts";
    private static final String CONTRACT_PRODUCT_AMOUNT_KEY = "contractProductSumAmount";
    private static final String ORDER_TOTAL_AMOUNT_KEY = "orderAmount";
    private static final String ORDER_PRODUCTS_KEY = "orderProducts";
    private static final String ORDER_PRODUCT_AMOUNT_KEY = "orderProductAmount";

    private final Map<String, RecalculateTask> taskMap = new ConcurrentHashMap<>();

    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private BaseMapper<ContractField> contractFieldMapper;
    @Resource
    private BaseMapper<ContractSnapshot> contractSnapshotMapper;
    @Resource
    private BaseMapper<Order> orderMapper;
    @Resource
    private BaseMapper<OrderField> orderFieldMapper;
    @Resource
    private BaseMapper<OrderSnapshot> orderSnapshotMapper;

    /**
     * 后台重算合同和订单, 立即返回任务ID。
     */
    public String startRecalculate(String orgId) {
        return startOrGetRecalculateTask(orgId).getTaskId();
    }

    /**
     * 后台重算合同, 立即返回任务ID。
     */
    public String startRecalculateContract(String orgId) {
        return startOrGetContractTask(orgId).getTaskId();
    }

    /**
     * 后台重算订单, 立即返回任务ID。
     */
    public String startRecalculateOrder(String orgId) {
        return startOrGetOrderTask(orgId).getTaskId();
    }

    /**
     * 存在运行中任务时返回进度, 否则启动合同和订单重算任务。
     */
    public synchronized RecalculateTask startOrGetRecalculateTask(String orgId) {
        RecalculateTask runningTask = getRunningTask();
        if (runningTask != null) {
            return runningTask;
        }
        RecalculatePlan plan = buildPlan(orgId, true, true);
        return startBackgroundTask(orgId, plan, () -> recalculate(plan));
    }

    /**
     * 存在运行中任务时返回进度, 否则启动合同重算任务。
     */
    public synchronized RecalculateTask startOrGetContractTask(String orgId) {
        RecalculateTask runningTask = getRunningTask();
        if (runningTask != null) {
            return runningTask;
        }
        RecalculatePlan plan = buildPlan(orgId, true, false);
        return startBackgroundTask(orgId, plan, () -> recalculate(plan));
    }

    /**
     * 存在运行中任务时返回进度, 否则启动订单重算任务。
     */
    public synchronized RecalculateTask startOrGetOrderTask(String orgId) {
        RecalculateTask runningTask = getRunningTask();
        if (runningTask != null) {
            return runningTask;
        }
        RecalculatePlan plan = buildPlan(orgId, false, true);
        return startBackgroundTask(orgId, plan, () -> recalculate(plan));
    }

    public RecalculateTask getTask(String taskId) {
        return taskMap.get(taskId);
    }

    public RecalculateResult recalculate(String orgId) {
        if (StringUtils.isBlank(orgId)) {
            return recalculateAllOrganizations();
        }
        return recalculate(buildPlan(orgId, true, true));
    }

    public RecalculateResult recalculateAllOrganizations() {
        RecalculateResult result = new RecalculateResult();
        getOrganizationIds(contractMapper.selectAll(null), Contract::getOrganizationId)
                .forEach(orgId -> result.merge(recalculateContract(orgId)));
        getOrganizationIds(orderMapper.selectAll(null), Order::getOrganizationId)
                .forEach(orgId -> result.merge(recalculateOrder(orgId)));
        return result;
    }

    public RecalculateResult recalculateContract(String orgId) {
        return recalculate(buildPlan(orgId, true, false));
    }

    public RecalculateResult recalculateOrder(String orgId) {
        return recalculate(buildPlan(orgId, false, true));
    }

    private RecalculateResult recalculate(RecalculatePlan plan) {
        RecalculateResult result = new RecalculateResult();
        if (plan == null) {
            return result;
        }
        for (List<String> batchIds : partition(plan.getContractIds())) {
            result.merge(recalculateBatch(batchIds, plan.getTask(), id -> recalculateContractResource(id,
                    plan.getContractFormulaFieldMap(), plan.getContractAmountFieldConfig())));
        }
        for (List<String> batchIds : partition(plan.getOrderIds())) {
            result.merge(recalculateBatch(batchIds, plan.getTask(), id -> recalculateOrderResource(id,
                    plan.getOrderFormulaFieldMap(), plan.getOrderAmountFieldConfig())));
        }
        return result;
    }

    private RecalculateTask startBackgroundTask(String orgId, RecalculatePlan plan, RecalculateRunner runner) {
        RecalculateTask task = new RecalculateTask();
        String taskId = IDGenerator.nextStr();
        task.setTaskId(taskId);
        task.setOrgId(orgId);
        task.setStatus(STATUS_RUNNING);
        task.setStartTime(System.currentTimeMillis());
        task.setTotal(plan.getTotal());
        plan.setTask(task);
        taskMap.put(taskId, task);

        Thread.startVirtualThread(() -> {
            try {
                RecalculateResult result = runner.run();
                task.setResult(result);
                task.setStatus(STATUS_SUCCESS);
                log.info("Formula decimal recalculate task success, taskId: {}, result: {}", taskId, JSON.toJSONString(result));
            } catch (Exception e) {
                task.setStatus(STATUS_ERROR);
                task.setErrorMessage(e.getMessage());
                log.error("Formula decimal recalculate task failed, taskId: {}", taskId, e);
            } finally {
                task.setEndTime(System.currentTimeMillis());
            }
        });
        return task;
    }

    private RecalculateTask getRunningTask() {
        return taskMap.values().stream()
                .filter(task -> Strings.CS.equals(task.getStatus(), STATUS_RUNNING))
                .findFirst()
                .orElse(null);
    }

    private RecalculatePlan buildPlan(String orgId, boolean includeContract, boolean includeOrder) {
        RecalculatePlan plan = new RecalculatePlan();
        if (StringUtils.isBlank(orgId)) {
            return plan;
        }
        plan.setOrgId(orgId);
        if (includeContract) {
            ModuleFormConfigDTO formConfig = getFormConfig(FormKey.CONTRACT, orgId);
            Map<String, FormulaField> formulaFieldMap = getFormulaFieldMap(formConfig);
            if (!formulaFieldMap.isEmpty()) {
                List<String> contractIds = contractMapper.selectListByLambda(new LambdaQueryWrapper<Contract>()
                                .eq(Contract::getOrganizationId, orgId))
                        .stream()
                        .map(Contract::getId)
                        .toList();
                plan.setContractFormulaFieldMap(formulaFieldMap);
                plan.setContractAmountFieldConfig(getAmountFieldConfig(formConfig, CONTRACT_TOTAL_AMOUNT_KEY,
                        CONTRACT_PRODUCTS_KEY, CONTRACT_PRODUCT_AMOUNT_KEY));
                plan.setContractIds(contractIds);
            }
        }
        if (includeOrder) {
            ModuleFormConfigDTO formConfig = getFormConfig(FormKey.ORDER, orgId);
            Map<String, FormulaField> formulaFieldMap = getFormulaFieldMap(formConfig);
            if (!formulaFieldMap.isEmpty()) {
                List<String> orderIds = orderMapper.selectListByLambda(new LambdaQueryWrapper<Order>()
                                .eq(Order::getOrganizationId, orgId))
                        .stream()
                        .map(Order::getId)
                        .toList();
                plan.setOrderFormulaFieldMap(formulaFieldMap);
                plan.setOrderAmountFieldConfig(getAmountFieldConfig(formConfig, ORDER_TOTAL_AMOUNT_KEY,
                        ORDER_PRODUCTS_KEY, ORDER_PRODUCT_AMOUNT_KEY));
                plan.setOrderIds(orderIds);
            }
        }
        plan.setTotal(plan.getContractIds().size() + plan.getOrderIds().size());
        return plan;
    }

    private RecalculateResult recalculateContractResource(String contractId, Map<String, FormulaField> formulaFieldMap,
                                                          AmountFieldConfig amountFieldConfig) {
        RecalculateResult result = new RecalculateResult();
        result.merge(recalculateContractOriginalFields(contractId, formulaFieldMap));
        result.addBusinessAmount(recalculateContractAmount(contractId, amountFieldConfig));

        List<ContractSnapshot> snapshots = contractSnapshotMapper.selectListByLambda(new LambdaQueryWrapper<ContractSnapshot>()
                .eq(ContractSnapshot::getContractId, contractId));
        for (ContractSnapshot snapshot : snapshots) {
            boolean propChanged = syncSnapshotProp(snapshot.getContractProp(), formulaFieldMap, snapshot::setContractProp);
            SnapshotValueRecalculateResult valueResult = recalculateSnapshotValue(snapshot.getContractValue(), ContractGetResponse.class,
                    formulaFieldMap, amountFieldConfig, snapshot::setContractValue);
            if (propChanged || valueResult.isChanged()) {
                contractSnapshotMapper.update(snapshot);
            }
            result.addSnapshotProp(propChanged);
            result.addSnapshotValue(valueResult.isChanged());
            result.addSnapshotAmount(valueResult.isAmountChanged());
        }
        return result;
    }

    private RecalculateResult recalculateOrderResource(String orderId, Map<String, FormulaField> formulaFieldMap,
                                                       AmountFieldConfig amountFieldConfig) {
        RecalculateResult result = new RecalculateResult();
        result.merge(recalculateOrderOriginalFields(orderId, formulaFieldMap));
        result.addBusinessAmount(recalculateOrderAmount(orderId, amountFieldConfig));

        List<OrderSnapshot> snapshots = orderSnapshotMapper.selectListByLambda(new LambdaQueryWrapper<OrderSnapshot>()
                .eq(OrderSnapshot::getOrderId, orderId));
        for (OrderSnapshot snapshot : snapshots) {
            boolean propChanged = syncSnapshotProp(snapshot.getOrderProp(), formulaFieldMap, snapshot::setOrderProp);
            SnapshotValueRecalculateResult valueResult = recalculateSnapshotValue(snapshot.getOrderValue(), OrderGetResponse.class,
                    formulaFieldMap, amountFieldConfig, snapshot::setOrderValue);
            if (propChanged || valueResult.isChanged()) {
                orderSnapshotMapper.update(snapshot);
            }
            result.addSnapshotProp(propChanged);
            result.addSnapshotValue(valueResult.isChanged());
            result.addSnapshotAmount(valueResult.isAmountChanged());
        }
        return result;
    }

    private ModuleFormConfigDTO getFormConfig(FormKey formKey, String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(formKey.getKey(), orgId);
    }

    private RecalculateResult recalculateOrderOriginalFields(String resourceId, Map<String, FormulaField> formulaFieldMap) {
        RecalculateResult result = new RecalculateResult();
        List<OrderField> fields = orderFieldMapper.selectListByLambda(new LambdaQueryWrapper<OrderField>()
                .eq(OrderField::getResourceId, resourceId)
                .in(OrderField::getFieldId, new ArrayList<>(formulaFieldMap.keySet())));
        for (OrderField field : fields) {
            FormulaField formulaField = formulaFieldMap.get(field.getFieldId());
            Object recalculatedValue = recalculateValue(formulaField, field.getFieldValue());
            if (!valueChanged(field.getFieldValue(), recalculatedValue)) {
                continue;
            }
            field.setFieldValue(recalculatedValue);
            orderFieldMapper.update(field);
            result.originalFields++;
        }
        return result;
    }

    private RecalculateResult recalculateContractOriginalFields(String resourceId, Map<String, FormulaField> formulaFieldMap) {
        RecalculateResult result = new RecalculateResult();
        List<ContractField> fields = contractFieldMapper.selectListByLambda(new LambdaQueryWrapper<ContractField>()
                .eq(ContractField::getResourceId, resourceId)
                .in(ContractField::getFieldId, new ArrayList<>(formulaFieldMap.keySet())));
        for (ContractField field : fields) {
            FormulaField formulaField = formulaFieldMap.get(field.getFieldId());
            Object recalculatedValue = recalculateValue(formulaField, field.getFieldValue());
            if (!valueChanged(field.getFieldValue(), recalculatedValue)) {
                continue;
            }
            field.setFieldValue(recalculatedValue);
            contractFieldMapper.update(field);
            result.originalFields++;
        }
        return result;
    }

    private boolean recalculateContractAmount(String contractId, AmountFieldConfig amountFieldConfig) {
        if (!amountFieldConfig.valid()) {
            return false;
        }
        BigDecimal amount = calculateContractAmount(contractId, amountFieldConfig);
        Contract contract = contractMapper.selectByPrimaryKey(contractId);
        if (contract == null || !amountChanged(contract.getAmount(), amount)) {
            return false;
        }
        Contract update = new Contract();
        update.setId(contractId);
        update.setAmount(amount);
        contractMapper.update(update);
        return true;
    }

    private boolean recalculateOrderAmount(String orderId, AmountFieldConfig amountFieldConfig) {
        if (!amountFieldConfig.valid()) {
            return false;
        }
        BigDecimal amount = calculateOrderAmount(orderId, amountFieldConfig);
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null || !amountChanged(order.getAmount(), amount)) {
            return false;
        }
        Order update = new Order();
        update.setId(orderId);
        update.setAmount(amount);
        orderMapper.update(update);
        return true;
    }

    private BigDecimal calculateContractAmount(String contractId, AmountFieldConfig amountFieldConfig) {
        List<ContractField> amountFields = contractFieldMapper.selectListByLambda(new LambdaQueryWrapper<ContractField>()
                .eq(ContractField::getResourceId, contractId)
                .eq(ContractField::getRefSubId, amountFieldConfig.getSubTableFieldId())
                .eq(ContractField::getFieldId, amountFieldConfig.getAmountSubFieldId()));
        return formatBusinessAmount(amountFieldConfig, sumFieldValues(amountFields.stream()
                .map(ContractField::getFieldValue)
                .toList()));
    }

    private BigDecimal calculateOrderAmount(String orderId, AmountFieldConfig amountFieldConfig) {
        List<OrderField> amountFields = orderFieldMapper.selectListByLambda(new LambdaQueryWrapper<OrderField>()
                .eq(OrderField::getResourceId, orderId)
                .eq(OrderField::getRefSubId, amountFieldConfig.getSubTableFieldId())
                .eq(OrderField::getFieldId, amountFieldConfig.getAmountSubFieldId()));
        return formatBusinessAmount(amountFieldConfig, sumFieldValues(amountFields.stream()
                .map(OrderField::getFieldValue)
                .toList()));
    }

    private RecalculateResult recalculateBatch(List<String> resourceIds, RecalculateTask task, Function<String, RecalculateResult> resourceHandler) {
        RecalculateResult result = new RecalculateResult();
        if (CollectionUtils.isEmpty(resourceIds)) {
            return result;
        }
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            ExecutorCompletionService<RecalculateResult> completionService = new ExecutorCompletionService<>(executor);
            for (String resourceId : resourceIds) {
                completionService.submit(() -> resourceHandler.apply(resourceId));
            }
            for (int i = 0; i < resourceIds.size(); i++) {
                try {
                    Future<RecalculateResult> future = completionService.take();
                    result.merge(future.get());
                } catch (Exception e) {
                    result.failures++;
                    log.warn("Formula decimal recalculate resource failed", e);
                } finally {
                    if (task != null) {
                        task.incrementProcessed();
                    }
                }
            }
        }
        return result;
    }

    private List<List<String>> partition(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        List<List<String>> partitions = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
            partitions.add(ids.subList(start, Math.min(start + BATCH_SIZE, ids.size())));
        }
        return partitions;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object recalculateValue(FormulaField formulaField, Object value) {
        if (value == null || formulaField == null) {
            return value;
        }
        AbstractModuleFieldResolver resolver = ModuleFieldResolverFactory.getResolver(FieldType.FORMULA.name());
        return resolver.transformToValue(formulaField, normalizeNumberText(value));
    }

    private String normalizeNumberText(Object value) {
        return value.toString().replace(",", "");
    }

    private SnapshotValueRecalculateResult recalculateSnapshotValue(String snapshotValue,
                                                                    Class<?> responseClass,
                                                                    Map<String, FormulaField> formulaFieldMap,
                                                                    AmountFieldConfig amountFieldConfig,
                                                                    Consumer<String> setter) {
        SnapshotValueRecalculateResult result = new SnapshotValueRecalculateResult();
        if (StringUtils.isBlank(snapshotValue)) {
            return result;
        }
        try {
            Object response = JSON.parseObject(snapshotValue, responseClass);
            List<BaseModuleFieldValue> moduleFields = getModuleFields(response);
            boolean moduleFieldsChanged = recalculateModuleFields(moduleFields, formulaFieldMap);
            boolean amountChanged = recalculateSnapshotAmount(response, moduleFields, amountFieldConfig);
            if (!moduleFieldsChanged && !amountChanged) {
                return result;
            }
            setter.accept(JSON.toJSONString(response));
            result.setChanged(true);
            result.setAmountChanged(amountChanged);
            return result;
        } catch (Exception e) {
            log.warn("Recalculate snapshot formula value failed", e);
            return result;
        }
    }

    private List<BaseModuleFieldValue> getModuleFields(Object response) {
        if (response instanceof ContractGetResponse contractResponse) {
            return contractResponse.getModuleFields();
        }
        if (response instanceof OrderGetResponse orderResponse) {
            return orderResponse.getModuleFields();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private boolean recalculateModuleFields(List<BaseModuleFieldValue> moduleFields, Map<String, FormulaField> formulaFieldMap) {
        if (CollectionUtils.isEmpty(moduleFields)) {
            return false;
        }
        boolean changed = false;
        for (BaseModuleFieldValue moduleField : moduleFields) {
            FormulaField formulaField = formulaFieldMap.get(moduleField.getFieldId());
            if (formulaField != null) {
                Object recalculatedValue = recalculateValue(formulaField, moduleField.getFieldValue());
                if (valueChanged(moduleField.getFieldValue(), recalculatedValue)) {
                    moduleField.setFieldValue(recalculatedValue);
                    changed = true;
                }
                continue;
            }
            if (!(moduleField.getFieldValue() instanceof List<?> rows)) {
                continue;
            }
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> rowMap)) {
                    continue;
                }
                for (Map.Entry<?, ?> entry : rowMap.entrySet()) {
                    FormulaField subFormulaField = formulaFieldMap.get(entry.getKey().toString());
                    if (subFormulaField == null) {
                        continue;
                    }
                    Object recalculatedValue = recalculateValue(subFormulaField, entry.getValue());
                    if (valueChanged(entry.getValue(), recalculatedValue)) {
                        ((Map<Object, Object>) rowMap).put(entry.getKey(), recalculatedValue);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private boolean recalculateSnapshotAmount(Object response, List<BaseModuleFieldValue> moduleFields, AmountFieldConfig amountFieldConfig) {
        if (!amountFieldConfig.valid() || CollectionUtils.isEmpty(moduleFields)) {
            return false;
        }
        BigDecimal amount = calculateSnapshotAmount(moduleFields, amountFieldConfig);
        if (response instanceof ContractGetResponse contractResponse) {
            return updateSnapshotAmount(contractResponse.getAmount(), amount, contractResponse::setAmount);
        }
        if (response instanceof OrderGetResponse orderResponse) {
            return updateSnapshotAmount(orderResponse.getAmount(), amount, orderResponse::setAmount);
        }
        return false;
    }

    private BigDecimal calculateSnapshotAmount(List<BaseModuleFieldValue> moduleFields, AmountFieldConfig amountFieldConfig) {
        return moduleFields.stream()
                .filter(moduleField -> Strings.CS.equals(moduleField.getFieldId(), amountFieldConfig.getSubTableFieldId()))
                .findFirst()
                .map(BaseModuleFieldValue::getFieldValue)
                .map(value -> sumSnapshotSubTableAmount(value, amountFieldConfig.getAmountSubFieldId()))
                .map(amount -> formatBusinessAmount(amountFieldConfig, amount))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal sumSnapshotSubTableAmount(Object subTableValue, String amountSubFieldId) {
        if (!(subTableValue instanceof List<?> rows)) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> rowMap)) {
                continue;
            }
            amount = amount.add(toBigDecimal(rowMap.get(amountSubFieldId)));
        }
        return amount;
    }

    private boolean updateSnapshotAmount(BigDecimal currentAmount, BigDecimal newAmount, Consumer<BigDecimal> setter) {
        if (!amountChanged(currentAmount, newAmount)) {
            return false;
        }
        setter.accept(newAmount);
        return true;
    }

    private boolean syncSnapshotProp(String snapshotProp, Map<String, FormulaField> currentFormulaFieldMap, Consumer<String> setter) {
        if (StringUtils.isBlank(snapshotProp)) {
            return false;
        }
        try {
            ModuleFormConfigDTO snapshotConfig = JSON.parseObject(snapshotProp, ModuleFormConfigDTO.class);
            boolean changed = syncFormulaConfig(snapshotConfig.getFields(), currentFormulaFieldMap);
            if (changed) {
                setter.accept(JSON.toJSONString(snapshotConfig));
            }
            return changed;
        } catch (Exception e) {
            log.warn("Sync snapshot formula prop failed", e);
            return false;
        }
    }

    private boolean syncFormulaConfig(List<BaseField> fields, Map<String, FormulaField> currentFormulaFieldMap) {
        if (CollectionUtils.isEmpty(fields)) {
            return false;
        }
        boolean changed = false;
        for (BaseField field : fields) {
            if (field instanceof FormulaField snapshotFormulaField) {
                FormulaField currentFormulaField = currentFormulaFieldMap.get(snapshotFormulaField.getId());
                if (currentFormulaField != null) {
                    changed |= syncFormulaConfig(snapshotFormulaField, currentFormulaField);
                }
            }
            if (field instanceof SubField subField) {
                changed |= syncFormulaConfig(subField.getSubFields(), currentFormulaFieldMap);
            }
        }
        return changed;
    }

    private boolean syncFormulaConfig(FormulaField snapshotField, FormulaField currentField) {
        boolean changed = false;
        if (!Strings.CS.equals(snapshotField.getFormulaResultFormat(), currentField.getFormulaResultFormat())) {
            snapshotField.setFormulaResultFormat(currentField.getFormulaResultFormat());
            changed = true;
        }
        if (!equals(snapshotField.getDecimalPlaces(), currentField.getDecimalPlaces())) {
            snapshotField.setDecimalPlaces(currentField.getDecimalPlaces());
            changed = true;
        }
        if (snapshotField.getPrecision() != currentField.getPrecision()) {
            snapshotField.setPrecision(currentField.getPrecision());
            changed = true;
        }
        if (!equals(snapshotField.getShowThousandsSeparator(), currentField.getShowThousandsSeparator())) {
            snapshotField.setShowThousandsSeparator(currentField.getShowThousandsSeparator());
            changed = true;
        }
        return changed;
    }

    private Map<String, FormulaField> getFormulaFieldMap(ModuleFormConfigDTO formConfig) {
        if (formConfig == null || CollectionUtils.isEmpty(formConfig.getFields())) {
            return Map.of();
        }
        ModuleFormConfigDTO copiedConfig = JSON.parseObject(JSON.toJSONString(formConfig), ModuleFormConfigDTO.class);
        return moduleFormService.flattenFormAllFields(copiedConfig).stream()
                .filter(field -> field instanceof FormulaField)
                .map(field -> (FormulaField) field)
                .filter(field -> Strings.CI.equals(field.getFormulaResultFormat(), "number"))
                .collect(Collectors.toMap(FormulaField::getId, Function.identity(), (prev, next) -> next, LinkedHashMap::new));
    }

    private AmountFieldConfig getAmountFieldConfig(ModuleFormConfigDTO formConfig,
                                                   String totalAmountInternalKey,
                                                   String subTableInternalKey,
                                                   String amountSubFieldInternalKey) {
        AmountFieldConfig amountFieldConfig = new AmountFieldConfig();
        if (formConfig == null || CollectionUtils.isEmpty(formConfig.getFields())) {
            return amountFieldConfig;
        }
        for (BaseField field : formConfig.getFields()) {
            if (field instanceof FormulaField formulaField
                    && Strings.CS.equals(formulaField.getInternalKey(), totalAmountInternalKey)) {
                amountFieldConfig.setTotalAmountField(formulaField);
                continue;
            }
            if (!(field instanceof SubField subField) || !Strings.CS.equals(subField.getInternalKey(), subTableInternalKey)) {
                continue;
            }
            amountFieldConfig.setSubTableFieldId(subField.getId());
            if (CollectionUtils.isEmpty(subField.getSubFields())) {
                continue;
            }
            subField.getSubFields().stream()
                    .filter(subTableField -> Strings.CS.equals(subTableField.getInternalKey(), amountSubFieldInternalKey))
                    .findFirst()
                    .ifPresent(subTableField -> amountFieldConfig.setAmountSubFieldId(subTableField.getId()));
        }
        return amountFieldConfig;
    }

    private BigDecimal formatBusinessAmount(AmountFieldConfig amountFieldConfig, BigDecimal amount) {
        Object formattedValue = recalculateValue(amountFieldConfig.getTotalAmountField(), toPlainAmount(amount));
        return toBigDecimal(formattedValue);
    }

    private BigDecimal sumFieldValues(List<?> values) {
        if (CollectionUtils.isEmpty(values)) {
            return BigDecimal.ZERO;
        }
        return values.stream()
                .map(this::toBigDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null || StringUtils.isBlank(value.toString())) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(normalizeNumberText(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String toPlainAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.toPlainString();
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private boolean amountChanged(BigDecimal currentAmount, BigDecimal newAmount) {
        if (currentAmount == null) {
            return newAmount != null && newAmount.compareTo(BigDecimal.ZERO) != 0;
        }
        if (newAmount == null) {
            return currentAmount.compareTo(BigDecimal.ZERO) != 0;
        }
        return currentAmount.compareTo(newAmount) != 0;
    }

    private <T> Set<String> getOrganizationIds(List<T> resources, Function<T, String> organizationGetter) {
        if (CollectionUtils.isEmpty(resources)) {
            return Set.of();
        }
        return resources.stream()
                .map(organizationGetter)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean valueChanged(Object oldValue, Object newValue) {
        return !Strings.CS.equals(oldValue == null ? null : oldValue.toString(), newValue == null ? null : newValue.toString());
    }

    private boolean equals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    @FunctionalInterface
    private interface RecalculateRunner {
        RecalculateResult run();
    }

    @Data
    private static class RecalculatePlan {
        private String orgId;
        private List<String> contractIds = List.of();
        private Map<String, FormulaField> contractFormulaFieldMap = Map.of();
        private AmountFieldConfig contractAmountFieldConfig = new AmountFieldConfig();
        private List<String> orderIds = List.of();
        private Map<String, FormulaField> orderFormulaFieldMap = Map.of();
        private AmountFieldConfig orderAmountFieldConfig = new AmountFieldConfig();
        private long total;
        private RecalculateTask task;
    }

    @Data
    private static class AmountFieldConfig {
        private FormulaField totalAmountField;
        private String subTableFieldId;
        private String amountSubFieldId;

        public boolean valid() {
            return totalAmountField != null
                    && StringUtils.isNotBlank(subTableFieldId)
                    && StringUtils.isNotBlank(amountSubFieldId);
        }
    }

    @Data
    private static class SnapshotValueRecalculateResult {
        private boolean changed;
        private boolean amountChanged;
    }

    @Data
    public static class RecalculateTask {
        private String taskId;
        private String orgId;
        private String status;
        private String errorMessage;
        private long startTime;
        private long endTime;
        private volatile long total;
        private volatile long processed;
        private RecalculateResult result;

        public synchronized void incrementProcessed() {
            processed++;
        }
    }

    @Data
    public static class RecalculateResult {
        private long originalFields;
        private long snapshotValues;
        private long snapshotProps;
        private long businessAmounts;
        private long snapshotAmounts;
        private long failures;

        public void addSnapshotValue(boolean changed) {
            if (changed) {
                snapshotValues++;
            }
        }

        public void addSnapshotProp(boolean changed) {
            if (changed) {
                snapshotProps++;
            }
        }

        public void addBusinessAmount(boolean changed) {
            if (changed) {
                businessAmounts++;
            }
        }

        public void addSnapshotAmount(boolean changed) {
            if (changed) {
                snapshotAmounts++;
            }
        }

        public void merge(RecalculateResult other) {
            if (other == null) {
                return;
            }
            originalFields += other.originalFields;
            snapshotValues += other.snapshotValues;
            snapshotProps += other.snapshotProps;
            businessAmounts += other.businessAmounts;
            snapshotAmounts += other.snapshotAmounts;
            failures += other.failures;
        }
    }
}
