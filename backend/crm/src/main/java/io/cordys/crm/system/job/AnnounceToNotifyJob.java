package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.dto.response.AnnouncementDTO;
import io.cordys.crm.system.mapper.ExtAnnouncementMapper;
import io.cordys.crm.system.service.AnnouncementService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnnounceToNotifyJob {
    @Resource
    private ExtAnnouncementMapper extAnnouncementMapper;
    @Resource
    private AnnouncementService announcementService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String ANNOUNCE_PREFIX = "announce_content:";  // Redis 存储信息前缀

    /**
     * 将到期发布的公告转成通知  每天凌晨三点执行
     */
    @QuartzScheduled(cron = "0 0 3 * * ?")
    public void addNotification() {
        LogUtils.info("add notification start.");
        LocalDateTime dateTime = LocalDateTime.now();
        long timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        this.doAddNotification(timestamp);
        LogUtils.info("add notification end.");
    }

    private void doAddNotification(long timestamp) {
        //查询所有在这个时间内生效的公告
        List<AnnouncementDTO> announcementDTOS = extAnnouncementMapper.selectInEffectUnConvertData(timestamp);
        if (CollectionUtils.isEmpty(announcementDTOS)) {
            return;
        }
        //将公告根据接收人生成相关的通知
        List<String> ids = new ArrayList<>();
        for (AnnouncementDTO announcementDTO : announcementDTOS) {
            List<String> userIds = JSON.parseArray(new String(announcementDTO.getReceiver()), String.class);
            announcementService.convertNotification("admin", announcementDTO, userIds);
            ids.add(announcementDTO.getId());
        }
        extAnnouncementMapper.updateNotice(ids, true, announcementDTOS.getFirst().getOrganizationId());
        //删除已过期公告的推送
        LocalDateTime dateTime = LocalDateTime.now();
        long expiredStamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        LocalDateTime startTime = LocalDateTime.now().minusDays(1L);
        long startStamp = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<String> expiredIds = extAnnouncementMapper.selectFixTimeExpiredIds(startStamp, expiredStamp);
        if (CollectionUtils.isNotEmpty(expiredIds)){
            for (String expiredId : expiredIds) {
                stringRedisTemplate.delete(ANNOUNCE_PREFIX+expiredId);
            }
        }
    }

}
