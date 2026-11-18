package cn.cordys.crm.system.constants;

import io.swagger.v3.oas.annotations.media.Schema;

public class NotificationConstants {

    public enum Type {
        ANNOUNCEMENT_NOTICE, SYSTEM_NOTICE
    }

    public enum Status {
        READ, UNREAD
    }

    public interface SendType {
        String MAIL = "MAIL";
        String IN_SITE = "IN_SITE";
    }

    public interface Module {
        //客户管理
        @Schema(description = "message.customer")
        String CUSTOMER = "CUSTOMER";
        //客户管理
        @Schema(description = "message.approval")
        String APPROVAL = "APPROVAL";
        //线索管理
        @Schema(description = "message.clue")
        String CLUE = "CLUE";
        //商机管理
        @Schema(description = "message.business")
        String OPPORTUNITY = "OPPORTUNITY";
        //公告
        @Schema(description = "message.announcement")
        String ANNOUNCEMENT = "ANNOUNCEMENT";
        //合同管理
        @Schema(description = "message.contract")
        String CONTRACT = "CONTRACT";
        //订单管理
        @Schema(description = "message.order")
        String ORDER = "ORDER";
        //系统
        @Schema(description = "message.system")
        String SYSTEM = "SYSTEM";

        @Schema(description = "message.follow_up")
        String FOLLOW_UP = "FOLLOW_UP";
        @Schema(description = "message.custom_form")
        String CUSTOM_FORM = "CUSTOM_FORM";

    }

    public interface Event {
        // -----客户管理----
        //新建客户
        @Schema(description = "message.customer_add")
        String CUSTOMER_ADD = "CUSTOMER_ADD";
        //编辑客户
        @Schema(description = "message.customer_update")
        String CUSTOMER_UPDATE = "CUSTOMER_UPDATE";
        //客户被删除
        @Schema(description = "message.customer_deleted")
        String CUSTOMER_DELETED = "CUSTOMER_DELETED";
        //新建协作人(系统)
        @Schema(description = "message.customer_collaboration_add")
        String CUSTOMER_COLLABORATION_ADD = "CUSTOMER_COLLABORATION_ADD";
        //新建协作人(手动)
        @Schema(description = "message.customer_collaboration_add_manual")
        String CUSTOMER_COLLABORATION_ADD_MANUAL = "CUSTOMER_COLLABORATION_ADD_MANUAL";
        //被转移客户
        @Schema(description = "message.customer_transferred_customer")
        String CUSTOMER_TRANSFERRED_CUSTOMER = "CUSTOMER_TRANSFERRED_CUSTOMER";
        //客户自动移入公海（到时间）
        @Schema(description = "message.customer_automatic_move_high_seas")
        String CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS = "CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS";
        //客户被动移入公海
        @Schema(description = "message.customer_moved_high_seas")
        String CUSTOMER_MOVED_HIGH_SEAS = "CUSTOMER_MOVED_HIGH_SEAS";
        //领取客户
        @Schema(description = "message.customer_receive")
        String CUSTOMER_RECEIVE = "CUSTOMER_RECEIVE";
        //公海客户被分配
        @Schema(description = "message.high_seas_customer_distributed")
        String HIGH_SEAS_CUSTOMER_DISTRIBUTED = "HIGH_SEAS_CUSTOMER_DISTRIBUTED";
        //客户审批
        @Schema(description = "message.customer_approval")
        String CUSTOMER_APPROVAL = "CUSTOMER_APPROVAL";
        //新建联系人
        @Schema(description = "message.customer_concat_add")
        String CUSTOMER_CONCAT_ADD = "CUSTOMER_CONCAT_ADD";
        //编辑联系人
        @Schema(description = "message.customer_concat_update")
        String CUSTOMER_CONCAT_UPDATE = "CUSTOMER_CONCAT_UPDATE";
        //删除联系人
        @Schema(description = "message.customer_concat_delete")
        String CUSTOMER_CONCAT_DELETE = "CUSTOMER_CONCAT_DELETE";


