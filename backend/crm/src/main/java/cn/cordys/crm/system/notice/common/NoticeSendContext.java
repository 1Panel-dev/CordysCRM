package cn.cordys.crm.system.notice.common;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 新版消息通知发送上下文
 */
@Data
@Builder
public class NoticeSendContext implements Serializable {

    /**
     * 任务类型：系统表单为模块(如 CUSTOMER)，自定义表单为表单ID
     */
    private String taskType;

    /**
     * 本次触发动作(如 CUSTOMER_UPDATE)
     */
    private String action;

    /**
     * 操作人
     */
    private String operatorId;

    /**
     * 组织ID
     */
    private String organizationId;

    /**
     * 业务资源ID
     */
    private String resourceId;

    /**
     * 业务资源名称
     */
    private String resourceName;

    /**
     * 模板附加参数
     */
    private Map<String, Object> paramMap;

    /**
     * 本次发送使用的模板名单：非空时优先于任务配置的全部模板，仅发送这些模板
     */
    private List<String> templateNames;

    /**
     * 本次操作涉及的协作人ID，如客户新增的协作人
     */
    private List<String> collaborationUserIds;

    /**
     * 待办任务的审批人ID，供 APPROVER 接收人解析
     */
    private List<String> approverIds;

    /**
     * 抄送人ID，供 CC 接收人解析
     */
    private List<String> ccUserIds;

    /**
     * 更新前的字段值(fieldId -> value)
     */
    private Map<String, Object> oldValues;

    /**
     * 更新后的字段值(fieldId -> value)
     */
    private Map<String, Object> newValues;

    /**
     * 是否排除操作人本人
     */
    @Builder.Default
    private boolean excludeSelf = true;

    /**
     * 是否仅按本次事件发送：
     * true 时只取出 event 与 action 一致的任务直接发送，不再逐一比较触发配置；
     * false 时按现有逻辑比较所有任务的触发配置后发送。
     */
    @Builder.Default
    private boolean eventOnly = false;
}
