package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.follow.domain.FollowUpPlan;
import io.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.notice.CommonNoticeSendService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FollowUpPlanRemindJob {

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
     * 跟进计划到期自动通知
     */
    @QuartzScheduled(cron = "0 0 0 * * ?")
    public void followUpPlanRemind() {
        LogUtils.info("follow up plan remind job start");
        long timestamp = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond() * 1000;

        List<FollowUpPlan> planList = extFollowUpPlanMapper.selectPlanByTimestamp(timestamp);

        if (CollectionUtils.isNotEmpty(planList)) {
            List<String> customerIds = planList.stream().map(FollowUpPlan::getCustomerId).toList();
            List<String> opportunityIds = planList.stream().map(FollowUpPlan::getOpportunityId).toList();
            List<String> clueIds = planList.stream().map(FollowUpPlan::getClueId).toList();

            Map<String, String> customerMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(customerIds)) {
                customerMap = extCustomerMapper.selectOptionByIds(customerIds)
                        .stream()
                        .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

            }

            Map<String, String> opportunityMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(opportunityIds)) {
                opportunityMap = extOpportunityMapper.getOpportunityOptionsByIds(opportunityIds)
                        .stream()
                        .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

            }

            Map<String, String> clueMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(clueIds)) {
                clueMap = extClueMapper.selectOptionByIds(clueIds)
                        .stream()
                        .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

            }

            for (FollowUpPlan followUpPlan : planList) {
                if (StringUtils.isNotBlank(followUpPlan.getCustomerId())) {
                    // 发送消息
                    commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                            NotificationConstants.Event.CUSTOMER_FOLLOW_UP_PLAN_DUE, customerMap.get(followUpPlan.getCustomerId()), followUpPlan.getCreateUser(),
                            followUpPlan.getOrganizationId(), List.of(followUpPlan.getOwner()), false);
                }

                if (StringUtils.isNotBlank(followUpPlan.getOpportunityId())) {
                    // 发送消息
                    commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                            NotificationConstants.Event.BUSINESS_FOLLOW_UP_PLAN_DUE, opportunityMap.get(followUpPlan.getOpportunityId()), followUpPlan.getCreateUser(),
                            followUpPlan.getOrganizationId(), List.of(followUpPlan.getOwner()), false);
                }

                if (StringUtils.isNotBlank(followUpPlan.getClueId())) {
                    // 发送消息
                    commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                            NotificationConstants.Event.CLUE_FOLLOW_UP_PLAN_DUE, clueMap.get(followUpPlan.getClueId()), followUpPlan.getCreateUser(),
                            followUpPlan.getOrganizationId(), List.of(followUpPlan.getOwner()), false);
                }
            }
        }


        LogUtils.info("follow up plan remind job end");
    }

}
