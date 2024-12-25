package io.cordys.common.util;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

public class HikariCPUtils {
    /**
     * 获取 HikariCP 连接池的使用情况
     *
     * @param dataSource HikariDataSource 实例
     * @return HikariCP 连接池状态信息
     */
    public static String getHikariCPStatus(HikariDataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("HikariDataSource cannot be null");
        }

        HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();

        return "HikariCP Status:\n" +
                "Active Connections: " + poolMXBean.getActiveConnections() + "\n" +
                "Idle Connections: " + poolMXBean.getIdleConnections() + "\n" +
                "Total Connections: " + poolMXBean.getTotalConnections() + "\n" +
                "Threads Awaiting Connection: " + poolMXBean.getThreadsAwaitingConnection() + "\n";
    }

    /**
     * 获取 HikariCP 连接池的配置情况
     *
     * @param dataSource HikariDataSource 实例
     * @return 连接池配置情况
     */
    public static String getHikariCPConfig(HikariDataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("HikariDataSource cannot be null");
        }

        return "HikariCP Configuration:\n" +
                "Maximum Pool Size: " + dataSource.getMaximumPoolSize() + "\n" +
                "Minimum Idle Connections: " + dataSource.getMinimumIdle() + "\n" +
                "Connection Timeout: " + dataSource.getConnectionTimeout() + " ms\n" +
                "Idle Timeout: " + dataSource.getIdleTimeout() + " ms\n" +
                "Max Lifetime: " + dataSource.getMaxLifetime() + " ms\n";
    }

    /**
     * 打印 HikariCP 的状态和配置信息
     */
    public static void printHikariCPStatus() {
        HikariDataSource dataSource = CommonBeanFactory.getBean(HikariDataSource.class);
        if (dataSource == null) {
            LogUtils.error("HikariDataSource not found");
            return;
        }
        LogUtils.info(getHikariCPStatus(dataSource));
        LogUtils.info(getHikariCPConfig(dataSource));
    }
}
