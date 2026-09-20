package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.dto.condition.FilterDBCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CaseFormatUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.StatisticDataScope;
import cn.cordys.crm.system.constants.StatisticEmptyResultMode;
import cn.cordys.crm.system.constants.StatisticEmptyValueMode;
import cn.cordys.crm.system.constants.StatisticType;
import cn.cordys.crm.system.constants.StatisticUpdateScope;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.dto.StatisticFieldSourceDTO;
import cn.cordys.crm.system.dto.field.StatisticField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtStatisticMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 统计字段刷新服务。
 *
 * <p>统计字段的值来源于「目标表单中通过数据源单选字段关联到当前记录的 N 条数据」, 与公式字段由前端
 * 计算提交不同, 统计值只能由后端聚合后写回, 因此需要四个触发入口:</p>
 * <ol>
 *   <li>保存表单配置时, 先对比新旧统计字段配置, 有变化才异步刷新该表单的存量数据
 *       ({@link #refreshOnConfigSave});</li>
 *   <li>目标表单新增或变更关联数据时自动刷新被关联记录 ({@link #refreshByRelatedDataChange});</li>
 *   <li>各模块新增资源后, 刷新该条记录上的全部统计字段 ({@link #refreshDataStatisticFields});</li>
 *   <li>用户在详情页/编辑页手动刷新某条数据上的某个统计字段 ({@link #refreshField})。</li>
 * </ol>
 *
 * <p>统计值写回宿主的字段值表({@code <table>_field}), 与普通自定义字段同表同格式,
 * 读取时由 {@code StatisticResolver} 格式化展示。</p>
 *
 * <p><b>已知边界</b>: 「统计范围」为「符合条件」({@code dataScope = CONDITION}) 的字段暂不参与刷新,
 * 原因见 {@link #buildContext}; 关联字段值发生变更时, 变更前指向的那条宿主记录不会被自动重算,
 * 原因见 {@link #refreshByRelatedDataChange}。</p>
 */
@Slf4j
@Service
public class StatisticFieldService {

    /**
     * 游标分页每页条数。
     */
    private static final int PAGE_SIZE = 500;

    /**
     * 单个统计字段最多同时处理几页。
     *
     * <p>并行度不是越高越好: 每页都要做一批聚合查询加写回, 并行度过高会打满数据库连接池。</p>
     */
    private static final int MAX_PARALLEL_PAGE = 4;

    /**
     * 字段值表后缀, 与 {@code ClueField} 等实体的 {@code @Table} 命名保持一致。
     */
    private static final String FIELD_TABLE_SUFFIX = "_field";

    /**
     * 大字段值表后缀。
     */
    private static final String BLOB_TABLE_SUFFIX = "_field_blob";

    /**
     * 主表列名的合法形式, 用于把 {@link BusinessModuleField} 的列名拼进 SQL 之前再卡一道。
     *
     * <p>不允许大写: 枚举里写的是驼峰, 必须经 {@link CaseFormatUtils#camelToUnderscore} 转成下划线,
     * 带上大写说明有人绕过了那一步, 拼出来的列名数据库里不存在。</p>
     */
    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");

    /**
     * 部门是虚拟字段, 各模块的取数方式不同(通常要 join sys_organization_user), 这里没有通用实现。
     */
    private static final String VIRTUAL_DEPARTMENT_COLUMN = "department_id";

    /**
     * 标准模块表单的物理表名, 取自各模块主实体的 {@code @Table} 注解。
     *
     * <p>没有直接复用 {@code FieldSourceType}: 那是「数据源」维度的枚举, 既缺跟进计划/跟进记录,
     * 也不能用 {@code safeValueOf(formKey)} 反查 —— 那个方法认得的是枚举名(大写),
     * 传表单Key进去会一律落到自定义表单上。表里没有的表单Key即视为自定义表单。</p>
     */
    private static final Map<String, String> FORM_KEY_TABLE = Map.ofEntries(
            Map.entry(FormKey.CLUE.getKey(), "clue"),
            Map.entry(FormKey.CUSTOMER.getKey(), "customer"),
            Map.entry(FormKey.CONTACT.getKey(), "customer_contact"),
            Map.entry(FormKey.FOLLOW_RECORD.getKey(), "follow_up_record"),
            Map.entry(FormKey.FOLLOW_PLAN.getKey(), "follow_up_plan"),
            Map.entry(FormKey.OPPORTUNITY.getKey(), "opportunity"),
            Map.entry(FormKey.PRODUCT.getKey(), "product"),
            Map.entry(FormKey.PRICE.getKey(), "product_price"),
            Map.entry(FormKey.QUOTATION.getKey(), "opportunity_quotation"),
            Map.entry(FormKey.CONTRACT.getKey(), "contract"),
            Map.entry(FormKey.INVOICE.getKey(), "contract_invoice"),
            Map.entry(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), "contract_payment_plan"),
            Map.entry(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), "contract_payment_record"),
            Map.entry(FormKey.ORDER.getKey(), "sales_order")
    );

    /**
     * 手动刷新统计字段时, 各表单要求的读权限(与 {@code ModuleFieldController} 里各模块的读接口一致)。
     *
     * <p>刷新接口只收一个字段ID, 参数越少越容易被构造, 所以必须在这里把「有没有资格看这个表单的字段」
     * 补回来, 否则知道字段ID就能拿到别人的数据。</p>
     *
     * <p>值是列表而不是单个权限: 跟进计划/跟进记录同时挂在线索、客户(记录还包括商机)下,
     * 有其中任意一个读权限即可, 与 {@code FollowUpPlanController} 的 {@code Logical.OR} 一致。</p>
     */
    private static final Map<String, List<String>> FORM_KEY_PERMISSIONS = Map.ofEntries(
            Map.entry(FormKey.CLUE.getKey(), List.of(PermissionConstants.CLUE_MANAGEMENT_READ)),
            Map.entry(FormKey.CUSTOMER.getKey(), List.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ)),
            Map.entry(FormKey.CONTACT.getKey(), List.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)),
            Map.entry(FormKey.FOLLOW_RECORD.getKey(), List.of(PermissionConstants.CLUE_MANAGEMENT_READ,
                    PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)),
            Map.entry(FormKey.FOLLOW_PLAN.getKey(), List.of(PermissionConstants.CLUE_MANAGEMENT_READ,
                    PermissionConstants.CUSTOMER_MANAGEMENT_READ)),
            Map.entry(FormKey.OPPORTUNITY.getKey(), List.of(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)),
            Map.entry(FormKey.PRODUCT.getKey(), List.of(PermissionConstants.PRODUCT_MANAGEMENT_READ)),
            Map.entry(FormKey.PRICE.getKey(), List.of(PermissionConstants.PRICE_READ)),
            Map.entry(FormKey.QUOTATION.getKey(), List.of(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)),
            Map.entry(FormKey.CONTRACT.getKey(), List.of(PermissionConstants.CONTRACT_READ)),
            Map.entry(FormKey.INVOICE.getKey(), List.of(PermissionConstants.CONTRACT_INVOICE_READ)),
            Map.entry(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), List.of(PermissionConstants.CONTRACT_PAYMENT_PLAN_READ)),
            Map.entry(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), List.of(PermissionConstants.CONTRACT_PAYMENT_RECORD_READ)),
            Map.entry(FormKey.ORDER.getKey(), List.of(PermissionConstants.ORDER_READ))
    );

    @Resource
    private ExtStatisticMapper extStatisticMapper;

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    @Resource
    private ResourcePermissionService resourcePermissionService;

    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;

    @Resource(name = "statisticRefreshExecutor")
    private Executor statisticRefreshExecutor;


    /**
     * 自注入代理: 刷新任务跑在线程池里, 需要走代理才能让 {@link #writeStatisticValue} 上的
     * {@code @Transactional} 生效(直接 this 调用不走代理)。
     */
    @Lazy
    @Resource
    private StatisticFieldService self;

    /**
     * 保存表单配置后, 刷新该表单存量数据上的统计字段 (异步)。
     *
     * <p>由 {@code ModuleFormService} 在配置保存事务的 afterCommit 回调中触发,
     * 保证这里读到的是已经提交的新配置。</p>
     *
     * @param formKey       表单Key
     * @param originFields  保存前的字段配置 (必须在删除旧字段之前取)
     * @param currentFields 保存后的字段配置
     * @param orgId         组织ID
     */
    @Async("threadPoolTaskExecutor")
    public void refreshOnConfigSave(String formKey, List<BaseField> originFields, List<BaseField> currentFields, String orgId) {
        try {
            // 1) 只处理标准模块表单: 自定义表单暂不支持统计字段, 直接返回。
            String hostDataTable = FORM_KEY_TABLE.get(formKey);
            if (hostDataTable == null) {
                log.warn("统计字段刷新跳过, 非标准模块表单: formKey={}", formKey);
                return;
            }

            // 2) 按字段ID给新旧配置建索引。字段ID以 sys_module_field.id 为准,
            //    设计器里改标题不会换ID, 换ID等价于「删一个 + 加一个」。
            Map<String, StatisticField> originMap = indexStatisticFields(originFields);
            Map<String, StatisticField> currentMap = indexStatisticFields(currentFields);

            // 3) 待刷新集合: 新增的字段, 以及配置指纹发生变化的字段。
            List<StatisticField> toRefresh = new ArrayList<>(currentMap.size());
            currentMap.forEach((fieldId, current) -> {
                StatisticField origin = originMap.get(fieldId);
                if (origin == null || !StringUtils.equals(configFingerprint(origin), configFingerprint(current))) {
                    toRefresh.add(current);
                }
            });

            // 4) 待清理集合: 本次配置里被删掉的统计字段。
            //    残留的旧值不清掉, 详情页会继续显示一个已经不存在的字段的值。
            Set<String> toPurge = new HashSet<>(originMap.keySet());
            toPurge.removeAll(currentMap.keySet());

            // 5) 两个集合都为空说明配置没动, 直接返回 —— 这是绝大多数保存的路径。
            if (toRefresh.isEmpty() && toPurge.isEmpty()) {
                return;
            }

            String hostFieldTable = hostDataTable + FIELD_TABLE_SUFFIX;

            // 6) 异步线程拿不到请求上下文, 但下游的表单配置读取依赖组织上下文, 这里显式设置并在 finally 恢复,
            //    否则线程池复用时会串组织。
            String originOrgId = OrganizationContext.getOrganizationId();
            OrganizationContext.setOrganizationId(orgId);
            try {
                // 7) 先清理被删字段的残留值, 再刷新, 避免被删字段刚清完又被写回。
                for (String fieldId : toPurge) {
                    try {
                        int purged = extStatisticMapper.deleteFieldValuesByFieldId(hostFieldTable, fieldId);
                        log.info("统计字段已删除, 清理历史值: formKey={}, fieldId={}, purged={}", formKey, fieldId, purged);
                    } catch (Exception e) {
                        log.error("统计字段历史值清理失败: formKey={}, fieldId={}", formKey, fieldId, e);
                    }
                }

                // 8) 逐个字段刷新。单个字段失败只记日志, 不影响其它字段。
                for (StatisticField field : toRefresh) {
                    try {
                        refreshFieldData(field, formKey, hostDataTable, orgId);
                    } catch (Exception e) {
                        log.error("统计字段刷新失败: formKey={}, fieldId={}", formKey, field.getId(), e);
                    }
                }
            } finally {
                OrganizationContext.setOrganizationId(originOrgId);
            }
        } catch (Exception e) {
            // @Async 方法的兜底: 刷新失败不能影响配置保存本身
            log.error("统计字段刷新异常: formKey={}, orgId={}", formKey, orgId, e);
        }
    }

    /**
     * 刷新单个统计字段在该表单存量数据上的全部取值 (保存配置时用)。
     *
     * <p>按页并行: 分页游标必须串行推进(下一页的起点依赖上一页的最后一条), 但「读一页ID」很便宜,
     * 「逐条聚合 + 写回」才是耗时大头, 所以游标在调用线程里顺序推进, 每读出一页就交给线程池,
     * 并保持最多 {@link #MAX_PARALLEL_PAGE} 页在跑。</p>
     *
     * <p>只用来算存量: 用户手动刷新走的是 {@link #refreshField}, 一次只算一条, 不从这里过,
     * 所以这里不再需要「手动刷新不受更新范围限制」那个开关。</p>
     */
    private void refreshFieldData(StatisticField field, String hostFormKey, String hostDataTable, String orgId) {
        String fieldId = field.getId();

        // 1) 更新范围决定要刷新哪些数据。NONE 表示「现有数据不计算」, 保存配置时跳过:
        //    用户新增字段时通常先选这个, 避免一次性全表重算, 该字段的新数据会在
        //    关联数据变更 / 新建记录时被算出来。
        StatisticUpdateScope updateScope = enumValue(StatisticUpdateScope.class, field.getUpdateScope());
        if (updateScope == StatisticUpdateScope.NONE) {
            log.info("统计字段更新范围为「不计算」, 跳过存量刷新: formKey={}, fieldId={}", hostFormKey, fieldId);
            return;
        }

        String hostFieldTable = hostDataTable + FIELD_TABLE_SUFFIX;
        String hostBlobTable = hostDataTable + BLOB_TABLE_SUFFIX;

        // 2) 更新范围条件就是宿主表单的高级搜索结构, 复用列表页同一套条件解析,
        //    保证「刷新时算的行」与「列表页看到的行」是同一个口径。
        List<FilterDBCondition> hostConditions = Collections.emptyList();
        String hostSearchMode = CombineSearch.SearchMode.AND.name();
        if (updateScope == StatisticUpdateScope.CONDITION) {
            CombineSearch hostCombineSearch = parseHostCondition(field.getUpdateScopeCondition(), hostFormKey);
            hostConditions = toDbConditions(hostCombineSearch.getConditions());
            hostSearchMode = hostCombineSearch.getSearchMode();
        }

        // 3) 目标表单的聚合上下文只构建一次: 里面的解析都与具体数据无关,
        //    放到每条数据里做会白白放大几倍开销。
        StatisticRefreshContext context = buildContext(field, hostDataTable);
        if (context == null) {
            // buildContext 已经打过日志说明原因
            return;
        }

        long start = System.currentTimeMillis();
        int total = 0;
        int failed = 0;

        // 4) 游标分页 + 有界并行。
        Deque<Future<RefreshResult>> inFlight = new ArrayDeque<>(MAX_PARALLEL_PAGE);
        String lastId = null;
        while (true) {
            List<String> dataIds = extStatisticMapper.selectDataIdsByCursor(
                    hostDataTable, hostFieldTable, hostBlobTable, orgId, lastId, PAGE_SIZE,
                    hostConditions, hostSearchMode);
            if (CollectionUtils.isEmpty(dataIds)) {
                break;
            }
            lastId = dataIds.get(dataIds.size() - 1);

            inFlight.addLast(CompletableFuture.supplyAsync(
                    () -> refreshPage(context, dataIds, orgId), statisticRefreshExecutor));

            // 维持并行窗口: 最早的先收获, 保证同时在跑的数据行数是有界的
            if (inFlight.size() >= MAX_PARALLEL_PAGE) {
                RefreshResult result = await(inFlight.removeFirst());
                total += result.success();
                failed += result.failed();
            }

            if (dataIds.size() < PAGE_SIZE) {
                break;
            }
        }
        while (!inFlight.isEmpty()) {
            RefreshResult result = await(inFlight.removeFirst());
            total += result.success();
            failed += result.failed();
        }

        // 5) 汇总日志: 刷新条数/失败条数, 便于排查「改了配置但值没变」这类问题
        log.info("统计字段刷新完成: formKey={}, fieldId={}, refreshed={}, failed={}, cost={}ms",
                hostFormKey, fieldId, total, failed, System.currentTimeMillis() - start);
    }

    /**
     * 刷新一页数据: 逐条重算并写回。
     *
     * <p>统计字段是派生数据, 单条算不出来不应该影响这一页的其它数据, 所以逐条 try/catch;
     * 写回走 {@link #writeStatisticValue}, 每条一个事务, 失败的那条不会把整页拖回滚。</p>
     */
    private RefreshResult refreshPage(StatisticRefreshContext context, List<String> dataIds, String orgId) {
        // 线程池线程没有请求上下文, 每条任务自己设置并恢复组织, 避免线程复用时串组织
        String originOrgId = OrganizationContext.getOrganizationId();
        OrganizationContext.setOrganizationId(orgId);
        int success = 0;
        int failed = 0;
        try {
            for (String dataId : dataIds) {
                try {
                    refreshOneRecord(context, dataId, orgId);
                    success++;
                } catch (Exception e) {
                    failed++;
                    log.error("统计字段单条刷新失败: fieldId={}, dataId={}", context.field.getId(), dataId, e);
                }
            }
        } finally {
            OrganizationContext.setOrganizationId(originOrgId);
        }
        return new RefreshResult(success, failed);
    }

    /**
     * 对单条宿主数据重算并写回。
     *
     * <p>四个入口里「算一条」的动作都是它, 口径只此一份 —— 分别实现迟早会出现
     * 「同一个字段在不同入口下算出不同的值」。</p>
     *
     * <p>组织上下文与异常处理由调用方负责: 批量刷新要按条容错, 单条入口要抛出去让用户看到失败。</p>
     *
     * @return 实际落库的值, 返回 null 表示按空值处理(值行被清掉, 前端显示「-」)
     */
    private BigDecimal refreshOneRecord(StatisticRefreshContext context, String dataId, String orgId) {
        BigDecimal value = extStatisticMapper.selectAggregate(
                context.targetTable, context.targetFieldTable, context.targetBlobTable,
                orgId, context.relatedFieldId, context.relatedBusinessKey,
                context.statisticFieldId, context.statisticBusinessKey,
                dataId, context.statisticType, context.avgSkipEmpty, Collections.emptyList(),
                CombineSearch.SearchMode.AND.name());
        // 走代理调用, 让 writeStatisticValue 上的 @Transactional 生效(直接 this 调用不走代理)
        return self.writeStatisticValue(context, dataId, value);
    }

    /**
     * 把聚合结果落到宿主的字段值表。
     *
     * <p>先删后插而不是 update: 字段值表没有 (resource_id, field_id) 唯一索引,
     * 直接 update 在历史脏数据下会更新不全, 先清再插天然幂等。</p>
     *
     * @param context 刷新上下文
     * @param dataId  宿主数据ID
     * @param value   聚合结果, null 表示没有命中数据
     *
     * @return 实际落库的值, 返回 null 表示按空值处理。返回值给手动刷新接口用:
     *         用户点一次刷新, 前端要立刻显示新值, 否则只能再查一遍详情, 白跑一趟。
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal writeStatisticValue(StatisticRefreshContext context, String dataId, BigDecimal value) {
        String hostFieldTable = context.hostDataTable + FIELD_TABLE_SUFFIX;
        // 空结果的判定分两种:
        // - SUM/AVG 没有任何命中数据时 SQL 返回 null(不是 0), 所以 value == null 就是空结果;
        //   注意不能把 0 也当成空结果 —— 一批金额恰好对冲为 0 的记录, 展示成「-」是错的。
        // - COUNT 没有命中数据时返回的是 0, 这里 0 就是空结果。
        boolean emptyResult = value == null || (isCount(context) && value.signum() == 0);
        if (emptyResult) {
            if (!StatisticEmptyResultMode.ZERO.name().equalsIgnoreCase(context.emptyResultMode)) {
                // 存空 == 不写值行, 详情页显示为「-」
                extStatisticMapper.deleteFieldValue(hostFieldTable, context.field.getId(), dataId);
                return null;
            }
            value = BigDecimal.ZERO;
        }
        extStatisticMapper.deleteFieldValue(hostFieldTable, context.field.getId(), dataId);
        // 数值统一按普通字符串入库, 展示格式化交给 StatisticResolver
        extStatisticMapper.insertFieldValue(hostFieldTable, IDGenerator.nextStr(), dataId,
                context.field.getId(), value.stripTrailingZeros().toPlainString());
        return value;
    }

    /**
     * 是否是计数统计。
     */
    private boolean isCount(StatisticRefreshContext context) {
        return StatisticType.COUNT.name().equalsIgnoreCase(context.statisticType);
    }

    /**
     * 目标表单的关联数据新增或变更后, 自动刷新被关联记录的统计字段。
     *
     * <p><b>已知边界</b>: 只拿得到变更「之后」的数据, 拿不到变更前的关联值。所以关联字段从 A 改成 B 时,
     * 只有 B 这条宿主记录会被重算, A 那条会一直停在旧值上, 直到 A 自己再被编辑一次或有人手动刷新。
     * 想在改关联关系时同时兜住 A, 需要在字段值被覆盖「之前」把旧值读出来一并传进来;
     * 退而求其次改成整张宿主表单重算的代价与「保存表单配置」相同, 放在这条高频路径上不可接受。</p>
     *
     * @param targetFormKey 发生变更的数据所属表单Key
     * @param targetDataId  发生变更的数据ID
     * @param orgId         组织ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshByRelatedDataChange(String targetFormKey, String targetDataId, String orgId) {
        // 1) 反查: 统计目标就是 targetFormKey 的统计字段, 连同它们的宿主表单一起取出来。
        //    字段表就是配置的镜像(保存配置时整表重建), 所以拿到的一定是当前的字段定义,
        //    不需要再拿什么缓存去校正, 也不会出现「按旧口径算」或「往已删字段写值」。
        List<StatisticFieldRef> refs = findStatisticFieldsByTargetForm(targetFormKey, orgId);

        // 2) 一个字段都没引用这个表单时直接返回 —— 这是最常见的路径(绝大多数表单不是统计目标),
        //    必须足够便宜, 否则会拖慢每一次数据新增。
        if (refs.isEmpty()) {
            return;
        }

        String targetDataTable = FORM_KEY_TABLE.get(targetFormKey);
        String originOrgId = OrganizationContext.getOrganizationId();
        OrganizationContext.setOrganizationId(orgId);
        try {
            // 3) 目标表单的字段配置只取一次(走缓存): 用来判断关联字段是不是大字段, 决定从哪张表读值。
            Map<String, BaseField> targetFields = toFieldMap(moduleFormCacheService.getConfig(targetFormKey, orgId));

            for (StatisticFieldRef ref : refs) {
                try {
                    // 4) 读关联字段的值, 它就是被这条数据关联上的宿主记录ID;
                    //    关联字段为空说明这条数据没有关联任何记录, 不需要刷新任何宿主记录。
                    String hostDataId = readRelatedValue(targetDataTable, targetDataId,
                            targetFields.get(ref.field().getRelatedFieldId()));
                    if (StringUtils.isBlank(hostDataId)) {
                        continue;
                    }

                    StatisticRefreshContext context = buildContext(ref.field(), ref.hostDataTable());
                    if (context == null) {
                        // buildContext 已经打过日志说明原因
                        continue;
                    }

                    // 5) 只重算被关联的那一条宿主记录, 不要退化成整张宿主表单重算 ——
                    //    四个入口共用 refreshOneRecord, 口径与批量刷新完全一致。
                    refreshOneRecord(context, hostDataId, orgId);
                } catch (Exception e) {
                    // 6) 统计字段是派生数据, 单个字段算不出来不能影响关联数据本身的保存
                    log.error("关联数据变更后刷新统计字段失败: targetFormKey={}, hostFormKey={}, fieldId={}, dataId={}",
                            targetFormKey, ref.hostFormKey(), ref.field().getId(), targetDataId, e);
                }
            }
        } finally {
            OrganizationContext.setOrganizationId(originOrgId);
        }
    }

    /**
     * 刷新单条数据上的全部统计字段。
     *
     * <p>各模块新增资源后调用, 与 {@link #refreshField} 的区别是这里一次性刷新该记录上的多个统计字段,
     * 而不是由用户指定某一个。</p>
     *
     * <p><b>调用方</b>(均为各模块 Service 的 {@code add} 方法, 在自身字段值与主记录落库之后):</p>
     * <ul>
     *   <li>{@code ClueService#add} / {@code CustomerService#add} / {@code CustomerContactService#add}</li>
     *   <li>{@code OpportunityService#add} / {@code OpportunityQuotationService#add}</li>
     *   <li>{@code ContractService#add} / {@code ContractInvoiceService#add}
     *       / {@code ContractPaymentPlanService#add} / {@code ContractPaymentRecordService#add}</li>
     *   <li>{@code OrderService#add} / {@code ProductService#add} / {@code ProductPriceService#add}</li>
     *   <li>{@code FollowUpPlanService#add} / {@code FollowUpRecordService#add}</li>
     * </ul>
     * <p>自定义表单的 {@code CustomFormDataService#add} 不在其中: 自定义表单暂不支持统计字段。</p>
     *
     * @param formKey 表单Key
     * @param dataId  新增数据的ID
     * @param orgId   组织ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshDataStatisticFields(String formKey, String dataId, String orgId) {
        // 1) formKey 反查物理表, 非标准模块表单直接返回 —— 这一条同时兜住了
        //    「统计字段暂不支持自定义表单」, 调用方不需要自己判。
        String hostDataTable = FORM_KEY_TABLE.get(formKey);
        if (hostDataTable == null) {
            return;
        }

        // 2) 3) 取该表单的字段配置(走缓存)筛出统计字段, 一个都没有就直接返回 ——
        //    这是绝大多数表单的路径, 必须足够便宜, 否则每个模块的每次新增都要多花一次配置查询。
        List<StatisticField> fields = getStatisticFields(formKey, orgId);
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }

        // 5) 不做数据权限校验: 这条数据是当前用户刚创建的; 存量批量刷新走的是系统身份(配置保存入口),
        //    手动刷新走 refreshField 的记录级校验, 三者边界要分清, 不要在这里重复校验(会拦掉合法的系统调用)。
        String originOrgId = OrganizationContext.getOrganizationId();
        OrganizationContext.setOrganizationId(orgId);
        try {
            for (StatisticField field : fields) {
                try {
                    StatisticRefreshContext context = buildContext(field, hostDataTable);
                    if (context == null) {
                        // buildContext 已经打过日志说明原因
                        continue;
                    }
                    // 4) 复用 refreshOneRecord, 不在这里另写一份聚合逻辑, 四个入口必须共用同一份计算口径。
                    //    6) 新建记录时统计目标通常为空(还没有目标数据指向本记录), 这一步的价值在于按
                    //    emptyResultMode 把空值落成 0 或空, 把字段值行先建出来, 避免详情页一直显示「未计算」。
                    refreshOneRecord(context, dataId, orgId);
                } catch (Exception e) {
                    // 7) 统计字段属于派生数据, 不允许因为算不出来而把「新建资源」这个主流程回滚掉
                    log.error("新增数据后刷新统计字段失败: formKey={}, fieldId={}, dataId={}",
                            formKey, field.getId(), dataId, e);
                }
            }
        } finally {
            OrganizationContext.setOrganizationId(originOrgId);
        }
    }

    /**
     * 手动刷新某条数据上的某个统计字段 (详情页/编辑页的刷新按钮)。
     *
     * <p>接口为 {@code POST /field/statistic/refresh/{resourceId}/{fieldId}}。刷新的是「一条数据的一个统计值」:
     * 统计字段按宿主记录各算各的, 用户点刷新时看的就是当前这一条, 没有理由连带把整列重算一遍 ——
     * 存量数据的整体重算是保存配置时的事({@link #refreshOnConfigSave}), 两者不要混在一起。</p>
     *
     * <p>也因此这里不受「更新范围」的限制: 更新范围描述的是保存配置时存量数据算哪些,
     * 而用户在这一条记录上点刷新, 就是明确要求算这一条。</p>
     *
     * @param fieldId    统计字段ID, 即 {@code sys_module_field.id}
     * @param resourceId 宿主表单的资源ID, 即要重算的那条记录
     * @param userId     用户ID
     * @param orgId      组织ID
     *
     * @return 重算后落库的值, 返回 null 表示按空值处理(值行被清掉, 前端显示「-」)。
     *         回值给前端是为了免掉「刷新完再查一遍详情」, 不是让调用方拿它做判断。
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal refreshField(String fieldId, String resourceId, String userId, String orgId) {
        // 1) 登录态: 服务层不假设调用方一定来自 web 请求(定时补数/内部调用也会进来), 网关那层之外再兜一层。
        if (StringUtils.isBlank(userId)) {
            log.warn("统计字段手动刷新缺少用户信息: fieldId={}, orgId={}", fieldId, orgId);
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        // 2) 资源ID必须显式给出来。不能省成「按字段刷整列」: 那等于让任何一个有表单读权限的用户
        //    触发一次全组织范围的重算, 而且他看到的只是自己那条记录的值有没有变。
        if (StringUtils.isBlank(resourceId)) {
            log.warn("统计字段手动刷新缺少资源ID: fieldId={}, userId={}, orgId={}", fieldId, userId, orgId);
            throw new GenericException("资源ID不能为空");
        }

        // 3) 由字段ID反查字段配置与宿主表单。fieldId 是字段配置ID而不是字段值行ID:
        //    用户刷新的是「某个统计字段」在「某条数据」上的值, 由这两个参数共同定位。
        ModuleField moduleField = moduleFieldMapper.selectByPrimaryKey(fieldId);
        if (moduleField == null || !FieldType.STATISTIC.name().equals(moduleField.getType())) {
            log.warn("统计字段手动刷新失败, 字段不存在或类型不匹配: fieldId={}, userId={}, orgId={}", fieldId, userId, orgId);
            throw new GenericException("统计字段不存在");
        }
        ModuleForm moduleForm = moduleFormMapper.selectByPrimaryKey(moduleField.getFormId());

        // 4) 组织隔离: 字段配置必须属于当前组织, 否则换个组织传同一个 fieldId 就能刷到别人的数据。
        //    这是最关键的一层; 校验失败要留痕, 这是越权探测的痕迹。
        if (moduleForm == null || !StringUtils.equals(moduleForm.getOrganizationId(), orgId)) {
            log.warn("统计字段手动刷新越权: fieldId={}, userId={}, orgId={}", fieldId, userId, orgId);
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        String formKey = moduleForm.getFormKey();
        String hostDataTable = FORM_KEY_TABLE.get(formKey);
        if (hostDataTable == null) {
            // 自定义表单暂不支持统计字段, 理论上不会走到这里
            throw new GenericException("自定义表单暂不支持统计字段");
        }

        // 5) 权限: 不通过要抛异常而不是静默 return, 否则用户点了刷新没有任何反馈。
        //    校验的是「这条数据」而不是「这张表单」, 原因见 checkDataPermission。
        checkDataPermission(formKey, resourceId, userId, orgId);

        // 6) 字段配置走缓存, 与其它三个入口读的是同一份, 避免手动刷新用的配置和自动刷新不一致。
        StatisticField field = getStatisticFields(formKey, orgId).stream()
                .filter(item -> StringUtils.equals(fieldId, item.getId()))
                .findFirst()
                .orElse(null);
        if (field == null) {
            log.warn("统计字段手动刷新失败, 表单配置里没有该字段: fieldId={}, formKey={}, orgId={}", fieldId, formKey, orgId);
            throw new GenericException("统计字段配置不存在");
        }

        // 7) 只重算这一条。复用 refreshOneRecord 而不是另写一份聚合, 四个入口必须共用同一份计算口径。
        StatisticRefreshContext context = buildContext(field, hostDataTable);
        if (context == null) {
            // buildContext 已经打过日志说明原因
            return null;
        }
        return refreshOneRecord(context, resourceId, orgId);
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /**
     * 从字段配置里筛出统计字段, 按字段ID建索引。
     */
    private Map<String, StatisticField> indexStatisticFields(List<BaseField> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return Collections.emptyMap();
        }
        return fields.stream()
                .filter(StatisticField.class::isInstance)
                .map(StatisticField.class::cast)
                .collect(Collectors.toMap(BaseField::getId, Function.identity(), (prev, next) -> next));
    }

    /**
     * 取某个表单上配置的全部统计字段。
     *
     * <p>走 {@code ModuleFormCacheService} 的缓存配置, 不直连库: 这个调用在「新增数据」这条高频路径上,
     * 而且它同时是「有没有统计字段」的唯一判据。</p>
     *
     * @return 统计字段列表, 没有时返回空集合
     */
    private List<StatisticField> getStatisticFields(String formKey, String orgId) {
        ModuleFormConfigDTO config = moduleFormCacheService.getConfig(formKey, orgId);
        if (config == null || CollectionUtils.isEmpty(config.getFields())) {
            return Collections.emptyList();
        }
        return config.getFields().stream()
                .filter(StatisticField.class::isInstance)
                .map(StatisticField.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * 找出所有「统计目标就是 targetFormKey」的统计字段, 连同它们的宿主表单。
     *
     * <p>统计字段只在宿主表单侧单向记录了「我统计谁」, 目标表单侧没有任何反向索引, 所以只能反过来找。</p>
     *
     * <p><b>为什么直接查字段表</b>: 反过来找的候选集合是「该组织的全部统计字段」, 这个集合很小
     * (每个都是设计器里手工配的), 一条按类型过滤的索引查询就能拿全, 而且拿到的一定是当前配置 ——
     * 表单配置保存时会把该表单的 {@code sys_module_field} 整表删掉重建, 字段表就是配置的镜像。</p>
     *
     * <p><b>为什么不用表单配置缓存</b>: 那条路要遍历十几个表单, 每个表单读一整份配置(Redis 往返 +
     * 反序列化全部字段), 而 {@link #refreshByRelatedDataChange} 挂在每条数据的新增/编辑路径上,
     * 这份开销会原样加到用户每次保存的延迟上。而它只是想问一句「有没有人统计我」,
     * 绝大多数时候答案是「没有」。</p>
     */
    private List<StatisticFieldRef> findStatisticFieldsByTargetForm(String targetFormKey, String orgId) {
        List<StatisticFieldSourceDTO> sources = extStatisticMapper.selectStatisticFields(
                orgId, FieldType.STATISTIC.name(), FORM_KEY_TABLE.keySet());
        if (CollectionUtils.isEmpty(sources)) {
            return Collections.emptyList();
        }
        List<StatisticFieldRef> refs = new ArrayList<>();
        for (StatisticFieldSourceDTO source : sources) {
            // 宿主表单的物理表名: 标准模块表单才有, 表里没有的即自定义表单
            String hostDataTable = FORM_KEY_TABLE.get(source.getHostFormKey());
            if (hostDataTable == null) {
                continue;
            }
            BaseField baseField = JSON.parseObject(source.getProp(), BaseField.class);
            if (!(baseField instanceof StatisticField field)) {
                // 字段类型是统计字段但属性解析不出统计字段: 属性被改坏了, 记下来否则会静默不刷新
                log.warn("统计字段属性无法解析, 跳过: fieldId={}, hostFormKey={}",
                        source.getFieldId(), source.getHostFormKey());
                continue;
            }
            // 「统计谁」只写在字段属性里, 只能逐条比对; 命中率极低, 不影响这条查询本身的开销
            if (StringUtils.equals(targetFormKey, field.getTargetFormId())) {
                refs.add(new StatisticFieldRef(source.getHostFormKey(), hostDataTable, field));
            }
        }
        return refs;
    }

    /**
     * 读目标表单某条数据上关联字段的值, 即被它关联上的宿主记录ID。
     *
     * <p>关联字段可能是业务字段(例如订单上的「合同」), 也可能是用户自建的数据源字段,
     * 前者读数据表自己的列, 后者读字段值表。</p>
     *
     * @return 关联的宿主记录ID, 没有关联或读不到时返回 null
     */
    private String readRelatedValue(String targetDataTable, String targetDataId, BaseField relatedField) {
        if (relatedField == null) {
            // 保存配置时校验过关联字段存在, 这里是兜底: 目标表单后来把字段删了
            log.warn("统计字段的关联字段已不存在, 跳过刷新: dataId={}", targetDataId);
            return null;
        }
        String businessKey = resolveBusinessKey(relatedField);
        if (businessKey != null) {
            return extStatisticMapper.selectBusinessFieldValue(targetDataTable, businessKey, targetDataId);
        }
        if (relatedField.hasBusinessKey()) {
            // internalKey 认不出来但属性里标了 businessKey: 这是内置的合成字段(部门、阶段这类),
            // 没有登记进 BusinessModuleField, 也就没有可靠的主表列名, 不猜。
            log.error("统计字段的关联字段是业务字段但取不到主表列名, 跳过刷新: fieldId={}", relatedField.getId());
            return null;
        }
        return extStatisticMapper.selectFieldValue(
                targetDataTable + FIELD_TABLE_SUFFIX, targetDataTable + BLOB_TABLE_SUFFIX,
                relatedField.getId(), targetDataId, relatedField.isBlob());
    }

    /**
     * 按表单Key把字段配置按字段ID建索引, 用于按ID取某个字段的属性。
     */
    private Map<String, BaseField> toFieldMap(ModuleFormConfigDTO config) {
        if (config == null || CollectionUtils.isEmpty(config.getFields())) {
            return Collections.emptyMap();
        }
        return config.getFields().stream()
                .filter(field -> StringUtils.isNotBlank(field.getId()))
                .collect(Collectors.toMap(BaseField::getId, Function.identity(), (prev, next) -> next));
    }

    /**
     * 校验用户能不能刷新「这一条数据」上的统计字段。
     *
     * <p>为什么不能只校验表单级读权限: 手动刷新是要落库的 —— 它会在宿主的字段值表里删掉旧值再插一条新值。
     * 只验表单权限的话, 一个只有「本人数据」数据范围的销售, 拿到别人的记录ID就能刷别人的统计值,
     * 顺带从返回结果里把那个值读出来。所以这里必须落到记录级。</p>
     *
     * <p>用 {@link ResourcePermissionService#checkResourcePermission} 而不是自己拼权限位与数据范围:
     * 那个方法一次性覆盖了「角色权限位 + 数据范围 + 审批状态 + 资源存在且属于本组织」四件事,
     * 各模块的资源读取接口走的都是它, 手动刷新跟着走才能保证「列表里看得到就能刷新」。
     * 自己拼一份的话, 漏掉审批状态这类规则时不会有任何编译期报错。</p>
     *
     * <p>一件事只能传一个权限码, 而有的表单登记了多个可读权限(比如跟进记录就分了全部/本人等),
     * 用户只要拿到其中任意一个就算通过, 所以这里逐个试。</p>
     *
     * @param formKey    宿主表单Key
     * @param resourceId 宿主资源ID
     * @param userId     用户ID
     * @param orgId      组织ID
     */
    private void checkDataPermission(String formKey, String resourceId, String userId, String orgId) {
        List<String> permissions = FORM_KEY_PERMISSIONS.get(formKey);
        if (CollectionUtils.isEmpty(permissions)) {
            // 没有登记权限点的表单不拦, 避免新增表单时这里静默变成「谁都刷不了」
            log.warn("表单未登记读权限点, 手动刷新只做登录态校验: formKey={}, userId={}", formKey, userId);
            return;
        }
        for (String permission : permissions) {
            try {
                resourcePermissionService.checkResourcePermission(permission, resourceId, formKey, userId, orgId);
                return;
            } catch (GenericException e) {
                // 这个权限点不通过就试下一个(多个权限点是「或」的关系)。
                // 但只吞「不通过」这一种: 别的错误码意味着校验本身没跑通(资源提供者缺失之类),
                // 那不是用户没权限, 当成不通过会把人引到错误的排查方向上。
                if (!CrmHttpResultCode.FORBIDDEN.equals(e.getErrorCode())) {
                    throw e;
                }
            }
        }
        log.warn("统计字段手动刷新越权: formKey={}, resourceId={}, userId={}, orgId={}, 已试权限点={}",
                formKey, resourceId, userId, orgId, permissions);
        throw new GenericException(CrmHttpResultCode.FORBIDDEN);
    }

    /**
     * 配置指纹: 只包含「会影响统计结果」的属性。
     *
     * <p>字段标题、描述、字段权限、移动端、字段宽度这些纯展示属性不参与比较,
     * 改个标题不应该触发全表重算。</p>
     *
     * <p>{@code updateScope / updateScopeCondition} 必须参与: 用户把更新范围从「不计算」改成「全部计算」,
     * 这个动作本身就是在要求刷新, 排除掉会出现「改了更新范围却什么都没发生」。</p>
     */
    private String configFingerprint(StatisticField field) {
        return String.join("|",
                StringUtils.defaultString(field.getTargetFormId()),
                StringUtils.defaultString(field.getRelatedFieldId()),
                StringUtils.defaultString(field.getStatisticType()),
                StringUtils.defaultString(field.getStatisticFieldId()),
                StringUtils.defaultString(field.getAvgEmptyValueMode()),
                StringUtils.defaultString(field.getEmptyResultMode()),
                StringUtils.defaultString(field.getDataScope()),
                StringUtils.defaultString(JSON.toJSONString(field.getCombineSearch())),
                StringUtils.defaultString(field.getUpdateScope()),
                StringUtils.defaultString(JSON.toJSONString(field.getUpdateScopeCondition())));
    }

    /**
     * 构建一个统计字段的刷新上下文。
     *
     * @return 上下文; 该字段当前无法刷新时返回 null (已记日志)
     */
    private StatisticRefreshContext buildContext(StatisticField field, String hostDataTable) {
        String targetFormKey = field.getTargetFormId();
        String targetTable = FORM_KEY_TABLE.get(targetFormKey);
        if (targetTable == null) {
            // 自定义表单暂时不能作为统计目标, 保存时已经拦过, 这里是兜底
            throw new GenericException("统计字段的目标表单不支持: " + targetFormKey);
        }

        if (enumValue(StatisticDataScope.class, field.getDataScope()) == StatisticDataScope.CONDITION) {
            // 统计范围条件用的是数据源字段的字段对字段比较结构
            // ({matchType, leftFieldId, leftFieldType, operator, rightFieldId, rightFieldCustom, rightFieldCustomValue}),
            // 其中 leftFieldType 可能是 DATA_SOURCE(显示字段, 要走 refFieldConditionJoin),
            // matchType 的取值语义后端也没有对应实现 —— 后端目前完全没有这套结构的 SQL,
            // 只有前端用它过滤数据源候选项。硬凑一个过滤会算出偏大的值且没人能发现, 所以这里不做刷新。
            log.error("统计范围暂不支持「符合条件」, 跳过该字段刷新: formKey={}, fieldId={}",
                    targetFormKey, field.getId());
            return null;
        }

        StatisticRefreshContext context = new StatisticRefreshContext();
        context.field = field;
        context.hostDataTable = hostDataTable;
        context.targetTable = targetTable;
        context.targetFieldTable = targetTable + FIELD_TABLE_SUFFIX;
        context.targetBlobTable = targetTable + BLOB_TABLE_SUFFIX;
        context.relatedFieldId = field.getRelatedFieldId();
        // 关联字段也有两种存法, 与下面被统计字段的判断完全对称:
        // 数据源字段如果是标准模块上的「客户」「合同」这类业务字段, 关联关系就落在主表列上
        // (商机的「客户」是 customer_id), 字段值表里没有它的行 —— 拿字段值表去 join 恒关联不到数据。
        // 认出来就能整条语句都不碰字段值表, 认不出来(用户自建的数据源字段)才回落到字段值表。
        context.relatedBusinessKey = resolveBusinessKey(field.getRelatedFieldId());
        context.statisticFieldId = field.isCount() ? null : field.getStatisticFieldId();
        context.statisticType = StringUtils.defaultIfBlank(field.getStatisticType(), StatisticType.SUM.name());
        // AVG 遇到空值: SKIP 不计入分母(等于 SQL 的 avg 默认行为), DEFAULT_ZERO 则当 0 计入
        context.avgSkipEmpty = enumValue(StatisticEmptyValueMode.class, field.getAvgEmptyValueMode())
                == StatisticEmptyValueMode.SKIP;
        context.emptyResultMode = field.getEmptyResultMode();
        if (!field.isCount()) {
            // 被统计字段的值也有两种存法, 取值的 SQL 也不一样:
            // 业务字段(商机金额、合同金额这类定义在主表上的标准字段)的值在目标表单主表的列上,
            // 字段值表里压根没有它的行; 自定义字段的值才在字段值表里。
            // 不区分的话, 统计业务字段会被当成自定义字段去 join 一个不存在的值行, SUM/AVG 恒为空。
            context.statisticBusinessKey = resolveBusinessKey(field.getStatisticFieldId());
        }
        return context;
    }

    /**
     * 查一个字段ID对应的业务字段主表列名。
     *
     * <p>直接查字段表的 {@code internal_key} 再走 {@link BusinessModuleField} 的闭集映射,
     * 而不是读目标表单的配置: 配置里没带这个信息(下发配置时才由
     * {@code ModuleFormService#setFieldBusinessParam} 补上), 而且这里每次刷新只问一次,
     * 用主键定位一行比重建整份配置便宜得多。</p>
     *
     * @return 主表列名; 字段不存在、没有 internalKey 或不是业务字段时返回 null(按自定义字段处理)
     */
    private String resolveBusinessKey(String fieldId) {
        if (StringUtils.isBlank(fieldId)) {
            return null;
        }
        ModuleField moduleField = moduleFieldMapper.selectByPrimaryKey(fieldId);
        if (moduleField == null) {
            // 字段可能刚被删掉: 返回 null 会退化去查字段值表, 查不到值, 结果按空值规则落库
            log.warn("字段不存在, 按自定义字段处理: fieldId={}", fieldId);
            return null;
        }
        return businessKeyOf(moduleField.getInternalKey(), fieldId);
    }

    /**
     * 从字段配置里直接取业务字段的主表列名。
     *
     * <p>字段配置已经带着 {@code internalKey}(由 {@code ModuleFormService#getAllFields} 从字段表补齐),
     * 所以这条路径不用再查一次库。</p>
     */
    private String resolveBusinessKey(BaseField field) {
        return businessKeyOf(field.getInternalKey(), field.getId());
    }

    /**
     * 把字段的 {@code internalKey} 映射成业务字段的主表列名。
     *
     * <p>枚举里写的是驼峰({@code planAmount}), 主表列是下划线({@code plan_amount}), 必须过一次
     * {@link CaseFormatUtils#camelToUnderscore} —— 直接用枚举值会拼出 {@code t.`planAmount`} 这样的语句,
     * 单测拿 {@code amount} 这种单单词列名跑是发现不了的, 要到「计划回款金额」「回款金额」上才会报
     * Unknown column。{@code ConditionFilterUtils} 解析筛选条件时走的也是同一个转换。</p>
     *
     * <p>列名会被拼进 SQL 的列名位置, 虽然 {@link BusinessModuleField} 是闭集, 这里仍然卡一道字符集,
     * 避免以后有人把外部输入接到这个参数上。</p>
     *
     * @param internalKey 字段表的 internal_key
     * @param fieldId     字段ID, 仅用于日志
     *
     * @return 主表列名; 不是业务字段或列名不合法时返回 null(按自定义字段处理)
     */
    private String businessKeyOf(String internalKey, String fieldId) {
        BusinessModuleField businessField = BusinessModuleField.ofKey(internalKey);
        if (businessField == null) {
            return null;
        }
        String column = CaseFormatUtils.camelToUnderscore(businessField.getBusinessKey());
        if (!COLUMN_NAME_PATTERN.matcher(column).matches()) {
            log.error("业务字段的列名不合法, 按自定义字段处理: fieldId={}, column={}", fieldId, column);
            return null;
        }
        return column;
    }

    /**
     * 解析宿主表单的更新范围条件。
     *
     * <p>结构就是该表单高级搜索的 {@code CombineSearch}, 直接复用列表页同一套解析。</p>
     *
     * @return 解析并补全后的 CombineSearch; 条件为空时返回一个空的 CombineSearch (等同于不过滤)
     */
    private CombineSearch parseHostCondition(Map<String, Object> rawCondition, String hostFormKey) {
        if (rawCondition == null || rawCondition.isEmpty()) {
            // 保存时有校验兜底(updateScope = CONDITION 必须有条件), 但历史数据或直接改库可能破坏这个前提
            log.warn("统计字段更新范围声明为「符合条件」却没有任何条件, 按不过滤处理: formKey={}", hostFormKey);
            return new CombineSearch();
        }
        CombineSearch combineSearch = JSON.parseObject(JSON.toJSONString(rawCondition), CombineSearch.class);
        if (combineSearch == null) {
            return new CombineSearch();
        }
        BaseCondition baseCondition = new BaseCondition();
        baseCondition.setCombineSearch(combineSearch);
        // parseCondition 内部会按 formKey 自己取表单配置(依赖组织上下文, 调用方已经设置),
        // 把 FilterCondition 补全成 FilterDBCondition, 并处理 DYNAMICS 这类相对时间条件 ——
        // 不自己实现一遍才不会和列表页产生口径差。
        ConditionFilterUtils.parseCondition(baseCondition, hostFormKey);
        return combineSearch;
    }

    /**
     * 把条件规整成可以直接拼 SQL 的形式。
     *
     * <p>{@code ConditionFilterUtils.parseCondition} 产出的已经是 {@link FilterDBCondition},
     * 这里只做两件事: 业务字段的名称转成物理列名, 以及拦掉没有通用实现的虚拟字段。</p>
     *
     * <p>业务字段的名称来自 {@code BaseField#idOrBusinessKey()}, 是驼峰形式(如 {@code customerId}),
     * 而各模块的物理列是下划线形式(如 {@code customer_id}), 需要转换; 自定义字段的名称就是字段ID,
     * 直接当成 field_id 使用, 不能转换。</p>
     */
    private List<FilterDBCondition> toDbConditions(List<FilterCondition> conditions) {
        if (CollectionUtils.isEmpty(conditions)) {
            return Collections.emptyList();
        }
        List<FilterDBCondition> dbConditions = new ArrayList<>(conditions.size());
        for (FilterCondition condition : conditions) {
            FilterDBCondition dbCondition = condition instanceof FilterDBCondition converted
                    ? converted
                    : JSON.parseObject(JSON.toJSONString(condition), FilterDBCondition.class);
            if (!Boolean.TRUE.equals(dbCondition.getCustomField()) && !Boolean.TRUE.equals(dbCondition.getRefFiled())) {
                String column = CaseFormatUtils.camelToUnderscore(dbCondition.getName());
                if (VIRTUAL_DEPARTMENT_COLUMN.equals(column)) {
                    // 抛出去由调用方按字段粒度 catch: 宁可这个字段不刷新,
                    // 也不要按一个错的筛选范围算出一个"看起来对"的值。
                    throw new GenericException("统计字段的更新范围不支持按部门筛选: " + dbCondition.getName());
                }
                dbCondition.setName(column);
            }
            dbConditions.add(dbCondition);
        }
        return dbConditions;
    }

    /**
     * 等待一个分页任务结束, 异常不外抛, 只记日志。
     */
    private RefreshResult await(Future<RefreshResult> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RefreshResult(0, 0);
        } catch (Exception e) {
            log.error("统计字段分页刷新失败", e);
            return new RefreshResult(0, 0);
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumClass, String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 一页的刷新结果。
     */
    private record RefreshResult(int success, int failed) {
    }

    /**
     * 一个统计字段 + 它所在的宿主表单。
     *
     * <p>「统计目标是谁」写在统计字段自己的配置里, 但刷新时要算的列在宿主表单上,
     * 所以反查出来时必须把宿主表单一起带上, 否则拿到字段也不知道该刷哪张表。</p>
     *
     * @param hostFormKey   宿主表单Key
     * @param hostDataTable 宿主表单数据表名
     * @param field         统计字段配置
     */
    private record StatisticFieldRef(String hostFormKey, String hostDataTable, StatisticField field) {
    }

    /**
     * 单个统计字段的刷新上下文, 构建一次后在一批数据上复用。
     */
    static final class StatisticRefreshContext {

        private StatisticField field;
        private String hostDataTable;
        private String targetTable;
        private String targetFieldTable;
        private String targetBlobTable;
        private String relatedFieldId;
        /** 关联字段是业务字段时, 它在目标表单主表上的列名; 是自定义字段时为 null。为 null 时 SQL 才去 join 字段值表。 */
        private String relatedBusinessKey;
        /**
         * 被统计字段ID(数值/计算/统计字段), COUNT 时为 null。
         *
         * <p>只用于 join 字段值表, 取不取值由 {@link #statisticBusinessKey} 决定。</p>
         */
        private String statisticFieldId;
        /**
         * 被统计字段是业务字段时, 它在目标表单主表上的列名; 不是业务字段时为 null。
         *
         * <p>业务字段的值在主表的列上, 字段值表里没有它的行, 所以有列名时就不能再去 join 字段值表 ——
         * 两个字段在 SQL 里是互斥的两条分支。</p>
         */
        private String statisticBusinessKey;
        private String statisticType;
        private boolean avgSkipEmpty;
        private String emptyResultMode;
    }
}
