package cn.cordys.crm.system.constants.message;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息通知  接收人id枚举
 */
@Getter
public enum ScopeIdTypeEnum {

    OWNER("message.scope.owner"),                              //记录所有人
    OLD_OWNER("message.scope.old_owner"),                      //(前)记录所有人
    COLLABORATION("message.scope.collaboration"),              //协作人
    CUSTOMER_SEAS_OWNER("message.scope.customer_seas_owner"),  //公海管理员
    SUBMITTER("message.scope.submitter"),                      //审批发起人
    CLUE_SEAS_OWNER("message.scope.clue_seas_owner"),          //线索池管理员
    REPLY_USER("message.scope.reply_user"),                    //被回复的人
    MENTION_USER("message.scope.mention_user"),                //被@的人
    COMMANDER("message.scope.commander"),                      //部门负责人
    SUPERVISOR("message.scope.supervisor");                    //直属上级

    private final String description;

    private static final Map<String, ScopeIdTypeEnum> SCOPE_ID_TYPE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    ScopeIdTypeEnum(String description) {
        this.description = description;
    }

    /**
     * 根据接收人ID获取枚举，非特殊接收人(用户/角色/部门)返回 null
     */
    public static ScopeIdTypeEnum get(String scopeId) {
        return SCOPE_ID_TYPE_MAP.get(scopeId);
    }
}
