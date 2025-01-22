package io.cordys.crm.system.notice.sender;

import io.cordys.crm.system.notice.message.MessageDetail;
import io.cordys.crm.system.notice.NoticeModel;

public interface NoticeSender {
    void send(MessageDetail messageDetail, NoticeModel noticeModel);
}
