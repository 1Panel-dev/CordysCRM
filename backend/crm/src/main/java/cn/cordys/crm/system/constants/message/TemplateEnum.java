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
public enum TemplateEnum {
    //客户
    CUSTOMER_ADD_TEXT("message.customer_add_text"),
    CUSTOMER_UPDATE_TEXT("message.customer_update_text"),
    CUSTOMER_DELETED_TEXT("message.customer_deleted_text"),
    CUSTOMER_COLLABORATION_ADD_TEXT("message.customer_collaboration_add_text"),
    CUSTOMER_COLLABORATION_ADD_MANUAL_TEXT("message.customer_collaboration_add_manual_text"),
    //转移客户 2条
    CUSTOMER_TRANSFERRED_CUSTOMER_OWNER_TEXT("message.customer_transferred_customer_owner_text"),
    CUSTOMER_TRANSFERRED_CUSTOMER_OLD_OWNER_TEXT("message.customer_transferred_customer_old_owner_text"),

    CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS_TEXT("message.customer_automatic_move_high_seas_text"),
    CUSTOMER_MOVED_HIGH_SEAS_TEXT("message.customer_moved_high_seas_text"),
    CUSTOMER_RECEIVE_TEXT("message.customer_receive_text"),
    HIGH_SEAS_CUSTOMER_DISTRIBUTED_TEXT("message.high_seas_customer_distributed_text"),
    CUSTOMER_APPROVAL_TEXT("message.customer_approval_text"),
    CUSTOMER_CONCAT_ADD_TEXT("message.customer_concat_add_text"),
    CUSTOMER_CONCAT_UPDATE_TEXT("message.customer_concat_update_text"),
    CUSTOMER_CONCAT_DELETE_TEXT("message.customer_concat_delete_text"),

    //线索
    CLUE_ADD_TEXT("message.clue_add_text"),
    CLUE_UPDATE_TEXT("message.clue_update_text"),
    CLUE_DELETED_TEXT("message.clue_deleted_text"),
    CLUE_AUTOMATIC_MOVE_POOL_TEXT("message.clue_automatic_move_pool_text"),
    CLUE_MOVED_POOL_TEXT("message.clue_moved_pool_text"),
    //转客户 2条
    CLUE_CONVERT_CUSTOMER_ADD_TEXT("message.clue_convert_customer_add_text"),
    CLUE_CONVERT_CUSTOMER_RELATE_TEXT("message.clue_convert_customer_relate_text"),
    CLUE_CONVERT_BUSINESS_TEXT("message.clue_convert_business_text"),
    //转线索两条
    TRANSFER_CLUE_OWNER_TEXT("message.transfer_clue_owner_text"),
    TRANSFER_CLUE_OLD_OWNER_TEXT("message.transfer_clue_old_owner_text"),
    CLUE_DISTRIBUTED_TEXT("message.clue_distributed_text"),
    CLUE_RECEIVE_TEXT("message.clue_receive_text"),
    CLUE_APPROVAL_TEXT("message.clue_approval_text"),


    //商机
    BUSINESS_ADD_TEXT("message.business_add_text"),
    BUSINESS_UPDATE_TEXT("message.business_update_text"),
    BUSINESS_DELETED_TEXT("message.business_deleted_text"),
    //转移 两条
    BUSINESS_TRANSFER_OLD_OWNER_TEXT("message.business_transfer_old_owner_text"),
    BUSINESS_TRANSFER_OWNER_TEXT("message.business_transfer_owner_text"),
    BUSINESS_APPROVAL_TEXT("message.business_approval_text"),
    BUSINESS_QUOTATION_ADD_TEXT("message.business_quotation_add_text"),
    BUSINESS_QUOTATION_UPDATE_TEXT("message.business_quotation_update_text"),
    BUSINESS_QUOTATION_DELETED_TEXT("message.business_quotation_deleted_text"),
    BUSINESS_QUOTATION_EXPIRED_TEXT("message.business_quotation_expired_text"),
    BUSINESS_QUOTATION_EXPIRING_TEXT("message.business_quotation_expiring_text"),
    BUSINESS_QUOTATION_APPROVAL_TEXT("message.business_quotation_approval_text"),

