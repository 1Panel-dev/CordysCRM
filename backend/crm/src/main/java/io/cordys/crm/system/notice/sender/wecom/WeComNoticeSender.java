package io.cordys.crm.system.notice.sender.wecom;


import io.cordys.crm.system.dto.MessageDetailDTO;
import io.cordys.crm.system.notice.common.NoticeModel;

public interface WeComNoticeSender {

    void sendWeCom(MessageDetailDTO clonedMessageDetail, NoticeModel clonedNoticeModel);
}
