package io.cordys.crm.system.consumer;

import io.cordys.common.constants.TopicConstants;
import org.springframework.stereotype.Component;

@Component
class ExportConsumer implements TopicConsumer {

    @Override
    public String getChannel() {
        return TopicConstants.DOWNLOAD_TOPIC;
    }

    @Override
    public void consume(String message) {

        //sseService.broadcastPeriodically(userId, NotificationConstants.Status.READ.toString());
    }
}
