package io.cordys.aspectj.constants;

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
    public static final String SYSTEM_NOTICE = "SYSTEM_NOTICE";
    /**
     * 用户
     */
    public static final String SYSTEM_USER = "SYSTEM_USER";
    /**
     * 公告
     */
    public static final String SYSTEM_ANNOUNCEMENT = "SYSTEM_ANNOUNCEMENT";
    /**
     * 组织架构
     */
    public static final String SYSTEM_DEPARTMENT = "SYSTEM_DEPARTMENT";
    /**
     * 组织架构用户
     */
    public static final String SYSTEM_DEPARTMENT_USER = "SYSTEM_DEPARTMENT_USER";
    /**
     * 角色权限
     */
    public static final String SYSTEM_ROLE = "SYSTEM_ROLE";

    /**
     * 个人信息模块（API密钥）
     */
    public static final String PERSONAL_INFORMATION_APIKEY = "PERSONAL_INFORMATION_APIKEY";

    /**
     * 模块设置
     */
    public static final String MODULE_SETTING = "MODULE_SETTING";


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
    public static final String OPPORTUNITY = "OPPORTUNITY";

    //todo end

    // 可以根据需要扩展其他模块常量
}
