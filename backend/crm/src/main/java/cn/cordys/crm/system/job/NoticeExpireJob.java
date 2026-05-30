package cn.cordys.crm.system.job;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityQuotationMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.MessageTaskConfig;
import cn.cordys.crm.system.dto.MessageTaskConfigDTO;
import cn.cordys.crm.system.dto.TimeDTO;
import cn.cordys.crm.system.mapper.ExtMessageTaskConfigMapper;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 商机报价单/合同/回款计划到期及即将到期提醒监听器
 * <p>
 * 该监听器每天8点执行，检查并提醒到期的业务对象。
 * </p>
 */
@Component
@Slf4j
public class NoticeExpireJob {

    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;
    @Resource
    private ExtMessageTaskConfigMapper extMessageTaskConfigMapper;
    @Resource
    private ExtOpportunityQuotationMapper extOpportunityQuotationMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private BaseMapper<Opportunity> opportunityBaseMapper;
    @Resource
    private BaseMapper<Customer> customerBaseMapper;
    @Resource
    private BaseMapper<Contract> contractBaseMapper;
    @Resource
    private ExtOrganizationMapper extOrganizationMapper;
    @Resource
    private ExtContractPaymentPlanMapper extContractPaymentPlanMapper;
    @Resource
    private ExtContractMapper extContractMapper;

    private static final String TIME_UNIT_DAY = "DAY";
    private static final String MODULE_CONTRACT = NotificationConstants.Module.CONTRACT;
    private static final String MODULE_OPPORTUNITY = NotificationConstants.Module.OPPORTUNITY;

    /**
     * 定时检查到期或即将到期的消息通知任务
     * 每天8点执行一次
     */
    @QuartzScheduled(cron = "0 0 8 * * ?")
    public void onEvent() {
        try {
            // 商机报价单
            processQuotationExpiring();
            processQuotationExpired();

            // 回款计划
            processPaymentPlanExpiring();
            processPaymentPlanExpired();

            // 合同
            processContractExpiring();
            processContractExpired();
        } catch (Exception e) {
            log.error("消息通知提醒异常: ", e);
        }
    }

    private void processQuotationExpiring() {
        processExpiringRemind(
                MODULE_OPPORTUNITY,
                NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRING,
                this::getOpportunityQuotationList,
                this::buildOpportunityNoticeContext
        );
    }

    private void processQuotationExpired() {
        processExpiredRemind(
                MODULE_OPPORTUNITY,
                NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRED,
                this::getOpportunityQuotationList,
                this::buildOpportunityNoticeContext
        );
    }

    private NoticeContext buildOpportunityNoticeContext(OpportunityQuotation quotation) {
        Opportunity opportunity = opportunityBaseMapper.selectByPrimaryKey(quotation.getOpportunityId());
        if (opportunity == null) {
            log.warn("商机报价单 {} 关联的商机不存在", quotation.getId());
            return null;
        }
        Customer customer = customerBaseMapper.selectByPrimaryKey(opportunity.getCustomerId());
        if (customer == null) {
            log.warn("商机报价单 {} 关联的客户不存在", quotation.getId());
            return null;
        }
        return NoticeContext.builder()
                .customerName(customer.getName())
                .createUser(quotation.getCreateUser())
                .owner(quotation.getCreateUser())
                .resourceId(quotation.getId())
                .orgId(quotation.getOrganizationId())   // 关键修复：设置组织ID
                .build();
    }

    private NoticeContext buildPaymentPlanNoticeContext(ContractPaymentPlan paymentPlan) {
        Contract contract = contractBaseMapper.selectByPrimaryKey(paymentPlan.getContractId());
        if (contract == null) {
            log.warn("回款计划 {} 关联的合同不存在", paymentPlan.getId());
            return null;
        }
        Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
        if (customer == null) {
            log.warn("回款计划 {} 关联的客户不存在", paymentPlan.getId());
            return null;
        }
        return NoticeContext.builder()
                .customerName(customer.getName())
                .createUser(paymentPlan.getCreateUser())
                .owner(paymentPlan.getOwner())
                .resourceId(paymentPlan.getId())
                .orgId(paymentPlan.getOrganizationId())   // 设置组织ID
                .build();
    }

