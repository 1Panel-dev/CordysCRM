package cn.cordys.crm.system.job;

import cn.cordys.common.schedule.BaseScheduleJob;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.sync.service.ThirdDepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;

@Slf4j
public class SyncUserScheduleJob extends BaseScheduleJob {


    @Override
    protected void businessExecute(JobExecutionContext context) {
        ThirdDepartmentService thirdDepartmentService = CommonBeanFactory.getBean(ThirdDepartmentService.class);
        assert thirdDepartmentService != null;
        List<String> departmentIds;
        if (StringUtils.isNotBlank(context.getJobDetail().getJobDataMap().getString("config"))) {
            departmentIds = JSON.parseObject(context.getJobDetail().getJobDataMap().getString("config"), List.class);
        } else {
            departmentIds = null;
        }
        String orgId = context.getJobDetail().getJobDataMap().getString("organizationId");
        String resourceType = context.getJobDetail().getJobDataMap().getString("resourceType");
        log.info("同步组织架构任务开始执行. 部门ID：" + departmentIds.toString());
        Locale locale = LocaleContextHolder.getLocale();
        Thread.startVirtualThread(() ->
                thirdDepartmentService.syncUserAndDepartment(departmentIds, userId, orgId, resourceType, locale)
        );
    }


    public static JobKey getJobKey(String resourceId) {
        return new JobKey(resourceId, SyncUserScheduleJob.class.getName());
    }

    public static TriggerKey getTriggerKey(String resourceId) {
        return new TriggerKey(resourceId, SyncUserScheduleJob.class.getName());
    }
}
