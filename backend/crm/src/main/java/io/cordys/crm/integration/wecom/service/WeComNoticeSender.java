package io.cordys.crm.integration.wecom.service;

import io.cordys.common.constants.ThirdConstants;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.integration.auth.dto.ThirdConfigurationDTO;
import io.cordys.crm.integration.sso.service.TokenService;
import io.cordys.crm.integration.wecom.dto.Text;
import io.cordys.crm.integration.wecom.dto.WeComSendDTO;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.MessageDetailDTO;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.crm.system.notice.common.NoticeModel;
import io.cordys.crm.system.notice.common.Receiver;
import io.cordys.crm.system.notice.sender.AbstractNoticeSender;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WeComNoticeSender extends AbstractNoticeSender {

    @Resource
    private TokenService tokenService;
    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;
    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;


    @Override
    public void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        String context = super.getContext(messageDetailDTO, noticeModel);
        String subjectText = super.getSubjectText(messageDetailDTO, noticeModel);
        try {
            sendWeCom(context, noticeModel, messageDetailDTO.getOrganizationId(), subjectText);
            LogUtils.debug("发送企业微信结束");
        } catch (Exception e) {
            LogUtils.error("企业微信消息通知失败：" + e);
        }
    }

    public void sendWeCom(MessageDetailDTO clonedMessageDetail, NoticeModel clonedNoticeModel) {
        this.send(clonedMessageDetail, clonedNoticeModel);
    }

    private void sendWeCom(String context, NoticeModel noticeModel, String organizationId, String subjectText) {
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
            LogUtils.warn("没有同步企业微信用户，无法发送消息");
            return;
        }
        //查询三方配置
        OrganizationConfig organizationConfig = extOrganizationConfigMapper
                .getOrganizationConfig(organizationId, OrganizationConfigConstants.ConfigType.THIRD.name());
        if (organizationConfig == null) {
            LogUtils.warn("没有配置企业微信信息，无法发送消息");
            return;
        }
        //获取企业微信通知的配置数据
        OrganizationConfigDetail orgConfigDetailByIdAndType = extOrganizationConfigDetailMapper.getOrgConfigDetailByIdAndType(organizationConfig.getId(), ThirdConstants.ThirdDetailType.WECOM_NOTICE.toString());
        if (orgConfigDetailByIdAndType == null || orgConfigDetailByIdAndType.getContent() == null) {
            LogUtils.warn("没有配置企业微信通知信息，无法发送消息");
            return;
        }
        ThirdConfigurationDTO thirdConfigurationDTO = JSON.parseObject(
                new String(orgConfigDetailByIdAndType.getContent()), ThirdConfigurationDTO.class
        );

        //构建企业微信消息
        WeComSendDTO weComSendDTO = new WeComSendDTO();
        String result = String.join("|", resourceUserIds);
        weComSendDTO.setTouser(result);
        weComSendDTO.setAgentid(Integer.parseInt(thirdConfigurationDTO.getAgentId()));
        weComSendDTO.setText(new Text(context));
        weComSendDTO.setMsgtype("text");

        tokenService.sendNoticeByToken(weComSendDTO, thirdConfigurationDTO.getCorpId(), thirdConfigurationDTO.getAppSecret());
    }
}
