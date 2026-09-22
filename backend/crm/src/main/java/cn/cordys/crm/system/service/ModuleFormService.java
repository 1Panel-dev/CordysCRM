package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.LinkScenarioKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.resolver.field.TextMultipleResolver;
import cn.cordys.common.resolver.field.TextResolver;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.common.service.FieldSourceServiceProvider;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.constants.BusinessTitleConstants;
import cn.cordys.crm.contract.constants.SystemFieldConstants;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.service.CustomFormDataFieldService;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.InternalDetailTab;
import cn.cordys.crm.system.constants.StatisticDataScope;
import cn.cordys.crm.system.constants.StatisticType;
import cn.cordys.crm.system.constants.StatisticUpdateScope;
import cn.cordys.crm.system.domain.*;
import cn.cordys.crm.system.dto.TransformSourceApplyDTO;
import cn.cordys.crm.system.dto.field.*;
import cn.cordys.crm.system.dto.field.base.*;
import cn.cordys.crm.system.dto.form.FormLinkFill;
import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.form.base.LinkField;
import cn.cordys.crm.system.dto.form.base.LinkScenario;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.FormPropLogDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigLogDTO;
import cn.cordys.crm.system.dto.response.RelatedFormDTO;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ModuleFormService {

    public static final Map<String, String> TYPE_SOURCE_MAP;
    private static final String DEFAULT_ORGANIZATION_ID = "100001";
    private static final String CONTROL_RULES_KEY = "showControlRules";
    private static final String SHOW_FIELD_KEY = "showFields";
    private static final String SUB_FIELDS = "subFields";
    public static final String SUM_PREFIX = "sum_";
    public static final String EXPORT_SYSTEM_TYPE = "system";
    private static final String PRICE_SUB_ROW_KEY = "price_sub";
    private static final String OPTION_DEFAULT_SOURCE = "custom";
    private static final String UPGRADE_EXT_FIELD = "ext_ver";
    public static final String SLASH = "/";
    private static final String REF_UNDERLINE = "_ref_";

    static {
        TYPE_SOURCE_MAP = Map.ofEntries(
                Map.entry(FieldType.MEMBER.name(), "sys_user"),
                Map.entry(FieldType.DEPARTMENT.name(), "sys_department"),
                Map.entry(FieldSourceType.CUSTOMER.name(), "customer"),
                Map.entry(FieldSourceType.CLUE.name(), "clue"),
                Map.entry(FieldSourceType.CONTACT.name(), "customer_contact"),
                Map.entry(FieldSourceType.OPPORTUNITY.name(), "opportunity"),
                Map.entry(FieldSourceType.PRODUCT.name(), "product"),
                Map.entry(FieldSourceType.QUOTATION.name(), "opportunity_quotation"),
                Map.entry(FieldSourceType.PRICE.name(), "product_price"),
                Map.entry(FieldSourceType.CONTRACT.name(), "contract"),
                Map.entry(FieldSourceType.PAYMENT_PLAN.name(), "contract_payment_plan"),
                Map.entry(FieldSourceType.BUSINESS_TITLE.name(), "business_title"),
                Map.entry(FieldSourceType.CONTRACT_PAYMENT_RECORD.name(), "contract_payment_record"),
                Map.entry(FieldSourceType.ORDER.name(), "sales_order"),
                Map.entry(FieldSourceType.INVOICE.name(), "contract_invoice")
        );
    }

    @Value("classpath:form/form.json")
    private org.springframework.core.io.Resource formResource;
    @Value("classpath:form/field.json")
    private org.springframework.core.io.Resource fieldResource;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<ModuleFormBlob> moduleFormBlobMapper;
    @Resource
    private BaseMapper<CustomForm> customFormMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;
    @Resource
    private ExtModuleFieldMapper extModuleFieldMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private SerialNumGenerator serialNumGenerator;
    @Lazy
    @Resource
    private FieldSourceServiceProvider fieldSourceServiceProvider;
    @Resource
    private ModuleFieldService moduleFieldService;
    /**
     * 统计字段刷新需要读取表单配置, 延迟注入避免与表单数据读写形成构造期循环依赖。
     */
    @Lazy
    @Resource
    private StatisticFieldService statisticFieldService;
    private static final String REF_SYMBOL = "🔗";

    /**
     * 获取模块表单配置
     *
     * @param formKey      表单Key
     * @param currentOrgId 当前组织ID
     * @return 字段集合
     */
    public ModuleFormConfigDTO getConfig(String formKey, String currentOrgId) {
        ModuleFormConfigDTO formConfig = new ModuleFormConfigDTO();
        // set form
        ModuleForm form = getModuleFormByKey(formKey, currentOrgId);

        ModuleFormBlob formBlob = moduleFormBlobMapper.selectByPrimaryKey(form.getId());
        FormProp formProp = JSON.parseObject(formBlob.getProp(), FormProp.class);
        // 配置中持久化的是关联 ID，读取时以当前表单、字段名称刷新 Option 回显。
        resolveDetailTabs(form.getFormKey(), currentOrgId, formProp);
        formConfig.setFormProp(formProp);
        // set fields
        formConfig.setFields(getAllFields(form.getId()));
        return formConfig;
    }

    /**
     * 获取业务表单配置
     *
     * @param formKey        表单Key
     * @param organizationId 组织ID
     * @return 表单配置
     */
    public ModuleFormConfigDTO getBusinessFormConfig(String formKey, String organizationId) {
        ModuleFormConfigDTO config = getConfig(formKey, organizationId);
        ModuleFormConfigDTO businessModuleFormConfig = new ModuleFormConfigDTO();
        businessModuleFormConfig.setFormProp(config.getFormProp());

        // 提前加载价格表子表格字段作为引用集合
        List<BaseField> subFields = moduleFieldService.getSubFieldsBySourceType(FieldSourceType.PRICE.name());
        Map<String, BaseField> refPriceSubFieldMap = subFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity(), (p, n) -> p));

        // 设置业务字段参数
        List<BaseField> flattenFields = flattenSourceRefFields(config.getFields(), refPriceSubFieldMap);
        businessModuleFormConfig.setFields(flattenFields.stream()
                .peek(this::setFieldRefOption)
                .peek(this::setFieldBusinessParam)
                .peek(field -> reloadPropOfSubRefFields(field, refPriceSubFieldMap))
                .collect(Collectors.toList())
        );
        return businessModuleFormConfig;
    }

    public ModuleFormConfigDTO getSourceDisplayFields(String formKey, String organizationId) {
        ModuleFormConfigDTO config = getConfig(formKey, organizationId);
        ModuleFormConfigDTO businessModuleFormConfig = new ModuleFormConfigDTO();
        businessModuleFormConfig.setFormProp(config.getFormProp());

        List<BaseField> fields = config.getFields().stream()
                .filter(f -> {
                    if (f instanceof ProductSubField || f instanceof PriceSubField) {
                        if (StringUtils.isBlank(f.getBusinessKey())) {
                            // 过滤掉非业务字段的子表格字段
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        config.setFields(fields);

        List<BaseField> flattenFields = flattenFormAllFieldsWithSubId(config);
        // 设置业务字段参数
        businessModuleFormConfig.setFields(flattenFields.stream()
                .filter(BaseField::canDisplay)
                .filter(f -> StringUtils.isEmpty(f.getResourceFieldId()))
                .peek(this::setFieldBusinessParam)
                .collect(Collectors.toList())
        );
        return businessModuleFormConfig;
    }

    /**
     * 保存表单配置
     *
     * @param saveParam 保存参数
     * @return 表单配置
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, resourceId = "{#saveParam.formKey}")
    public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
        // 设置表单日志上下文
        OperationLogContext.setContext(getModuleFormChangeLogContext(saveParam.getFormKey(), currentOrgId, saveParam));
        // 返回表单配置
        return saveWithoutLog(saveParam, currentUserId, currentOrgId);
    }

    /**
     * 保存表单配置（不记录日志）
     *
     * @param saveParam     保存参数
     * @param currentUserId 当前用户ID
     * @param currentOrgId  当前组织ID
     * @return 表单配置
     */
    public ModuleFormConfigDTO saveWithoutLog(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
        // 处理表单
        ModuleForm form = getModuleFormByKey(saveParam.getFormKey(), currentOrgId);
        if (saveParam.getFormProp() != null) {
            // 在更新表单及字段前完成校验，失败时不产生任何部分写入。
            validateAndResolveDetailTabs(saveParam.getFormKey(), currentOrgId, saveParam.getFormProp());
        }
        form.setUpdateUser(currentUserId);
        form.setUpdateTime(System.currentTimeMillis());
        moduleFormMapper.updateById(form);

        if (saveParam.getFormProp() != null) {
            ModuleFormBlob formBlob = new ModuleFormBlob();
            formBlob.setId(form.getId());
            formBlob.setProp(JSON.toJSONString(saveParam.getFormProp()));
            moduleFormBlobMapper.updateById(formBlob);
        }

        if (saveParam.getFields() != null) {
            // 字段合规校验
            preCheckForFieldSave(saveParam.getFormKey(), saveParam.getFields(), currentOrgId);

            // 处理字段 (删除&&新增)
            LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
            fieldWrapper.eq(ModuleField::getFormId, form.getId());
            List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);

            // 统计字段刷新要对比新旧配置, 旧配置必须在下面删除字段之前取出。
            // 只有新旧任一侧存在统计字段时才读完整字段属性, 避免普通保存多出两次查询。
            boolean statisticFieldInvolved = hasStatisticField(fields, saveParam.getFields());
            List<BaseField> originFields = statisticFieldInvolved ? getAllFields(form.getId()) : List.of();

            // 重置流水号
            resetSerial(fields, saveParam.getFields(), saveParam.getFormKey(), currentOrgId);
            if (CollectionUtils.isNotEmpty(fields)) {
                List<String> fieldIds = fields.stream().map(ModuleField::getId).toList();
                extModuleFieldMapper.deleteByIds(fieldIds);
                extModuleFieldMapper.deletePropByIds(fieldIds);
            }
            if (CollectionUtils.isNotEmpty(saveParam.getFields())) {
                saveFields(saveParam.getFields(), form.getId(), currentUserId);
            }

            // 统计字段刷新: 由 StatisticFieldService 对比新旧配置, 配置没改就不刷新。
            // 必须在事务提交后触发, 否则异步线程读到的是尚未提交的旧配置。
            if (statisticFieldInvolved) {
                triggerStatisticRefresh(saveParam.getFormKey(), originFields, saveParam.getFields(), currentOrgId);
            }
        }

        // 返回表单配置
        return getConfig(form.getFormKey(), currentOrgId);
    }

    /**
     * 获取字段变更日志信息（供外部调用方合并日志使用）
     *
     * @param formKey      表单Key
     * @param currentOrgId 当前组织ID
     * @param saveParam    保存参数
     * @return 字段变更日志信息
     */
    public LogContextInfo getModuleFormChangeLogContext(String formKey, String currentOrgId, ModuleFormSaveRequest saveParam) {
        ModuleForm form = getModuleFormByKey(formKey, currentOrgId);
        ModuleFormConfigDTO oldConfig = getModuleFormConfigDTO(formKey, form.getId(), currentOrgId);

        ModuleFormConfigDTO newConfig = new ModuleFormConfigDTO();
        newConfig.setFields(saveParam.getFields());
        newConfig.setFormProp(saveParam.getFormProp());

        return LogContextInfo.builder()
                .resourceName(Translator.get(formKey) + Translator.get("module.form.setting"))
                .originalValue(buildModuleFormLogDTO(oldConfig))
                .modifiedValue(buildModuleFormLogDTO(newConfig))
                .build();
    }

    private ModuleFormConfigDTO getModuleFormConfigDTO(String formKey, String formId, String orgId) {
        ModuleFormConfigDTO oldConfig = new ModuleFormConfigDTO();
        oldConfig.setFields(getAllFields(formKey, orgId));
        ModuleFormBlob moduleFormBlob = moduleFormBlobMapper.selectByPrimaryKey(formId);
        if (moduleFormBlob != null && StringUtils.isNotEmpty(moduleFormBlob.getProp())) {
            oldConfig.setFormProp(JSON.parseObject(moduleFormBlob.getProp(), FormProp.class));
        }
        return oldConfig;
    }

    private ModuleForm getModuleFormByKey(String forKey, String currentOrgId) {
        LambdaQueryWrapper<ModuleForm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleForm::getFormKey, forKey).eq(ModuleForm::getOrganizationId, currentOrgId);
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(queryWrapper);
        if (CollectionUtils.isEmpty(forms)) {
            throw new GenericException(cn.cordys.common.response.result.CrmHttpResultCode.NOT_FOUND);
        }
        return forms.getFirst();
    }

    /** 删除已通过业务权限校验的表单定义及其字段属性。 */
    public void deleteForm(String formKey, String orgId) {
        ModuleForm form = getModuleFormByKey(formKey, orgId);
        LambdaQueryWrapper<ModuleField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleField::getFormId, form.getId());
        List<String> ids = moduleFieldMapper.selectListByLambda(wrapper).stream().map(ModuleField::getId).toList();
        if (!ids.isEmpty()) {
            extModuleFieldMapper.deletePropByIds(ids);
            extModuleFieldMapper.deleteByIds(ids);
        }
        moduleFormBlobMapper.deleteByPrimaryKey(form.getId());
        moduleFormMapper.deleteByPrimaryKey(form.getId());
    }

    /**
     * 重置流水号
     *
     * @param oldFields  旧的字段集合
     * @param saveFields 保存的字段集合
     */
    private void resetSerial(List<ModuleField> oldFields, List<BaseField> saveFields, String formKey, String orgId) {
        Optional<ModuleField> serialOld = oldFields.stream().filter(f -> Strings.CS.equals(f.getType(), FieldType.SERIAL_NUMBER.name())).findAny();
        Optional<BaseField> serialNew = saveFields.stream().filter(BaseField::isSerialNumber).findAny();
        if (serialOld.isEmpty()) {
            return;
        }
        ModuleFieldBlob oldFieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(serialOld.get().getId());
        List<String> oldRules = JSON.parseObject(oldFieldBlob.getProp(), SerialNumberField.class).getSerialNumberRules();
        if (serialNew.isEmpty()) {
            serialNumGenerator.resetKey(oldRules.get(2), formKey, orgId);
            return;
        }
        List<String> newRules = ((SerialNumberField) serialNew.get()).getSerialNumberRules();
        if (serialNumGenerator.sameRule(oldRules, newRules)) {
            return;
        }
        serialNumGenerator.resetKey(oldRules.get(2), formKey, orgId);
    }

    /**
     * 保存所有字段集合
     *
     * @param saveFields    字段集合
     * @param saveFormId    表单ID
     * @param currentUserId 当前用户ID
     */
    public void saveFields(List<BaseField> saveFields, String saveFormId, String currentUserId) {
        if (CollectionUtils.isEmpty(saveFields)) {
            return;
        }
        // 剔除引用字段&&并保留到数据源引用字段
        List<BaseField> fieldToSave = new ArrayList<>(saveFields);
        AtomicLong pos = new AtomicLong(0L);
        List<BaseField> refFields = fieldToSave.stream().peek(f -> f.setPos(pos.getAndIncrement()))
                .filter(f -> StringUtils.isNotEmpty(f.getResourceFieldId()))
                .collect(Collectors.toMap(BaseField::getId, Function.identity(), (a, b) -> a)).values().stream()
                .toList();
        fieldToSave.removeAll(refFields);

        List<ModuleField> addFields = new ArrayList<>();
        List<ModuleFieldBlob> addFieldBlobs = new ArrayList<>();
        fieldToSave.forEach(field -> {
            ModuleField moduleField = new ModuleField();
            moduleField.setId(field.getId());
            moduleField.setFormId(saveFormId);
            moduleField.setMobile(field.getMobile() != null && field.getMobile());
            moduleField.setInternalKey(field.getInternalKey());
            moduleField.setType(field.getType());
            moduleField.setName(field.getName());
            moduleField.setPos(field.getPos());
            moduleField.setCreateTime(System.currentTimeMillis());
            moduleField.setCreateUser(currentUserId);
            moduleField.setUpdateTime(System.currentTimeMillis());
            moduleField.setUpdateUser(currentUserId);
            addFields.add(moduleField);
            ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
            fieldBlob.setId(field.getId());
            if (field instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields())) {
                List<BaseField> sourceRefFields = refFields.stream().filter(rf -> {
                    String actualId = rf.getId().replace(sourceField.getId() + REF_UNDERLINE, StringUtils.EMPTY);
                    return sourceField.getShowFields().contains(actualId);
                }).toList();
                sourceField.setRefFields(sourceRefFields);
            }
            fieldBlob.setProp(JSON.toJSONString(field));
            addFieldBlobs.add(fieldBlob);
        });
        if (CollectionUtils.isNotEmpty(addFields)) {
            moduleFieldMapper.batchInsert(addFields);
        }
        if (CollectionUtils.isNotEmpty(addFieldBlobs)) {
            moduleFieldBlobMapper.batchInsert(addFieldBlobs);
        }
    }

    public ModuleFormConfigLogDTO buildModuleFormLogDTO(ModuleFormConfigDTO config) {
        ModuleFormConfigLogDTO logDTO = new ModuleFormConfigLogDTO();
        FormPropLogDTO formPropLog = BeanUtils.copyBean(new FormPropLogDTO(), config.getFormProp());
        BeanUtils.copyBean(logDTO, config);
        logDTO.setFormProp(formPropLog);
        // 目前只处理联动字段方便日志详情解析
        Map<String, List<LinkField>> parseLinkFieldMap = new HashMap<>(8);
        Map<String, List<LinkScenario>> linkProp = config.getFormProp().getLinkProp();
        if (linkProp != null && !linkProp.isEmpty()) {
            linkProp.forEach((key, value) -> value.forEach(scenario -> parseLinkFieldMap.put(key + "-" + scenario.getKey(), scenario.getLinkFields())));
        }
        logDTO.getFormProp().setLinkProp(parseLinkFieldMap);
        return logDTO;
    }

    /**
     * 获取详情页内置标签及包含当前表单数据源字段的表单。
     *
     * <p>除枚举维护的内置标签外，动态关联表单必须至少存在一个指向当前表单的数据源字段。查询范围限定在
     * 当前组织，避免跨组织表单名称和字段配置泄露。查询时先批量筛选数据源单选字段及其属性，
     * 再按命中的表单 ID 查询表单，避免逐表单加载全部字段产生 N+1 查询。无直接字段关联的内置标签
     * 从 {@link InternalDetailTab} 补充，并通过 internalKey 标识。</p>
     *
     * @param formKey 当前详情表单 Key；自定义表单使用表单 ID
     * @param organizationId 当前组织 ID
     * @return 内置标签、关联表单及其匹配的数据源字段
     */
    public List<RelatedFormDTO> getRelatedForms(String formKey, String organizationId) {
        String sourceType = getFormSourceType(formKey);

        // 关联字段只允许数据源单选类型，先从字段主表缩小需要解析的属性范围。
        List<ModuleField> datasourceFields = moduleFieldMapper.selectListByLambda(new LambdaQueryWrapper<ModuleField>()
                .eq(ModuleField::getType, FieldType.DATA_SOURCE.name()));
        if (CollectionUtils.isEmpty(datasourceFields)) {
            return getInternalRelatedForms(formKey, new HashMap<>());
        }

        Map<String, ModuleField> datasourceFieldMap = datasourceFields.stream()
                .collect(Collectors.toMap(ModuleField::getId, Function.identity()));
        List<ModuleFieldBlob> fieldBlobs = moduleFieldBlobMapper.selectListByLambda(new LambdaQueryWrapper<ModuleFieldBlob>()
                .in(ModuleFieldBlob::getId, new ArrayList<>(datasourceFieldMap.keySet())));

        // sourceType 存储在字段属性表中；一次解析所有候选属性，记录真正指向当前表单的字段。
        Set<String> relatedFieldIds = new HashSet<>();
        for (ModuleFieldBlob fieldBlob : fieldBlobs) {
            if (StringUtils.isBlank(fieldBlob.getProp())) {
                continue;
            }
            BaseField field = JSON.parseObject(fieldBlob.getProp(), BaseField.class);
            if (isRelatedField(field, sourceType)) {
                relatedFieldIds.add(fieldBlob.getId());
            }
        }
        Set<String> relatedFormIds = relatedFieldIds.stream()
                .map(datasourceFieldMap::get)
                .filter(Objects::nonNull)
                .map(ModuleField::getFormId)
                .collect(Collectors.toSet());
        List<ModuleForm> relatedForms = relatedFormIds.isEmpty() ? List.of()
                : moduleFormMapper.selectListByLambda(new LambdaQueryWrapper<ModuleForm>()
                        .in(ModuleForm::getId, new ArrayList<>(relatedFormIds))
                        .eq(ModuleForm::getOrganizationId, organizationId));
        Map<String, String> formNameMap = buildFormNameMap(relatedForms);
        Map<String, List<OptionDTO>> sourceTypeFieldMap = datasourceFields.stream()
                .filter(field -> relatedFieldIds.contains(field.getId()))
                .sorted(Comparator.comparing(ModuleField::getPos, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.groupingBy(ModuleField::getFormId,
                        Collectors.mapping(field -> new OptionDTO(field.getId(), field.getName()), Collectors.toList())));

        Map<String, RelatedFormDTO> relatedFormMap = relatedForms.stream()
                .collect(Collectors.toMap(ModuleForm::getFormKey, form -> new RelatedFormDTO(
                        form.getFormKey(), formNameMap.get(form.getFormKey()),
                        sourceTypeFieldMap.getOrDefault(form.getId(), List.of()), null)));
        List<RelatedFormDTO> result = getInternalRelatedForms(formKey, relatedFormMap);
        result.addAll(relatedFormMap.values().stream()
                .sorted(Comparator.comparing(RelatedFormDTO::getName, Comparator.nullsLast(String::compareTo)))
                .toList());
        // 自定义表单删除后可能残留 sys_module_form 配置，此时无法解析名称，不应继续提供给前端选择。
        return result.stream()
                .filter(item -> StringUtils.isNotBlank(item.getName()))
                .toList();
    }

    /**
     * 按枚举顺序组装内置标签，并从动态关联表单中移除已被内置标签覆盖的项。
     */
    private List<RelatedFormDTO> getInternalRelatedForms(String formKey,
                                                         Map<String, RelatedFormDTO> relatedFormMap) {
        List<RelatedFormDTO> internalForms = new ArrayList<>();
        Arrays.stream(InternalDetailTab.values())
                .filter(tab -> tab.getFormKey().equals(formKey))
                .forEach(tab -> {
                    RelatedFormDTO relatedForm = tab.getRelatedFormKey() == null
                            ? null : relatedFormMap.remove(tab.getRelatedFormKey());
                    List<OptionDTO> fields = relatedForm == null ? List.of() : relatedForm.getSourceTypeFields();
                    String id = tab.getRelatedFormKey() == null ? tab.name() : tab.getRelatedFormKey();
                    internalForms.add(new RelatedFormDTO(id, Translator.get(tab.getLabelKey()),
                            fields, tab.name()));
                });
        return internalForms;
    }

    /**
     * 校验标签约束，并使用服务端名称刷新关联表单、关联字段的回显值。
     *
     * <p>该方法会原地规范化 {@code formProp.detailTabs}：去除名称两端空白、补全默认启用状态、
     * 刷新 Option 名称，并自动补回请求中缺失的系统标签，以兼容尚未提交新属性的旧客户端。</p>
     *
     * @param formKey 标签所属表单 Key
     * @param organizationId 当前组织 ID
     * @param formProp 待保存的表单属性
     */
    public void validateAndResolveDetailTabs(String formKey, String organizationId, FormProp formProp) {
        DetailTabResolveContext context = loadDetailTabResolveContext(formKey, organizationId, formProp.getDetailTabs());
        List<FormDetailTab> tabs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(formProp.getDetailTabs())) {
            for (FormDetailTab tab : formProp.getDetailTabs()) {
                tabs.add(resolveDetailTab(formKey, tab, true, context));
            }
        }
        mergeInternalDetailTabs(tabs, context);

        Set<String> names = new HashSet<>();
        Set<String> relations = new HashSet<>();
        Set<String> internalKeys = new HashSet<>();
        for (FormDetailTab tab : tabs) {
            String name = StringUtils.trim(tab.getName());
            if (StringUtils.isBlank(name) || name.length() > 50) {
                throw new GenericException(Translator.get("module.form.detail_tab.name.invalid"));
            }
            tab.setName(name);
            if (!names.add(name)) {
                throw new GenericException(Translator.get("module.form.detail_tab.name.repeat"));
            }
            if (StringUtils.isNotBlank(tab.getInternalKey()) && !internalKeys.add(tab.getInternalKey())) {
                throw new GenericException(Translator.get("module.form.detail_tab.system.readonly"));
            }
            if (tab.getRelatedForm() != null && tab.getRelatedField() != null) {
                String relationKey = tab.getRelatedForm().getIdAsString() + ":" + tab.getRelatedField().getIdAsString();
                if (!relations.add(relationKey)) {
                    throw new GenericException(Translator.get("module.form.detail_tab.relation.repeat"));
                }
            }
        }
        formProp.setDetailTabs(tabs);
    }

    private void resolveDetailTabs(String formKey, String organizationId, FormProp formProp) {
        if (formProp == null) {
            return;
        }
        DetailTabResolveContext context = loadDetailTabResolveContext(formKey, organizationId, formProp.getDetailTabs());
        List<FormDetailTab> tabs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(formProp.getDetailTabs())) {
            for (FormDetailTab tab : formProp.getDetailTabs()) {
                FormDetailTab resolved = resolveDetailTab(formKey, tab, false, context);
                if (resolved != null) {
                    tabs.add(resolved);
                }
            }
        }
        mergeInternalDetailTabs(tabs, context);
        formProp.setDetailTabs(tabs);
    }

    private FormDetailTab resolveDetailTab(String formKey, FormDetailTab tab, boolean strict,
                                           DetailTabResolveContext context) {
        if (tab == null) {
            return invalidDetailTab(strict);
        }

        Optional<InternalDetailTab> internalTab = InternalDetailTab.findByInternalKey(formKey, tab.getInternalKey());
        if (internalTab.isPresent()) {
            return resolveInternalDetailTab(tab, internalTab.get(), strict, context);
        }
        if (StringUtils.isNotBlank(tab.getInternalKey())) {
            return invalidInternalDetailTab(strict);
        }

        if (tab.getRelatedForm() == null || tab.getRelatedField() == null
                || StringUtils.isBlank(tab.getRelatedForm().getIdAsString())
                || StringUtils.isBlank(tab.getRelatedField().getIdAsString())) {
            return invalidDetailTab(strict);
        }

        String relatedFormKey = tab.getRelatedForm().getIdAsString();
        ModuleForm relatedForm = context.formMap().get(relatedFormKey);
        if (relatedForm == null) {
            return invalidDetailTab(strict);
        }
        ModuleField relatedField = context.fieldMap().get(tab.getRelatedField().getIdAsString());
        if (relatedField == null || !Objects.equals(relatedField.getFormId(), relatedForm.getId())) {
            return invalidDetailTab(strict);
        }

        tab.setRelatedForm(new OptionDTO(relatedFormKey, context.formNameMap().get(relatedFormKey)));
        tab.setRelatedField(new OptionDTO(relatedField.getId(), relatedField.getName()));
        tab.setEnable(tab.getEnable() == null || tab.getEnable());
        tab.setInternalKey(null);
        return tab;
    }

    private FormDetailTab resolveInternalDetailTab(FormDetailTab tab, InternalDetailTab internalTab, boolean strict,
                                                   DetailTabResolveContext context) {
        ModuleForm relatedForm = null;
        ModuleField relatedField = null;
        if (internalTab.getRelatedFormKey() != null) {
            relatedForm = context.formMap().get(internalTab.getRelatedFormKey());
            if (relatedForm == null) {
                return invalidDetailTab(strict);
            }
        }
        if (internalTab.getRelatedFieldInternalKey() != null) {
            Map<String, ModuleField> fieldMap = context.internalKeyFieldMap().get(relatedForm.getId());
            relatedField = MapUtils.isEmpty(fieldMap) ? null : fieldMap.get(internalTab.getRelatedFieldInternalKey());
            if (relatedField == null) {
                return invalidDetailTab(strict);
            }
        }

        // 系统标签只允许改名；保存时关联表单、字段和启用状态必须与枚举定义一致。
        if (strict && (!Objects.equals(getOptionId(tab.getRelatedForm()), internalTab.getRelatedFormKey())
                || !Objects.equals(getOptionId(tab.getRelatedField()), relatedField == null ? null : relatedField.getId()))) {
            throw new GenericException(Translator.get("module.form.detail_tab.system.readonly"));
        }

        tab.setRelatedForm(relatedForm == null ? null : new OptionDTO(relatedForm.getFormKey(),
                context.formNameMap().get(relatedForm.getFormKey())));
        tab.setRelatedField(relatedField == null ? null : new OptionDTO(relatedField.getId(), relatedField.getName()));
        tab.setInternalKey(internalTab.name());
        // 如果没有改过，则是默认的需要翻译的名字
        tab.setName(Translator.get(tab.getName(), tab.getName()));
        return tab;
    }

    private String getOptionId(OptionDTO option) {
        return option == null ? null : option.getIdAsString();
    }

    private FormDetailTab invalidInternalDetailTab(boolean strict) {
        if (strict) {
            throw new GenericException(Translator.get("module.form.detail_tab.system.illegal"));
        }
        return null;
    }

    private FormDetailTab invalidDetailTab(boolean strict) {
        // 保存时严格拒绝非法引用；读取历史配置时忽略已被删除的表单或字段，保证配置接口可用。
        if (strict) {
            throw new GenericException(Translator.get("module.form.detail_tab.relation.invalid"));
        }
        return null;
    }

    private void mergeInternalDetailTabs(List<FormDetailTab> tabs, DetailTabResolveContext context) {
        // 缺失的系统标签始终补回，因此删除系统标签或旧客户端漏传 detailTabs 都不会造成数据丢失。
        Set<String> configuredInternalKeys = tabs.stream()
                .map(FormDetailTab::getInternalKey)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        for (InternalDetailTab internalTab : context.internalTabs()) {
            if (configuredInternalKeys.contains(internalTab.name())) {
                continue;
            }
            FormDetailTab tab = new FormDetailTab();
            tab.setEnable(true);
            tab.setInternalKey(internalTab.name());
            FormDetailTab resolved = resolveInternalDetailTab(tab, internalTab, false, context);
            // 这里初始化国际化的key
            tab.setName(internalTab.getLabelKey());
            if (resolved != null) {
                tabs.add(tab);
            }
        }
    }

    /**
     * 批量加载标签校验和回显所需数据，整个标签集合只访问一次表单、字段主表和字段属性表。
     */
    private DetailTabResolveContext loadDetailTabResolveContext(String formKey, String organizationId,
                                                                List<FormDetailTab> configuredTabs) {
        List<InternalDetailTab> internalTabs = Arrays.stream(InternalDetailTab.values())
                .filter(internalTab -> internalTab.getFormKey().equals(formKey))
                .toList();
        Set<String> relatedFormKeys = internalTabs.stream()
                .map(InternalDetailTab::getRelatedFormKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(configuredTabs)) {
            configuredTabs.stream()
                    .filter(Objects::nonNull)
                    .map(FormDetailTab::getRelatedForm)
                    .filter(Objects::nonNull)
                    .map(OptionDTO::getIdAsString)
                    .filter(StringUtils::isNotBlank)
                    .forEach(relatedFormKeys::add);
        }

        if (relatedFormKeys.isEmpty()) {
            return new DetailTabResolveContext(Map.of(), Map.of(), Map.of(), Map.of(), internalTabs);
        }

        List<ModuleForm> relatedForms = moduleFormMapper.selectListByLambda(new LambdaQueryWrapper<ModuleForm>()
                .in(ModuleForm::getFormKey, new ArrayList<>(relatedFormKeys))
                .eq(ModuleForm::getOrganizationId, organizationId));
        Map<String, ModuleForm> formMap = relatedForms.stream()
                .collect(Collectors.toMap(ModuleForm::getFormKey, Function.identity()));
        if (relatedForms.isEmpty()) {
            return new DetailTabResolveContext(formMap, Map.of(), Map.of(), Map.of(), internalTabs);
        }

        List<String> formIds = relatedForms.stream().map(ModuleForm::getId).toList();
        List<ModuleField> datasourceFields = moduleFieldMapper.selectListByLambda(new LambdaQueryWrapper<ModuleField>()
                .in(ModuleField::getFormId, formIds)
                .eq(ModuleField::getType, FieldType.DATA_SOURCE.name()));
        Map<String, ModuleField> datasourceFieldMap = datasourceFields.stream()
                .collect(Collectors.toMap(ModuleField::getId, Function.identity()));

        Map<String, ModuleField> fieldMap = new HashMap<>();
        Map<String, Map<String, ModuleField>> internalKeyFieldMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(datasourceFields)) {
            List<ModuleFieldBlob> fieldBlobs = moduleFieldBlobMapper.selectListByLambda(new LambdaQueryWrapper<ModuleFieldBlob>()
                    .in(ModuleFieldBlob::getId, new ArrayList<>(datasourceFieldMap.keySet())));
            String sourceType = getFormSourceType(formKey);
            for (ModuleFieldBlob fieldBlob : fieldBlobs) {
                if (StringUtils.isBlank(fieldBlob.getProp())) {
                    continue;
                }
                BaseField fieldProp = JSON.parseObject(fieldBlob.getProp(), BaseField.class);
                ModuleField field = datasourceFieldMap.get(fieldBlob.getId());
                if (field != null && isRelatedField(fieldProp, sourceType)) {
                    fieldMap.put(field.getId(), field);
                    internalKeyFieldMap.computeIfAbsent(field.getFormId(), key -> new HashMap<>())
                            .putIfAbsent(field.getInternalKey(), field);
                }
            }
        }

        Map<String, String> formNameMap = buildFormNameMap(relatedForms);
        return new DetailTabResolveContext(formMap, fieldMap, internalKeyFieldMap, formNameMap, internalTabs);
    }

    private Map<String, String> buildFormNameMap(List<ModuleForm> forms) {
        Set<String> customFormIds = forms.stream()
                .map(ModuleForm::getFormKey)
                .filter(key -> FormKey.ofKey(key) == null)
                .collect(Collectors.toSet());
        Map<String, String> formNameMap = customFormIds.isEmpty() ? new HashMap<>()
                : customFormMapper.selectByIds(new ArrayList<>(customFormIds)).stream()
                .collect(Collectors.toMap(CustomForm::getId, CustomForm::getName));
        forms.stream()
                .map(ModuleForm::getFormKey)
                .filter(key -> FormKey.ofKey(key) != null)
                .forEach(key -> formNameMap.put(key, Translator.get(key)));
        return formNameMap;
    }

    private record DetailTabResolveContext(Map<String, ModuleForm> formMap,
                                           Map<String, ModuleField> fieldMap,
                                           Map<String, Map<String, ModuleField>> internalKeyFieldMap,
                                           Map<String, String> formNameMap,
                                           List<InternalDetailTab> internalTabs) {
    }

    private String getFormSourceType(String formKey) {
        FormKey standardForm = FormKey.ofKey(formKey);
        if (standardForm == null) {
            // 自定义表单的数据源类型直接使用其表单 ID。
            return formKey;
        }
        return switch (standardForm) {
            case CUSTOMER -> FieldSourceType.CUSTOMER.name();
            case CLUE -> FieldSourceType.CLUE.name();
            case CONTACT -> FieldSourceType.CONTACT.name();
            case OPPORTUNITY -> FieldSourceType.OPPORTUNITY.name();
            case PRODUCT -> FieldSourceType.PRODUCT.name();
            case PRICE -> FieldSourceType.PRICE.name();
            case QUOTATION -> FieldSourceType.QUOTATION.name();
            case CONTRACT -> FieldSourceType.CONTRACT.name();
            case INVOICE -> FieldSourceType.INVOICE.name();
            case CONTRACT_PAYMENT_PLAN -> FieldSourceType.PAYMENT_PLAN.name();
            case CONTRACT_PAYMENT_RECORD -> FieldSourceType.CONTRACT_PAYMENT_RECORD.name();
            case ORDER -> FieldSourceType.ORDER.name();
            default -> formKey;
        };
    }

    private boolean isRelatedField(BaseField field, String sourceType) {
        return field instanceof DatasourceField datasourceField
                && StringUtils.equals(datasourceField.getDataSourceType(), sourceType);
    }

    public List<BaseField> getAllFields(String formKey, String orgId) {
        ModuleForm example = new ModuleForm();
        example.setFormKey(formKey);
        example.setOrganizationId(orgId);
        ModuleForm moduleForm = moduleFormMapper.selectOne(example);
        List<BaseField> allFields = getAllFields(moduleForm.getId());

        // 提前加载价格表子表格字段作为引用集合
        List<BaseField> subFields = moduleFieldService.getSubFieldsBySourceType(FieldSourceType.PRICE.name());
        Map<String, BaseField> refPriceSubFieldMap = subFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity(), (p, n) -> p));
        // 处理字段信息
        List<BaseField> flattenFields = flattenSourceRefFields(allFields, refPriceSubFieldMap);
        return flattenFields.stream()
                .peek(this::setFieldRefOption)
                .peek(this::setFieldBusinessParam)
                .peek(field -> reloadPropOfSubRefFields(field, refPriceSubFieldMap))
                .collect(Collectors.toList());
    }

    /**
     * 获取表单所有字段及其属性集合
     *
     * @param formId 表单ID
     * @return 字段集合
     */
    public List<BaseField> getAllFields(String formId) {
        // set field
        List<BaseField> fieldDTOList = new ArrayList<>();
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(ModuleField::getFormId, formId);
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);

        if (CollectionUtils.isNotEmpty(fields)) {
            fields.sort(Comparator.comparing(ModuleField::getPos));
            List<String> fieldIds = fields.stream().map(ModuleField::getId).toList();
            LambdaQueryWrapper<ModuleFieldBlob> blobWrapper = new LambdaQueryWrapper<>();
            blobWrapper.in(ModuleFieldBlob::getId, fieldIds);
            List<ModuleFieldBlob> fieldBlobs = moduleFieldBlobMapper.selectListByLambda(blobWrapper);
            Map<String, String> fieldBlobMap = fieldBlobs.stream().collect(Collectors.toMap(ModuleFieldBlob::getId, ModuleFieldBlob::getProp));
            fields.forEach(field -> {
                BaseField baseField = JSON.parseObject(fieldBlobMap.get(field.getId()), BaseField.class);
                baseField.setPos(field.getPos());
                baseField.setType(field.getType());
                baseField.setMobile(field.getMobile());
                baseField.setInternalKey(field.getInternalKey());
                // 刷新默认值选项
                freshInitialOptions(baseField);
                // 文本字段默认值格式 || 流水号前缀固定字符格式
                if (baseField instanceof SerialNumberField serialField && StringUtils.isEmpty(serialField.getPrefixType())) {
                    serialField.setPrefixType(OPTION_DEFAULT_SOURCE);
                }
                if (baseField instanceof InputField inputField && StringUtils.isEmpty(inputField.getDefaultValueType())) {
                    inputField.setDefaultValueType(OPTION_DEFAULT_SOURCE);
                }
                fieldDTOList.add(baseField);
            });
        }
        return fieldDTOList;
    }

    /**
     * 刷新默认值选项
     * @param baseField
     */
    public void freshInitialOptions(BaseField baseField) {
        if (baseField.needInitialOptions()) {
            handleInitialOption(baseField);
        }
        if (baseField instanceof SubField subField) {
            for (BaseField subFieldSubField : subField.getSubFields()) {
                if (subFieldSubField.needInitialOptions()) {
                    handleInitialOption(subFieldSubField);
                }
            }
        }
    }

    /**
     * 获取字段选项集合
     *
     * @param formConfig    表单配置
     * @param allDataFields 所有数据字段
     * @return 字段选项集合
     */
    public Map<String, List<OptionDTO>> getOptionMap(ModuleFormConfigDTO formConfig, List<BaseModuleFieldValue> allDataFields) {
        var optionMap = new HashMap<String, List<OptionDTO>>(4);
        var optionMeta = collectOptionMetadata(formConfig);
        optionMap.putAll(optionMeta.staticOptions());
        if (CollectionUtils.isEmpty(allDataFields)) {
            return optionMap;
        }
        var allFieldValues = flattenSubFieldValues(formConfig, allDataFields);
        var typeIdsMap = collectOptionIds(allFieldValues, optionMeta.idTypeMap());

        // 按照sourceType聚合id, 以便批量查询
        Map<String, Set<String>> sourceTypeIdsMap = new HashMap<>();
        Map<String, String> fieldIdSourceTypeMap = new HashMap<>();

        typeIdsMap.forEach((fieldId, ids) -> {
            var sourceType = optionMeta.idTypeMap().get(getFieldIdForSubFieldId(fieldId));
            if (CollectionUtils.isEmpty(ids)) {
                return;
            }
            fieldIdSourceTypeMap.put(fieldId, sourceType);
            // 去重
            sourceTypeIdsMap.computeIfAbsent(sourceType, k -> new HashSet<>()).addAll(ids);
        });

        // 按照类型, 整体查询
        Map<String, List<OptionDTO>> sourceTypeOptionsMap = new HashMap<>();
        sourceTypeIdsMap.forEach((sourceType, ids) -> {
            List<OptionDTO> options;
            String tableName = TYPE_SOURCE_MAP.get(sourceType);
            if (StringUtils.isBlank(tableName)) {
                options = extModuleFieldMapper.getCustomFormOptionsByIds(new ArrayList<>(ids));
            } else {
                options = extModuleFieldMapper.getSourceOptionsByIds(tableName, new ArrayList<>(ids));
            }
            if (CollectionUtils.isNotEmpty(options)) {
                sourceTypeOptionsMap.put(sourceType, options);
            }
        });

        // 按照fieldId, 分配选项
        typeIdsMap.forEach((fieldId, ids) -> {
            var sourceType = fieldIdSourceTypeMap.get(fieldId);
            if (sourceType == null) {
                return;
            }
            var allOptions = sourceTypeOptionsMap.get(sourceType);
            if (CollectionUtils.isEmpty(allOptions)) {
                return;
            }
            // 只保留当前 fieldId 需要的
            var optionList = allOptions.stream().filter(opt -> ids.contains(opt.getId())).toList();
            if (CollectionUtils.isNotEmpty(optionList)) {
                optionMap.put(fieldId, optionList);
            }
        });

        return optionMap;
    }

    private OptionMetadata collectOptionMetadata(ModuleFormConfigDTO formConfig) {
        var allFields = flattenFormAllFields(formConfig);
        var showFields = allFields.stream()
                .filter(f -> f instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields()))
                .flatMap(f -> ((DatasourceField) f).getShowFields().stream().map(sf -> f.getId() + REF_UNDERLINE + sf))
                .distinct()
                .toList();
        var staticOptions = new HashMap<String, List<OptionDTO>>(4);
        var idTypeMap = new HashMap<String, String>(8);
        for (var field : allFields) {
            if (showFields.contains(field.getId())) {
                putOptionMap(staticOptions, idTypeMap, field, field.getId());
            } else {
                putOptionMap(staticOptions, idTypeMap, field, field.getId());
                if (StringUtils.isNotBlank(field.getBusinessKey())) {
                    putOptionMap(staticOptions, idTypeMap, field, field.getBusinessKey());
                }
            }
        }
        return new OptionMetadata(staticOptions, idTypeMap);
    }

    private void putOptionMap(HashMap<String, List<OptionDTO>> staticOptions, HashMap<String, String> idTypeMap, BaseField field, String key) {
        switch (field) {
            case RadioField radioField when Strings.CS.equals(field.getType(), FieldType.RADIO.name()) ->
                    staticOptions.put(key, optionPropToDto(radioField.getOptions()));
            case CheckBoxField checkBoxField when Strings.CS.equals(field.getType(), FieldType.CHECKBOX.name()) ->
                    staticOptions.put(key, optionPropToDto(checkBoxField.getOptions()));
            case HasOption optionField when Strings.CS.equalsAny(field.getType(), FieldType.SELECT.name(), FieldType.SELECT_MULTIPLE.name()) ->
                    staticOptions.put(key, optionPropToDto(optionField.getOptions()));
            default -> {
            }
        }
        if (Strings.CS.equalsAny(field.getType(), FieldType.DATA_SOURCE.name(), FieldType.DATA_SOURCE_MULTIPLE.name()) && field instanceof DatasourceField sourceField) {
            idTypeMap.put(key, sourceField.getDataSourceType());
        }
        if (Strings.CS.equalsAny(field.getType(), FieldType.MEMBER.name(), FieldType.MEMBER_MULTIPLE.name())) {
            idTypeMap.put(key, FieldType.MEMBER.name());
        }
        if (Strings.CS.equalsAny(field.getType(), FieldType.DEPARTMENT.name(), FieldType.DEPARTMENT_MULTIPLE.name())) {
            idTypeMap.put(key, FieldType.DEPARTMENT.name());
        }
    }

    private Map<String, List<String>> collectOptionIds(List<BaseModuleFieldValue> allFieldValues, Map<String, String> idTypeMap) {
        var typeIdsMap = new HashMap<String, List<String>>(8);
        allFieldValues.stream()
                .filter(fv -> {
                    String fieldId = getFieldIdForSubFieldId(fv.getFieldId());
                    return idTypeMap.containsKey(fieldId);
                })
                .forEach(fv -> {
                    typeIdsMap.putIfAbsent(fv.getFieldId(), new ArrayList<>());
                    var value = fv.getFieldValue();
                    if (value == null) {
                        return;
                    }
                    if (value instanceof List<?> listValue) {
                        typeIdsMap.get(fv.getFieldId()).addAll(JSON.parseArray(JSON.toJSONString(listValue), String.class));
                    } else {
                        typeIdsMap.get(fv.getFieldId()).add(value.toString());
                    }
                });
        return typeIdsMap;
    }

    private String getFieldIdForSubFieldId(String fieldId) {
        if (fieldId.contains(".")) {
            String[] split = fieldId.split("\\.");
            if (split.length > 1) {
                return split[1];
            }
        }
        return fieldId;
    }

    private record OptionMetadata(Map<String, List<OptionDTO>> staticOptions, Map<String, String> idTypeMap) {
    }

    /**
     * 平铺列表字段(子表格)
     *
     * @param formConfig 表单配置
     * @return 字段集合
     */
    public List<BaseField> flattenFormAllFields(ModuleFormConfigDTO formConfig) {
        List<BaseField> toFlattenFields = new ArrayList<>();
        formConfig.getFields().stream().filter(f -> f instanceof SubField).map(f -> ((SubField) f).getSubFields()).forEach(toFlattenFields::addAll);
        formConfig.getFields().addAll(toFlattenFields);
        return formConfig.getFields();
    }

    public List<BaseField> flattenFormAllFieldsWithSubId(ModuleFormConfigDTO formConfig) {
        List<BaseField> toFlattenFields = new ArrayList<>();
        formConfig.getFields().stream().filter(f -> f instanceof SubField).forEach(sf -> {
            SubField subField = ((SubField) sf);
            if (CollectionUtils.isEmpty(subField.getSubFields())) {
                return;
            }
            subField.getSubFields().forEach(field -> field.setSubTableFieldId(subField.getId()));
            toFlattenFields.addAll(subField.getSubFields());
        });
        formConfig.getFields().addAll(toFlattenFields);
        return formConfig.getFields();
    }

    /**
     * 获得所有平铺的字段
     *
     * @param formKey 表单key
     * @param orgId   组织ID
     * @return 平铺的字段集合
     */
    @Cacheable(value = "field_cache", key = "#orgId + ':' + #formKey", unless = "#result == null or #orgId == null")
    public List<BaseField> getFlattenFormFields(String formKey, String orgId) {
        ModuleFormConfigDTO formConfig = getBusinessFormConfig(formKey, orgId);
        return flattenFormAllFields(formConfig);
    }

    /**
     * 平铺子表字段值
     *
     * @param formConfig     表单配置
     * @param allFieldValues 所有字段值
     * @return 平铺字段值集合
     */
    @SuppressWarnings("unchecked")
    public List<BaseModuleFieldValue> flattenSubFieldValues(ModuleFormConfigDTO formConfig, List<BaseModuleFieldValue> allFieldValues) {
        // 类型为子表的字段
        List<BaseField> subFields = formConfig.getFields().stream().filter(f -> f instanceof SubField).toList();
        Set<String> subIdSet = subFields.stream().map(BaseField::getId).collect(Collectors.toSet());
        // 过滤出子表的字段值进行平铺
        List<BaseModuleFieldValue> allFlattenFieldValues = new ArrayList<>();
        allFieldValues.stream().filter(fv -> subIdSet.contains(fv.getFieldId())).forEach(fv -> {
            List<Map<String, Object>> subRowList = (List<Map<String, Object>>) fv.getFieldValue();
            if (CollectionUtils.isEmpty(subRowList)) {
                return;
            }
            Map<String, List<String>> subFieldIdValueMap = new HashMap<>(subRowList.getFirst().size());
            subRowList.forEach(subRow ->
                    subRow.forEach((k, v) -> {
                        if (v == null) {
                            return;
                        }
                        subFieldIdValueMap.putIfAbsent(k, new ArrayList<>());
                        if (v instanceof List) {
                            subFieldIdValueMap.get(k).addAll(JSON.parseArray(JSON.toJSONString(v), String.class));
                        } else {
                            subFieldIdValueMap.get(k).add(v.toString());
                        }
                    }));

            subFieldIdValueMap.forEach((subFieldId, values) -> {
                if (CollectionUtils.isNotEmpty(values)) {
                    BaseModuleFieldValue subFieldValue = new BaseModuleFieldValue();
                    subFieldValue.setFieldId(subFieldId);
                    subFieldValue.setFieldValue(values);
                    allFlattenFieldValues.add(subFieldValue);
                }
            });
        });
        // 插入所有字段值集合中
        allFlattenFieldValues.addAll(allFieldValues);
        return allFlattenFieldValues;
    }

    public Map<String, List<Attachment>> getAttachmentMap(ModuleFormConfigDTO formConfig, List<BaseModuleFieldValue> allDataFields) {
        List<String> attachmentFieldIds = formConfig.getFields()
                .stream()
                .filter(field -> Strings.CS.equalsAny(field.getType(), FieldType.ATTACHMENT.name()))
                .map(BaseField::getId)
                .toList();

        if (CollectionUtils.isEmpty(attachmentFieldIds)) {
            return null;
        }

        Map<String, List<String>> fieldAttachmentIds = new HashMap<>(attachmentFieldIds.size());
        allDataFields.stream()
                .filter(field -> attachmentFieldIds.contains(field.getFieldId()) && field.getFieldValue() != null)
                .forEach(field -> {
                    Object fieldValue = field.getFieldValue();
                    List<String> attachmentIds = new ArrayList<>();
                    if (fieldValue instanceof List) {
                        attachmentIds.addAll(JSON.parseArray(JSON.toJSONString(fieldValue), String.class));
                    } else {
                        attachmentIds.add(fieldValue.toString());
                    }
                    fieldAttachmentIds.put(field.getFieldId(), attachmentIds);
                });

        List<String> attachmentIds = fieldAttachmentIds.values()
                .stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

        if (CollectionUtils.isEmpty(attachmentIds)) {
            return null;
        }

        List<Attachment> attachments = attachmentMapper.selectByIds(attachmentIds);
        List<String> createUserIds = attachments.stream().map(Attachment::getCreateUser).toList();
        List<String> updateUserIds = attachments.stream().map(Attachment::getUpdateUser).toList();

        List<User> users = userExtendService.getUserOptionByIds(ListUtils.union(createUserIds, updateUserIds));
        Map<String, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getName));
        attachments.forEach(attachment -> {
            attachment.setCreateUser(userMap.get(attachment.getCreateUser()));
            attachment.setUpdateUser(userMap.get(attachment.getUpdateUser()));
        });

        Map<String, Attachment> attachmentMap = attachments.stream().collect(Collectors.toMap(Attachment::getId, Function.identity()));
        Map<String, List<Attachment>> attachmentMapResult = new HashMap<>(fieldAttachmentIds.size());
        for (Map.Entry<String, List<String>> entry : fieldAttachmentIds.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                continue;
            }
            List<Attachment> fieldAttachments = new ArrayList<>();
            entry.getValue().forEach(attachmentId -> {
                if (attachmentMap.containsKey(attachmentId)) {
                    fieldAttachments.add(attachmentMap.get(attachmentId));
                }
            });
            attachmentMapResult.put(entry.getKey(), fieldAttachments);
        }
        return attachmentMapResult;
    }

    /**
     * 处理自定义字段中业务子表单值
     *
     * @param resource    资源详情
     * @param fieldValues 自定义字段值
     * @param formConfig  表单配置
     */
    public void processBusinessFieldValues(Object resource, List<BaseModuleFieldValue> fieldValues, ModuleFormConfigDTO formConfig) {
        List<BaseField> subFields = formConfig.getFields()
                .stream()
                .filter(f -> f instanceof SubField && StringUtils.isNotEmpty(f.getBusinessKey()))
                .toList();

        Map<String, String> subFieldBusinessMap = subFields.stream().collect(Collectors.toMap(BaseField::getId, BaseField::getBusinessKey));
        List<BaseModuleFieldValue> businessFieldValues = fieldValues
                .stream()
                .filter(fv -> subFieldBusinessMap.containsKey(fv.getFieldId()))
                .toList();

        businessFieldValues.forEach(bfv -> {
            String businessKey = subFieldBusinessMap.get(bfv.getFieldId());
            Field field = ReflectionUtils.findField(resource.getClass(), f -> Strings.CS.equals(f.getName(), businessKey));
            if (field == null) {
                log.error("Cannot find field `{}`", businessKey);
                return;
            }
            ReflectionUtils.setField(field, resource, bfv.getFieldValue());
        });

        fieldValues = new ArrayList<>(fieldValues);
        fieldValues.removeIf(fv -> subFieldBusinessMap.containsKey(fv.getFieldId()));
        Field moduleFields = ReflectionUtils.findField(resource.getClass(), f -> Strings.CS.equals(f.getName(), "moduleFields"));
        if (moduleFields == null) {
            log.error("No such field `moduleFields` in resource");
            return;
        }
        ReflectionUtils.setField(moduleFields, resource, fieldValues);
    }

    /**
     * 替换字段引用选项
     *
     * @param field 自定义字段
     */
    public void setFieldRefOption(BaseField field) {
        if (field instanceof SubField subField) {
            subField.getSubFields().forEach(this::setFieldRefOption);
        }
        if (!(field instanceof HasOption of)) {
            return;
        }
        if (StringUtils.isEmpty(of.getOptionSource()) || Strings.CS.equals(of.getOptionSource(), OPTION_DEFAULT_SOURCE)) {
            if (CollectionUtils.isEmpty(of.getCustomOptions())) {
                of.setCustomOptions(of.getOptions());
            } else {
                of.setOptions(of.getCustomOptions());
            }
        } else {
            // 引用字段选项, 清空再替换
            of.setOptions(new ArrayList<>());
            String refId = of.getRefId();
            ModuleFieldBlob fieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(refId);
            if (fieldBlob != null) {
                BaseField refField = JSON.parseObject(fieldBlob.getProp(), BaseField.class);
                if (refField instanceof HasOption refOption) {
                    of.setOptions(CollectionUtils.isNotEmpty(refOption.getOptions()) ? refOption.getOptions() : refOption.getCustomOptions());
                }
            }
        }
    }

    /**
     * 设置自定义字段业务参数
     *
     * @param field 自定义字段
     */
    public void setFieldBusinessParam(BaseField field) {
        Set<String> businessTitleIdSet = Arrays.stream(BusinessTitleConstants.values())
                .map(BusinessTitleConstants::getId)
                .collect(Collectors.toSet());
        if (field.isSys()) {
            return;
        }
        if (StringUtils.isNotBlank(field.getResourceFieldId())) {
            String actualFieldId = field.getId().replace((field.getResourceFieldId() + REF_UNDERLINE), StringUtils.EMPTY);
            if (businessTitleIdSet.contains(actualFieldId)) {
                return;
            }
        }

        // 获取特殊的业务字段
        Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
                collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));
        if (field instanceof SubField subField) {
            subField.getSubFields().forEach(this::setFieldBusinessParam);
        }
        BusinessModuleField businessEnum = businessModuleFieldMap.get(field.getInternalKey());
        if (businessEnum != null) {
            // 设置特殊的业务字段 key
            field.setBusinessKey(businessEnum.getBusinessKey());
            field.setDisabledProps(businessEnum.getDisabledProps());
        } else {
            field.setBusinessKey(null);
            field.setDisabledProps(null);
        }
    }

    /**
     * 重载子表引用字段最新的属性
     *
     * @param field 自定义字段
     */
    public void reloadPropOfSubRefFields(BaseField field, Map<String, BaseField> priceSubFieldMap) {
        if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
            List<BaseField> subSourceField = subField.getSubFields().stream()
                    .filter(f -> f instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields())).toList();
            subSourceField.forEach(sf -> {
                List<String> oldRefIds = ((DatasourceField) sf).getShowFields().stream()
                        .map(splitRefId(sf.getId())).distinct().toList();
                List<ModuleFieldBlob> reloadFieldBlobs = moduleFieldBlobMapper.selectByIds(oldRefIds);
                Map<String, BaseField> reloadFieldMap = reloadFieldBlobs.stream().collect(Collectors.toMap(ModuleFieldBlob::getId,
                        filedBlob -> JSON.parseObject(filedBlob.getProp(), BaseField.class)));

                // 补充一些内置字段信息
                getSystemExtendFields(((DatasourceField) sf).getDataSourceType())
                        .forEach(extField -> reloadFieldMap.put(extField.getId(), extField));
                // 合并可能引用的字段属性 (数据源引用字段 & 价格表子表格字段)
                reloadFieldMap.putAll(priceSubFieldMap);

                Function<String, String> refIdSplitter = splitRefId(sf.getId());
                ListIterator<BaseField> it = subField.getSubFields().listIterator();
                while (it.hasNext()) {
                    BaseField oldField = it.next();
                    // 只处理符合条件的引用字段
                    if (StringUtils.isEmpty(oldField.getResourceFieldId()) || !Strings.CI.equals(oldField.getResourceFieldId(), sf.getId())) {
                        continue;
                    }
                    // 兼容旧引用字段
                    String oldRefFieldId = refIdSplitter.apply(oldField.getId());
                    BaseField refField = reloadFieldMap.get(oldRefFieldId);
                    if (refField == null) {
                        // 引用的字段已删
                        it.remove();
                        ((DatasourceField) sf).getShowFields().remove(oldRefFieldId);
                        continue;
                    }
                    BaseField combineField = combineFieldsProps(oldField, refField);
                    // 子表格的引用字段特殊属性
                    BusinessModuleField businessField = BusinessModuleField.ofKey(combineField.getInternalKey());
                    if (businessField != null) {
                        combineField.setBusinessKey(businessField.getBusinessKey());
                    }
                    if (combineField.isSys()) {
                        combineField.setBusinessKey(refField.getBusinessKey());
                    }
                    combineField.setSubTableFieldId(oldField.getSubTableFieldId());
                    it.set(combineField);
                }
            });
        }
    }

    /**
     * 平铺数据源引用字段
     *
     * @param fields 入库字段集合
     * @return 平铺后的字段集合 (数据源引用字段被平铺成普通字段, 并且属性被更新为最新引用字段属性)
     */
    @SuppressWarnings("unchecked")
    public List<BaseField> flattenSourceRefFields(List<BaseField> fields, Map<String, BaseField> priceSubFieldMap) {
        List<BaseField> flatFields = new ArrayList<>();
        fields.forEach(field -> {
            flatFields.add(field);
            if (field instanceof DatasourceField sourceField && MapUtils.isNotEmpty(sourceField.getCombineSearch())) {
                Object obj = sourceField.getCombineSearch().get("conditions");
                if (obj instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            Map<String, Object> condition = (Map<String, Object>) map;
                            if (!condition.containsKey("matchType")) {
                                condition.put("matchType", "MATCH_FIELD");
                            }
                        }
                    }
                }
            }

            if (field instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields())) {
                // 兼容新旧引用字段
                List<String> oldRefIds = sourceField.getShowFields().stream().map(splitRefId(sourceField.getId())).distinct().toList();
                List<ModuleFieldBlob> reloadFieldBlobs = moduleFieldBlobMapper.selectByIds(oldRefIds);
                Map<String, BaseField> reloadFieldMap = reloadFieldBlobs.stream().collect(Collectors.toMap(ModuleFieldBlob::getId,
                        filedBlob -> JSON.parseObject(filedBlob.getProp(), BaseField.class)));

                // 补充内置扩展的系统字段
                getSystemExtendFields(sourceField.getDataSourceType())
                        .forEach(extField -> reloadFieldMap.put(extField.getId(), extField));

                // 合并可能引用的字段属性 (数据源引用字段 & 价格表子表格字段)
                reloadFieldMap.putAll(priceSubFieldMap);

                // 兼容处理旧版本引用字段没有refFields属性的情况，直接从showFields解析出引用字段并设置属性
                if (CollectionUtils.isEmpty(sourceField.getRefFields())) {
                    sourceField.setRefFields(new ArrayList<>());
                    for (String showFieldKey : sourceField.getShowFields()) {
                        BaseField refField = reloadFieldMap.get(showFieldKey);
                        if (refField == null) {
                            continue;
                        }
                        refField.setResourceFieldId(sourceField.getId());
                        sourceField.getRefFields().add(refField);
                    }
                }

                // 平铺引用字段
                sourceField.getRefFields().forEach(oldRefField -> {
                    // 兼容旧引用字段
                    String oldRefFieldId = splitRefId(oldRefField.getResourceFieldId()).apply(oldRefField.getId());
                    BaseField refField = reloadFieldMap.get(oldRefFieldId);
                    if (refField == null) {
                        // 引用的字段过期或已被删除
                        sourceField.getShowFields().remove(oldRefFieldId);
                        return;
                    }
                    BaseField combineField = combineFieldsProps(oldRefField, refField);
                    combineField.setPos(oldRefField.getPos() == null ? sourceField.getPos() : oldRefField.getPos());
                    flatFields.add(flatFields.size(), combineField);
                });
            }
        });
        // 按照pos排序保证数据源引用字段被平铺后顺序不变
        flatFields.sort(Comparator.comparing(BaseField::getPos));
        return flatFields;
    }

    private List<BaseField> getSystemExtendFields(String dataSourceType) {
        if (Strings.CI.equals(dataSourceType, FieldSourceType.BUSINESS_TITLE.name())) {
            return initBusinessTitleFields();
        }
        if (Strings.CI.equalsAny(dataSourceType, FieldSourceType.CONTRACT.name(), FieldSourceType.INVOICE.name(), FieldSourceType.ORDER.name(), FieldSourceType.QUOTATION.name())) {
            // 目前只有这几种数据源支持系统字段
            return initSourceSystemFields(FieldSourceType.valueOf(dataSourceType));
        }
        return List.of();
    }

    public List<BaseField> initBusinessTitleFields() {
        List<BaseField> fields = new ArrayList<>();
        Locale locale = LocaleContextHolder.getLocale();
        BusinessTitleConstants[] values = BusinessTitleConstants.values();
        for (BusinessTitleConstants constant : values) {
            InputField field = new InputField();
            field.setId(constant.getId());
            field.setBusinessKey(constant.getKey());
            if (Locale.US.toString().equalsIgnoreCase(locale.toString())) {
                field.setName(constant.getUs());
            } else {
                field.setName(constant.getCh());
            }
            field.setInternalKey(constant.getKey());
            field.setBusinessKey(constant.getKey());
            field.setType(FieldType.INPUT.name());
            field.setShowLabel(true);
            field.setReadable(true);
            fields.add(field);
        }
        return fields;
    }

    public List<BaseField> initSourceSystemFields(FieldSourceType sourceType) {
        if (sourceType == null) {
            return List.of();
        }
        List<BaseField> fields = new ArrayList<>();
        for (SystemFieldConstants sf : SystemFieldConstants.values()) {
            // 目前只有下拉类型系统字段, 后续可根据枚举扩展
            SelectField field = new SelectField();
            field.setId(sf.getKey());
            field.setSys(true);
            fields.add(field);
        }
        return fields;
    }

    /**
     * OptionProp转OptionDTO
     *
     * @param options 选项集合
     * @return 选项DTO集合
     */
    public List<OptionDTO> optionPropToDto(List<OptionProp> options) {
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }
        return options.stream().map(option -> {
            OptionDTO optionDTO = new OptionDTO();
            optionDTO.setName(option.getLabel());
            optionDTO.setId(option.getValue());
            return optionDTO;
        }).toList();
    }

    /**
     * 表单初始化
     */
    public void initForm() {
        initFormAndFields(FormKey.allKeys());
    }

    /**
     * 初始化升级表单
     */
    public void initUpgradeForm() {
        List<String> allKeys = FormKey.allKeys();
        LambdaQueryWrapper<ModuleForm> moduleFormWrapper = new LambdaQueryWrapper<>();
        moduleFormWrapper.in(ModuleForm::getFormKey, allKeys);
        List<ModuleForm> oldForms = moduleFormMapper.selectListByLambda(moduleFormWrapper);
        allKeys.removeAll(oldForms.stream().map(ModuleForm::getFormKey).toList());
        if (CollectionUtils.isEmpty(allKeys)) {
            // 初始化完成, 无升级表单.
            return;
        }
        initFormAndFields(allKeys);
    }

    /**
     * 初始化线索转联系人表单联动规则
     */
    @SuppressWarnings("unchecked")
    public void initContactFormLinkRules() {
        // 加载初始化的字段信息
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.in(ModuleField::getInternalKey, List.of("contactName", "contactPhone", "clueContactName", "clueContactPhone"));
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
        if (CollectionUtils.isEmpty(fields) || fields.size() < 4) {
            log.error("未找到对应的内置字段，无法初始化联动规则");
            return;
        }
        Map<String, String> fieldMap = fields.stream().collect(Collectors.toMap(ModuleField::getInternalKey, ModuleField::getId));
        // 构建联动规则
        LinkField contactNameLink = new LinkField();
        contactNameLink.setCurrent(fieldMap.get("contactName"));
        contactNameLink.setLink(fieldMap.get("clueContactName"));
        contactNameLink.setEnable(true);
        LinkField contactPhoneLink = new LinkField();
        contactPhoneLink.setCurrent(fieldMap.get("contactPhone"));
        contactPhoneLink.setLink(fieldMap.get("clueContactPhone"));
        contactPhoneLink.setEnable(true);
        // 更新表单属性
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.CONTACT.getKey());
        ModuleForm contactForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob formBlob = moduleFormBlobMapper.selectByPrimaryKey(contactForm.getId());
        Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
        List<LinkScenario> contactLinkProp = List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_CONTACT.name())
                .linkFields(List.of(contactNameLink, contactPhoneLink)).build());
        propMap.put("linkProp", Map.of(FormKey.CLUE.getKey(), contactLinkProp));
        formBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(formBlob);
    }

    /**
     * 初始化订单(合同)联动规则
     */
    @SuppressWarnings("unchecked")
    public void initContractToOrderLinkScenario() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.ORDER.getKey());
        ModuleForm orderForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob formBlob = moduleFormBlobMapper.selectByPrimaryKey(orderForm.getId());
        Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
        propMap.put("linkProp", Map.of(FormKey.CONTRACT.getKey(), List.of(
                LinkScenario.builder().key(LinkScenarioKey.CONTRACT_TO_ORDER.name()).linkFields(new ArrayList<>()))));
        formBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(formBlob);
    }

    @SuppressWarnings("unchecked")
    public void initExtFieldsByVer(String version) {
        try {
            List<ModuleField> fields = new ArrayList<>();
            List<ModuleFieldBlob> fieldBlobs = new ArrayList<>();
            Map<String, List<Map<String, Object>>> fieldMap = JSON.parseObject(fieldResource.getInputStream(), Map.class);
            fieldMap.forEach((formKey, formFields) -> {
                boolean existExtVerField = formFields.stream().anyMatch(f -> f.containsKey(UPGRADE_EXT_FIELD) && Strings.CS.equals(version, f.get(UPGRADE_EXT_FIELD).toString()));
                if (!existExtVerField) {
                    return;
                }
                ModuleForm example = new ModuleForm();
                example.setFormKey(formKey);
                ModuleForm form = moduleFormMapper.selectOne(example);
                if (form == null) {
                    log.error("未找到表单 {}, 无法初始化扩展字段", formKey);
                    return;
                }
                Long maxPos = extModuleFieldMapper.getMaxFieldPosByFormId(form.getId());
                List<Map<String, Object>> extFields = formFields.stream().filter(f -> f.containsKey(UPGRADE_EXT_FIELD) && Strings.CS.equals(version, f.get(UPGRADE_EXT_FIELD).toString())).toList();
                AtomicLong pos = new AtomicLong(maxPos + 1);
                extFields.forEach(initField -> {
                    ModuleField field = supplyFieldInfo(initField, form.getId(), pos.getAndIncrement(), new HashMap<>(2));
                    initField.put("id", field.getId());
                    fields.add(field);
                    ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
                    fieldBlob.setId(field.getId());
                    fieldBlob.setProp(JSON.toJSONString(initField));
                    fieldBlobs.add(fieldBlob);
                });
            });
            if (CollectionUtils.isNotEmpty(fields)) {
                moduleFieldMapper.batchInsert(fields);
            }
            if (CollectionUtils.isNotEmpty(fieldBlobs)) {
                moduleFieldBlobMapper.batchInsert(fieldBlobs);
            }
        } catch (Exception e) {
            log.error("表单扩展字段初始化失败", e);
            throw new GenericException("表单扩展字段初始化失败", e);
        }
    }

    /**
     * 表单及字段初始化 (升级)
     *
     * @param initKeys 初始化Key集合
     */
    private void initFormAndFields(List<String> initKeys) {
        Map<String, String> formKeyMap = new HashMap<>(FormKey.values().length);
        List<ModuleForm> forms = new ArrayList<>();
        List<ModuleFormBlob> formBlobs = new ArrayList<>();
        initKeys.forEach(formKey -> {
            ModuleForm form = new ModuleForm();
            form.setId(IDGenerator.nextStr());
            form.setFormKey(formKey);
            form.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            form.setCreateUser(InternalUser.ADMIN.getValue());
            form.setCreateTime(System.currentTimeMillis());
            form.setUpdateUser(InternalUser.ADMIN.getValue());
            form.setUpdateTime(System.currentTimeMillis());
            forms.add(form);
            formKeyMap.put(formKey, form.getId());
            ModuleFormBlob formBlob = new ModuleFormBlob();
            formBlob.setId(form.getId());
            try {
                FormProp formProp = JSON.parseObject(formResource.getInputStream(), FormProp.class);
                formBlob.setProp(JSON.toJSONString(formProp));
            } catch (IOException e) {
                throw new GenericException("表单属性初始化失败", e);
            }
            formBlobs.add(formBlob);
        });
        moduleFormMapper.batchInsert(forms);
        moduleFormBlobMapper.batchInsert(formBlobs);
        // init form fields
        initFormFields(formKeyMap);
    }

    /**
     * 字段初始化 (静态json文件)
     *
     * @param formKeyMap 表单Key映射
     */
    @SuppressWarnings("unchecked")
    public void initFormFields(Map<String, String> formKeyMap) {
        List<ModuleField> fields = new ArrayList<>();
        List<ModuleFieldBlob> fieldBlobs = new ArrayList<>();
        try {
            Map<String, List<Map<String, Object>>> fieldMap = JSON.parseObject(fieldResource.getInputStream(), Map.class);
            formKeyMap.keySet().forEach(key -> {
                String formId = formKeyMap.get(key);
                List<Map<String, Object>> initFields = fieldMap.get(key);
                AtomicLong pos = new AtomicLong(1L);
                // 显隐规则Key-ID映射
                Map<String, String> controlKeyPreMap = new HashMap<>(2);
                initFields.forEach(initField -> {
                    if (initField.containsKey(UPGRADE_EXT_FIELD)) {
                        return;
                    }
                    ModuleField field = supplyFieldInfo(initField, formId, pos.getAndIncrement(), controlKeyPreMap);
                    initField.put("id", field.getId());
                    fields.add(field);
                    if (initField.containsKey(CONTROL_RULES_KEY)) {
                        List<ControlRuleProp> controlRules = JSON.parseArray(JSON.toJSONString(initField.get(CONTROL_RULES_KEY)), ControlRuleProp.class);
                        controlRules.forEach(controlRule -> {
                            List<String> showFieldIds = new ArrayList<>();
                            controlRule.getFieldIds().forEach(fieldKey -> {
                                if (!controlKeyPreMap.containsKey(fieldKey)) {
                                    controlKeyPreMap.put(fieldKey, IDGenerator.nextStr());
                                }
                                showFieldIds.add(controlKeyPreMap.get(fieldKey));
                            });
                            controlRule.setFieldIds(showFieldIds);
                        });
                        initField.put(CONTROL_RULES_KEY, controlRules);
                    }
                    handleShowFieldsInit(initField, fields);
                    if (initField.containsKey(SUB_FIELDS)) {
                        List<BaseField> subFields = JSON.parseArray(JSON.toJSONString(initField.get(SUB_FIELDS)), BaseField.class);
                        subFields.forEach(subField -> subField.setId(IDGenerator.nextStr()));
                        initField.put(SUB_FIELDS, subFields);
                    }
                    ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
                    fieldBlob.setId(field.getId());
                    fieldBlob.setProp(JSON.toJSONString(initField));
                    fieldBlobs.add(fieldBlob);
                });
            });
            moduleFieldMapper.batchInsert(fields);
            moduleFieldBlobMapper.batchInsert(fieldBlobs);
        } catch (Exception e) {
            log.error("表单字段初始化失败", e);
            throw new GenericException("表单字段初始化失败", e);
        }
    }

    /**
     * 处理显示字段初始化
     *
     * @param initField  初始化字段
     * @param initFields 如果 initForm 初始化，数据库没有数据，需要从 initFields 中获取
     */
    @SuppressWarnings("unchecked")
    private void handleShowFieldsInit(Map<String, Object> initField, List<ModuleField> initFields) {
        if (initField.containsKey(SHOW_FIELD_KEY)) {
            List<String> showFieldKeys = (List<String>) initField.get(SHOW_FIELD_KEY);
            List<ModuleField> showFields = moduleFieldService.selectFieldsByInternalKeys(showFieldKeys);

            if (CollectionUtils.isEmpty(showFields)) {
                // initForm 初始化，数据库没有数据，需要从 initFields 中获取
                showFields = initFields.stream()
                        .filter(f -> showFieldKeys.contains(f.getInternalKey()))
                        .collect(Collectors.toList());
            }

            if (CollectionUtils.isNotEmpty(showFieldKeys)) {
                Set<String> internalKeys = showFields.stream()
                        .map(ModuleField::getInternalKey)
                        .collect(Collectors.toSet());

                // 添加表单字段
                List<String> showFieldResult = new ArrayList<>(showFields.stream().map(ModuleField::getId).toList());
                // 添加表单中没有的系统字段
                List<String> systemFieldKeys = showFieldKeys.stream()
                        .filter(fieldKey -> !internalKeys.contains(fieldKey))
                        .toList();
                showFieldResult.addAll(systemFieldKeys);
                initField.put(SHOW_FIELD_KEY, showFieldResult);
            }
        }
    }


    /**
     * 组装字段基础信息
     *
     * @param fieldMap         字段集合
     * @param formId           表单ID
     * @param pos              字段位置
     * @param controlKeyPreMap 显隐规则Key-ID映射
     * @return 字段
     */
    private ModuleField supplyFieldInfo(Map<String, Object> fieldMap, String formId, Long pos, Map<String, String> controlKeyPreMap) {
        ModuleField field = new ModuleField();
        field.setInternalKey(fieldMap.get("internalKey").toString());
        field.setId(controlKeyPreMap.containsKey(field.getInternalKey()) ? controlKeyPreMap.get(field.getInternalKey()) : IDGenerator.nextStr());
        field.setFormId(formId);
        field.setType(fieldMap.get("type").toString());
        field.setName(fieldMap.get("name").toString());
        field.setMobile((Boolean) fieldMap.getOrDefault("mobile", false));
        field.setPos(pos);
        field.setCreateTime(System.currentTimeMillis());
        field.setCreateUser(InternalUser.ADMIN.getValue());
        field.setUpdateTime(System.currentTimeMillis());
        field.setUpdateUser(InternalUser.ADMIN.getValue());
        return field;
    }

    /**
     * 初始化数据类型-数据源映射
     *
     * @return 集合
     */
    public Map<String, String> initTypeSourceMap() {
        Map<String, String> typeSourceMap = new HashMap<>(8);
        typeSourceMap.put(FieldType.MEMBER.name(), "sys_user");
        typeSourceMap.put(FieldType.DEPARTMENT.name(), "sys_department");
        typeSourceMap.put(FieldSourceType.CUSTOMER.name(), "customer");
        typeSourceMap.put(FieldSourceType.CLUE.name(), "clue");
        typeSourceMap.put(FieldSourceType.CONTACT.name(), "customer_contact");
        typeSourceMap.put(FieldSourceType.OPPORTUNITY.name(), "opportunity");
        typeSourceMap.put(FieldSourceType.PRODUCT.name(), "product");
        return typeSourceMap;
    }

    /**
     * 处理默认值初始化选项
     *
     * @param field 基础字段
     */
    private void handleInitialOption(BaseField field) {
        if (field instanceof MemberField memberField) {
            memberField.setInitialOptions(userExtendService.getUserOptionById(memberField.getDefaultValue()));
        }
        if (field instanceof MemberMultipleField memberMultipleField) {
            memberMultipleField.setInitialOptions(userExtendService.getUserOptionByIds(memberMultipleField.getDefaultValue()));
        }
        if (field instanceof DepartmentField departmentField) {
            departmentField.setInitialOptions(departmentService.getDepartmentOptionsById(departmentField.getDefaultValue()));
        }
        if (field instanceof DepartmentMultipleField departmentMultipleField) {
            departmentMultipleField.setInitialOptions(departmentService.getDepartmentOptionsByIds(departmentMultipleField.getDefaultValue()));
        }
    }

    /**
     * 将业务字段选项放入optionMap
     *
     * @param list              列表数据
     * @param getOptionIdFunc   获取选项ID函数
     * @param getOptionNameFunc 获取选项名称函数
     * @param <T>               实体
     */
    public <T> List<OptionDTO> getBusinessFieldOption(List<T> list,
                                                      Function<T, String> getOptionIdFunc,
                                                      Function<T, String> getOptionNameFunc) {
        return list.stream()
                .map(item -> {
                    OptionDTO optionDTO = new OptionDTO();
                    optionDTO.setId(getOptionIdFunc.apply(item));
                    optionDTO.setName(getOptionNameFunc.apply(item));
                    return optionDTO;
                })
                .distinct()
                .filter(option -> StringUtils.isNotEmpty(option.getIdAsString()))
                .toList();
    }

    public <T> List<OptionDTO> getBusinessFieldOption(T item,
                                                      Function<T, String> getOptionIdFunc,
                                                      Function<T, String> getOptionNameFunc) {
        return getBusinessFieldOption(List.of(item), getOptionIdFunc, getOptionNameFunc);
    }

    public <T> List<BaseModuleFieldValue> getBaseModuleFieldValues(List<T> list, Function<T, List<BaseModuleFieldValue>> getModuleFieldFunc) {
        // 处理自定义字段选项数据
        return list.stream()
                .map(getModuleFieldFunc)
                .filter(org.apache.commons.collections.CollectionUtils::isNotEmpty)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 获取自定义表头集合 (包括引用显示字段)
     *
     * @param exportHeads 导出头
     * @param formKey     表单Key
     * @param currentOrg  当前组织
     * @return 自定义导入表头集合
     */
    public List<List<String>> getAllExportHeads(List<ExportHeadDTO> exportHeads, String formKey, String currentOrg) {
        Map<String, BaseField> fieldConfigMap = getFieldConfigMapByName(formKey, currentOrg);
        List<List<String>> heads = new ArrayList<>();
        exportHeads.forEach(exportHead -> {
            if (Strings.CS.equals(exportHead.getColumnType(), EXPORT_SYSTEM_TYPE)) {
                heads.add(new ArrayList<>(Collections.singletonList(exportHead.getTitle())));
                return;
            }
            if (!fieldConfigMap.containsKey(exportHead.getTitle())) {
                return;
            }
            BaseField field = fieldConfigMap.get(exportHead.getTitle());
            if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
                Map<String, String> subFieldMap = subField.getSubFields()
                        .stream()
                        .collect(Collectors.toMap(f ->
                                StringUtils.isNotBlank(f.getResourceFieldId()) ? f.getId() :
                                        f.idOrBusinessKey(), BaseField::getName, (oldValue, newValue) -> oldValue));

                subField.getSubFields().stream()
                        .filter(BaseField::canExport)
                        .forEach(f -> {
                            List<String> head = new ArrayList<>();
                            head.add(field.getName());
                            head.add(StringUtils.isNotEmpty(f.getResourceFieldId()) ? f.getName() + REF_SYMBOL : f.getName());
                            heads.add(head);
                        });

                if (CollectionUtils.isNotEmpty(subField.getSumColumns())) {
                    subField.getSumColumns().forEach(sumColumn -> {
                        if (!subFieldMap.containsKey(sumColumn)) {
                            return;
                        }
                        heads.add(new ArrayList<>(Collections.singletonList(Translator.get("sum") + "-" + subFieldMap.get(sumColumn))));
                    });
                }
            } else {
                heads.add(new ArrayList<>(Collections.singletonList(StringUtils.isNotEmpty(field.getResourceFieldId()) ? field.getName() + REF_SYMBOL : field.getName())));
            }
        });
        return heads;
    }

    /**
     * 获取导出的合并头ID集合
     *
     * @param formKey     表单Key
     * @param currentOrg  当前组织
     * @param exportHeads 导出表头集合
     * @return 导出字段ID集合
     */
    public List<String> getExportMergeHeads(String formKey, String currentOrg, List<ExportHeadDTO> exportHeads) {
        Map<String, BaseField> fieldConfigMap = getFieldConfigMapByName(formKey, currentOrg);
        List<String> heads = new ArrayList<>();
        exportHeads.forEach(exportHead -> {
            if (Strings.CS.equals(exportHead.getColumnType(), EXPORT_SYSTEM_TYPE)) {
                heads.add(exportHead.getKey());
                return;
            }
            if (!fieldConfigMap.containsKey(exportHead.getTitle())) {
                return;
            }

            BaseField field = fieldConfigMap.get(exportHead.getTitle());
            if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
                Map<String, BaseField> subFieldMap = subField.getSubFields()
                        .stream()
                        .collect(Collectors.toMap(f -> StringUtils.isNotBlank(f.getResourceFieldId()) ? f.getId()
                                : f.idOrBusinessKey(), Function.identity(), (oldValue, newValue) -> oldValue));

                // 子表格的表头ID格式: 子表格ID|字段ID
                subField.getSubFields().stream()
                        .filter(BaseField::canExport)
                        .map(BaseField::getId)
                        .forEach(bf -> heads.add(subField.getId() + SLASH + bf));

                // 子表格汇总字段ID格式: sum_子表格ID|字段ID
                if (CollectionUtils.isNotEmpty(subField.getSumColumns())) {
                    subField.getSumColumns().forEach(sumColumn -> {
                        if (!subFieldMap.containsKey(sumColumn)) {
                            return;
                        }
                        heads.add(SUM_PREFIX + subField.getId() + SLASH + subFieldMap.get(sumColumn).getId());
                    });
                }
            } else {
                heads.add(field.getId());
            }
        });
        return heads;
    }

    /**
     * 获取不含引用字段的表头集合
     *
     * @param formKey    表单Key
     * @param currentOrg 当前组织
     * @return 表头集合
     */
    public List<List<String>> getCustomImportHeadsNoRef(String formKey, String currentOrg) {
        List<BaseField> allFields = getAllFields(formKey, currentOrg);
        if (CollectionUtils.isEmpty(allFields)) {
            return null;
        }

        List<BaseField> fields = allFields
                .stream()
                .filter(f -> StringUtils.isEmpty(f.getResourceFieldId()) && f.canImport(f))
                .toList();

        List<List<String>> heads = new ArrayList<>();
        fields.forEach(field -> {
            if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
                subField.getSubFields().forEach(f -> {
                    if (!f.canImport(f)) {
                        return;
                    }
                    List<String> head = new ArrayList<>();
                    head.add(field.getName());
                    head.add(StringUtils.isNotEmpty(f.getResourceFieldId()) ? f.getName() + REF_SYMBOL : f.getName());
                    heads.add(head);
                });
            } else {
                heads.add(new ArrayList<>(Collections.singletonList(field.getName())));
            }
        });
        return heads;
    }

    /**
     * 获取自定义导出字段集合
     *
     * @param formKey    表单Key
     * @param currentOrg 当前组织
     * @return 字段集合
     */
    public List<BaseField> getAllCustomImportFields(String formKey, String currentOrg) {
        List<BaseField> allFields = getAllFields(formKey, currentOrg);
        if (CollectionUtils.isEmpty(allFields)) {
            return null;
        }
        return new ArrayList<>(allFields);
    }

    /**
     * 支持子表头
     *
     * @param headFields 表头字段集合
     * @return 是否支持
     */
    public boolean supportSubHead(List<BaseField> headFields) {
        if (CollectionUtils.isEmpty(headFields)) {
            return false;
        }

        return headFields
                .stream()
                .anyMatch(field -> field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields()));
    }

    /**
     * 新旧字段配置中是否涉及统计字段。
     *
     * <p>统计字段是跨表单聚合配置, 只有涉及统计字段的保存才需要读取完整字段属性做新旧对比,
     * 普通字段的保存不应因此多出查询。</p>
     *
     * @param originFields  保存前的字段主表记录
     * @param currentFields 保存后的字段配置
     * @return 是否涉及统计字段
     */
    private boolean hasStatisticField(List<ModuleField> originFields, List<BaseField> currentFields) {
        boolean inOrigin = originFields.stream()
                .anyMatch(field -> Strings.CS.equals(field.getType(), FieldType.STATISTIC.name()));
        boolean inCurrent = currentFields.stream().anyMatch(StatisticField.class::isInstance);
        return inOrigin || inCurrent;
    }

    /**
     * 触发统计字段异步刷新。
     *
     * <p>保存表单配置本身处于事务中, 而刷新是异步任务; 直接调用会让异步线程可能先于事务提交执行,
     * 读到尚未提交的旧配置。因此登记到事务提交之后再触发, 无事务上下文时退化为同步调用。</p>
     *
     * @param formKey       表单Key
     * @param originFields  保存前的字段配置
     * @param currentFields 保存后的字段配置
     * @param orgId         组织ID
     */
    private void triggerStatisticRefresh(String formKey, List<BaseField> originFields,
                                         List<BaseField> currentFields, String orgId) {
        Runnable trigger = () ->
                statisticFieldService.refreshOnConfigSave(formKey, originFields, currentFields, orgId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    trigger.run();
                }
            });
        } else {
            trigger.run();
        }
    }

    /**
     * 字段保存预检查
     *
     * @param formKey 表单Key
     * @param fields  字段集合
     * @param orgId   组织ID
     */
    public void preCheckForFieldSave(String formKey, List<BaseField> fields, String orgId) {
        boolean businessDeleted = BusinessModuleField.isBusinessDeleted(formKey, fields);
        if (businessDeleted) {
            throw new GenericException(Translator.get("module.form.business_field.deleted"));
        }
        boolean hasRepeatName = BusinessModuleField.hasRepeatName(fields);
        if (hasRepeatName) {
            throw new GenericException(Translator.get("module.form.fields.repeat"));
        }
        Optional<BaseField> repeatOptional = fields.stream().filter(field -> {
            if (field instanceof HasOption optionField) {
                List<OptionProp> options = optionField.getOptions();
                return CollectionUtils.isNotEmpty(options) && hasRepeatOption(options);
            }
            return false;
        }).findAny();
        if (repeatOptional.isPresent()) {
            BaseField field = repeatOptional.get();
            throw new GenericException(Translator.getWithArgs("module.form.fields.option.repeat", field.getName()));
        }
        // 统计字段需要跨表单解析目标字段, 放在本地校验之后, 避免为非法配置做多余的查询。
        checkStatisticFields(formKey, fields, orgId);
    }

    /**
     * 统计字段配置校验。
     *
     * <p>统计字段是跨表单的聚合配置, 目标表单、关联字段与被统计字段三者必须自洽。
     * 目标表单与关联字段的合法组合由 {@link #getRelatedForms} 定义, 这里复用同一份解析结果做服务端
     * 校验, 避免前端提交未建立数据源关联的组合。</p>
     *
     * @param formKey 当前表单Key
     * @param fields  待保存的字段集合
     * @param orgId   组织ID
     */
    private void checkStatisticFields(String formKey, List<BaseField> fields, String orgId) {
        List<StatisticField> statisticFields = fields.stream()
                .filter(StatisticField.class::isInstance)
                .map(StatisticField.class::cast)
                .toList();
        if (CollectionUtils.isEmpty(statisticFields)) {
            return;
        }

        // TODO 自定义表单暂不支持添加统计字段: 其字段配置模型与数据权限链路尚未适配统计聚合, 后续版本放开。
        if (FormKey.ofKey(formKey) == null) {
            throw new GenericException(Translator.get("module.form.statistic.custom.form.unsupported"));
        }

        Map<String, RelatedFormDTO> relatedFormMap = getRelatedForms(formKey, orgId).stream()
                .collect(Collectors.toMap(RelatedFormDTO::getId, Function.identity(), (p, n) -> p));

        for (StatisticField statisticField : statisticFields) {
            checkStatisticField(statisticField, orgId, relatedFormMap);
        }
    }

    /**
     * 校验单个统计字段的配置自洽性。
     *
     * @param field          统计字段
     * @param orgId          组织ID
     * @param relatedFormMap 当前表单的关联表单, key 为目标表单Key
     */
    private void checkStatisticField(StatisticField field, String orgId,
                                      Map<String, RelatedFormDTO> relatedFormMap) {
        String name = field.getName();

        // 统计类型决定聚合方式, 非法值会让统计任务无法执行, 必须尽早拦截。
        boolean validType = Arrays.stream(StatisticType.values())
                .anyMatch(type -> type.name().equals(field.getStatisticType()));
        if (!validType) {
            throw new GenericException(Translator.getWithArgs("module.form.statistic.type.invalid", name));
        }

        if (StringUtils.isBlank(field.getTargetFormId())) {
            throw new GenericException(Translator.getWithArgs("module.form.statistic.target.required", name));
        }
        // TODO 自定义表单暂不支持作为被统计的目标表单, 目标表单目前只允许标准模块表单, 后续版本放开。
        if (FormKey.ofKey(field.getTargetFormId()) == null) {
            throw new GenericException(Translator.getWithArgs("module.form.statistic.target.custom.form.unsupported", name));
        }
        if (StringUtils.isBlank(field.getRelatedFieldId())) {
            throw new GenericException(Translator.getWithArgs("module.form.statistic.related.field.required", name));
        }

        RelatedFormDTO targetForm = relatedFormMap.get(field.getTargetFormId());
        if (targetForm == null) {
            throw new GenericException(Translator.getWithArgs("module.form.statistic.target.invalid", name));
        }
        boolean relatedFieldMatched = targetForm.getSourceTypeFields().stream()
                .anyMatch(option -> option.getId().equals(field.getRelatedFieldId()));
        if (!relatedFieldMatched) {
            throw new GenericException(Translator.getWithArgs("module.form.statistic.related.field.invalid", name));
        }

        // COUNT 只统计关联数据条数, 无需被统计字段; SUM / AVG 必须指定可聚合的数值类字段。
        if (field.needStatisticField()) {
            if (StringUtils.isBlank(field.getStatisticFieldId())) {
                throw new GenericException(Translator.getWithArgs("module.form.statistic.field.required", name));
            }
            if (!isStatisticableField(field.getStatisticFieldId(), field.getTargetFormId(), orgId)) {
                throw new GenericException(Translator.getWithArgs("module.form.statistic.field.invalid", name));
            }
        }

        if (Strings.CS.equals(field.getDataScope(), StatisticDataScope.CONDITION.name())
                && MapUtils.isEmpty(field.getCombineSearch())) {
            throw new GenericException(Translator.getWithArgs("module.form.statistic.data.scope.required", name));
        }
        if (Strings.CS.equals(field.getUpdateScope(), StatisticUpdateScope.CONDITION.name())
                && MapUtils.isEmpty(field.getUpdateScopeCondition())) {
            throw new GenericException(Translator.getWithArgs("module.form.statistic.update.scope.required", name));
        }
    }

    /**
     * 被统计字段是否可聚合: 存在于目标表单, 且为子表格之外的数值、计算、统计字段。
     *
     * @param statisticFieldId 被统计字段ID
     * @param targetFormId     目标表单Key
     * @param orgId            组织ID
     * @return 是否可聚合
     */
    private boolean isStatisticableField(String statisticFieldId, String targetFormId, String orgId) {
        ModuleForm example = new ModuleForm();
        example.setFormKey(targetFormId);
        example.setOrganizationId(orgId);
        ModuleForm targetForm = moduleFormMapper.selectOne(example);
        if (targetForm == null) {
            return false;
        }

        return getAllFields(targetForm.getId()).stream()
                // 子表格字段与数据源显示字段不作为统计口径。
                .filter(field -> StringUtils.isBlank(field.getSubTableFieldId())
                        && StringUtils.isBlank(field.getResourceFieldId()))
                .anyMatch(field -> field.getId().equals(statisticFieldId)
                        && Strings.CS.equalsAny(field.getType(), FieldType.INPUT_NUMBER.name(),
                        FieldType.FORMULA.name(), FieldType.STATISTIC.name()));
    }

    /**
     * 包含重复选项
     *
     * @param options 选项
     * @return 是否重复选项
     */
    private boolean hasRepeatOption(List<OptionProp> options) {
        if (CollectionUtils.isEmpty(options)) {
            return false;
        }
        return options.stream()
                .collect(Collectors.groupingBy(OptionProp::getLabel, Collectors.counting()))
                .values().stream()
                .anyMatch(count -> count > 1);
    }

    /**
     * 判断内置字段是否包含唯一性校验
     *
     * @param formKey 表单Key
     * @param orgId   组织ID
     * @return 是否唯一
     */
    public boolean hasFieldUniqueCheck(String formKey, String orgId, String internalKey) {
        List<BaseField> allFields = getAllFields(formKey, orgId);
        if (CollectionUtils.isEmpty(allFields)) {
            return false;
        }
        Optional<BaseField> internalField = allFields.stream().filter(field -> Strings.CS.equals(field.getInternalKey(), internalKey)).findFirst();
        return internalField.isPresent() && internalField.get().needRepeatCheck();
    }

    /**
     * 填充表单联动值
     *
     * @param target           目标对象
     * @param source           源对象
     * @param targetFormConfig 目标表单配置
     * @param orgId            组织ID
     * @param <T>              实体类型
     * @param <S>              数据来源类型
     * @param scenarioKey      场景Key
     * @return 填充结果
     */
    public <T, S> FormLinkFill<T> fillFormLinkValue(T target, S source, ModuleFormConfigDTO targetFormConfig,
                                                    String orgId, String sourceFormKey, String scenarioKey) throws Exception {
        FormLinkFill.FormLinkFillBuilder<T> fillBuilder = FormLinkFill.builder();
        Map<String, List<LinkScenario>> linkProp = targetFormConfig.getFormProp().getLinkProp();
        if (linkProp == null || CollectionUtils.isEmpty(linkProp.get(sourceFormKey))) {
            return fillBuilder.entity(target).build();
        }
        // 未找到联动场景，直接返回目标对象
        Optional<LinkScenario> scenarioOptional = linkProp.get(sourceFormKey).stream().filter(scenario -> Strings.CS.equals(scenario.getKey(), scenarioKey)).findFirst();
        if (scenarioOptional.isEmpty()) {
            return fillBuilder.entity(target).build();
        }

        ModuleFormConfigDTO sourceFormConfig = getBusinessFormConfig(sourceFormKey, orgId);
        List<BaseField> sourceFields = sourceFormConfig.getFields();
        List<BaseField> targetFields = targetFormConfig.getFields();
        if (CollectionUtils.isEmpty(sourceFields) || CollectionUtils.isEmpty(targetFields)) {
            return fillBuilder.entity(target).build();
        }
        // 目标表单字段
        Map<String, BaseField> targetFieldMap = targetFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity()));
        // 来源表单字段
        Map<String, BaseField> sourceFieldMap = sourceFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity()));
        // 填充数据
        Class<?> targetClass = target.getClass();
        Class<?> sourceClass = source.getClass();
        List<BaseModuleFieldValue> targetFieldVals = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<BaseModuleFieldValue> sourceFieldVals = (List<BaseModuleFieldValue>) sourceClass.getMethod("getModuleFields").invoke(source);
        for (LinkField linkField : scenarioOptional.get().getLinkFields()) {
            if (!linkField.isEnable()) {
                continue;
            }
            BaseField targetField = targetFieldMap.get(linkField.getCurrent());
            BaseField sourceField = sourceFieldMap.get(linkField.getLink());
            if (targetField == null || sourceField == null) {
                continue;
            }
            // 从源对象字段取值
            TransformSourceApplyDTO sourceValue;
            try {
                sourceValue = applySourceValue(sourceField, sourceClass, source, sourceFieldVals);
            } catch (Exception e) {
                sourceValue = null;
                log.error("Apply source value error", e);
            }

            // 源对象中无值, 跳过取值
            if (sourceValue == null || sourceValue.getActualVal() == null) {
                continue;
            }

            // 放入目标对象字段
            putTargetFieldVal(targetField, sourceValue, targetClass, target, targetFieldVals);
        }
        return fillBuilder.entity(target).fields(targetFieldVals).build();
    }

    /**
     * 属性转方法 (name -> setName)
     *
     * @param param 属性名
     * @return 方法名
     */
    private String capitalizeSetParam(String param) {
        return "set" + param.substring(0, 1).toUpperCase() + param.substring(1);
    }

    /**
     * 属性转方法 (name -> getName)
     *
     * @param param 属性名
     * @return 方法名
     */
    private String capitalizeGetParam(String param) {
        return "get" + param.substring(0, 1).toUpperCase() + param.substring(1);
    }

    /**
     * 选项值转文本
     *
     * @param options 选项集合
     * @param value   值
     * @return 文本
     */
    private Object val2Text(List<OptionProp> options, Object value) {
        if (CollectionUtils.isEmpty(options) || value == null) {
            return null;
        }
        Map<String, String> optionMap = options.stream()
                .filter(option -> option.getValue() != null)
                .collect(Collectors.toMap(option -> option.getValue().toString(), OptionProp::getLabel, (a, b) -> a));
        if (value instanceof List) {
            return ((List<?>) value).stream().map(v -> optionMap.get(v.toString())).toList();
        } else {
            return optionMap.get(value.toString());
        }
    }

    /**
     * 选项文本转值
     *
     * @param options 选项集合
     * @param text    文本
     * @return 值
     */
    private Object text2Val(List<OptionProp> options, Object text) {
        if (CollectionUtils.isEmpty(options) || text == null) {
            return null;
        }
        Map<String, String> optionMap = options.stream()
                .filter(option -> option.getValue() != null)
                .collect(Collectors.toMap(OptionProp::getLabel, option -> option.getValue().toString(), (a, b) -> a));
        if (text instanceof List) {
            return ((List<?>) text).stream().map(v -> optionMap.get(v.toString())).filter(Objects::nonNull).toList();
        } else {
            return optionMap.get(text.toString());
        }
    }

    /**
     * 从源对象取值
     *
     * @param sourceField     来源字段
     * @param sourceClass     类对象
     * @param source          数据来源
     * @param sourceFieldVals 自定义数据来源
     * @return 值
     * @throws Exception 取值异常
     */
    private TransformSourceApplyDTO applySourceValue(BaseField sourceField, Class<?> sourceClass, Object source, List<BaseModuleFieldValue> sourceFieldVals) throws Exception {
        // 来源字段取值
        TransformSourceApplyDTO sourceApply = new TransformSourceApplyDTO();
        Object tmpVal;
        if (StringUtils.isNotEmpty(sourceField.getBusinessKey())) {
            // 业务字段取值
            tmpVal = sourceClass.getMethod(capitalizeGetParam(sourceField.getBusinessKey())).invoke(source);
        } else {
            // 自定义字段取值
            Optional<BaseModuleFieldValue> find = sourceFieldVals.stream().filter(fieldVal -> Strings.CS.equals(sourceField.getId(), fieldVal.getFieldId())).findFirst();
            tmpVal = find.map(BaseModuleFieldValue::getFieldValue).orElse(null);
        }
        sourceApply.setActualVal(tmpVal);
        // 取展示值
        if (tmpVal == null) {
            sourceApply.setDisplayVal(null);
        } else {
            sourceApply.setDisplayVal(displayOfType(sourceField, sourceApply.getActualVal()));
        }
        if (sourceField instanceof InputNumberField) {
            sourceApply.setActualVal(sourceApply.getDisplayVal());
        }
        return sourceApply;
    }

    /**
     * 放入目标对象字段值
     *
     * @param targetField     字段
     * @param putVal          放入值
     * @param targetClass     目标类
     * @param target          目标实例
     * @param targetFieldVals 目标自定义字段值集合
     * @throws Exception 入值异常
     */
    private void putTargetFieldVal(BaseField targetField, TransformSourceApplyDTO putVal, Class<?> targetClass, Object target, List<BaseModuleFieldValue> targetFieldVals) throws Exception {
        Object val = resolveTargetPutVal(targetField, putVal);
        if (val == null) {
            return;
        }
        if (StringUtils.isNotEmpty(targetField.getBusinessKey())) {
            // 目标字段是业务字段
            Method method = targetClass.getMethod(capitalizeSetParam(targetField.getBusinessKey()), targetClass.getDeclaredField(targetField.getBusinessKey()).getType());
            method.invoke(target, val);
        } else {
            // 目标字段是自定义字段
            BaseModuleFieldValue targetFieldVal = new BaseModuleFieldValue();
            targetFieldVal.setFieldId(targetField.getId());
            targetFieldVal.setFieldValue(val);
            targetFieldVals.add(targetFieldVal);
        }
    }

    /**
     * 根据类型获取展示值
     *
     * @param sourceField 来源字段
     * @param actualVal   实际值
     * @return 展示值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object displayOfType(BaseField sourceField, Object actualVal) {
        if (actualVal == null) {
            return null;
        }
        if (sourceField instanceof HasOption fieldWithOption) {
            return val2Text(fieldWithOption.getOptions(), actualVal);
        }
        AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(sourceField.getType());
        // 将数据库中的字符串值,转换为对应的对象值
        return customFieldResolver.transformToValue(sourceField, actualVal instanceof List ? JSON.toJSONString(actualVal) : actualVal.toString());
    }

    /**
     * 解析目标字段值
     *
     * @param targetField 目标字段
     * @param sourceVal   来源值
     * @return 值
     */
    @SuppressWarnings({"unchecked"})
    public Object resolveTargetPutVal(BaseField targetField, TransformSourceApplyDTO sourceVal) {
        if (targetField.multiple() && sourceVal.getActualVal() instanceof String) {
            // 兼容处理: 单值映射多值的情况
            sourceVal.setActualVal(List.of(sourceVal.getActualVal()));
            sourceVal.setDisplayVal(List.of(sourceVal.getDisplayVal()));
        }
        if (targetField instanceof InputField || targetField instanceof TextAreaField) {
            // 兼容处理: [文本, 多行文本] 按照展示值处理即可.
            Object displayVal = sourceVal.getDisplayVal();
            if (displayVal == null) {
                return null;
            }
            String displayStr;
            if (displayVal instanceof List) {
                displayStr = String.join(",", (List<String>) displayVal);
            } else {
                displayStr = displayVal.toString();
            }
            if (targetField instanceof InputField) {
                return new TextResolver().getCorrectInputString(displayStr);
            } else {
                return displayStr;
            }
        }
        switch (targetField) {
            case InputMultipleField ignored -> {
                TextMultipleResolver textMultipleResolver = new TextMultipleResolver();
                return textMultipleResolver.getCorrectFormatInput(
                        sourceVal.getDisplayVal() instanceof List
                                ? (List<String>) sourceVal.getDisplayVal() : List.of(sourceVal.getDisplayVal().toString().split(","))
                );
            }
            case HasOption targetFieldWithOption -> {
                return text2Val(targetFieldWithOption.getOptions(), sourceVal.getDisplayVal());
            }
            case InputNumberField ignored -> {
                String actualVal = sourceVal.getActualVal().toString();
                if (StringUtils.isEmpty(actualVal)) {
                    return null;
                }
                if (actualVal.contains("%") || actualVal.contains(",")) {
                    actualVal = actualVal.replace(",", "").replace("%", "");
                }
                try {
                    return new BigDecimal(actualVal);
                } catch (NumberFormatException e) {
                    log.error("Invalid source value: {}", actualVal, e);
                    return null;
                }
            }
            default -> {
            }
        }
        return sourceVal.getActualVal();
    }

    /**
     * 表单联动处理(旧数据)
     */
    @SuppressWarnings("unchecked")
    public void modifyFormLinkProp() {
        List<ModuleForm> moduleForms = moduleFormMapper.selectAll(null);
        List<String> formIds = moduleForms.stream().map(ModuleForm::getId).toList();
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formIds);
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            Object linkProp = propMap.get("linkProp");
            if (linkProp == null) {
                continue;
            }
            Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
            if (linkPropMap.containsKey("formKey") && linkPropMap.containsKey("linkFields")) {
                Map<String, List<LinkField>> dataMap = new HashMap<>(2);
                String formKey = linkPropMap.get("formKey").toString();
                List<LinkField> linkFields = (List<LinkField>) linkPropMap.get("linkFields");
                dataMap.put(formKey, linkFields);
                propMap.put("linkProp", dataMap);
                formBlob.setProp(JSON.toJSONString(propMap));
                moduleFormBlobMapper.updateById(formBlob);
            }
        }
    }

    /**
     * 处理表单联动的旧数据&&支持多场景 (客户&商机&记录)
     */
    @SuppressWarnings("unchecked")
    public void processOldLinkData() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ModuleForm::getFormKey, List.of(FormKey.CUSTOMER.getKey(), FormKey.OPPORTUNITY.getKey()));
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(wrapper);
        Map<String, String> formKeyMap = forms.stream().collect(Collectors.toMap(ModuleForm::getId, ModuleForm::getFormKey));
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formKeyMap.keySet().stream().toList());
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            Object linkProp = propMap.get("linkProp");
            Map<String, List<LinkScenario>> dataMap = new HashMap<>(2);
            String formKey = formKeyMap.get(formBlob.getId());
            if (linkProp == null) {
                if (Strings.CS.equals(formKey, FormKey.CUSTOMER.getKey())) {
                    dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_CUSTOMER.name()).linkFields(new ArrayList<>()).build()));
                } else if (Strings.CS.equals(formKey, FormKey.OPPORTUNITY.getKey())) {
                    dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_OPPORTUNITY.name()).linkFields(new ArrayList<>()).build()));
                    dataMap.put(FormKey.CUSTOMER.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CUSTOMER_TO_OPPORTUNITY.name()).linkFields(new ArrayList<>()).build()));
                }
            } else {
                Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
                for (Map.Entry<String, Object> entry : linkPropMap.entrySet()) {
                    if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null || !(entry.getValue() instanceof List)) {
                        continue;
                    }
                    List<Map<String, Object>> fields = (List<Map<String, Object>>) entry.getValue();
                    List<LinkField> fieldList = fields.stream().map(field -> {
                        field.put("enable", true);
                        LinkField linkField = new LinkField();
                        try {
                            org.apache.commons.beanutils.BeanUtils.populate(linkField, field);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            log.error("Populate old link field error", e);
                        }
                        return linkField;
                    }).toList();
                    String scenarioKey = (Strings.CS.equals(formKey, FormKey.CUSTOMER.getKey()) && Strings.CS.equals(entry.getKey(), FormKey.CLUE.getKey()) ?
                            LinkScenarioKey.CLUE_TO_CUSTOMER.name() :
                            (Strings.CS.equals(formKey, FormKey.OPPORTUNITY.getKey()) && Strings.CS.equals(entry.getKey(), FormKey.CLUE.getKey()) ?
                                    LinkScenarioKey.CLUE_TO_OPPORTUNITY.name() : LinkScenarioKey.CUSTOMER_TO_OPPORTUNITY.name()));
                    LinkScenario linkScenario = LinkScenario.builder().key(scenarioKey).linkFields(fieldList).build();
                    dataMap.put(entry.getKey(), List.of(linkScenario));
                }
            }
            propMap.put("linkProp", dataMap);
            formBlob.setProp(JSON.toJSONString(propMap));
            moduleFormBlobMapper.updateById(formBlob);
        }
    }

    /**
     * 初始化跟进记录表单联动场景
     */
    @SuppressWarnings("unchecked")
    public void initFormScenarioProp() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.FOLLOW_RECORD.getKey());
        ModuleForm recordForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob recordFormBlob = moduleFormBlobMapper.selectByPrimaryKey(recordForm.getId());
        Map<String, Object> propMap = JSON.parseMap(recordFormBlob.getProp());
        Object linkProp = propMap.get("linkProp");
        Map<String, List<LinkScenario>> dataMap = new HashMap<>(4);
        dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        dataMap.put(FormKey.CUSTOMER.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CUSTOMER_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        dataMap.put(FormKey.OPPORTUNITY.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.OPPORTUNITY_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        if (linkProp != null) {
            Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
            List<Map<String, Object>> fields = (List<Map<String, Object>>) linkPropMap.get(FormKey.FOLLOW_PLAN.getKey());
            List<LinkField> fieldList = fields.stream().map(field -> {
                LinkField linkField = JSON.parseObject(JSON.toJSONString(field), LinkField.class);
                linkField.setEnable(true);
                return linkField;
            }).toList();
            dataMap.put(FormKey.FOLLOW_PLAN.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.PLAN_TO_RECORD.name()).linkFields(fieldList).build()));
        } else {
            dataMap.put(FormKey.FOLLOW_PLAN.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.PLAN_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        }
        propMap.put("linkProp", dataMap);
        recordFormBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(recordFormBlob);
    }

    /**
     * 初始化发票表单联动场景
     */
    @SuppressWarnings("unchecked")
    public void initInvoiceFormScenarioProp() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.INVOICE.getKey());
        ModuleForm invoiceForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob invoiceFormBlob = moduleFormBlobMapper.selectByPrimaryKey(invoiceForm.getId());
        Map<String, Object> propMap = JSON.parseMap(invoiceFormBlob.getProp());
        List<LinkScenario> contractLinkProp = List.of(LinkScenario.builder().key(LinkScenarioKey.CONTRACT_TO_INVOICE.name()).linkFields(List.of()).build());
        propMap.put("linkProp", Map.of(FormKey.CONTRACT.getKey(), contractLinkProp));
        invoiceFormBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(invoiceFormBlob);
    }

    /**
     * 初始化订单表单联动场景
     */
    @SuppressWarnings("unchecked")
    public void initOrderFormScenarioProp() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.ORDER.getKey());
        ModuleForm orderForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob orderFormBlob = moduleFormBlobMapper.selectByPrimaryKey(orderForm.getId());
        Map<String, Object> propMap = JSON.parseMap(orderFormBlob.getProp());
        List<LinkScenario> contractLinkProp = List.of(LinkScenario.builder().key(LinkScenarioKey.CONTRACT_TO_ORDER.name()).linkFields(List.of()).build());
        propMap.put("linkProp", Map.of(FormKey.CONTRACT.getKey(), contractLinkProp));
        orderFormBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(orderFormBlob);
    }

    /**
     * 表单属性处理(视图)
     */
    @SuppressWarnings("unchecked")
    public void modifyFormProp() {
        List<ModuleForm> moduleForms = moduleFormMapper.selectAll(null);
        List<String> formIds = moduleForms.stream().map(ModuleForm::getId).toList();
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formIds);
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            if (propMap.containsKey("viewSize")) {
                continue;
            }
            propMap.put("viewSize", "large");
            formBlob.setProp(JSON.toJSONString(propMap));
            moduleFormBlobMapper.updateById(formBlob);
        }
    }

    /**
     * 为所有组织的标准表单初始化内置详情标签。
     *
     * <p>只处理 {@link InternalDetailTab} 中声明过的父表单，并复用详情标签解析逻辑补全当前组织实际的
     * 关联表单和字段。已有合法标签会被保留，缺失的内置标签按枚举顺序补回。</p>
     */
    public void initInternalDetailTabs() {
        LocaleContextHolder.setLocale(Locale.US);

        List<String> internalFormKeys = Arrays.stream(InternalDetailTab.values())
                .map(InternalDetailTab::getFormKey)
                .distinct()
                .toList();
        List<ModuleForm> moduleForms = moduleFormMapper.selectListByLambda(new LambdaQueryWrapper<ModuleForm>()
                .in(ModuleForm::getFormKey, internalFormKeys));
        if (CollectionUtils.isEmpty(moduleForms)) {
            return;
        }

        List<String> formIds = moduleForms.stream().map(ModuleForm::getId).toList();
        Map<String, ModuleFormBlob> formBlobMap = moduleFormBlobMapper.selectByIds(formIds).stream()
                .collect(Collectors.toMap(ModuleFormBlob::getId, Function.identity()));
        for (ModuleForm moduleForm : moduleForms) {
            ModuleFormBlob formBlob = formBlobMap.get(moduleForm.getId());
            if (formBlob == null) {
                continue;
            }
            FormProp formProp = StringUtils.isBlank(formBlob.getProp())
                    ? new FormProp() : JSON.parseObject(formBlob.getProp(), FormProp.class);
            resolveDetailTabs(moduleForm.getFormKey(), moduleForm.getOrganizationId(), formProp);
            formBlob.setProp(JSON.toJSONString(formProp));
            moduleFormBlobMapper.updateById(formBlob);
        }
    }

    @SuppressWarnings("unchecked")
    public void modifyFieldMobile() {
        List<ModuleField> moduleFields = moduleFieldMapper.selectAll(null);
        List<String> fieldIds = moduleFields.stream().map(ModuleField::getId).toList();
        List<ModuleFieldBlob> moduleFieldBlobs = moduleFieldBlobMapper.selectByIds(fieldIds);
        for (ModuleFieldBlob fieldBlob : moduleFieldBlobs) {
            Map<String, Object> propMap = JSON.parseMap(fieldBlob.getProp());
            propMap.put("mobile", true);
            fieldBlob.setProp(JSON.toJSONString(propMap));
            moduleFieldBlobMapper.updateById(fieldBlob);
        }
        extModuleFieldMapper.batchUpdateMobile(fieldIds, true);
    }

    /**
     * 获取MCP表单需要的字段
     *
     * @param formKey        表单Key
     * @param organizationId 组织ID
     * @return 字段列表
     */
    public List<SimpleField> getMcpFields(String formKey, String organizationId) {
        ModuleFormConfigDTO businessFormConfig = getBusinessFormConfig(formKey, organizationId);
        return businessFormConfig.getFields().stream().filter(BaseField::canImport).map(field -> {
            SimpleField simpleField = new SimpleField();
            simpleField.setId(field.getId());
            simpleField.setBusinessKey(field.getBusinessKey());
            simpleField.setName(field.getName());
            simpleField.setType(field.getType());
            simpleField.setRequired(field.needRequireCheck());
            simpleField.setShowControlRules(field.getShowControlRules());
            if (field instanceof HasOption fieldWithOption) {
                simpleField.setOptions(fieldWithOption.getOptions());
            }
            if (field instanceof DatasourceField datasourceField) {
                simpleField.setDataSourceType(datasourceField.getDataSourceType());
            } else if (field instanceof LocationField locationField) {
                simpleField.setLocationType(locationField.getLocationType());
            } else if (field instanceof DateTimeField dateTimeField) {
                simpleField.setDateType(dateTimeField.getDateType());
            }
            return simpleField;
        }).toList();
    }

    /**
     * 业务Key => 字段ID (子字段)
     *
     * @param fieldValues 自定义字段值
     * @param formConfig  字段配置
     * @return 处理后的自定义字段值
     */
    public <T extends BaseResourceField, V extends BaseResourceField> List<BaseModuleFieldValue> resolveSnapshotFields(List<BaseModuleFieldValue> fieldValues,
                                                                                                                       ModuleFormConfigDTO formConfig, BaseResourceFieldService<T, V> baseResourceFieldService, String resourceId) {
        if (CollectionUtils.isEmpty(fieldValues)) {
            return new ArrayList<>();
        }

        // 1. 扁平化所有字段
        final List<BaseField> flattenFields = flattenFormAllFields(formConfig);
        List<BaseModuleFieldValue> resolveFvs = resolveSubKeyToId(fieldValues, flattenFields);

        // 2. 处理数据源引用字段
        List<BaseModuleFieldValue> resolveRefFvs = resolveRefFields(resolveFvs, flattenFields, baseResourceFieldService);

        // 3. 补充流水号字段
        List<BaseField> fs = flattenFields.stream().filter(BaseField::isSerialNumber).toList();
        for (BaseField bf : fs) {
            if (StringUtils.isEmpty(bf.getBusinessKey())) {
                Object serialVal = baseResourceFieldService.getResourceFieldValue(resourceId, bf.getId());
                if (serialVal != null) {
                    resolveRefFvs.removeIf(rvs -> Strings.CS.equals(rvs.getFieldId(), bf.getId()));
                    resolveRefFvs.add(new BaseModuleFieldValue(bf.getId(), serialVal));
                }
            }
        }

        return resolveRefFvs;
    }

    /**
     * 处理数据源引用字段
     *
     * @param resolveFvs               字段值列表
     * @param flattenFields            平铺字段配置
     * @param baseResourceFieldService 字段服务类
     * @param <F>                      字段类型
     * @param <B>                      大字段类型
     * @return 处理后的字段值列表
     */
    @SuppressWarnings("unchecked")
    private <F extends BaseResourceField, B extends BaseResourceField> List<BaseModuleFieldValue> resolveRefFields(
            List<BaseModuleFieldValue> resolveFvs, List<BaseField> flattenFields,
            BaseResourceFieldService<F, B> baseResourceFieldService) {
        // 1. 数据源字段配置映射 && 字段配置映射
        final Map<String, BaseField> sourceConfigMap = flattenFields
                .stream()
                .filter(f -> f instanceof DatasourceField ds && CollectionUtils.isNotEmpty(ds.getShowFields()))
                .collect(Collectors.toMap(BaseField::idOrBusinessKey, f -> f, (f1, f2) -> f1));

        final Map<String, BaseField> fieldMap = flattenFields
                .stream()
                .collect(Collectors.toMap(BaseField::getId, f -> f, (f1, f2) -> f1));

        // 2. 处理引用字段值
        List<BaseModuleFieldValue> reFvs = new ArrayList<>();
        resolveFvs.forEach(fv -> {
            if (fv.getFieldValue() == null) {
                return;
            }
            final String fieldId = fv.getFieldId();
            // 数据源字段（普通字段）
            if (sourceConfigMap.containsKey(fieldId)) {
                final DatasourceField sourceField = (DatasourceField) sourceConfigMap.get(fieldId);
                final FieldSourceType sourceType = FieldSourceType.safeValueOf(sourceField.getDataSourceType());
                final Object detail;
                if (sourceType == FieldSourceType.CUSTOM_FORM) {
                    // 自定义表单：设置 formId 到 ThreadLocal
                    try {
                        CustomFormDataFieldService.setFormKey(sourceField.getDataSourceType());
                        detail = fieldSourceServiceProvider.safeGetSimpleById(sourceType, fv.getFieldValue().toString());
                    } finally {
                        CustomFormDataFieldService.clearFormKey();
                    }
                } else {
                    detail = fieldSourceServiceProvider.safeGetSimpleById(sourceType, fv.getFieldValue().toString());
                }
                if (detail == null) {
                    return;
                }

                final Map<String, Object> detailMap = JSON.MAPPER.convertValue(detail, Map.class);
                sourceField.getShowFields().forEach(refId -> {
                    final BaseField showFieldConf = fieldMap.get(sourceField.getId() + REF_UNDERLINE + refId);
                    if (showFieldConf != null) {
                        final Object val = baseResourceFieldService.getFieldValueOfDetailMap(showFieldConf, detailMap, sourceField);
                        if (val != null) {
                            reFvs.add(new BaseModuleFieldValue(showFieldConf.getId(), val));
                        }
                    }
                });
                return;
            }

            // 子字段嵌套数据源字段
            if (fieldMap.containsKey(fieldId) && fieldMap.get(fieldId).isSubField()) {
                final List<Map<String, Object>> subValues = (List<Map<String, Object>>) fv.getFieldValue();
                subValues.forEach(sfv -> {
                    final Map<String, Object> showFieldMap = new HashMap<>(8);
                    sfv.forEach((k, v) -> {
                        if (v == null || !sourceConfigMap.containsKey(k)) {
                            return;
                        }

                        final DatasourceField sourceField = (DatasourceField) sourceConfigMap.get(k);
                        final FieldSourceType sourceType = FieldSourceType.safeValueOf(sourceField.getDataSourceType());
                        final Object detail;
                        if (sourceType == FieldSourceType.CUSTOM_FORM) {
                            // 自定义表单：设置 formId 到 ThreadLocal
                            try {
                                CustomFormDataFieldService.setFormKey(sourceField.getDataSourceType());
                                detail = fieldSourceServiceProvider.safeGetSimpleById(sourceType, v.toString());
                            } finally {
                                CustomFormDataFieldService.clearFormKey();
                            }
                        } else {
                            detail = fieldSourceServiceProvider.safeGetSimpleById(sourceType, v.toString());
                        }

                        final Map<String, Object> detailMap = JSON.MAPPER.convertValue(detail, Map.class);
                        if (MapUtils.isEmpty(detailMap)) {
                            return;
                        }

                        sourceField.getShowFields().forEach(id -> {
                            final BaseField showFieldConf = fieldMap.get(sourceField.getId() + REF_UNDERLINE + id);
                            if (showFieldConf == null) {
                                return;
                            }
                            if (StringUtils.isNotEmpty(showFieldConf.getSubTableFieldId()) && sfv.containsKey(PRICE_SUB_ROW_KEY)) {
                                Object matchVal = baseResourceFieldService.matchSubFieldValueOfDetailMap(
                                        showFieldConf.idOrBusinessKey(),
                                        detailMap,
                                        BusinessModuleField.PRICE_PRODUCT_TABLE.getBusinessKey(),
                                        sfv.get(PRICE_SUB_ROW_KEY).toString()
                                );

                                if (matchVal != null) {
                                    showFieldMap.put(showFieldConf.getId(), matchVal);
                                }
                            } else {
                                showFieldMap.put(showFieldConf.getId(), baseResourceFieldService.getFieldValueOfDetailMap(showFieldConf, detailMap, sourceField));
                            }
                        });
                    });
                    sfv.putAll(showFieldMap);
                });
            }
        });
        if (!reFvs.isEmpty()) {
            resolveFvs.addAll(reFvs);
        }
        return resolveFvs;
    }

    /**
     * 解析子表格Key=>ID
     *
     * @param fvs           所有字段值
     * @param flattenFields 所有字段配置
     * @return 处理后的字段值
     */
    private List<BaseModuleFieldValue> resolveSubKeyToId(List<BaseModuleFieldValue> fvs, List<BaseField> flattenFields) {
        // 获取替换后的子表格值
        final Map<String, BaseField> subFieldConfigMap = flattenFields
                .stream()
                .filter(f -> f instanceof SubField)
                .collect(Collectors.toMap(BaseField::idOrBusinessKey, f -> f));

        final List<BaseModuleFieldValue> subFieldValues = fvs.stream()
                .filter(fv -> subFieldConfigMap.containsKey(fv.getFieldId()) && subFieldConfigMap.get(fv.getFieldId()).isSubField())
                .map(fv -> new BaseModuleFieldValue(
                        subFieldConfigMap.get(fv.getFieldId()).getId(),
                        fv.getFieldValue()
                )).toList();

        // 删除原 subField 项，并加入替换后的
        fvs.removeIf(fv -> subFieldConfigMap.containsKey(fv.getFieldId())
                && subFieldConfigMap.get(fv.getFieldId()).isSubField());

        if (!subFieldValues.isEmpty()) {
            fvs.addAll(subFieldValues);
        }
        return fvs;
    }

    /**
     * 获取字段配置映射 (name => BaseField)
     *
     * @param formKey 表单Key
     * @param orgId   组织ID
     * @return 配置映射
     */
    private Map<String, BaseField> getFieldConfigMapByName(String formKey, String orgId) {
        List<BaseField> allFields = getAllFields(formKey, orgId);
        if (CollectionUtils.isEmpty(allFields)) {
            return Map.of();
        }
        return allFields.stream().collect(Collectors.toMap(BaseField::getName, Function.identity(), (p, n) -> p));
    }

    /**
     * 组合引用字段配置
     *
     * @param old 旧字段
     * @param ref 引用字段
     * @return 组合后的字段配置
     */
    private BaseField combineFieldsProps(BaseField old, BaseField ref) {
        if (ref.isSys()) {
            // 系统字段, 直接返回前端引用的字段配置即可, 后端不组装字段
            old.setSys(true);
            return old;
        }
        // 深拷贝 ref 对象, 避免修改原始对象
        BaseField refCopy = JSON.parseObject(JSON.toJSONString(ref), BaseField.class);
        // 保留一些可用的属性
        refCopy.setName(old.getName());
        refCopy.setFieldWidth(old.getFieldWidth());
        refCopy.setResourceFieldId(old.getResourceFieldId());
        // 兼容新旧引用字段
        if (Strings.CI.contains(old.getId(), REF_UNDERLINE)) {
            // 新版本引用字段, 直接替换
            refCopy.setId(old.getId());
        } else {
            // 旧版本拼接
            refCopy.setId(old.getResourceFieldId() + REF_UNDERLINE + old.getId());
        }
        // 清空多级引用的属性, 禁止数据源引用数据源
        if (refCopy instanceof DatasourceField refSourceField) {
            refSourceField.setRefFields(null);
            refSourceField.setShowFields(null);
        }
        return refCopy;
    }

    /**
     * 引用字段ID截取
     *
     * @param sourceId 数据源ID
     * @return 截取后的字段ID
     */
    private Function<String, String> splitRefId(String sourceId) {
        return fieldId -> {
            if (StringUtils.isEmpty(fieldId)) {
                return fieldId;
            }
            int idx = fieldId.indexOf(sourceId + REF_UNDERLINE);
            return idx >= 0 ? fieldId.substring(idx + sourceId.length() + REF_UNDERLINE.length()) : fieldId;
        };
    }

    /**
     * 获取业务数据详情
     *
     * @param formKey    表单Key
     * @param resourceId 资源ID
     * @return 通用的条件值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<BaseModuleFieldValue> compressResourceDetail(String formKey, String resourceId) {
        List<BaseModuleFieldValue> fvs = new ArrayList<>();
        Object resourceDetail = fieldSourceServiceProvider.safeGetSimpleById(formKey, resourceId);
        if (resourceDetail == null) {
            return fvs;
        }
        Map<String, Object> detailMap = JSON.MAPPER.convertValue(resourceDetail, Map.class);
        if (org.apache.commons.collections.MapUtils.isEmpty(detailMap)) {
            return fvs;
        }

        detailMap.forEach((k, v) -> {
            if (Strings.CI.equals(BaseResourceFieldService.DETAIL_FIELD_PARAM_NAME, k)) {
                List<Map> moduleFieldValues = (List<Map>) v;
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(moduleFieldValues)) {
                    for (Map mfv : moduleFieldValues) {
                        BaseModuleFieldValue bfv = new BaseModuleFieldValue();
                        bfv.setFieldId(mfv.get("fieldId").toString());
                        bfv.setFieldValue(mfv.get("fieldValue"));
                        fvs.add(bfv);
                    }
                }
            } else {
                BaseModuleFieldValue bfv = new BaseModuleFieldValue();
                bfv.setFieldId(k);
                bfv.setFieldValue(v);
                fvs.add(bfv);
            }
        });

        return fvs;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<BaseModuleFieldValue> compressResourceRefDetail(String formKey, String resourceId) {
        List<BaseModuleFieldValue> fvs = new ArrayList<>();
        Object resourceDetail = fieldSourceServiceProvider.safeGetFieldsById(formKey, resourceId);
        if (resourceDetail == null) {
            return fvs;
        }
        Map<String, Object> detailMap = JSON.MAPPER.convertValue(resourceDetail, Map.class);
        if (org.apache.commons.collections.MapUtils.isEmpty(detailMap)) {
            return fvs;
        }

        detailMap.forEach((k, v) -> {
            if (Strings.CI.equals(BaseResourceFieldService.DETAIL_FIELD_PARAM_NAME, k)) {
                List<Map> moduleFieldValues = (List<Map>) v;
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(moduleFieldValues)) {
                    for (Map mfv : moduleFieldValues) {
                        BaseModuleFieldValue bfv = new BaseModuleFieldValue();
                        bfv.setFieldId(mfv.get("fieldId").toString());
                        bfv.setFieldValue(mfv.get("fieldValue"));
                        fvs.add(bfv);
                    }
                }
            } else {
                BaseModuleFieldValue bfv = new BaseModuleFieldValue();
                bfv.setFieldId(k);
                bfv.setFieldValue(v);
                fvs.add(bfv);
            }
        });

        return fvs;
    }


    public List<List<String>> getCustomImportHeadsNoRefAndOwner(String formKey, String orgId) {
        List<BaseField> allFields = getAllFields(formKey, orgId);
        if (CollectionUtils.isEmpty(allFields)) {
            return null;
        }

        List<BaseField> fields = allFields
                .stream()
                .filter(f -> StringUtils.isEmpty(f.getResourceFieldId()) && f.canImport(f) && !Strings.CI.equals(f.getBusinessKey(), BusinessModuleField.CLUE_OWNER.getBusinessKey()))
                .toList();

        List<List<String>> heads = new ArrayList<>();
        fields.forEach(field -> {
            if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
                subField.getSubFields().forEach(f -> {
                    if (StringUtils.isNotEmpty(f.getResourceFieldId()) || !f.canImport(f)) {
                        return;
                    }
                    List<String> head = new ArrayList<>();
                    head.add(field.getName());
                    head.add(f.getName());
                    heads.add(head);
                });
            } else {
                heads.add(new ArrayList<>(Collections.singletonList(field.getName())));
            }
        });
        return heads;
    }

    public List<BaseField> getAllCustomImportFieldsNoOwner(String formKey, String orgId) {
        List<BaseField> allFields = getAllFields(formKey, orgId);
        if (CollectionUtils.isEmpty(allFields)) {
            return null;
        }

        List<BaseField> fields = allFields
                .stream()
                .filter(f -> StringUtils.isEmpty(f.getResourceFieldId()) && f.canImport(f) && !Strings.CI.equals(f.getBusinessKey(), BusinessModuleField.CLUE_OWNER.getBusinessKey()))
                .toList();
        return fields;
    }

}
