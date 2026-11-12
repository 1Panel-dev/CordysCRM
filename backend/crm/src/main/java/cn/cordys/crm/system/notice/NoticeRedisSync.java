package cn.cordys.crm.system.notice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 通知落库成功后再同步 Redis，避免回滚时留下缓存或广播。
 */
@Slf4j
public final class NoticeRedisSync {

    private NoticeRedisSync() {
    }

    /**
     * 有事务时等待提交，无事务时在调用方完成落库后立即执行。
     *
     * @param action 仅同步通知缓存和广播，不在提交回调中写数据库
     */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    synchronize(action);
                }
            });
        } else {
            synchronize(action);
        }
    }

    private static void synchronize(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            // 数据库已经提交，Redis 故障不能把已保存的操作报告为失败。
            log.error("同步通知缓存或广播失败", e);
        }
    }
}
