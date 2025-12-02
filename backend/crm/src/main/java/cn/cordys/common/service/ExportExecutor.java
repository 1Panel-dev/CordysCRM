package cn.cordys.common.service;

import cn.cordys.crm.system.domain.ExportTask;

/**
 * 通用导出执行器
 * @author song-cc-rock
 */
@FunctionalInterface
public interface ExportExecutor {

	/**
	 * 导出执行方法
	 * @param task 导出任务
	 * @throws Exception 异常信息
	 */
	void execute(ExportTask task) throws Exception;
}
