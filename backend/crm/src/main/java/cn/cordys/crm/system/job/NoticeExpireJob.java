package cn.cordys.crm.system.job;

import cn.cordys.common.util.JSON;

import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
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

/**
 * 商机报价单即将到期和到期提醒监听器
 * <p>
 * 该监听器负责监听执行事件，当触发时检查并提醒到期的商机报价单。
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


    /**
     * 定时检查到期或即将到期的消息通知任务
     * <p>
     * 该方法每天8点执行一次，检查商机报价单，回款计划以及合同的到期情况，并发送相应的通知提醒。
     * </p>
     *
     */
    @QuartzScheduled(cron = "0 0 8 * * ?")
    public void onEvent() {
        try {
            this.quotationExpiringRemind();
            this.quotationExpiredRemind();
            this.contractPaymentPlanExpiringRemind();
            this.contractPaymentPlanExpiredRemind();
        } catch (Exception e) {
            log.error("消息通知提醒异常: ", e.getMessage());
        }
    }


    /**
     * 商机报价单即将到期自动通知
     * <p>
     * 查询即将到期的商机报价单，并向相关负责人发送通知提醒。
     * </p>
     */
    private void quotationExpiringRemind() {
        log.info("商机报价单即将到期提醒");
        //查询所有组织
        Set<String> organizationIds = extOrganizationMapper.selectAllOrganizationIds();
        for (String organizationId : organizationIds) {
            //查询商机报价单即将到期和到期的消息通知是否开启
            MessageTask expiringEvent = extMessageTaskMapper.getMessageByModuleAndEvent(NotificationConstants.Module.OPPORTUNITY, NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRING, organizationId);
            //判断是否开启
            if (expiringEvent == null || (!expiringEvent.getDingTalkEnable() && !expiringEvent.getEmailEnable() && !expiringEvent.getSysEnable() && !expiringEvent.getWeComEnable() && !expiringEvent.getLarkEnable())) {
                log.info("组织{}商机报价单即将到期提醒未开启", organizationId);
                return;
            }
            MessageTaskConfig expiringConfig = extMessageTaskConfigMapper.getConfigByModuleAndEvent(NotificationConstants.Module.OPPORTUNITY, NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRING, organizationId);
            if (expiringConfig == null || expiringConfig.getValue() == null) {
                log.info("组织{}商机报价单即将到期提醒配置不存在", organizationId);
                return;
            }
            MessageTaskConfigDTO expiringConfigDTO = JSON.parseObject(expiringConfig.getValue(), MessageTaskConfigDTO.class);
            if (CollectionUtils.isEmpty(expiringConfigDTO.getTimeList())) {
                log.info("组织{}商机报价单即将到期提醒时间配置不存在", organizationId);
                return;
            }
            for (TimeDTO timeDTO : expiringConfigDTO.getTimeList()) {
                if (timeDTO.getTimeUnit().equals("DAY")) {
                    long startTime = LocalDate.now().minusDays(timeDTO.getTimeValue())
                            .atStartOfDay(ZoneId.systemDefault())
                            .toEpochSecond() * 1000;
                    long endTime = LocalDate.now().minusDays(timeDTO.getTimeValue() + 1)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toEpochSecond() * 1000;
                    List<OpportunityQuotation> quotationList = getOpportunityQuotationList(organizationId, startTime, endTime);
                    if (CollectionUtils.isEmpty(quotationList)) {
                        log.info("组织{}无即将到期商机报价单", organizationId);
                        return;
                    }
                    for (OpportunityQuotation opportunityQuotation : quotationList) {
                        //发送通知
                        Opportunity opportunity = opportunityBaseMapper.selectByPrimaryKey(opportunityQuotation.getOpportunityId());
                        if (opportunity == null) {
                            log.info("组织{}商机报价单{}关联的商机不存在", organizationId, opportunityQuotation.getId());
                            continue;
                        }
                        Customer customer = customerBaseMapper.selectByPrimaryKey(opportunity.getCustomerId());
                        if (customer == null) {
                            log.info("组织{}商机报价单{}关联的客户不存在", organizationId, opportunityQuotation.getId());
                            continue;
                        }
                        sendNotice(expiringConfigDTO, NotificationConstants.Module.OPPORTUNITY, opportunityQuotation.getCreateUser(), opportunityQuotation.getOrganizationId(), NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRING, customer.getName(), opportunityQuotation.getCreateUser(), opportunityQuotation.getCreateUser(), timeDTO.getTimeValue());
                    }
                }
            }

            log.info("组织{}商机报价单即将到期提醒发送通知成功", organizationId);
        }


    }

    /**
     * 商机报价单到期自动通知
     * <p>
     * 查询已到期的商机报价单，并向相关负责人发送通知提醒。
     * </p>
     */
    private void quotationExpiredRemind() {
        log.info("商机报价单到期提醒");
        Set<String> organizationIds = extOrganizationMapper.selectAllOrganizationIds();
        for (String organizationId : organizationIds) {
            MessageTask expiredEvent = extMessageTaskMapper.getMessageByModuleAndEvent(NotificationConstants.Module.OPPORTUNITY, NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRED, organizationId);
            if (expiredEvent == null || (!expiredEvent.getDingTalkEnable() && !expiredEvent.getEmailEnable() && !expiredEvent.getSysEnable() && !expiredEvent.getWeComEnable() && !expiredEvent.getLarkEnable())) {
                log.info("商机报价单到期提醒未开启");
                return;
            }
            MessageTaskConfig expiredConfig = extMessageTaskConfigMapper.getConfigByModuleAndEvent(NotificationConstants.Module.OPPORTUNITY, NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRED, organizationId);
            if (expiredConfig == null || expiredConfig.getValue() == null) {
                log.info("组织{}商机报价单到期提醒配置不存在", organizationId);
                return;
            }
            MessageTaskConfigDTO expiredConfigDTO = JSON.parseObject(expiredConfig.getValue(), MessageTaskConfigDTO.class);
            if (CollectionUtils.isEmpty(expiredConfigDTO.getTimeList())) {
                log.info("组织{}商机报价单到期提醒时间配置不存在", organizationId);
                return;
            }
            long timestamp = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toEpochSecond() * 1000;

            long timestampOld = LocalDate.now().minusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toEpochSecond() * 1000;
            List<OpportunityQuotation> quotationList = getOpportunityQuotationList(organizationId, timestamp, timestampOld);
            if (CollectionUtils.isEmpty(quotationList)) {
                log.info("组织{}无到期商机报价单", organizationId);
                return;
            }
            for (OpportunityQuotation opportunityQuotation : quotationList) {
                //发送通知
                Opportunity opportunity = opportunityBaseMapper.selectByPrimaryKey(opportunityQuotation.getOpportunityId());
                if (opportunity == null) {
                    log.info("组织{}商机报价单{}关联的商机不存在", organizationId, opportunityQuotation.getId());
                    continue;
                }
                Customer customer = customerBaseMapper.selectByPrimaryKey(opportunity.getCustomerId());
                if (customer == null) {
                    log.info("组织{}商机报价单{}关联的客户不存在", organizationId, opportunityQuotation.getId());
                    continue;
                }
                sendNotice(expiredConfigDTO, NotificationConstants.Module.OPPORTUNITY, opportunityQuotation.getCreateUser(), opportunityQuotation.getOrganizationId(), NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRED, customer.getName(), opportunityQuotation.getCreateUser(), opportunityQuotation.getCreateUser(), null);
            }
            log.info("组织{}商机报价单到期提醒发送通知成功", organizationId);
        }
    }

    /**
     * 根据时间戳获取报价单列表
     *
     * @param organizationId 组织ID
     * @param timestamp      时间戳
     * @param timestampOld   旧时间戳
     * @return 报价单列表
     */
    private List<OpportunityQuotation> getOpportunityQuotationList(String organizationId, long timestamp, long timestampOld) {
        return extOpportunityQuotationMapper.getQuotationByTimestamp(timestamp, timestampOld, organizationId);
    }

    /**
     * 根据时间戳获取回款计划列表
     *
     * @param organizationId 组织ID
     * @param timestamp      时间戳
     * @param timestampOld   旧时间戳
     * @return 回款计划列表
     */
    private List<ContractPaymentPlan> getContractPaymentPlanList(String organizationId, long timestamp, long timestampOld) {
        return extContractPaymentPlanMapper.selectByTimestamp(timestamp, timestampOld, organizationId);
    }

    /**
     * 回款计划即将到期自动通知
     * <p>
     * 查询即将到期的回款计划，并向相关负责人发送通知提醒。
     * </p>
     */
    private void contractPaymentPlanExpiringRemind() {
        log.info("回款计划即将到期提醒");
        Set<String> organizationIds = extOrganizationMapper.selectAllOrganizationIds();
        for (String organizationId : organizationIds) {
            MessageTask expiringEvent = extMessageTaskMapper.getMessageByModuleAndEvent(NotificationConstants.Module.CONTRACT, NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRING, organizationId);
            if (expiringEvent == null || (!expiringEvent.getDingTalkEnable() && !expiringEvent.getEmailEnable() && !expiringEvent.getSysEnable() && !expiringEvent.getWeComEnable() && !expiringEvent.getLarkEnable())) {
                log.info("组织{}回款计划即将到期提醒未开启", organizationId);
                return;
            }
            MessageTaskConfig expiringConfig = extMessageTaskConfigMapper.getConfigByModuleAndEvent(NotificationConstants.Module.CONTRACT, NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRING, organizationId);
            if (expiringConfig == null || expiringConfig.getValue() == null) {
                log.info("组织{}回款计划即将到期提醒配置不存在", organizationId);
                return;
            }
            MessageTaskConfigDTO expiringConfigDTO = JSON.parseObject(expiringConfig.getValue(), MessageTaskConfigDTO.class);
            if (CollectionUtils.isEmpty(expiringConfigDTO.getTimeList())) {
                log.info("组织{}回款计划到期提醒时间配置不存在", organizationId);
                return;
            }
            for (TimeDTO timeDTO : expiringConfigDTO.getTimeList()) {
                if (timeDTO.getTimeUnit().equals("DAY")) {
                    long startTime = LocalDate.now().minusDays(timeDTO.getTimeValue())
                            .atStartOfDay(ZoneId.systemDefault())
                            .toEpochSecond() * 1000;
                    long endTime = LocalDate.now().minusDays(timeDTO.getTimeValue() + 1)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toEpochSecond() * 1000;
                    List<ContractPaymentPlan> paymentPlanList = getContractPaymentPlanList(organizationId, startTime, endTime);
                    if (CollectionUtils.isEmpty(paymentPlanList)) {
                        log.info("组织{}无到期回款计划", organizationId);
                        return;
                    }
                    for (ContractPaymentPlan paymentPlan : paymentPlanList) {
                        //发送通知
                        Contract contract = contractBaseMapper.selectByPrimaryKey(paymentPlan.getContractId());
                        if (contract == null) {
                            log.info("组织{}回款计划{}关联的合同不存在", organizationId, paymentPlan.getId());
                            continue;
                        }
                        Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
                        if (customer == null) {
                            log.info("组织{}回款计划{}关联的客户不存在", organizationId, paymentPlan.getId());
                            continue;
                        }
                        sendNotice(expiringConfigDTO, NotificationConstants.Module.CONTRACT, paymentPlan.getCreateUser(), paymentPlan.getOrganizationId(), NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRING, customer.getName(), paymentPlan.getCreateUser(), paymentPlan.getOwner(), timeDTO.getTimeValue());
                    }
                }
            }
            log.info("组织{}到期回款计划提醒完成", organizationId);
        }
    }

    /**
     * 回款计划到期自动通知
     * <p>
     * 查询已到期的回款计划，并向相关负责人发送通知提醒。
     * </p>
     */
    private void contractPaymentPlanExpiredRemind() {
        log.info("回款计划到期提醒");
        Set<String> organizationIds = extOrganizationMapper.selectAllOrganizationIds();
        for (String organizationId : organizationIds) {
            MessageTask expiredEvent = extMessageTaskMapper.getMessageByModuleAndEvent(NotificationConstants.Module.CONTRACT, NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRED, organizationId);
            if (expiredEvent == null || (!expiredEvent.getDingTalkEnable() && !expiredEvent.getEmailEnable() && !expiredEvent.getSysEnable() && !expiredEvent.getWeComEnable() && !expiredEvent.getLarkEnable())) {
                log.info("组织{}回款计划到期提醒未开启", organizationId);
                return;
            }
            MessageTaskConfig expiredConfig = extMessageTaskConfigMapper.getConfigByModuleAndEvent(NotificationConstants.Module.CONTRACT, NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRED, organizationId);
            if (expiredConfig == null || expiredConfig.getValue() == null) {
                log.info("组织{}回款计划到期提醒配置不存在", organizationId);
                return;
            }
            MessageTaskConfigDTO expiredConfigDTO = JSON.parseObject(expiredConfig.getValue(), MessageTaskConfigDTO.class);
            if (CollectionUtils.isEmpty(expiredConfigDTO.getTimeList())) {
                log.info("组织{}回款计划到期提醒时间配置不存在", organizationId);
                return;
            }
            long timestamp = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toEpochSecond() * 1000;

            long timestampOld = LocalDate.now().minusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toEpochSecond() * 1000;
            List<ContractPaymentPlan> paymentPlanList = getContractPaymentPlanList(organizationId, timestamp, timestampOld);
            if (CollectionUtils.isEmpty(paymentPlanList)) {
                log.info("组织{}无到期回款计划", organizationId);
                return;
            }
            for (ContractPaymentPlan paymentPlan : paymentPlanList) {
                //发送通知
                Contract contract = contractBaseMapper.selectByPrimaryKey(paymentPlan.getContractId());
                if (contract == null) {
                    log.info("组织{}回款计划{}关联的合同不存在", organizationId, paymentPlan.getId());
                    continue;
                }
                Customer customer = customerBaseMapper.selectByPrimaryKey(contract.getCustomerId());
                if (customer == null) {
                    log.info("组织{}回款计划{}关联的客户不存在", organizationId, paymentPlan.getId());
                    continue;
                }
                sendNotice(expiredConfigDTO, NotificationConstants.Module.CONTRACT, paymentPlan.getCreateUser(), paymentPlan.getOrganizationId(), NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRED, customer.getName(), paymentPlan.getCreateUser(), paymentPlan.getOwner(), null);
            }
        }
    }


    /**
     * 发送通知
     *
     * @param messageTaskConfigDTO 消息任务配置DTO
     * @param userId               用户ID
     * @param orgId                组织ID
     * @param event                事件类型
     */
    private void sendNotice(MessageTaskConfigDTO messageTaskConfigDTO, String model, String userId, String orgId, String event, String customerName, String createUser, String owner, Integer days) {
        //查询通知配置的接收范围
        List<String> receiveUserIds = commonNoticeSendService.getNoticeReceiveUserIds(messageTaskConfigDTO, createUser, owner, orgId);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("customerName", customerName);
        paramMap.put("name", customerName);
        if (days != null) {
            paramMap.put("expireDays", days);
        }
        commonNoticeSendService.sendNotice(model, event,
                paramMap, userId, orgId, receiveUserIds, false);
    }
}
