package cn.cordys.crm.integration.dingtalk.dto;

import lombok.Data;

@Data
public class DingTalkSendDTO {

private String agent_id;
private String userid_list;
private String to_all_user;
private DingTalkMsgDTO msg;
}
