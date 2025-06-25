package io.cordys.crm.system.service;

import io.cordys.common.constants.TopicConstants;
import io.cordys.common.exception.GenericException;
import io.cordys.common.redis.MessagePublisher;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.ExportConstants;
import io.cordys.crm.system.domain.ExportTask;
import io.cordys.crm.system.dto.request.ExportTaskCenterQueryRequest;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * @author song-cc-rock
 */
@Service
public class ExportTaskCenterService {

	@Resource
	private BaseMapper<ExportTask> exportTaskMapper;
	@Resource
	private MessagePublisher messagePublisher;

	/**
	 * 查询导出任务列表
	 *
	 * @param request 请求参数
	 * @return 导出任务列表
	 */
	public List<ExportTask> list(ExportTaskCenterQueryRequest request, String userId) {
		LocalDateTime oneDayBefore = LocalDateTime.now().minusDays(1);
		LambdaQueryWrapper<ExportTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.like(ExportTask::getFileName, request.getKeyword())
				.eq(ExportTask::getCreateUser, userId)
				.eq(ExportTask::getResourceType, request.getExportType())
				.eq(ExportTask::getStatus, request.getExportStatus())
				.gt(ExportTask::getCreateTime, oneDayBefore.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
				.orderByDesc(ExportTask::getCreateTime);
		return exportTaskMapper.selectListByLambda(queryWrapper);
	}

	/**
	 * 取消导出任务
	 *
	 * @param taskId 任务ID
	 */
	public void cancel(String taskId) {
		ExportTask exportTask = exportTaskMapper.selectByPrimaryKey(taskId);
		if (exportTask == null) {
			throw new GenericException(Translator.get("task_not_found"));
		}
		if (StringUtils.equalsIgnoreCase(exportTask.getStatus(), ExportConstants.ExportStatus.STOP.name())) {
			throw new GenericException(Translator.get("task_already_stopped"));
		}
		exportTask.setStatus(ExportConstants.ExportStatus.STOP.name());
		exportTaskMapper.updateById(exportTask);
		//停止任务
		messagePublisher.publish(TopicConstants.DOWNLOAD_TOPIC, taskId);
	}

	/**
	 * 下载任务文件
	 *
	 * @param taskId   任务ID
	 * @param response HTTP响应对象
	 */
	public void download(String taskId, HttpServletResponse response) {
		ExportTask exportTask = exportTaskMapper.selectByPrimaryKey(taskId);
		if (exportTask == null) {
			throw new GenericException(Translator.get("task_not_found"));
		}
		// todo: wait for download implementation
	}

	public void clean() {
		LocalDateTime oneDayBefore = LocalDateTime.now().minusDays(1);
		LambdaQueryWrapper<ExportTask> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.lt(ExportTask::getCreateTime, oneDayBefore.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
				.orderByDesc(ExportTask::getCreateTime);
		exportTaskMapper.deleteByLambda(queryWrapper);
		// todo: clean deprecated resources from storage
	}
}