        //----线索管理----
        //新建线索
        @Schema(description = "message.clue_add")
        String CLUE_ADD = "CLUE_ADD";
        //编辑线索
        @Schema(description = "message.clue_update")
        String CLUE_UPDATE = "CLUE_UPDATE";
        //删除线索
        @Schema(description = "message.clue_deleted")
        String CLUE_DELETED = "CLUE_DELETED";
        //移入线索池(系统)
        @Schema(description = "message.clue_automatic_move_pool")
        String CLUE_AUTOMATIC_MOVE_POOL = "CLUE_AUTOMATIC_MOVE_POOL";
        //移入线索池(手动)
        @Schema(description = "message.clue_moved_pool")
        String CLUE_MOVED_POOL = "CLUE_MOVED_POOL";
        //转为客户
        @Schema(description = "message.clue_convert_customer")
        String CLUE_CONVERT_CUSTOMER = "CLUE_CONVERT_CUSTOMER";
        //转为商机
        @Schema(description = "message.clue_convert_business")
        String CLUE_CONVERT_BUSINESS = "CLUE_CONVERT_BUSINESS";
        //转移线索
        @Schema(description = "message.transfer_clue")
        String TRANSFER_CLUE = "TRANSFER_CLUE";
        //分配线索
        @Schema(description = "message.clue_distributed")
        String CLUE_DISTRIBUTED = "CLUE_DISTRIBUTED";
        //领取线索
        @Schema(description = "message.clue_receive")
        String CLUE_RECEIVE = "CLUE_RECEIVE";
        //线索审批
        @Schema(description = "message.clue_approval")
        String CLUE_APPROVAL = "CLUE_APPROVAL";


        //-------商机管理----
        //新建商机
        @Schema(description = "message.business_add")
        String BUSINESS_ADD = "BUSINESS_ADD";
        //新建商机
        @Schema(description = "message.business_update")
        String BUSINESS_UPDATE = "BUSINESS_UPDATE";
        //商机删除
        @Schema(description = "message.business_deleted")
        String BUSINESS_DELETED = "BUSINESS_DELETED";
        //商机转移
        @Schema(description = "message.business_transfer")
        String BUSINESS_TRANSFER = "BUSINESS_TRANSFER";
        //商机审批
        @Schema(description = "message.business_approval")
        String BUSINESS_APPROVAL = "BUSINESS_APPROVAL";
        //新建报价
        @Schema(description = "message.business_quotation_add")
        String BUSINESS_QUOTATION_ADD = "BUSINESS_QUOTATION_ADD";
        //编辑报价
        @Schema(description = "message.business_quotation_update")
        String BUSINESS_QUOTATION_UPDATE = "BUSINESS_QUOTATION_UPDATE";
        //删除报价
        @Schema(description = "message.business_quotation_deleted")
        String BUSINESS_QUOTATION_DELETED = "BUSINESS_QUOTATION_DELETED";
        //报价到期
        @Schema(description = "message.business_quotation_expired")
        String BUSINESS_QUOTATION_EXPIRED = "BUSINESS_QUOTATION_EXPIRED";
        //报价即将到期
        @Schema(description = "message.business_quotation_expiring")
        String BUSINESS_QUOTATION_EXPIRING = "BUSINESS_QUOTATION_EXPIRING";
        //报价审批
        @Schema(description = "message.business_quotation_approval")
        String BUSINESS_QUOTATION_APPROVAL = "BUSINESS_QUOTATION_APPROVAL";


        //-----订单-----
        //新建订单
        @Schema(description = "message.order_add")
        String ORDER_ADD = "ORDER_ADD";
        //编辑订单
        @Schema(description = "message.order_update")
        String ORDER_UPDATE = "ORDER_UPDATE";
        //删除订单
        @Schema(description = "message.order_delete")
        String ORDER_DELETE = "ORDER_DELETE";
        //订单审批
        @Schema(description = "message.order_approval")
        String ORDER_APPROVAL = "ORDER_APPROVAL";


