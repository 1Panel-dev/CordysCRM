package io.cordys.crm.system.notice.sender;

import io.cordys.crm.system.dto.MessageDetailDTO;
import io.cordys.crm.system.notice.common.NoticeModel;

public interface NoticeSender {
    void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel);
}
