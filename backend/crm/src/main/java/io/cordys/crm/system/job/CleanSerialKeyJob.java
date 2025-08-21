package io.cordys.crm.system.job;

import com.fit2cloud.quartz.anno.QuartzScheduled;
import io.cordys.common.util.LogUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

@Component
public class CleanSerialKeyJob {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @QuartzScheduled(cron = "0 0 1 1,16 * ?")
    public void clean() {
        LogUtils.info("清理流水号过期Key");
        try {
            Set<String> keys = stringRedisTemplate.keys("serial:*:*:*");
            for (String key : keys) {
                String date = new SimpleDateFormat("yyyyMM").format(new Date());
                String serialDate = key.substring(key.lastIndexOf(":") + 1);
                if (!Strings.CS.equals(date, serialDate)) {
                    stringRedisTemplate.delete(key);
                }
            }
        } catch (Exception e) {
            LogUtils.error("流水号过期Key清理异常: ", e.getMessage());
        }
        LogUtils.info("流水号过期Key清理完成");
    }

}
