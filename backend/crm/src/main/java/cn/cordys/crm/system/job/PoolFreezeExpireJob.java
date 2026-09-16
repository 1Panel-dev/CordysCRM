package cn.cordys.crm.system.job;

import cn.cordys.crm.clue.service.PoolClueService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PoolFreezeExpireJob {

    @Resource
    private PoolCustomerService poolCustomerService;

    @Resource
    private PoolClueService poolClueService;

    @QuartzScheduled(cron = "0 * * * * ?")
    public void execute() {
        try {
            poolCustomerService.unfreezeExpired();
        } catch (Exception e) {
            log.error("自动解冻公海客户失败", e);
        }
        try {
            poolClueService.unfreezeExpired();
        } catch (Exception e) {
            log.error("自动解冻线索池线索失败", e);
        }
    }
}
