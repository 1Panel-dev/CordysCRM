package cn.cordys.aspectj.constants;

/**
 * 操作日志类型枚举类。
 * 用于定义不同操作的日志类型，便于日志分类和处理。
 */
public final class LogType {
    public static final String SELECT = "SELECT";
    /**
     * 添加操作
     */
    public static final String ADD = "ADD";
    /**
     * 删除操作
     */
    public static final String DELETE = "DELETE";
    /**
     * 更新操作
     */
    public static final String UPDATE = "UPDATE";
    /**
     * 移入公海
     */
    public static final String MOVE_TO_CUSTOMER_POOL = "MOVE_TO_CUSTOMER_POOL";
    /**
     * 审核操作
     */
    public static final String REVIEW = "REVIEW";
    /**
     * 登出操作
     */
    public static final String LOGOUT = "LOGOUT";
    /**
     * 登录操作
     */
    public static final String LOGIN = "LOGIN";
    /**
     * 复制操作
     */
    public static final String COPY = "COPY";

    /**
     * 同步
     */
    public static final String SYNC = "SYNC";
    /**
     * 领取
     */
    public static final String PICK = "PICK";
    /**
     * 分配
     */
    public static final String ASSIGN = "ASSIGN";
    /**
     * 取消
     */
    public static final String CANCEL = "CANCEL";
    /**
     * 添加用户
     */
    public static final String ADD_USER = "ADD_USER";
    /**
     * 移除用户
     */
    public static final String REMOVE_USER = "REMOVE_USER";
    /**
     * 导出
     */
    public static final String EXPORT = "EXPORT";

    private LogType() {
        // 私有构造函数防止实例化
    }

    /**
     * 判断给定的日志类型是否包含关键字。
     *
     * @param logType 当前日志类型
     * @param keyword 待匹配的日志类型关键字
     * @return 如果日志类型包含关键字，则返回 true，否则返回 false
     */
    public static boolean contains(String logType, String keyword) {
        if (logType == null || keyword == null) {
            return false;
        }
        return logType.contains(keyword);
    }
}