    private NoticeContext buildContractNoticeContext(Contract contract) {
        Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
        if (customer == null) {
            log.warn("合同 {} 关联的客户不存在", contract.getId());
            return null;
        }
        return NoticeContext.builder()
                .customerName(customer.getName())
                .createUser(contract.getCreateUser())
                .owner(contract.getOwner())
                .resourceId(contract.getId())
                .orgId(contract.getOrganizationId())   // 设置组织ID
                .build();
    }

    private void processPaymentPlanExpiring() {
        processExpiringRemind(
                MODULE_CONTRACT,
                NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRING,
                this::getContractPaymentPlanList,
                this::buildPaymentPlanNoticeContext
        );
    }

    private void processPaymentPlanExpired() {
        processExpiredRemind(
                MODULE_CONTRACT,
                NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRED,
                this::getContractPaymentPlanList,
                this::buildPaymentPlanNoticeContext
        );
    }

    private void processContractExpiring() {
        processExpiringRemind(
                MODULE_CONTRACT,
                NotificationConstants.Event.CONTRACT_EXPIRING,
                this::getContractList,
                this::buildContractNoticeContext
        );
    }

    private void processContractExpired() {
        processExpiredRemind(
                MODULE_CONTRACT,
                NotificationConstants.Event.CONTRACT_EXPIRED,
                this::getContractList,
                this::buildContractNoticeContext
        );
    }

    /**
     * 处理到期提醒（当天到期）
     */
    private <T> void processExpiredRemind(
            String module,
            String event,
            TriFunction<String, Long, Long, List<T>> dataFetcher,
            Function<T, NoticeContext> contextBuilder
    ) {
        log.info("开始处理 {} 到期提醒", event);
        Set<String> organizationIds = extOrganizationMapper.selectAllOrganizationIds();

        for (String orgId : organizationIds) {
            try {
                // 1. 检查开关
                MessageTask messageTask = extMessageTaskMapper.getMessageByModuleAndEvent(module, event, orgId);
                if (isNotificationEnabled(messageTask)) {
                    log.debug("组织 {} 的 {} 提醒未开启", orgId, event);
                    continue;
                }

                // 2. 获取配置
                MessageTaskConfig config = extMessageTaskConfigMapper.getConfigByModuleAndEvent(module, event, orgId);
                if (config == null || config.getValue() == null) {
                    log.debug("组织 {} 的 {} 提醒配置不存在", orgId, event);
                    continue;
                }
                MessageTaskConfigDTO configDTO = JSON.parseObject(config.getValue(), MessageTaskConfigDTO.class);

                // 3. 查询当天到期的数据
                long todayZero = TimeUtils.getZeroHourTimestamp(0);
                long yesterdayZero = TimeUtils.getZeroHourTimestamp(-1);
                List<T> dataList = dataFetcher.apply(orgId, todayZero, yesterdayZero);

                if (CollectionUtils.isEmpty(dataList)) {
                    log.debug("组织 {} 无 {} 到期数据", orgId, event);
                    continue;
                }

                // 4. 发送通知
                for (T item : dataList) {
                    NoticeContext ctx = contextBuilder.apply(item);
                    if (ctx != null) {
                        sendNotice(configDTO, module, ctx, event, null);
                    }
                }
                log.info("组织 {} 的 {} 到期提醒处理完成，共 {} 条", orgId, event, dataList.size());
            } catch (Exception e) {
                log.error("处理组织 {} 的 {} 到期提醒失败", orgId, event, e);
            }
        }
    }