    //订单
    ORDER_ADD_TEXT("message.order_add_text"),
    ORDER_UPDATE_TEXT("message.order_update_text"),
    ORDER_DELETE_TEXT("message.order_delete_text"),
    ORDER_APPROVAL_TEXT("message.order_approval_text"),

    //合同
    CONTRACT_ADD_TEXT("message.contract_add_text"),
    CONTRACT_UPDATE_TEXT("message.contract_update_text"),
    CONTRACT_DELETE_TEXT("message.contract_delete_text"),
    CONTRACT_VOID_TEXT("message.contract_void_text"),
    CONTRACT_EXPIRED_TEXT("message.contract_expired_text"),
    CONTRACT_EXPIRING_TEXT("message.contract_expiring_text"),
    CONTRACT_APPROVAL_TEXT("message.contract_approval_text"),
    CONTRACT_PAYMENT_ADD_TEXT("message.contract_payment_add_text"),
    CONTRACT_PAYMENT_UPDATE_TEXT("message.contract_payment_update_text"),
    CONTRACT_PAYMENT_DELETE_TEXT("message.contract_payment_delete_text"),
    CONTRACT_PAYMENT_EXPIRED_TEXT("message.contract_payment_expired_text"),
    CONTRACT_PAYMENT_EXPIRING_TEXT("message.contract_payment_expiring_text"),
    CONTRACT_RECORD_ADD_TEXT("message.contract_record_add_text"),
    CONTRACT_RECORD_UPDATE_TEXT("message.contract_record_update_text"),
    CONTRACT_RECORD_DELETE_TEXT("message.contract_record_delete_text"),
    INVOICE_ADD_TEXT("message.invoice_add_text"),
    INVOICE_UPDATE_TEXT("message.invoice_update_text"),
    INVOICE_DELETE_TEXT("message.invoice_delete_text"),
    INVOICE_APPROVAL_TEXT("message.invoice_approval_text"),

    //跟进
    FOLLOW_UP_RECORD_ADD_TEXT("message.follow_up_record_add_text"),
    FOLLOW_UP_RECORD_UPDATE_TEXT("message.follow_up_record_update_text"),
    FOLLOW_UP_RECORD_DELETE_TEXT("message.follow_up_record_delete_text"),
    FOLLOW_UP_RECORD_COMMENT_TEXT("message.follow_up_record_comment_text"),
    FOLLOW_UP_RECORD_COMMENT_REPALY_TEXT("message.follow_up_record_comment_repaly_text"),
    FOLLOW_UP_RECORD_COMMENT_MENTIONED_TEXT("message.follow_up_record_comment_mentioned_text"),
    FOLLOW_UP_PLAN_ADD_TEXT("message.follow_up_plan_add_text"),
    FOLLOW_UP_PLAN_UPDATE_TEXT("message.follow_up_plan_update_text"),
    FOLLOW_UP_PLAN_DELETE_TEXT("message.follow_up_plan_delete_text"),
    FOLLOW_UP_PLAN_COMMENT_TEXT("message.follow_up_plan_comment_text"),
    FOLLOW_UP_PLAN_COMMENT_REPALY_TEXT("message.follow_up_plan_comment_repaly_text"),
    FOLLOW_UP_PLAN_COMMENT_MENTIONED_TEXT("message.follow_up_plan_comment_mentioned_text"),
    FOLLOW_UP_PLAN_DUE_TEXT("message.follow_up_plan_due_text"),
    APPROVAL_TODO_TEXT("message.approval_todo_text"),
    APPROVAL_CC_TEXT("message.approval_cc_text"),
    APPROVAL_COMMENT_TEXT("message.approval_comment_text"),
    APPROVAL_COMMENT_REPALY_TEXT("message.approval_comment_repaly_text"),
    APPROVAL_COMMENT_MENTIONED_TEXT("message.approval_comment_mentioned_text"),

    //自定义表单
    CUSTOM_ADD_TEXT("message.custom_add_text"),
    CUSTOM_UPDATE_TEXT("message.custom_update_text"),
    CUSTOM_DELETED_TEXT("message.custom_deleted_text"),
    CUSTOM_APPROVAL_TEXT("message.custom_approval_text");

    private final String description;

    private static final Map<String, TemplateEnum> TEMPLATE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    TemplateEnum(String description) {
        this.description = description;
    }


    public static TemplateEnum get(String scopeId) {
        return TEMPLATE_MAP.get(scopeId);
    }
}