        //-----合同----
        //新建合同
        @Schema(description = "message.contract_add")
        String CONTRACT_ADD = "CONTRACT_ADD";
        //编辑合同
        @Schema(description = "message.contract_update")
        String CONTRACT_UPDATE = "CONTRACT_UPDATE";
        //删除合同
        @Schema(description = "message.contract_delete")
        String CONTRACT_DELETE = "CONTRACT_DELETE";
        //合同作废
        @Schema(description = "message.contract_void")
        String CONTRACT_VOID = "CONTRACT_VOID";
        //合同归档
        @Schema(description = "message.contract_archived")
        String CONTRACT_ARCHIVED = "CONTRACT_ARCHIVED";
        //合同到期
        @Schema(description = "message.contract_expired")
        String CONTRACT_EXPIRED = "CONTRACT_EXPIRED";
        //合同即将到期
        @Schema(description = "message.contract_expiring")
        String CONTRACT_EXPIRING = "CONTRACT_EXPIRING";
        //合同审批
        @Schema(description = "message.contract_approval")
        String CONTRACT_APPROVAL = "CONTRACT_APPROVAL";
        // 新建回款计划
        @Schema(description = "message.contract_payment_add")
        String CONTRACT_PAYMENT_ADD = "CONTRACT_PAYMENT_ADD";
        // 编辑回款计划
        @Schema(description = "message.contract_payment_update")
        String CONTRACT_PAYMENT_UPDATE = "CONTRACT_PAYMENT_UPDATE";
        // 删除回款计划
        @Schema(description = "message.contract_payment_delete")
        String CONTRACT_PAYMENT_DELETE = "CONTRACT_PAYMENT_DELETE";
        //回款计划到期
        @Schema(description = "message.contract_payment_expired")
        String CONTRACT_PAYMENT_EXPIRED = "CONTRACT_PAYMENT_EXPIRED";
        //回款计划即将到期
        @Schema(description = "message.contract_payment_expiring")
        String CONTRACT_PAYMENT_EXPIRING = "CONTRACT_PAYMENT_EXPIRING";
        // 新建回款记录
        @Schema(description = "message.contract_record_add")
        String CONTRACT_RECORD_ADD = "CONTRACT_RECORD_ADD";
        // 编辑回款记录
        @Schema(description = "message.contract_record_update")
        String CONTRACT_RECORD_UPDATE = "CONTRACT_RECORD_UPDATE";
        // 删除回款记录
        @Schema(description = "message.contract_record_delete")
        String CONTRACT_RECORD_DELETE = "CONTRACT_RECORD_DELETE";
        // 新建发票记录
        @Schema(description = "message.invoice_add")
        String INVOICE_ADD = "INVOICE_ADD";
        // 编辑发票记录
        @Schema(description = "message.invoice_update")
        String INVOICE_UPDATE = "INVOICE_UPDATE";
        // 删除发票记录
        @Schema(description = "message.invoice_delete")
        String INVOICE_DELETE = "INVOICE_DELETE";
        //审批发票
        @Schema(description = "message.invoice_approval")
        String INVOICE_APPROVAL = "INVOICE_APPROVAL";