    /**
     * 处理即将到期提醒（按配置的天数提前提醒）
     */
    private <T> void processExpiringRemind(
            String module,
            String event,
            TriFunction<String, Long, Long, List<T>> dataFetcher,
            Function<T, NoticeContext> contextBuilder
    ) {
        log.info("开始处理 {} 即将到期提醒", event);
        Set<String> organizationIds = extOrganizationMapper.selectAllOrganizationIds();

        for (String orgId : organizationIds) {
            try {
                // 1. 检查开关
                MessageTask messageTask = extMessageTaskMapper.getMessageByModuleAndEvent(module, event, orgId);
                if (isNotificationEnabled(messageTask)) {
                    log.debug("组织 {} 的 {} 提醒未开启", orgId, event);
                    continue;
                }

                // 2. 获取配置
                MessageTaskConfig config = extMessageTaskConfigMapper.getConfigByModuleAndEvent(module, event, orgId);
                if (config == null || config.getValue() == null) {
                    log.debug("组织 {} 的 {} 提醒配置不存在", orgId, event);
                    continue;
                }
                MessageTaskConfigDTO configDTO = JSON.parseObject(config.getValue(), MessageTaskConfigDTO.class);
                if (CollectionUtils.isEmpty(configDTO.getTimeList())) {
                    log.debug("组织 {} 的 {} 时间配置为空", orgId, event);
                    continue;
                }

                // 3. 遍历每个提前天数配置
                for (TimeDTO timeDTO : configDTO.getTimeList()) {
                    if (!TIME_UNIT_DAY.equals(timeDTO.getTimeUnit())) {
                        continue;
                    }
                    int days = timeDTO.getTimeValue();
                    long startTime = TimeUtils.getZeroHourTimestamp(days + 1);
                    long endTime = TimeUtils.getZeroHourTimestamp(days);

                    List<T> dataList = dataFetcher.apply(orgId, startTime, endTime);
                    if (CollectionUtils.isEmpty(dataList)) {
                        log.debug("组织 {} 提前 {} 天无 {} 数据", orgId, days, event);
                        continue;
                    }

                    for (T item : dataList) {
                        NoticeContext ctx = contextBuilder.apply(item);
                        if (ctx != null) {
                            sendNotice(configDTO, module, ctx, event, days);
                        }
                    }
                }
                log.info("组织 {} 的 {} 即将到期提醒处理完成", orgId, event);
            } catch (Exception e) {
                log.error("处理组织 {} 的 {} 即将到期提醒失败", orgId, event, e);
            }
        }
    }

    /**
     * 判断通知渠道是否至少开启一种
     */
    private boolean isNotificationEnabled(MessageTask task) {
        if (task == null) {
            return true;
        }
        return !Boolean.TRUE.equals(task.getDingTalkEnable())
                && !Boolean.TRUE.equals(task.getEmailEnable())
                && !Boolean.TRUE.equals(task.getSysEnable())
                && !Boolean.TRUE.equals(task.getWeComEnable())
                && !Boolean.TRUE.equals(task.getLarkEnable());
    }

    /**
     * 发送通知
     */
    private void sendNotice(MessageTaskConfigDTO configDTO, String model,
                            NoticeContext ctx, String event, Integer days) {
        List<String> receiveUserIds = commonNoticeSendService.getNoticeReceiveUserIds(
                configDTO, ctx.getCreateUser(), ctx.getOwner(), ctx.getOrgId());

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("customerName", ctx.getCustomerName());
        paramMap.put("name", ctx.getCustomerName());
        if (days != null) {
            paramMap.put("expireDays", days);
        }
        if (ctx.getResourceId() != null) {
            paramMap.put("resourceId", ctx.getResourceId());
        }

        commonNoticeSendService.sendNotice(model, event, paramMap,
                ctx.getUserId(), ctx.getOrgId(), receiveUserIds, false);
    }

    private List<Contract> getContractList(String orgId, long timestamp, long timestampOld) {
        return extContractMapper.selectByTimestamp(orgId, timestampOld, timestamp);
    }

    private List<OpportunityQuotation> getOpportunityQuotationList(String orgId, long timestamp, long timestampOld) {
        return extOpportunityQuotationMapper.getQuotationByTimestamp(timestamp, timestampOld, orgId);
    }

    private List<ContractPaymentPlan> getContractPaymentPlanList(String orgId, long timestamp, long timestampOld) {
        return extContractPaymentPlanMapper.selectByTimestamp(timestamp, timestampOld, orgId);
    }

    /**
     * 时间计算工具
     */
    private static final class TimeUtils {
        private static final ZoneId ZONE = ZoneId.systemDefault();

        static long getZeroHourTimestamp(int daysOffset) {
            return LocalDate.now().plusDays(daysOffset)
                    .atStartOfDay(ZONE)
                    .toInstant()
                    .toEpochMilli();
        }
    }

    /**
     * 函数式接口：三个参数，一个返回值
     */
    @FunctionalInterface
    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    /**
     * 通知上下文数据对象
     */
    @lombok.Builder
    @lombok.Getter
    private static class NoticeContext {
        private String customerName;
        private String createUser;
        private String owner;
        private String resourceId;
        private String userId;
        private String orgId;

        public String getUserId() {
            return userId != null ? userId : createUser;
        }
    }
}