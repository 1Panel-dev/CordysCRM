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
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 商机报价单/回款计划/合同 到期和即将到期提醒
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

    @QuartzScheduled(cron = "0 0 8 * * ?")
    public void onEvent() {
        try {
            // 报价单到期提醒
            handleExpiringRemind(
                    NotificationConstants.Module.OPPORTUNITY,
                    NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRING,
                    this::getOpportunityQuotationList,
                    this::buildQuotationNotifyParams
            );
            handleExpiredRemind(
                    NotificationConstants.Module.OPPORTUNITY,
                    NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRED,
                    this::getOpportunityQuotationList,
                    this::buildQuotationNotifyParams
            );
            // 回款计划到期提醒
            handleExpiringRemind(
                    NotificationConstants.Module.CONTRACT,
                    NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRING,
                    this::getContractPaymentPlanList,
                    this::buildPaymentPlanNotifyParams
            );
            handleExpiredRemind(
                    NotificationConstants.Module.CONTRACT,
                    NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRED,
                    this::getContractPaymentPlanList,
                    this::buildPaymentPlanNotifyParams
            );
            // 合同到期提醒
            handleExpiringRemind(
                    NotificationConstants.Module.CONTRACT,
                    NotificationConstants.Event.CONTRACT_EXPIRING,
                    this::getContractList,
                    this::buildContractNotifyParams
            );
            handleExpiredRemind(
                    NotificationConstants.Module.CONTRACT,
                    NotificationConstants.Event.CONTRACT_EXPIRED,
                    this::getContractList,
                    this::buildContractNotifyParams
            );
        } catch (Exception e) {
            log.error("消息通知提醒异常: ", e);
        }
    }

    /**
     * 处理"即将到期"提醒的通用模板
     */
    private <T> void handleExpiringRemind(String module, String event,
                                          BiFunction<String, TimeRange, List<T>> dataFetcher,
                                          Function<T, NotifyParams<T>> paramsBuilder) {
        log.info("{} - {} 即将到期提醒", module, event);
        Set<String> organizationIds = extOrganizationMapper.selectAllOrganizationIds();
        for (String organizationId : organizationIds) {
            MessageTask msgTask = extMessageTaskMapper.getMessageByModuleAndEvent(module, event, organizationId);
            if (isNotifyEnabled(msgTask)) {
                log.info("组织{} {} 即将到期提醒未开启", organizationId, event);
                continue;
            }
            MessageTaskConfigDTO configDTO = getConfigDTO(module, event, organizationId);
            if (configDTO == null || CollectionUtils.isEmpty(configDTO.getTimeList())) continue;

            for (TimeDTO timeDTO : configDTO.getTimeList()) {
                if (!"DAY".equals(timeDTO.getTimeUnit())) continue;
                TimeRange range = TimeRange.beforeDays(timeDTO.getTimeValue());
                List<T> dataList = dataFetcher.apply(organizationId, range);
                if (CollectionUtils.isEmpty(dataList)) continue;

                for (T data : dataList) {
                    NotifyParams<T> params = paramsBuilder.apply(data);
                    if (params.skipReason != null) {
                        log.info("组织{} {} {}: {}", organizationId, event, params.resourceId, params.skipReason);
                        continue;
                    }
                    sendNotice(configDTO, module, params, event, timeDTO.getTimeValue());
                }
            }
            log.info("组织{} {}即将到期提醒完成", organizationId, event);
        }
    }

    /**
     * 处理"已到期"提醒的通用模板
     */
    private <T> void handleExpiredRemind(String module, String event,
                                         BiFunction<String, TimeRange, List<T>> dataFetcher,
                                         Function<T, NotifyParams<T>> paramsBuilder) {
        log.info("{} - {} 到期提醒", module, event);
        Set<String> organizationIds = extOrganizationMapper.selectAllOrganizationIds();
        for (String organizationId : organizationIds) {
            MessageTask msgTask = extMessageTaskMapper.getMessageByModuleAndEvent(module, event, organizationId);
            if (isNotifyEnabled(msgTask)) {
                log.info("组织{} {}到期提醒未开启", organizationId, event);
                continue;
            }
            MessageTaskConfigDTO configDTO = getConfigDTO(module, event, organizationId);
            if (configDTO == null) continue;

            TimeRange range = TimeRange.yesterday();
            List<T> dataList = dataFetcher.apply(organizationId, range);
            if (CollectionUtils.isEmpty(dataList)) continue;

            for (T data : dataList) {
                NotifyParams<T> params = paramsBuilder.apply(data);
                if (params.skipReason != null) {
                    log.info("组织{} {} {}: {}", organizationId, event, params.resourceId, params.skipReason);
                    continue;
                }
                sendNotice(configDTO, module, params, event, null);
            }
        }
    }


    private List<OpportunityQuotation> getOpportunityQuotationList(String organizationId, TimeRange range) {
        return extOpportunityQuotationMapper.getQuotationByTimestamp(range.start, range.end, organizationId);
    }

    private List<ContractPaymentPlan> getContractPaymentPlanList(String organizationId, TimeRange range) {
        return extContractPaymentPlanMapper.selectByTimestamp(range.start, range.end, organizationId);
    }

    private List<Contract> getContractList(String organizationId, TimeRange range) {
        return extContractMapper.selectByTimestamp(organizationId, range.start, range.end);
    }

    private NotifyParams<OpportunityQuotation> buildQuotationNotifyParams(OpportunityQuotation q) {
        Opportunity opp = opportunityBaseMapper.selectByPrimaryKey(q.getOpportunityId());
        if (opp == null) return NotifyParams.skip(q.getId(), "关联的商机不存在");
        Customer customer = customerBaseMapper.selectByPrimaryKey(opp.getCustomerId());
        if (customer == null) return NotifyParams.skip(q.getId(), "关联的客户不存在");
        return new NotifyParams<>(q, customer.getName(), q.getCreateUser(), q.getOrganizationId(), q.getCreateUser(), q.getCreateUser(), q.getId());
    }

    private NotifyParams<ContractPaymentPlan> buildPaymentPlanNotifyParams(ContractPaymentPlan plan) {
        Contract contract = contractBaseMapper.selectByPrimaryKey(plan.getContractId());
        if (contract == null) return NotifyParams.skip(plan.getId(), "关联的合同不存在");
        Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
        if (customer == null) return NotifyParams.skip(plan.getId(), "关联的客户不存在");
        return new NotifyParams<>(plan, customer.getName(), plan.getCreateUser(), plan.getOrganizationId(), plan.getCreateUser(), plan.getOwner(), plan.getId());
    }

    private NotifyParams<Contract> buildContractNotifyParams(Contract contract) {
        Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
        if (customer == null) return NotifyParams.skip(contract.getId(), "关联的客户不存在");
        return new NotifyParams<>(contract, customer.getName(), contract.getCreateUser(), contract.getOrganizationId(), contract.getCreateUser(), contract.getOwner(), contract.getId());
    }

    private boolean isNotifyEnabled(MessageTask task) {
        return task == null || (!task.getDingTalkEnable() && !task.getEmailEnable() && !task.getSysEnable()
                && !task.getWeComEnable() && !task.getLarkEnable());
    }

    private MessageTaskConfigDTO getConfigDTO(String module, String event, String organizationId) {
        MessageTaskConfig config = extMessageTaskConfigMapper.getConfigByModuleAndEvent(module, event, organizationId);
        if (config == null || config.getValue() == null) {
            log.info("组织{} {}配置不存在", organizationId, event);
            return null;
        }
        return JSON.parseObject(config.getValue(), MessageTaskConfigDTO.class);
    }

    private void sendNotice(MessageTaskConfigDTO configDTO, String module, NotifyParams<?> params, String event, Integer days) {
        List<String> receiveUserIds = commonNoticeSendService.getNoticeReceiveUserIds(configDTO, params.createUser, params.owner, params.orgId);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("customerName", params.customerName);
        paramMap.put("name", params.customerName);
        if (days != null) paramMap.put("expireDays", days);
        if (params.resourceId != null) paramMap.put("resourceId", params.resourceId);
        commonNoticeSendService.sendNotice(module, event, paramMap, params.userId, params.orgId, receiveUserIds, false);
    }

    /**
     * 时间范围（毫秒时间戳，前闭后开）
     */
    record TimeRange(long start, long end) {
        static TimeRange yesterday() {
            long now = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
            long yesterday = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
            return new TimeRange(yesterday, now);
        }

        static TimeRange beforeDays(int days) {
            long start = LocalDate.now().plusDays(days + 1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
            long end = LocalDate.now().plusDays(days).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
            return new TimeRange(start, end);
        }
    }

    /**
     * 通知参数封装
     */
    static class NotifyParams<T> {
        final T data;
        final String customerName;
        final String userId;
        final String orgId;
        final String createUser;
        final String owner;
        final String resourceId;
        final String skipReason;

        NotifyParams(T data, String customerName, String userId, String orgId, String createUser, String owner, String resourceId) {
            this.data = data;
            this.customerName = customerName;
            this.userId = userId;
            this.orgId = orgId;
            this.createUser = createUser;
            this.owner = owner;
            this.resourceId = resourceId;
            this.skipReason = null;
        }

        private NotifyParams(String resourceId, String skipReason) {
            this.data = null;
            this.customerName = null;
            this.userId = null;
            this.orgId = null;
            this.createUser = null;
            this.owner = null;
            this.resourceId = resourceId;
            this.skipReason = skipReason;
        }

        static <T> NotifyParams<T> skip(String resourceId, String reason) {
            return new NotifyParams<>(resourceId, reason);
        }
    }
}