        //----跟进-----
        //新建跟进记录
        @Schema(description = "message.follow_up_record_add")
        String FOLLOW_UP_RECORD_ADD = "FOLLOW_UP_RECORD_ADD";
        //编辑跟进记录
        @Schema(description = "message.follow_up_record_update")
        String FOLLOW_UP_RECORD_UPDATE = "FOLLOW_UP_RECORD_UPDATE";
        //删除跟进记录
        @Schema(description = "message.follow_up_record_delete")
        String FOLLOW_UP_RECORD_DELETE = "FOLLOW_UP_RECORD_DELETE";
        // 跟进记录被评论
        @Schema(description = "message.follow_up_record_comment")
        String FOLLOW_UP_RECORD_COMMENT = "FOLLOW_UP_RECORD_COMMENT";
        // 评论被回复
        @Schema(description = "message.follow_up_record_comment_repaly")
        String FOLLOW_UP_RECORD_COMMENT_REPALY = "FOLLOW_UP_RECORD_COMMENT_REPALY";
        // 评论@我
        @Schema(description = "message.follow_up_record_comment_mentioned")
        String FOLLOW_UP_RECORD_COMMENT_MENTIONED = "FOLLOW_UP_RECORD_COMMENT_MENTIONED";
        //新建跟进计划
        @Schema(description = "message.follow_up_plan_add")
        String FOLLOW_UP_PLAN_ADD = "FOLLOW_UP_PLAN_ADD";
        // 编辑跟进计划
        @Schema(description = "message.follow_up_plan_update")
        String FOLLOW_UP_PLAN_UPDATE = "FOLLOW_UP_PLAN_UPDATE";
        // 删除跟进计划
        @Schema(description = "message.follow_up_plan_delete")
        String FOLLOW_UP_PLAN_DELETE = "FOLLOW_UP_PLAN_DELETE";
        // 跟进计划被评论
        @Schema(description = "message.follow_up_plan_comment")
        String FOLLOW_UP_PLAN_COMMENT = "FOLLOW_UP_PLAN_COMMENT";
        // 计划评论被回复
        @Schema(description = "message.follow_up_plan_comment_repaly")
        String FOLLOW_UP_PLAN_COMMENT_REPALY = "FOLLOW_UP_PLAN_COMMENT_REPALY";
        // 计划评论@我
        @Schema(description = "message.follow_up_plan_comment_mentioned")
        String FOLLOW_UP_PLAN_COMMENT_MENTIONED = "FOLLOW_UP_PLAN_COMMENT_MENTIONED";
        //跟进计划到期提醒
        @Schema(description = "message.follow_up_plan_due")
        String FOLLOW_UP_PLAN_DUE = "FOLLOW_UP_PLAN_DUE";
        //代办提醒
        @Schema(description = "message.approval_todo")
        String APPROVAL_TODO = "APPROVAL_TODO";
        //抄送代办
        @Schema(description = "message.approval_cc")
        String APPROVAL_CC = "APPROVAL_CC";
        // 单据被评论
        @Schema(description = "message.approval_comment")
        String APPROVAL_COMMENT = "APPROVAL_COMMENT";
        // 回复
        @Schema(description = "message.approval_comment_repaly")
        String APPROVAL_COMMENT_REPALY = "APPROVAL_COMMENT_REPALY";
        // 评论@
        @Schema(description = "message.approval_comment_mentioned")
        String APPROVAL_COMMENT_MENTIONED = "APPROVAL_COMMENT_MENTIONED";

    }

    public interface RelatedUser {
        @Schema(description = "message.operator")
        String OPERATOR = "OPERATOR"; //操作人
        @Schema(description = "message.owner")
        String OWNER = "OWNER"; //负责人
        @Schema(description = "message.create_user")
        String CREATE_USER = "CREATE_USER"; //创建人
    }

    public interface TemplateText {
        //请注意！${OPERATOR}新建${customerName}客户给您，请知悉！
        @Schema(description = "message.customer_add_text")
        String CUSTOMER_ADD_TEXT = "CUSTOMER_ADD_TEXT";

        @Schema(description = "message.customer_concat_add_text")
        String CUSTOMER_CONCAT_ADD_TEXT = "CUSTOMER_CONCAT_ADD_TEXT";

        @Schema(description = "message.customer_collaboration_add_text")
        String CUSTOMER_COLLABORATION_ADD_TEXT = "CUSTOMER_COLLABORATION_ADD_TEXT";

        // 请注意！${OPERATOR}将${customerName}客户转移给您,请知悉！
        @Schema(description = "message.customer_transferred_customer_text")
        String CUSTOMER_TRANSFERRED_CUSTOMER_TEXT = "CUSTOMER_TRANSFERRED_CUSTOMER_TEXT";

