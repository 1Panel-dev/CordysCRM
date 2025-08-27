package cn.cordys.crm.system.job;

import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.system.dto.response.AnnouncementDTO;
import cn.cordys.crm.system.mapper.ExtAnnouncementMapper;
import cn.cordys.crm.system.service.AnnouncementService;
import com.fit2cloud.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class NotifyOnJob {
    @Resource
    private ExtAnnouncementMapper extAnnouncementMapper;
    @Resource
    private AnnouncementService announcementService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String ANNOUNCE_PREFIX = "announce_content:";  // Redis 存储信息前缀

    @QuartzScheduled(cron = "0 0/5 * * * ?")
    public void onEvent() {
        try {
            this.addNotification();
        } catch (Exception e) {
            LogUtils.error("公告通知异常: ", e.getMessage());
        }
    }

    /**
     * 将到期发布的公告转成通知
     */
    public void addNotification() {
        LocalDateTime dateTime = LocalDateTime.now();
        long timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        this.doAddNotification(timestamp);
    }

    private void doAddNotification(long timestamp) {
        //查询所有在这个时间内生效的公告
        List<AnnouncementDTO> announcementDTOS = extAnnouncementMapper.selectInEffectUnConvertData(timestamp);
        if (CollectionUtils.isEmpty(announcementDTOS)) {
            return;
        }
        LogUtils.info("公告通知数量: {}", announcementDTOS.size());
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
        if (CollectionUtils.isNotEmpty(expiredIds)) {
            for (String expiredId : expiredIds) {
                stringRedisTemplate.delete(ANNOUNCE_PREFIX + expiredId);
            }
        }
    }

}
