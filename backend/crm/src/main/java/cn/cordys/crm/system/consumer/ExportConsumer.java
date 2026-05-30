package cn.cordys.crm.system.consumer;

import cn.cordys.common.constants.TopicConstants;
import cn.cordys.common.redis.TopicConsumer;
import cn.cordys.registry.ExportThreadRegistry;
import org.springframework.stereotype.Component;

@Component
class ExportConsumer implements TopicConsumer {

    @Override
    public String getChannel() {
        return TopicConstants.DOWNLOAD_TOPIC;
    }

    @Override
    public void consume(String message) {
        ExportThreadRegistry.stop(message);
    }
}
