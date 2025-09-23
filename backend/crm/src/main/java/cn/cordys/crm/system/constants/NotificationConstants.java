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
        //线索管理
        @Schema(description = "message.clue")
        String CLUE = "CLUE";
        //商机管理
        @Schema(description = "message.business")
        String OPPORTUNITY = "OPPORTUNITY";
        //公告
        @Schema(description = "message.announcement")
        String ANNOUNCEMENT = "ANNOUNCEMENT";
    }

    public interface Event {
        // -----客户管理----
        //新建客户
        @Schema(description = "message.customer_add")
        String CUSTOMER_ADD = "CUSTOMER_ADD";
        @Schema(description = "message.customer_concat_add")
        String CUSTOMER_CONCAT_ADD = "CUSTOMER_CONCAT_ADD";
        @Schema(description = "message.customer_collaboration_add")
        String CUSTOMER_COLLABORATION_ADD = "CUSTOMER_COLLABORATION_ADD";
        //被转移客户
        @Schema(description = "message.customer_transferred_customer")
        String CUSTOMER_TRANSFERRED_CUSTOMER = "CUSTOMER_TRANSFERRED_CUSTOMER";
        //客户自动移入公海
        @Schema(description = "message.customer_automatic_move_high_seas")
        String CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS = "CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS";
        //客户被动移入公海（到时间）
        @Schema(description = "message.customer_moved_high_seas")
        String CUSTOMER_MOVED_HIGH_SEAS = "CUSTOMER_MOVED_HIGH_SEAS";
        //客户被删除
        @Schema(description = "message.customer_deleted")
        String CUSTOMER_DELETED = "CUSTOMER_DELETED";
        //公海客户被分配
        @Schema(description = "message.high_seas_customer_distributed")
        String HIGH_SEAS_CUSTOMER_DISTRIBUTED = "HIGH_SEAS_CUSTOMER_DISTRIBUTED";
        //跟进计划到期
        @Schema(description = "message.customer_follow_up_plan_due")
        String CUSTOMER_FOLLOW_UP_PLAN_DUE = "CUSTOMER_FOLLOW_UP_PLAN_DUE";
        //----线索管理----
        //自动移入线索池
        @Schema(description = "message.clue_automatic_move_pool")
        String CLUE_AUTOMATIC_MOVE_POOL = "CLUE_AUTOMATIC_MOVE_POOL";
        //被动移入线索池
        @Schema(description = "message.clue_moved_pool")
        String CLUE_MOVED_POOL = "CLUE_MOVED_POOL";
        //转为客户
        @Schema(description = "message.clue_convert_customer")
        String CLUE_CONVERT_CUSTOMER = "CLUE_CONVERT_CUSTOMER";
        /// 转为商机
        @Schema(description = "message.clue_convert_business")
        String CLUE_CONVERT_BUSINESS = "CLUE_CONVERT_BUSINESS";
        //转移线索
        @Schema(description = "message.transfer_clue")
        String TRANSFER_CLUE = "TRANSFER_CLUE";
        //删除线索
        @Schema(description = "message.clue_deleted")
        String CLUE_DELETED = "CLUE_DELETED";
        //分配线索
        @Schema(description = "message.clue_distributed")
        String CLUE_DISTRIBUTED = "CLUE_DISTRIBUTED";
        //导入线索
        @Schema(description = "message.clue_import")
        String CLUE_IMPORT = "CLUE_IMPORT";
        //跟进计划到期
        @Schema(description = "message.clue_follow_up_plan_due")
        String CLUE_FOLLOW_UP_PLAN_DUE = "CLUE_FOLLOW_UP_PLAN_DUE";
        //-------商机管理----
        //商机删除
        @Schema(description = "message.business_deleted")
        String BUSINESS_DELETED = "BUSINESS_DELETED";
        //商机转移
        @Schema(description = "message.business_transfer")
        String BUSINESS_TRANSFER = "BUSINESS_TRANSFER";
        //商机导入
        @Schema(description = "message.business_import")
        String BUSINESS_IMPORT = "BUSINESS_IMPORT";
        //跟进计划提醒
        @Schema(description = "message.business_follow_up_plan_due")
        String BUSINESS_FOLLOW_UP_PLAN_DUE = "BUSINESS_FOLLOW_UP_PLAN_DUE";
    }

    public interface RelatedUser {
        @Schema(description = "message.operator")
        String OPERATOR = "OPERATOR"; //操作人
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

        //请注意！${OPERATOR}已成功导入${count}条线索，请及时领取！
        @Schema(description = "message.clue_import_text")
        String CLUE_IMPORT_TEXT = "CLUE_IMPORT_TEXT";

        //请注意！您创建的${clueName}线索跟进计划，已到预定时间，请及时跟进！
        @Schema(description = "message.clue_follow_up_plan_due_text")
        String CLUE_FOLLOW_UP_PLAN_DUE_TEXT = "CLUE_FOLLOW_UP_PLAN_DUE_TEXT";

        //请注意！您负责的${businessName}商机，已被${OPERATOR}删除！
        @Schema(description = "message.business_deleted_text")
        String BUSINESS_DELETED_TEXT = "BUSINESS_DELETED_TEXT";

        //请注意！${OPERATOR}将${businessName}商机转移给您，请知悉！
        @Schema(description = "message.business_transfer_text")
        String BUSINESS_TRANSFER_TEXT = "BUSINESS_TRANSFER_TEXT";

        //请注意！${OPERATOR}已成功导入${count}条线索，请及时领取！
        @Schema(description = "message.business_import_text")
        String BUSINESS_IMPORT_TEXT = "BUSINESS_IMPORT_TEXT";

        //请注意！您创建的${businessName}商机跟进计划，已到预定时间，请及时跟进！
        @Schema(description = "message.business_follow_up_plan_due_text")
        String BUSINESS_FOLLOW_UP_PLAN_DUE_TEXT = "BUSINESS_FOLLOW_UP_PLAN_DUE_TEXT";
    }


    public interface SensitiveField {
        String id = "id";
        String organizationId = "organizationId";
    }
}
