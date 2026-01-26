package cn.cordys.crm.integration.wecom.service;

import cn.cordys.common.constants.ThirdDetailType;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.WecomThirdConfigRequest;
import cn.cordys.crm.integration.sso.service.TokenService;
import cn.cordys.crm.integration.wecom.dto.Text;
import cn.cordys.crm.integration.wecom.dto.WeComSendDTO;
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

@Component
@Slf4j
public class WeComNoticeSender extends AbstractNoticeSender {

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
            sendWeCom(context, noticeModel, messageDetailDTO.getOrganizationId());
            log.debug("发送企业微信结束");
        } catch (Exception e) {
            log.error("企业微信消息通知失败：{}", String.valueOf(e));
        }
    }

    public void sendWeCom(MessageDetailDTO clonedMessageDetail, NoticeModel clonedNoticeModel) {
        this.send(clonedMessageDetail, clonedNoticeModel);
    }

    private void sendWeCom(String context, NoticeModel noticeModel, String organizationId) {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        List<String> userIds = receivers.stream()
                .map(Receiver::getUserId)
                .distinct()
                .toList();
        //查询同步过的resourceId，这里已经确定是否同步了企业微信，没同步，消息无法发送
        List<String> resourceUserIds = super.getResourceUserIds(userIds, organizationId);
        if (CollectionUtils.isEmpty(resourceUserIds)) {
            log.warn("没有同步企业微信用户，无法发送消息");
            return;
        }
        //查询三方配置
        OrganizationConfig organizationConfig = extOrganizationConfigMapper
                .getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
        if (organizationConfig == null) {
            log.warn("没有配置企业微信信息，无法发送消息");
            return;
        }
        //获取企业微信通知的配置数据
        OrganizationConfigDetail orgConfigDetailByIdAndType = extOrganizationConfigDetailMapper.getOrgConfigDetailByIdAndType(organizationConfig.getId(), ThirdDetailType.WECOM_SYNC.toString());
        if (orgConfigDetailByIdAndType == null || orgConfigDetailByIdAndType.getContent() == null) {
            log.warn("没有配置企业微信通知信息，无法发送消息");
            return;
        }
        var thirdConfigurationDTO = JSON.parseObject(
                new String(orgConfigDetailByIdAndType.getContent()), ThirdConfigBaseDTO.class
        );
        WecomThirdConfigRequest wecomThirdConfigRequest = new WecomThirdConfigRequest();
        if (thirdConfigurationDTO.getConfig() == null) {
            wecomThirdConfigRequest = JSON.parseObject(new String(orgConfigDetailByIdAndType.getContent()), WecomThirdConfigRequest.class);
        } else {
            wecomThirdConfigRequest = JSON.MAPPER.convertValue(thirdConfigurationDTO.getConfig(), WecomThirdConfigRequest.class);
        }

        //构建企业微信消息
        WeComSendDTO weComSendDTO = new WeComSendDTO();
        String result = String.join("|", resourceUserIds);
        weComSendDTO.setTouser(result);
        weComSendDTO.setAgentid(Integer.parseInt(wecomThirdConfigRequest.getAgentId()));
        weComSendDTO.setText(new Text(context));
        weComSendDTO.setMsgtype("text");

        tokenService.sendNoticeByToken(weComSendDTO, wecomThirdConfigRequest.getCorpId(), wecomThirdConfigRequest.getAppSecret());
    }
}