        //请注意！根据系统规则，您负责的${customerName}客户已被移入公海，请知悉！
        @Schema(description = "message.customer_automatic_move_high_seas_text")
        String CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS_TEXT = "CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS_TEXT";

        //请注意！${OPERATOR}已将您负责的${customerName}移入公海，请知悉！
        @Schema(description = "message.customer_moved_high_seas_text")
        String CUSTOMER_MOVED_HIGH_SEAS_TEXT = "CUSTOMER_MOVED_HIGH_SEAS_TEXT";

        //请注意！您负责的${customerName}已被${OPERATOR}删除！
        @Schema(description = "message.customer_deleted_text")
        String CUSTOMER_DELETED_TEXT = "CUSTOMER_DELETED_TEXT";

        //请注意！${customerName}已由公海分配给您，请及时跟进处理！
        @Schema(description = "message.high_seas_customer_distributed_text")
        String HIGH_SEAS_CUSTOMER_DISTRIBUTED_TEXT = "HIGH_SEAS_CUSTOMER_DISTRIBUTED_TEXT";

        //请注意！您创建的${customerName}跟进计划，已到预定时间，请及时跟进！
        @Schema(description = "message.customer_follow_up_plan_due_text")
        String CUSTOMER_FOLLOW_UP_PLAN_DUE_TEXT = "CUSTOMER_FOLLOW_UP_PLAN_DUE_TEXT";

        //请注意！${OPERATOR}新建${customerName}线索给您，请知悉！
        @Schema(description = "message.clue_add_text")
        String CLUE_ADD_TEXT = "CLUE_ADD_TEXT";

        //请注意！根据系统规则，您负责的${clueName}的销售线索，已被移入线索池！
        @Schema(description = "message.clue_automatic_move_pool_text")
        String CLUE_AUTOMATIC_MOVE_POOL_TEXT = "CLUE_AUTOMATIC_MOVE_POOL_TEXT";

        //请注意！${OPERATOR}已将您负责的${clueName}线索已被移入线索池，请知悉！
        @Schema(description = "message.clue_moved_pool_text")
        String CLUE_MOVED_POOL_TEXT = "CLUE_MOVED_POOL_TEXT";

        //请注意！您负责的${clueName}的销售线索，已成功转为客户！请知悉！
        @Schema(description = "message.clue_convert_customer_text")
        String CLUE_CONVERT_CUSTOMER_TEXT = "CLUE_CONVERT_CUSTOMER_TEXT";

        //请注意！您负责的${clueName}的销售线索，已成功转为商机！请知悉！
        @Schema(description = "message.clue_convert_business_text")
        String CLUE_CONVERT_BUSINESS_TEXT = "CLUE_CONVERT_BUSINESS_TEXT";

        //请注意！${OPERATOR}将${clueName}线索转移给您，请知悉！
        @Schema(description = "message.transfer_clue_text")
        String TRANSFER_CLUE_TEXT = "TRANSFER_CLUE_TEXT";

        //请注意！您负责的${clueName}线索，已被${OPERATOR}删除！
        @Schema(description = "message.clue_deleted_text")
        String CLUE_DELETED_TEXT = "CLUE_DELETED_TEXT";

        //请注意！${clueName}由线索，已由线索池分配给您，请及时跟进处理！
        @Schema(description = "message.clue_distributed_text")
        String CLUE_DISTRIBUTED_TEXT = "CLUE_DISTRIBUTED_TEXT";

        //请注意！您创建的${clueName}线索跟进计划，已到预定时间，请及时跟进！
        @Schema(description = "message.clue_follow_up_plan_due_text")
        String CLUE_FOLLOW_UP_PLAN_DUE_TEXT = "CLUE_FOLLOW_UP_PLAN_DUE_TEXT";

        //请注意！${OPERATOR}新建${customerName}客户给您，请知悉！
        @Schema(description = "message.business_add_text")
        String BUSINESS_ADD_TEXT = "BUSINESS_ADD_TEXT";

        //请注意！您负责的${businessName}商机，已被${OPERATOR}删除！
        @Schema(description = "message.business_deleted_text")
        String BUSINESS_DELETED_TEXT = "BUSINESS_DELETED_TEXT";

