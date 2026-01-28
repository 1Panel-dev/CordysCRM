package cn.cordys.crm.system.notice.sender;

import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.notice.common.NoticeModel;

public interface NoticeSender {
  void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel);
}
