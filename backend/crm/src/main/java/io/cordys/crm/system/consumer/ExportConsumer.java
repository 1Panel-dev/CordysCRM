package io.cordys.crm.system.consumer;

import io.cordys.common.constants.TopicConstants;
import io.cordys.crm.system.service.ExportTaskService;
import io.cordys.registry.ExportThreadRegistry;
import jakarta.annotation.Resource;
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