        //请注意！${OPERATOR}将${businessName}商机转移给您，请知悉！
        @Schema(description = "message.business_transfer_text")
        String BUSINESS_TRANSFER_TEXT = "BUSINESS_TRANSFER_TEXT";

        //请注意！您创建的${businessName}商机跟进计划，已到预定时间，请及时跟进！
        @Schema(description = "message.business_follow_up_plan_due_text")
        String BUSINESS_FOLLOW_UP_PLAN_DUE_TEXT = "BUSINESS_FOLLOW_UP_PLAN_DUE_TEXT";

        //${OPERATOR}删除了${name}报价单
        @Schema(description = "message.business_quotation_deleted_text")
        String BUSINESS_QUOTATION_DELETED_TEXT = "BUSINESS_QUOTATION_DELETED_TEXT";

        //您负责的${customerName}报价单还有${expireDays}天到期；
        @Schema(description = "message.business_quotation_expiring_text")
        String BUSINESS_QUOTATION_EXPIRING_TEXT = "BUSINESS_QUOTATION_EXPIRING_TEXT";

        //您负责的${customerName}的报价单已经到期；
        @Schema(description = "message.business_quotation_expired_text")
        String BUSINESS_QUOTATION_EXPIRED_TEXT = "BUSINESS_QUOTATION_EXPIRED_TEXT";

        //您负责的${customerName}合同已被归档；
        @Schema(description = "message.contract_archived_text")
        String CONTRACT_ARCHIVED_TEXT = "CONTRACT_ARCHIVED_TEXT";
        //您负责的${customerName}合同已被作废；
        @Schema(description = "message.contract_void_text")
        String CONTRACT_VOID_TEXT = "CONTRACT_VOID_TEXT";
        //您负责的${customerName}合同还有${expireDays}天到期；
        @Schema(description = "message.contract_expiring_text")
        String CONTRACT_EXPIRING_TEXT = "CONTRACT_EXPIRING_TEXT";
        //您负责的${customerName}合同已经到期；
        @Schema(description = "message.contract_expired_text")
        String CONTRACT_EXPIRED_TEXT = "CONTRACT_EXPIRED_TEXT";

        //您负责的${customerName}合同的回款计划还有${expireDays}天到期；
        @Schema(description = "message.contract_payment_expiring_text")
        String CONTRACT_PAYMENT_EXPIRING_TEXT = "CONTRACT_PAYMENT_EXPIRING_TEXT";
        //您负责的${customerName}合同的回款计划已经到期；
        @Schema(description = "message.contract_payment_expired_text")
        String CONTRACT_PAYMENT_EXPIRED_TEXT = "CONTRACT_PAYMENT_EXPIRED_TEXT";

        //同步组织架构完成
        @Schema(description = "message.sync_organization_structure_text")
        String SYNC_ORGANIZATION_STRUCTURE_TEXT = "SYNC_ORGANIZATION_STRUCTURE_TEXT";

        @Schema(description = "message.approval_result_text")
        String CONTRACT_APPROVAL_TEXT = "CONTRACT_APPROVAL_TEXT";

        @Schema(description = "message.approval_result_text")
        String ORDER_APPROVAL_TEXT = "ORDER_APPROVAL_TEXT";

        @Schema(description = "message.approval_result_text")
        String INVOICE_APPROVAL_TEXT = "INVOICE_APPROVAL_TEXT";

        @Schema(description = "message.approval_result_text")
        String BUSINESS_QUOTATION_APPROVAL_TEXT = "BUSINESS_QUOTATION_APPROVAL_TEXT";

        @Schema(description = "message.approval_result_text")
        String BUSINESS_APPROVAL_TEXT = "BUSINESS_APPROVAL_TEXT";

        @Schema(description = "message.approval_result_text")
        String CLUE_APPROVAL_TEXT = "CLUE_APPROVAL_TEXT";

