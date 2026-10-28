package cn.cordys.common.interceptor;

import cn.cordys.common.util.CompressUtils;
import cn.cordys.config.MybatisInterceptorConfig;
import cn.cordys.crm.contract.domain.ContractInvoiceSnapshot;
import cn.cordys.crm.contract.domain.ContractSnapshot;
import cn.cordys.crm.opportunity.domain.OpportunityQuotationSnapshot;
import cn.cordys.crm.system.domain.Announcement;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.domain.OperationLogBlob;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统拦截器配置类
 * <p>
 * 该类用于配置 MyBatis 的拦截器，特别是字段压缩等功能的配置。
 * </p>
 */
@Configuration
public class SystemInterceptor {

    /**
     * 配置系统拦截器列表
     *
     * @return 返回 MyBatis 拦截器配置列表，目前支持字段压缩等功能。
     */
    @Bean
    public List<MybatisInterceptorConfig> systemCompressConfigs() {
        List<MybatisInterceptorConfig> configList = new ArrayList<>();

        configList
                .add(new MybatisInterceptorConfig(MessageTask.class, "template", CompressUtils.class, "zip", "unzip"));
        configList
                .add(new MybatisInterceptorConfig(Announcement.class, "content", CompressUtils.class, "zip", "unzip"));
        configList
                .add(new MybatisInterceptorConfig(Announcement.class, "receiver", CompressUtils.class, "zip", "unzip"));
        configList.add(
                new MybatisInterceptorConfig(Announcement.class, "receiveType", CompressUtils.class, "zip", "unzip"));
        configList.add(new MybatisInterceptorConfig(OrganizationConfigDetail.class, "content", CompressUtils.class,
                "zip", "unzip"));
        configList
                .add(new MybatisInterceptorConfig(Notification.class, "content", CompressUtils.class, "zip", "unzip"));

        configList.add(new MybatisInterceptorConfig(OperationLogBlob.class, "originalValue", CompressUtils.class, "zip",
                "unzip"));

        configList.add(new MybatisInterceptorConfig(OpportunityQuotationSnapshot.class, "quotationProp",
                CompressUtils.class, "zipString", "unzipString"));
        configList.add(new MybatisInterceptorConfig(ContractSnapshot.class, "contractProp", CompressUtils.class,
                "zipString", "unzipString"));
        configList.add(new MybatisInterceptorConfig(ContractInvoiceSnapshot.class, "invoiceProp", CompressUtils.class,
                "zipString", "unzipString"));

        registerAgentTraceEventCompression(configList);
        return configList;
    }

    /**
     * 注册 agent_trace_event 表 BLOB 字段（prompt / trace）的压缩配置。
     * <p>
     * AgentTraceEvent 类位于 Xpack 模块，本类位于 crm 模块，故采用
     * {@link Class#forName(String)} 按全限定名运行时解析；找不到时跳过注册，
     * 不影响其他拦截器。
     * </p>
     */
    private static void registerAgentTraceEventCompression(List<MybatisInterceptorConfig> configList) {
        try {
            Class<?> agentTraceEventClass = Class.forName("cn.cordys.crm.integration.agent.domain.AgentTraceEvent");
            configList.add(
                    new MybatisInterceptorConfig(agentTraceEventClass, "prompt", CompressUtils.class, "zip", "unzip"));
            configList.add(
                    new MybatisInterceptorConfig(agentTraceEventClass, "trace", CompressUtils.class, "zip", "unzip"));
        } catch (ClassNotFoundException e) {
            // Xpack 未引入，AgentTraceEvent 不在类路径上，跳过注册。
        }
    }
}
