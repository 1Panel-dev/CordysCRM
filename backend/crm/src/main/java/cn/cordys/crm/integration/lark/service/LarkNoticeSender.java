package cn.cordys.crm.integration.lark.service;

import cn.cordys.common.constants.ThirdDetailType;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.LarkThirdConfigRequest;
import cn.cordys.crm.integration.lark.dto.LarkSendMessageDTO;
import cn.cordys.crm.integration.lark.dto.LarkTextDTO;
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
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class LarkNoticeSender extends AbstractNoticeSender {

    @Resource
    private TokenService tokenService;
    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;
    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    @Override
    public void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        try {
            String context = super.getContext(messageDetailDTO, noticeModel);
            sendLark(context, noticeModel, messageDetailDTO.getOrganizationId());
            log.debug("发送飞书消息结束");
        } catch (Exception e) {
            log.error("飞书消息通知失败：{}", String.valueOf(e));
        }
    }

    public void sendLark(MessageDetailDTO clonedMessageDetail, NoticeModel clonedNoticeModel) {
        this.send(clonedMessageDetail, clonedNoticeModel);
    }

    private void sendLark(String context, NoticeModel noticeModel, String organizationId) {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        List<String> userIds = receivers.stream()
                .map(Receiver::getUserId)
                .distinct()
                .toList();
        //查询同步过的resourceId，这里已经确定是否同步了飞书，没同步，消息无法发送
        List<String> resourceUserIds = super.getResourceUserIds(userIds, organizationId);
        if (CollectionUtils.isEmpty(resourceUserIds)) {
            log.warn("没有同步飞书用户，无法发送消息");
            return;
        }
        //查询三方配置
        OrganizationConfig organizationConfig = extOrganizationConfigMapper
                .getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
        if (organizationConfig == null) {
            log.warn("没有配置飞书信息，无法发送消息");
            return;
        }
        //获取通知的配置数据
        OrganizationConfigDetail orgConfigDetailByIdAndType = extOrganizationConfigDetailMapper.getOrgConfigDetailByIdAndType(organizationConfig.getId(), ThirdDetailType.LARK_SYNC.name());
        if (orgConfigDetailByIdAndType == null || orgConfigDetailByIdAndType.getContent() == null) {
            log.warn("没有配置飞书通知信息，无法发送消息");
            return;
        }
        ThirdConfigBaseDTO<?> thirdConfigurationDTO = JSON.parseObject(
                new String(orgConfigDetailByIdAndType.getContent()), ThirdConfigBaseDTO.class
        );

        LarkThirdConfigRequest larkThirdConfigRequest =
                Optional.ofNullable(thirdConfigurationDTO.getConfig())
                        .map(cfg -> JSON.MAPPER.convertValue(cfg, LarkThirdConfigRequest.class))
                        .orElseGet(() -> JSON.parseObject(
                                new String(orgConfigDetailByIdAndType.getContent()),
                                LarkThirdConfigRequest.class
                        ));

        for (String resourceUserId : resourceUserIds) {
            log.info("发送飞书消息，飞书用户ID：{}", resourceUserId);
            LarkSendMessageDTO larkSendMessageDTO = new LarkSendMessageDTO();
            larkSendMessageDTO.setReceive_id(resourceUserId);
            larkSendMessageDTO.setMsg_type("text");
            LarkTextDTO larkTextDTO = new LarkTextDTO();
            larkTextDTO.setText(context);
            larkSendMessageDTO.setContent(JSON.toJSONString(larkTextDTO));
            tokenService.sendLarkNoticeByToken(larkSendMessageDTO, larkThirdConfigRequest.getAgentId(), larkThirdConfigRequest.getAppSecret());
        }

    }
}
