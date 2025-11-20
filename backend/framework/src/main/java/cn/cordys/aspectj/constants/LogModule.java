package cn.cordys.aspectj.constants;

/**
 * 系统日志模块常量类。
 * 用于定义不同模块的名称，便于日志记录时进行分类。
 */
public class LogModule {

    /**
     * 系统管理模块
     */
    public static final String SYSTEM = "SYSTEM";
    /**
     * 消息通知
     */
    public static final String SYSTEM_MESSAGE_MESSAGE = "SYSTEM_MESSAGE_MESSAGE";
    /**
     * 邮件设置
     */
    public static final String SYSTEM_BUSINESS_MAIL = "SYSTEM_BUSINESS_MAIL";
    /**
     * 认证设置
     */
    public static final String SYSTEM_BUSINESS_AUTH = "SYSTEM_BUSINESS_AUTH";

    /**
     * 三方设置
     */
    public static final String SYSTEM_BUSINESS_THIRD = "SYSTEM_BUSINESS_THIRD";

    /**
     * 界面设置
     */
    public static final String SYSTEM_BUSINESS_UI = "SYSTEM_BUSINESS_UI";


    /**
     * 用户
     */
    public static final String SYSTEM_USER = "SYSTEM_USER";
    /**
     * 公告
     */
    public static final String SYSTEM_MESSAGE_ANNOUNCEMENT = "SYSTEM_MESSAGE_ANNOUNCEMENT";
    /**
     * 组织架构
     */
    public static final String SYSTEM_ORGANIZATION = "SYSTEM_ORGANIZATION";

    /**
     * 角色权限
     */
    public static final String SYSTEM_ROLE = "SYSTEM_ROLE";

    /**
     * 个人信息模块（API密钥）
     */
    public static final String PERSONAL_INFORMATION_APIKEY = "PERSONAL_INFORMATION_APIKEY";

    /**
     * 模块配置
     */
    public static final String SYSTEM_MODULE = "SYSTEM_MODULE";

    /**
     * 客户
     */
    public static final String CUSTOMER_INDEX = "CUSTOMER_INDEX";
    /**
     * 线索-线索
     */
    public static final String CLUE_INDEX = "CLUE_MANAGEMENT_CLUE";
    /**
     * 线索-线索池
     */
    public static final String CLUE_POOL_INDEX = "CLUE_MANAGEMENT_POOL";

    /**
     * 客户联系人
     */
    public static final String CUSTOMER_CONTACT = "CUSTOMER_CONTACT";

    //TODO  start  暂定跟进记录模块常量
    /**
     * 跟进记录
     */
    public static final String FOLLOW_UP_RECORD = "FOLLOW_UP_RECORD";

    /**
     * 跟进计划
     */
    public static final String FOLLOW_UP_PLAN = "FOLLOW_UP_PLAN";
    /**
     * 商机
     */
    public static final String OPPORTUNITY_INDEX = "OPPORTUNITY_INDEX";

     /**
     * 商机报价
     */
    public static final String OPPORTUNITY_QUOTATION = "OPPORTUNITY_QUOTATION";


    //todo end

    // 可以根据需要扩展其他模块常量

    /**
     * 产品
     */
    public static final String PRODUCT_MANAGEMENT = "PRODUCT_MANAGEMENT";

	/**
	 * 产品价格管理
	 */
	public static final String PRODUCT_PRICE_MANAGEMENT = "PRODUCT_PRICE_MANAGEMENT";

    /**
     * 客户公海
     */
    public static final String CUSTOMER_POOL = "CUSTOMER_POOL";

    /**
     * 仪表板
     */
    public static final String DASHBOARD = "DASHBOARD";
    public static final String AGENT = "AGENT";
}
