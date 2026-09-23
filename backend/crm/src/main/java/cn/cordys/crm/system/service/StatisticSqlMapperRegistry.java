package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.contract.mapper.ExtContractInvoiceMapper;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentRecordMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationMapper;
import cn.cordys.crm.order.mapper.ExtOrderMapper;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.product.mapper.ExtProductPriceMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表单Key -> 统计聚合语句所在 Mapper 的分发表。
 *
 * <p>统计字段的聚合 SQL 必须按表单分开写(同一个条件名在不同表单上落到哪个列只有该表单的 Mapper 知道),
 * 而 MyBatis 的 {@code <include refid>} 在解析期就定死了, 没法在运行时选片段, 所以「选哪个表单的语句」
 * 只能在 Java 侧做 —— 这张表就是那个选择的唯一出处。</p>
 *
 * <p>用显式 {@code @Resource} 字段注入而不是收集 {@code List<StatisticSqlMapper>}: 前者在容器启动期
 * 就能发现「漏接一个 Mapper」, 后者会安静地少一个表单, 直到用户配到那个表单才暴露。</p>
 *
 * <p><b>键集合必须与 {@code StatisticFieldService.FORM_KEY_TABLE} 一致</b>: 那张表决定哪些表单允许配
 * 统计字段, 这里决定配了之后拿哪条语句去算。少一个就会在刷新时抛 {@link IllegalStateException},
 * 单元测试遍历 {@link #all()} 能提前发现。</p>
 */
@Component
public class StatisticSqlMapperRegistry {

    @Resource
    private ExtClueMapper extClueMapper;

    @Resource
    private ExtCustomerMapper extCustomerMapper;

    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;

    @Resource
    private ExtFollowUpRecordMapper extFollowUpRecordMapper;

    @Resource
    private ExtFollowUpPlanMapper extFollowUpPlanMapper;

    @Resource
    private ExtOpportunityMapper extOpportunityMapper;

    @Resource
    private ExtProductMapper extProductMapper;

    @Resource
    private ExtProductPriceMapper extProductPriceMapper;

    @Resource
    private ExtOpportunityQuotationMapper extOpportunityQuotationMapper;

    @Resource
    private ExtContractMapper extContractMapper;

    @Resource
    private ExtContractInvoiceMapper extContractInvoiceMapper;

    @Resource
    private ExtContractPaymentPlanMapper extContractPaymentPlanMapper;

    @Resource
    private ExtContractPaymentRecordMapper extContractPaymentRecordMapper;

    @Resource
    private ExtOrderMapper extOrderMapper;

    /**
     * 分发表本体。用 {@link LinkedHashMap} 是为了让 {@link #all()} 的遍历顺序稳定 —— 测试报错时
     * 「第几个表单挂了」每次都不一样会很难排查。
     */
    private final Map<String, StatisticSqlMapper> mappers = new LinkedHashMap<>(16);

    /**
     * 装配分发表。{@code @PostConstruct} 而不是静态初始化: 依赖是注入进来的, 静态块里拿不到。
     */
    @PostConstruct
    private void init() {
        // 1) 键取 FormKey 的 key 字符串而不是枚举名: 上游传进来的就是配置里的字符串(如 contractPaymentPlan),
        //    用枚举名做键会在跟进计划/回款计划这类多词表单上查不到。
        mappers.put(FormKey.CLUE.getKey(), extClueMapper);
        mappers.put(FormKey.CUSTOMER.getKey(), extCustomerMapper);
        mappers.put(FormKey.CONTACT.getKey(), extCustomerContactMapper);
        mappers.put(FormKey.FOLLOW_RECORD.getKey(), extFollowUpRecordMapper);
        mappers.put(FormKey.FOLLOW_PLAN.getKey(), extFollowUpPlanMapper);
        mappers.put(FormKey.OPPORTUNITY.getKey(), extOpportunityMapper);
        mappers.put(FormKey.PRODUCT.getKey(), extProductMapper);
        mappers.put(FormKey.PRICE.getKey(), extProductPriceMapper);
        mappers.put(FormKey.QUOTATION.getKey(), extOpportunityQuotationMapper);
        mappers.put(FormKey.CONTRACT.getKey(), extContractMapper);
        mappers.put(FormKey.INVOICE.getKey(), extContractInvoiceMapper);
        mappers.put(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), extContractPaymentPlanMapper);
        mappers.put(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), extContractPaymentRecordMapper);
        mappers.put(FormKey.ORDER.getKey(), extOrderMapper);
    }

    /**
     * 取某个表单的统计聚合语句。
     *
     * <p>拿不到时抛而不是返回 null: 调用方(刷新服务)已经把「表单是否支持统计字段」校验过了,
     * 走到这里还缺只能是漏注册, 属于部署期问题, 静默返回 null 会退化成「统计值永远是空」这种最难查的故障。</p>
     *
     * @param formKey 表单Key
     *
     * @return 该表单的 Mapper, 不为 null
     *
     * @throws IllegalStateException 该表单没有注册统计聚合语句
     */
    public StatisticSqlMapper get(String formKey) {
        StatisticSqlMapper mapper = mappers.get(formKey);
        if (mapper == null) {
            throw new IllegalStateException("表单没有注册统计聚合语句: " + formKey);
        }
        return mapper;
    }

    /**
     * 全部分发表项, 供测试遍历 —— 让测试跑的正是生产用的这条分发路径。
     *
     * @return 表单Key -> Mapper 的只读视图
     */
    public Map<String, StatisticSqlMapper> all() {
        return Collections.unmodifiableMap(mappers);
    }
}
