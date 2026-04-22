package cn.cordys.crm.system.job;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.response.AnnouncementDTO;
import cn.cordys.crm.system.mapper.ExtAnnouncementMapper;
import cn.cordys.crm.system.service.AnnouncementService;
import cn.cordys.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class NotifyOnJob {

    private static final String ANNOUNCE_PREFIX = "announce_content:";

    @Resource
    private ExtAnnouncementMapper extAnnouncementMapper;
    @Resource
    private AnnouncementService announcementService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @QuartzScheduled(cron = "0 0/5 * * * ?")
    public void onEvent() {
        try {
            addNotification();
        } catch (Exception e) {
            log.error("公告通知异常", e);
        }
    }

    public void addNotification() {
        long timestamp = currentEpochMs();
        doAddNotification(timestamp);
    }

    private void doAddNotification(long timestamp) {
        List<AnnouncementDTO> announcements = extAnnouncementMapper.selectInEffectUnConvertData(timestamp);
        if (CollectionUtils.isEmpty(announcements)) return;

        log.info("公告通知数量: {}", announcements.size());
        List<String> ids = new ArrayList<>();

        for (AnnouncementDTO dto : announcements) {
            List<String> userIds = JSON.parseArray(new String(dto.getReceiver()), String.class);
            announcementService.convertNotification("admin", dto, userIds);
            ids.add(dto.getId());
        }

        extAnnouncementMapper.updateNotice(ids, true, announcements.getFirst().getOrganizationId());
        cleanExpiredPushes();
    }

    /**
     * 清理已过期的 Redis 公告推送缓存
     */
    private void cleanExpiredPushes() {
        long now = currentEpochMs();
        long yesterdayStart = LocalDateTime.now().minusDays(1)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<String> expiredIds = extAnnouncementMapper.selectFixTimeExpiredIds(yesterdayStart, now);
        if (CollectionUtils.isEmpty(expiredIds)) return;

        expiredIds.forEach(id -> stringRedisTemplate.delete(ANNOUNCE_PREFIX + id));
    }

    private static long currentEpochMs() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
