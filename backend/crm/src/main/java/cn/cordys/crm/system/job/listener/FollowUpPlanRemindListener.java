package cn.cordys.crm.system.job.listener;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 跟进计划提醒监听器
 * <p>
 * 该监听器负责监听执行事件，当触发时检查并提醒到期的跟进计划。
 * 支持客户、商机和线索三种类型的跟进计划提醒。
 * </p>
 */
@Component
public class FollowUpPlanRemindListener implements ApplicationListener<ExecuteEvent> {

    @Resource
    private ExtFollowUpPlanMapper extFollowUpPlanMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtClueMapper extClueMapper;

    /**
     * 处理执行事件
     * <p>
     * 当接收到执行事件时，触发跟进计划提醒功能
     * </p>
     *
     * @param event 执行事件对象
     */
    @Override
    public void onApplicationEvent(ExecuteEvent event) {
        try {
            this.followUpPlanRemind();
        } catch (Exception e) {
            LogUtils.error("跟进计划到期提醒异常: ", e.getMessage());
        }
    }

    /**
     * 跟进计划到期自动通知
     * <p>
     * 查询当天到期的跟进计划，并向相关负责人发送通知提醒。
     * </p>
     */
    public void followUpPlanRemind() {
        LogUtils.info("跟进计划到期提醒");
        // 获取当天零点的时间戳（毫秒）
        long timestamp = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond() * 1000;

        // 查询到期的跟进计划列表
        List<FollowUpPlan> planList = extFollowUpPlanMapper.selectPlanByTimestamp(timestamp);

        if (CollectionUtils.isNotEmpty(planList)) {
            // 提取客户、商机和线索的ID列表
            List<String> customerIds = planList.stream().map(FollowUpPlan::getCustomerId).toList();
            List<String> opportunityIds = planList.stream().map(FollowUpPlan::getOpportunityId).toList();
            List<String> clueIds = planList.stream().map(FollowUpPlan::getClueId).toList();

            // 构建客户ID与名称的映射
            Map<String, String> customerMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(customerIds)) {
                customerMap = extCustomerMapper.selectOptionByIds(customerIds)
                        .stream()
                        .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            }

            // 构建商机ID与名称的映射
            Map<String, String> opportunityMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(opportunityIds)) {
                opportunityMap = extOpportunityMapper.getOpportunityOptionsByIds(opportunityIds)
                        .stream()
                        .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            }

            // 构建线索ID与名称的映射
            Map<String, String> clueMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(clueIds)) {
                clueMap = extClueMapper.selectOptionByIds(clueIds)
                        .stream()
                        .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            }

            // 遍历所有到期的跟进计划，发送相应通知
            for (FollowUpPlan followUpPlan : planList) {
                // 发送客户跟进计划提醒
                if (StringUtils.isNotBlank(followUpPlan.getCustomerId())) {
                    commonNoticeSendService.sendNotice(
                            NotificationConstants.Module.CUSTOMER,
                            NotificationConstants.Event.CUSTOMER_FOLLOW_UP_PLAN_DUE,
                            customerMap.get(followUpPlan.getCustomerId()),
                            followUpPlan.getCreateUser(),
                            followUpPlan.getOrganizationId(),
                            List.of(followUpPlan.getOwner()),
                            false
                    );
                }

                // 发送商机跟进计划提醒
                if (StringUtils.isNotBlank(followUpPlan.getOpportunityId())) {
                    commonNoticeSendService.sendNotice(
                            NotificationConstants.Module.OPPORTUNITY,
                            NotificationConstants.Event.BUSINESS_FOLLOW_UP_PLAN_DUE,
                            opportunityMap.get(followUpPlan.getOpportunityId()),
                            followUpPlan.getCreateUser(),
                            followUpPlan.getOrganizationId(),
                            List.of(followUpPlan.getOwner()),
                            false
                    );
                }

                // 发送线索跟进计划提醒
                if (StringUtils.isNotBlank(followUpPlan.getClueId())) {
                    commonNoticeSendService.sendNotice(
                            NotificationConstants.Module.CLUE,
                            NotificationConstants.Event.CLUE_FOLLOW_UP_PLAN_DUE,
                            clueMap.get(followUpPlan.getClueId()),
                            followUpPlan.getCreateUser(),
                            followUpPlan.getOrganizationId(),
                            List.of(followUpPlan.getOwner()),
                            false
                    );
                }
            }
        }

        LogUtils.info("跟进计划到期提醒完成");
    }
}