        @Schema(description = "message.approval_result_text")
        String CUSTOMER_APPROVAL_TEXT = "CUSTOMER_APPROVAL_TEXT";

        @Schema(description = "message.approval_todo_text")
        String APPROVAL_TODO_TEXT = "APPROVAL_TODO_TEXT";

        @Schema(description = "message.approval_result_text")
        String CUSTOM_FORM_DATA_APPROVAL_TEXT = "CUSTOM_FORM_DATA_APPROVAL_TEXT";

        @Schema(description = "message.approval_cc_text")
        String APPROVAL_CC_TEXT = "APPROVAL_CC_TEXT";

        @Schema(description = "message.approval_comment_mentioned_text")
        String APPROVAL_COMMENT_MENTIONED_TEXT = "APPROVAL_COMMENT_MENTIONED_TEXT";

        @Schema(description = "message.follow_up_plan_comment_added_text")
        String CLUE_FOLLOW_UP_PLAN_COMMENT_ADDED_TEXT = "CLUE_FOLLOW_UP_PLAN_COMMENT_ADDED_TEXT";

        @Schema(description = "message.follow_up_plan_comment_mentioned_text")
        String CLUE_FOLLOW_UP_PLAN_COMMENT_MENTIONED_TEXT = "CLUE_FOLLOW_UP_PLAN_COMMENT_MENTIONED_TEXT";

        @Schema(description = "message.follow_up_record_comment_added_text")
        String CLUE_FOLLOW_UP_RECORD_COMMENT_ADDED_TEXT = "CLUE_FOLLOW_UP_RECORD_COMMENT_ADDED_TEXT";

        @Schema(description = "message.follow_up_record_comment_mentioned_text")
        String CLUE_FOLLOW_UP_RECORD_COMMENT_MENTIONED_TEXT = "CLUE_FOLLOW_UP_RECORD_COMMENT_MENTIONED_TEXT";

        @Schema(description = "message.follow_up_plan_comment_added_text")
        String OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_ADDED_TEXT = "OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_ADDED_TEXT";

        @Schema(description = "message.follow_up_plan_comment_mentioned_text")
        String OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_MENTIONED_TEXT = "OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_MENTIONED_TEXT";

        @Schema(description = "message.follow_up_record_comment_added_text")
        String OPPORTUNITY_FOLLOW_UP_RECORD_COMMENT_ADDED_TEXT = "OPPORTUNITY_FOLLOW_UP_RECORD_COMMENT_ADDED_TEXT";

        @Schema(description = "message.follow_up_record_comment_mentioned_text")
        String OPPORTUNITY_FOLLOW_UP_RECORD_COMMENT_MENTIONED_TEXT = "OPPORTUNITY_FOLLOW_UP_RECORD_COMMENT_MENTIONED_TEXT";

        @Schema(description = "message.follow_up_plan_comment_added_text")
        String CUSTOMER_FOLLOW_UP_PLAN_COMMENT_ADDED_TEXT = "CUSTOMER_FOLLOW_UP_PLAN_COMMENT_ADDED_TEXT";

        @Schema(description = "message.follow_up_plan_comment_mentioned_text")
        String CUSTOMER_FOLLOW_UP_PLAN_COMMENT_MENTIONED_TEXT = "CUSTOMER_FOLLOW_UP_PLAN_COMMENT_MENTIONED_TEXT";

        @Schema(description = "message.follow_up_record_comment_added_text")
        String CUSTOMER_FOLLOW_UP_RECORD_COMMENT_ADDED_TEXT = "CUSTOMER_FOLLOW_UP_RECORD_COMMENT_ADDED_TEXT";

        @Schema(description = "message.follow_up_record_comment_mentioned_text")
        String CUSTOMER_FOLLOW_UP_RECORD_COMMENT_MENTIONED_TEXT = "CUSTOMER_FOLLOW_UP_RECORD_COMMENT_MENTIONED_TEXT";
    }

    public interface SensitiveField {
        String id = "id";
        String organizationId = "organizationId";
    }
}
