package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.statistic.StatisticAggregateRequest;
import cn.cordys.common.statistic.StatisticConditionConverter;
import cn.cordys.common.statistic.StatisticCursorRequest;
import cn.cordys.common.statistic.StatisticSqlMapper;
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
import java.util.HashMap;
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
 * <p><b>已知边界</b>: 关联字段值发生变更时, 变更前指向的那条宿主记录不会被自动重算,
 * 原因见 {@link #refreshByRelatedDataChange}; 「统计范围」为「符合条件」时右值取不到的条件会被整条丢掉,
 * 口径见 {@link #resolveScopeConditions}。</p>
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
     * 统计范围条件的右值取自宿主记录时, {@code value} 里用来记引用字段ID的键名。
     *
     * <p>{@link FilterCondition} 没有地方放「值来自哪个字段」, 而宿主记录的值在保存配置时还不存在,
     * 配置里只能先存一个引用对象, 刷新时再按每条数据把值换进去。详见 {@link #scopeCondition}。</p>
     */
    private static final String REF_FIELD_ID = "refFieldId";

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
    private StatisticSqlMapperRegistry statisticSqlMapperRegistry;

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

        // 2) 更新范围条件描述的是宿主表单自己的数据, 与列表页高级搜索是同一个口径:
        //    先由 parseHostCondition 把配置里的弹窗结构转过来, 再复用列表页同一套条件解析,
        //    保证「刷新时算的行」与「列表页看到的行」对得上。
        //    非「符合条件」时给一个空的 CombineSearch 而不是 null: 条件片段里有 `${conditions}.size() > 0`
        //    这样的判断, 传 null 会让 OGNL 直接抛, 而不是短路成「不过滤」。
        CombineSearch hostCondition = new CombineSearch();
        if (updateScope == StatisticUpdateScope.CONDITION) {
            hostCondition = parseHostCondition(field.getUpdateScopeCondition(), hostFormKey);
        }

        // 2.1) 宿主侧的取数语句由宿主表单自己的 Mapper 提供: 条件里的名字是宿主表单上的字段,
        //      哪个名字对应哪个列、要不要 join 部门表, 只有那张表单的 Mapper 知道。
        StatisticSqlMapper hostMapper = statisticSqlMapperRegistry.get(hostFormKey);

        // 3) 目标表单的聚合上下文只构建一次: 里面的解析都与具体数据无关,
        //    放到每条数据里做会白白放大几倍开销。
        //    构建不出来时 buildContext 直接抛, 由调用方的按字段 try/catch 兜住 —— 比返回 null 让这里
        //    静默 return 好: 用户手动刷新时能看到原因, 而不是「点了没反应」。
        StatisticRefreshContext context = buildContext(field, hostDataTable);

        long start = System.currentTimeMillis();
        int total = 0;
        int failed = 0;

        // 4) 游标分页 + 有界并行。
        Deque<Future<RefreshResult>> inFlight = new ArrayDeque<>(MAX_PARALLEL_PAGE);
        String lastId = null;
        while (true) {
            // 每页一份请求对象: 游标位置是这一页独有的, 逐页新建比复用同一个实例再改字段更难写错
            StatisticCursorRequest cursorRequest = new StatisticCursorRequest();
            cursorRequest.setOrgId(orgId);
            cursorRequest.setLastId(lastId);
            cursorRequest.setLimit(PAGE_SIZE);
            cursorRequest.setScopeCondition(hostCondition);
            List<String> dataIds = hostMapper.selectStatisticHostDataIds(cursorRequest);
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
        // 1) 统计范围条件按当前这条数据现算: 其中「右值取自宿主记录」的那类条件, 每条数据的值都不一样。
        //    每条数据一份请求对象, 不能改上下文里那份模板 —— 一页数据是并行刷的, 几个线程共用同一个上下文,
        //    改共享对象会串值。
        StatisticAggregateRequest request = new StatisticAggregateRequest();
        request.setOrgId(orgId);
        request.setDataId(dataId);
        request.setRelatedFieldId(context.relatedFieldId);
        request.setRelatedBusinessKey(context.relatedBusinessKey);
        request.setStatisticFieldId(context.statisticFieldId);
        request.setStatisticBusinessKey(context.statisticBusinessKey);
        request.setStatisticType(context.statisticType);
        request.setAvgSkipEmpty(context.avgSkipEmpty);
        request.setScopeCondition(resolveScopeConditions(context, dataId));

        // 2) 聚合语句由目标表单自己的 Mapper 提供: 统计范围条件里的名字是目标表单上的字段,
        //    同一个名字(如 products)在线索上是 JSON 数组列、在价格上是子表, 一套通用渲染必然猜错。
        BigDecimal value = statisticSqlMapperRegistry.get(context.targetFormKey).selectStatisticAggregate(request);
        // 3) 走代理调用, 让 writeStatisticValue 上的 @Transactional 生效(直接 this 调用不走代理)
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

                    // 构建不出来会抛, 由下面的 catch 兜住
                    StatisticRefreshContext context = buildContext(ref.field(), ref.hostDataTable());

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
                    // 构建不出来会抛, 由下面的 catch 兜住
                    StatisticRefreshContext context = buildContext(field, hostDataTable);
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
        //    构建不出来会抛, 不吞: 用户点了刷新就必须得到反馈, 不能静默什么都不做。
        StatisticRefreshContext context = buildContext(field, hostDataTable);
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
     * <p>字段配置本身有毛病(目标表单不支持、关联字段被删之类)时直接抛:
     * 批量刷新的三个入口都在按字段的 try/catch 里, 会把这一个字段记下来继续跑别的;
     * 手动刷新不吞, 让用户看到原因。</p>
     *
     * @return 刷新上下文, 不为 null
     */
    private StatisticRefreshContext buildContext(StatisticField field, String hostDataTable) {
        String targetFormKey = field.getTargetFormId();
        // 目标表单必须能反查到物理表: 这张表同时是「哪些表单支持统计字段」的判据, 拿不到就直接失败,
        // 不要放进去让它在刷新时变成一条查不出东西的语句。
        if (FORM_KEY_TABLE.get(targetFormKey) == null) {
            // 自定义表单暂时不能作为统计目标, 保存时已经拦过, 这里是兜底
            throw new GenericException("统计字段的目标表单不支持: " + targetFormKey);
        }

        StatisticRefreshContext context = new StatisticRefreshContext();
        context.field = field;
        context.hostDataTable = hostDataTable;
        context.targetFormKey = targetFormKey;
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

        // 统计范围: 「全部」不过滤, 「符合条件」按配置的条件过滤目标表单里关联过来的数据。
        // 配置里存的是筛选弹窗的「字段对字段」结构, 先转成列表页高级搜索的 CombineSearch;
        // 再把「右值取自宿主字段」的条件解析成取值方式, 真正拼成 SQL 条件是逐条数据做的,
        // 原因见 resolveScopeConditions。
        context.scopeCondition = new CombineSearch();
        context.scopeRefSources = Collections.emptyMap();
        if (enumValue(StatisticDataScope.class, field.getDataScope()) == StatisticDataScope.CONDITION) {
            Map<String, HostValueSource> refSources = new HashMap<>();
            context.scopeCondition = scopeCondition(
                    StatisticConditionConverter.toCombineSearch(field.getCombineSearch()), field.getId(), refSources);
            context.scopeRefSources = refSources;
        }
        return context;
    }

    /**
     * 预处理统计范围条件(统计范围 = 「符合条件」)。
     *
     * <p>入参是已经转好的 {@link CombineSearch}, 与列表页高级搜索、与「更新范围条件」完全同构,
     * 区别只在 {@code name} 指的是「目标表单」上的字段 —— 过滤的是目标表单里关联过来的哪些数据,
     * 而不是宿主的哪些数据要算(那是更新范围的事)。所以除了右值的来源, 这里不做任何结构转换;
     * 配置里那份「字段对字段」结构的转换在调用点, 由
     * {@link StatisticConditionConverter#toCombineSearch} 完成。</p>
     *
     * <p><b>右值取自宿主记录时</b>: 保存配置的时候宿主记录还不存在, 拿不到值, 配置里只能存一个引用
     * ({@code value} 是 {@code {"refFieldId": 宿主字段ID}} 这样的对象)。这里把引用解析成
     * 「去宿主表单的哪一列取值」, 路子与 {@link #resolveBusinessKey(String)} 一致:
     * 业务字段的值在宿主主表列上, 自定义字段的值在字段值表里, 还要再分是不是大字段。
     * 认错一种就会恒取不到值, 条件被丢掉, 统计值静默偏大。</p>
     *
     * <p>引用字段不存在(被删了)时整条条件丢掉: 与前端数据源候选项的口径一致, 取不到值就筛掉,
     * 不拼 {@code col = null} 这种恒不成立的条件。</p>
     *
     * <p><b>为什么不在这里一并解析成 SQL 条件</b>: 右值随数据变, 而
     * {@link ConditionFilterUtils#parseCondition} 是把值一起解析进去的。按当前数据的值逐条重新解析,
     * 代价是每条数据都要重建一次目标表单配置; 所以这里只把「右值从哪来」定下来,
     * 真正的解析留到 {@link #resolveScopeConditions}。</p>
     *
     * @param combineSearch 统计范围条件, 已由 {@link StatisticConditionConverter} 从配置的弹窗结构转好
     * @param fieldId       统计字段ID, 仅用于日志
     * @param refSources    出参: 引用字段ID -> 它在宿主表单上的取值方式
     *
     * @return 引用字段仍存在的条件; 配置为空时返回空条件(等同于不过滤)
     */
    private CombineSearch scopeCondition(CombineSearch combineSearch, String fieldId,
                                         Map<String, HostValueSource> refSources) {
        if (combineSearch == null || CollectionUtils.isEmpty(combineSearch.getConditions())) {
            // 保存时校验过「符合条件」必须带条件, 但历史数据或直接改库可能破坏这个前提
            log.warn("统计范围声明为「符合条件」却没有任何条件, 按不过滤处理: fieldId={}", fieldId);
            return new CombineSearch();
        }

        List<FilterCondition> conditions = new ArrayList<>(combineSearch.getConditions().size());
        for (FilterCondition condition : combineSearch.getConditions()) {
            String refFieldId = refFieldId(condition.getValue());
            if (refFieldId != null && !refSources.containsKey(refFieldId)) {
                HostValueSource source = resolveRefSource(refFieldId);
                if (source == null) {
                    // 引用字段被删了, 这条条件取不到值。丢掉并留痕: 留着会拼出恒不成立的条件,
                    // 统计值偏小, 而且从结果上看不出是配置问题。
                    log.warn("统计范围条件的取值字段不存在, 已忽略该条件: fieldId={}, refFieldId={}", fieldId, refFieldId);
                    continue;
                }
                refSources.put(refFieldId, source);
            }
            // 条件对象本身不改, 逐条数据时各复制一份再换右值 —— 一页数据是并行刷的, 共用同一个上下文
            conditions.add(condition);
        }

        CombineSearch scope = new CombineSearch();
        scope.setSearchMode(combineSearch.getSearchMode());
        scope.setConditions(conditions);
        return scope;
    }

    /**
     * 取条件右值里的引用字段ID。
     *
     * <p>右值取自宿主记录时存的是一个对象, 其余情况是普通字面量, 靠这一点区分。</p>
     *
     * <p>之所以塞在 {@code value} 里而不是给 {@link FilterCondition} 加字段:
     * 那个类是列表页高级搜索共用的, 加一个只在统计字段里有意义的字段会污染其它场景;
     * 而条件里的值本来就是 {@code Object}, 存一个引用对象不破坏任何既有解析。</p>
     *
     * @return 引用字段ID; 右值是字面量时返回 null
     */
    private static String refFieldId(Object value) {
        return value instanceof Map<?, ?> ref ? stringValue(ref.get(REF_FIELD_ID)) : null;
    }

    /**
     * 解析一个引用字段在宿主表单上的取值方式。
     *
     * @return 取值方式; 字段不存在时返回 null (调用方会丢掉这条条件)
     */
    private HostValueSource resolveRefSource(String refFieldId) {
        ModuleField hostField = moduleFieldMapper.selectByPrimaryKey(refFieldId);
        if (hostField == null) {
            return null;
        }
        // 主表列名判 null 会让取值回落到字段值表, 与 readRelatedValue 里「认不出来就当自定义字段」的口径一致
        return new HostValueSource(businessKeyOf(hostField.getInternalKey(), refFieldId), refFieldId,
                BaseField.isBlob(hostField.getType()));
    }

    /**
     * 把统计范围条件解析成可以直接拼 SQL 的形式, 右值按当前这条数据现取。
     *
     * <p><b>为什么要逐条数据做一次</b>: 条件右值可能取自宿主记录(字段对字段比较), 每条数据的值都不同,
     * 而 {@link ConditionFilterUtils#parseCondition} 是把值一起解析进去的。
     * 复用同一份解析而不是另写一套, 是为了让「统计范围」与列表页、与数据源字段的候选项过滤
     * 永远对同一个字面量得出同一个结果。</p>
     *
     * <p><b>右值取不到时整条条件丢掉</b>: 与前端数据源候选项的口径一致 ——
     * 那边也是取不到值就把这条条件筛掉({@code dataSource.vue} 的 {@code getParams})。
     * 不丢的话会拼出 {@code col = null} 这种恒不成立的条件, 统计值偏小, 而且看不出是配置问题。
     * 这是一个刻意的取舍: 它和列表页一致, 但「匹配字段」为空时确实等价于「不过滤」。</p>
     *
     * <p><b>注意</b>: 成员字段条件里的 {@code CURRENT_USER} 由 {@code ConditionFilterUtils} 换成当前登录用户,
     * 而批量刷新跑在线程池里拿不到会话, 会被换成 null(条件失效)。更新范围条件走的是同一个方法,
     * 两边行为一致。</p>
     *
     * @return 解析后的条件; 没有条件时返回一个空的 CombineSearch(等同于不过滤), 不为 null ——
     *         各表单的条件片段里都写着 {@code ${conditions}.size() > 0}, 传 null 会直接抛。
     */
    private CombineSearch resolveScopeConditions(StatisticRefreshContext context, String dataId) {
        if (CollectionUtils.isEmpty(context.scopeCondition.getConditions())) {
            return new CombineSearch();
        }

        List<FilterCondition> conditions = new ArrayList<>(context.scopeCondition.getConditions().size());
        for (FilterCondition condition : context.scopeCondition.getConditions()) {
            FilterCondition resolved = copyOf(condition);
            String refFieldId = refFieldId(condition.getValue());
            if (refFieldId != null) {
                // 引用宿主字段的条件: 值换成这条数据自己的值。其余字段(name/operator/type/multipleValue)
                // 描述的是「拿目标表单的哪一列怎么比」, 对同一条统计字段的所有数据都一样, 原样带过去。
                resolved.setValue(readHostFieldValue(context.hostDataTable, context.scopeRefSources.get(refFieldId), dataId));
            }
            conditions.add(resolved);
        }

        CombineSearch combineSearch = new CombineSearch();
        combineSearch.setSearchMode(context.scopeCondition.getSearchMode());
        combineSearch.setConditions(conditions);
        BaseCondition baseCondition = new BaseCondition();
        baseCondition.setCombineSearch(combineSearch);
        // formKey 传目标表单: name 是目标表单上的字段, 物理列名、显示字段要 join 哪张主表都由它决定。
        // 依赖组织上下文, 调用方已经设置好。
        ConditionFilterUtils.parseCondition(baseCondition, context.targetFormKey);
        // 返回解析后的那份: 它已经是各表单条件片段要的形态 —— 字段名是驼峰的业务键或字段ID、customField /
        // blob / refFiled 这些标志位都已填好。这里不能再把业务键转成下划线列名: 各表单的 condition 片段
        // 是按驼峰名匹配的(condition.name == 'followTime'), 一转就全都命中不了, 会静默落到按字段值表查的分支上。
        // getConditions() 会再按 valid() 过一遍, 值取不到的条件(以及相对时间解析后为空的)在这里被丢掉。
        return baseCondition.getCombineSearch();
    }

    /**
     * 复制一份条件对象。
     *
     * <p>逐条数据都要换右值, 而上下文里那份模板是一页数据并行共用的 ——
     * 就地改会把上一条数据的条件带到下一条上, 值会串。</p>
     */
    private static FilterCondition copyOf(FilterCondition condition) {
        FilterCondition copy = new FilterCondition();
        copy.setName(condition.getName());
        copy.setOperator(condition.getOperator());
        copy.setType(condition.getType());
        copy.setMultipleValue(condition.getMultipleValue());
        copy.setValue(condition.getValue());
        copy.setContainChildIds(condition.getContainChildIds());
        return copy;
    }

    /**
     * 读宿主记录上某个字段的值, 作为统计范围条件的右值。
     *
     * <p>与 {@link #readRelatedValue} 是同一个路子: 业务字段读主表列, 自定义字段读字段值表。</p>
     *
     * @return 字段值; 字段没有值或行不存在时返回 null
     */
    private String readHostFieldValue(String hostDataTable, HostValueSource source, String dataId) {
        if (source == null) {
            // 构建上下文时已经把取不到值的条件筛掉了, 走到这里说明映射缺了条目, 属于内部不一致
            log.warn("统计范围条件找不到取值方式, 按取不到值处理: dataId={}", dataId);
            return null;
        }
        if (source.column() != null) {
            return extStatisticMapper.selectBusinessFieldValue(hostDataTable, source.column(), dataId);
        }
        return extStatisticMapper.selectFieldValue(
                hostDataTable + FIELD_TABLE_SUFFIX, hostDataTable + BLOB_TABLE_SUFFIX,
                source.fieldId(), dataId, source.blob());
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
     * <p>配置里存的是设计器筛选弹窗的「字段对字段」结构, 与统计范围条件同一套
     * (见 {@link StatisticConditionConverter}), 所以先转成高级搜索的 {@link CombineSearch},
     * 再复用列表页同一套解析。</p>
     *
     * <p><b>弹窗的「匹配字段」在这里只能丢掉</b>: 更新范围是在一条 SQL 里对整个宿主表判定的
     * (右值不随数据变, 所以条件只解析一次), 没有统计范围那种「逐条数据现取右值」的机会,
     * 右值引用变不成谓词。丢的时候必须留痕 —— 不丢的话右值会以 Map 的形式进到绑定参数里,
     * 刷新直接报错, 而配置和日志里都看不出是哪条条件的问题。</p>
     *
     * @return 解析并补全后的 CombineSearch; 条件为空时返回一个空的 CombineSearch (等同于不过滤)
     */
    private CombineSearch parseHostCondition(Map<String, Object> rawCondition, String hostFormKey) {
        if (rawCondition == null || rawCondition.isEmpty()) {
            // 保存时有校验兜底(updateScope = CONDITION 必须有条件), 但历史数据或直接改库可能破坏这个前提
            log.warn("统计字段更新范围声明为「符合条件」却没有任何条件, 按不过滤处理: formKey={}", hostFormKey);
            return new CombineSearch();
        }

        CombineSearch combineSearch = StatisticConditionConverter.toCombineSearch(rawCondition);
        List<FilterCondition> usable = new ArrayList<>();
        for (FilterCondition condition : combineSearch.getConditions()) {
            // 右值是对象 = 弹窗里的「匹配字段」留下的取值引用。按结构判断而不是按 refFieldId 有没有值判断:
            // 右侧字段被选空的引用(操作符选「为空」时前端允许)一样是引用, 一样变不成谓词。
            if (condition.getValue() instanceof Map) {
                log.warn("统计字段更新范围不支持「匹配字段」条件, 已忽略该条件: formKey={}, field={}",
                        hostFormKey, condition.getName());
                continue;
            }
            usable.add(condition);
        }
        if (usable.isEmpty()) {
            log.warn("统计字段更新范围的条件没有一条能用在 SQL 里, 按不过滤处理: formKey={}", hostFormKey);
            return new CombineSearch();
        }
        combineSearch.setConditions(usable);

        BaseCondition baseCondition = new BaseCondition();
        baseCondition.setCombineSearch(combineSearch);
        // parseCondition 内部会按 formKey 自己取表单配置(依赖组织上下文, 调用方已经设置),
        // 把 FilterCondition 补全成 FilterDBCondition, 并处理 DYNAMICS 这类相对时间条件 ——
        // 不自己实现一遍才不会和列表页产生口径差。
        ConditionFilterUtils.parseCondition(baseCondition, hostFormKey);
        return combineSearch;
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
     * 统计范围条件里「右值取自宿主记录的某个字段」时, 去宿主表单的哪儿取值。
     *
     * <p>按字段ID查一次字段表得到, 而不是读宿主表单配置: 一个统计字段每次刷新只解析一次引用,
     * 用主键定位一行比重建整份表单配置便宜得多。</p>
     *
     * @param column  引用字段是业务字段时它在宿主主表上的列名, 否则为 null
     * @param fieldId 引用字段ID, 仅 {@code column} 为 null 时有意义
     * @param blob    引用字段是否是大字段, 决定读字段值表还是大字段值表
     */
    private record HostValueSource(String column, String fieldId, boolean blob) {
    }

    /**
     * 取字符串形式的配置值 (JSON 反序列化出来的可能是任何类型)。
     */
    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
        /**
         * 目标表单Key。
         *
         * <p>解析统计范围条件时要用: 左字段是目标表单上的字段, 物理列名、显示字段要 join 哪张主表,
         * 都得按目标表单的表单配置来。</p>
         */
        private String targetFormKey;
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
        /**
         * 统计范围条件(统计范围 = 「符合条件」); 「全部」时是空条件。
         *
         * <p>存的是配置里的原始形态而不是解析后的 SQL 形态, 因为右值可能取自宿主记录、要按每条宿主数据现取,
         * 见 {@link #resolveScopeConditions}。这里只把「引用字段去哪儿取值」提前解析好放在
         * {@link #scopeRefSources} 里。这份条件是只读的, 每条数据各复制一份再换右值。</p>
         */
        private CombineSearch scopeCondition;
        /**
         * 引用字段ID -> 它在宿主表单上的取值方式, 即 {@link #scopeCondition} 里那些右值取自宿主记录的条件。
         */
        private Map<String, HostValueSource> scopeRefSources;
    }
}
