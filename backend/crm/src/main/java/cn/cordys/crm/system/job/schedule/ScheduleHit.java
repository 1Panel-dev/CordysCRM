package cn.cordys.crm.system.job.schedule;

/**
 * 定时提醒命中记录（用于组装通知）
 *
 * @param id           业务资源id
 * @param resourceName 展示名称（合同/报价/回款/跟进计划对应的客户名或父对象名，沿用旧提醒约定）
 * @param owner        负责人id（用于 OWNER/SUPERVISOR 接收人解析）
 * @param createUser   创建人id（作为通知操作人）
 * @param dueTime      到期时间戳(毫秒)
 */
public record ScheduleHit(String id, String resourceName, String owner, String createUser, long dueTime) {
}
