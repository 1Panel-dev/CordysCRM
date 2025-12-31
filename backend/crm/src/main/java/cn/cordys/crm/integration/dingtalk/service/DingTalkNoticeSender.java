package cn.cordys.crm.integration.dingtalk.service;

import cn.cordys.common.constants.ThirdConstants;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.DingTalkThirdConfigRequest;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkMsgDTO;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkSendDTO;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkTextDTO;
import cn.cordys.crm.integration.sso.service.TokenService;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.notice.sender.AbstractNoticeSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DingTalkNoticeSender extends AbstractNoticeSender {

    @Resource
    private TokenService tokenService;
    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;
    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    @Override
    public void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        String context = super.getContext(messageDetailDTO, noticeModel);
        try {
            sendDingTalk(context, noticeModel, messageDetailDTO.getOrganizationId());
            LogUtils.debug("发送钉钉消息结束");
        } catch (Exception e) {
            LogUtils.error("钉钉消息通知失败：" + e);
        }
    }

    public void sendDingTalk(MessageDetailDTO clonedMessageDetail, NoticeModel clonedNoticeModel) {
        this.send(clonedMessageDetail, clonedNoticeModel);
    }

    private void sendDingTalk(String context, NoticeModel noticeModel, String organizationId) {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        List<String> userIds = receivers.stream()
                .map(Receiver::getUserId)
                .distinct()
                .toList();
        //查询同步过的resourceId，这里已经确定是否同步了钉钉，没同步，消息无法发送
        List<String> resourceUserIds = super.getResourceUserIds(userIds, organizationId);
        if (CollectionUtils.isEmpty(resourceUserIds)) {
            LogUtils.warn("没有同步钉钉用户，无法发送消息");
            return;
        }
        //查询三方配置
        OrganizationConfig organizationConfig = extOrganizationConfigMapper
                .getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
        if (organizationConfig == null) {
            LogUtils.warn("没有配置钉钉信息，无法发送消息");
            return;
        }
        //获取企业微信通知的配置数据
        OrganizationConfigDetail orgConfigDetailByIdAndType = extOrganizationConfigDetailMapper.getOrgConfigDetailByIdAndType(organizationConfig.getId(), ThirdConstants.ThirdDetailType.DINGTALK_SYNC.toString());
        if (orgConfigDetailByIdAndType == null || orgConfigDetailByIdAndType.getContent() == null) {
            LogUtils.warn("没有配置钉钉通知信息，无法发送消息");
            return;
        }
        ThirdConfigBaseDTO thirdConfigurationDTO = JSON.parseObject(
                new String(orgConfigDetailByIdAndType.getContent()), ThirdConfigBaseDTO.class
        );
        DingTalkThirdConfigRequest dingTalkThirdConfigRequest = new DingTalkThirdConfigRequest();
        if (thirdConfigurationDTO.getConfig() == null) {
            dingTalkThirdConfigRequest = JSON.parseObject(new String(orgConfigDetailByIdAndType.getContent()), DingTalkThirdConfigRequest.class);
        } else {
            dingTalkThirdConfigRequest = JSON.MAPPER.convertValue(thirdConfigurationDTO.getConfig(), DingTalkThirdConfigRequest.class);
        }

        //构建钉钉消息
        DingTalkSendDTO dingTalkSendDTO = new DingTalkSendDTO();
        String result = String.join(",", resourceUserIds);
        dingTalkSendDTO.setUserid_list(result);
        dingTalkSendDTO.setTo_all_user("false");
        dingTalkSendDTO.setAgent_id(dingTalkThirdConfigRequest.getAppId());
        dingTalkSendDTO.setMsg(new DingTalkMsgDTO());
        dingTalkSendDTO.getMsg().setText(new DingTalkTextDTO(context));
        dingTalkSendDTO.getMsg().setMsgtype("text");


        tokenService.sendDingNoticeByToken(dingTalkSendDTO, dingTalkThirdConfigRequest.getAgentId(), dingTalkThirdConfigRequest.getAppSecret());
    }
}
