package io.cordys.crm.clue.service;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.ExportHeadDTO;
import io.cordys.common.dto.ExportSelectRequest;
import io.cordys.common.service.BaseExportService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.SubListUtils;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.dto.request.ClueExportRequest;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.clue.utils.ClueFieldUtils;
import io.cordys.crm.system.constants.ExportConstants;
import io.cordys.crm.system.domain.ExportTask;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.service.ExportTaskService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.registry.ExportThreadRegistry;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ClueExportService extends BaseExportService {

	@Resource
	private ClueService clueService;
	@Resource
	private BaseMapper<Clue> clueMapper;
	@Resource
	private ExtClueMapper extClueMapper;
	@Resource
	private ExportTaskService exportTaskService;

	public String exportAll(ClueExportRequest request, String userId, String orgId, DeptDataPermissionDTO dataPermission) {
		//用户导出数量 限制
		exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportType.CLUE.toString(), ExportConstants.ExportStatus.PREPARED.toString());

		String fileId = IDGenerator.nextStr();
		ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CLUE.toString(), request.getFileName());
		Thread.startVirtualThread(() -> {
			try {
				ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
				//表头信息
				List<List<String>> headList = request.getHeads().stream()
						.map(head -> Collections.singletonList(head.getTitle()))
						.toList();

				//分批查询数据并写入文件
				batchHandleData(fileId,
						headList,
						exportTask.getId(),
						request.getFileName(),
						request,
						t -> getExportData(request, userId, orgId, dataPermission, exportTask.getId()));

				//更新状态
				exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), userId);

			} catch (InterruptedException e) {
				LogUtils.error("任务停止中断", e);
			} catch (Exception e) {
				//更新任务
				exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
			} finally {
				//从注册中心移除
				ExportThreadRegistry.remove(exportTask.getId());
				//日志
				exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.CLUE_INDEX, request.getFileName());
			}
		});
		return exportTask.getId();
	}

	public String exportSelect(ExportSelectRequest request, String userId, String orgId) {
		// 用户导出数量限制
		exportTaskService.checkUserTaskLimit(userId, ExportConstants.ExportType.CLUE.toString(), ExportConstants.ExportStatus.PREPARED.toString());

		String fileId = IDGenerator.nextStr();
		ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CLUE.toString(), request.getFileName());
		Thread.startVirtualThread(() -> {
			try {
				ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
				//表头信息
				List<List<String>> headList = request.getHeadList().stream()
						.map(head -> Collections.singletonList(head.getTitle()))
						.toList();

				// 准备导出文件
				File file = prepareExportFile(fileId, request.getFileName());
				try (ExcelWriter writer = EasyExcel.write(file)
						.head(headList)
						.excelType(ExcelTypeEnum.XLSX)
						.build()) {
					WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();

					SubListUtils.dealForSubList(request.getIds(), 500, (subIds) -> {
						List<List<Object>> data = new ArrayList<>();
						try {
							data = getExportDataBySelect(request.getHeadList(), subIds, orgId, exportTask.getId());
						} catch (InterruptedException e) {
							LogUtils.error("任务停止中断", e);
							exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.STOP.toString(), userId);
						}
						writer.write(data, sheet);
					});
				}

				//更新导出任务状态
				exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), userId);
			} catch (Exception e) {
				//更新任务
				exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
			} finally {
				//从注册中心移除
				ExportThreadRegistry.remove(exportTask.getId());
				//日志
				exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.CLUE_INDEX, request.getFileName());
			}
		});
		return exportTask.getId();
	}

	private List<List<Object>> getExportData(ClueExportRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission, String taskId) throws InterruptedException {
		PageHelper.startPage(request.getCurrent(), request.getPageSize());
		List<ClueListResponse> exportList = extClueMapper.list(request, orgId, userId, deptDataPermission);
		List<ClueListResponse> dataList = clueService.buildListData(exportList, orgId);
		Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CLUE.getKey(), orgId);
		//构建导出数据
		//构建导出数据
		List<List<Object>> data = new ArrayList<>();
		for (ClueListResponse response : dataList) {
			if (ExportThreadRegistry.isInterrupted(taskId)) {
				throw new InterruptedException("线程已被中断，主动退出");
			}
			List<Object> value = buildData(request.getHeads(), response, fieldConfigMap);
			data.add(value);
		}

		return data;
	}

	private List<Object> buildData(List<ExportHeadDTO> headList, ClueListResponse data, Map<String, BaseField> fieldConfigMap) {
		List<Object> dataList = new ArrayList<>();
		//固定字段map
		LinkedHashMap<String, Object> systemFiledMap = ClueFieldUtils.getSystemFieldMap(data);
		//自定义字段map
		AtomicReference<Map<String, Object>> moduleFieldMap = new AtomicReference<>(new LinkedHashMap<>());
		Optional.ofNullable(data.getModuleFields()).ifPresent(moduleFields -> moduleFieldMap.set(moduleFields.stream().collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue))));
		//处理数据转换
		transModuleFieldValue(headList, systemFiledMap, moduleFieldMap.get(), dataList, fieldConfigMap);
		return dataList;
	}

	private List<List<Object>> getExportDataBySelect(List<ExportHeadDTO> headList, List<String> ids, String orgId, String taskId) throws InterruptedException {
		//获取数据
		List<Clue> clues = clueMapper.selectByIds(ids);
		List<ClueListResponse> allList = clues.stream().map(clue -> {
			ClueListResponse clueListResponse = new ClueListResponse();
			BeanUtils.copyBean(clueListResponse, clue);
			return clueListResponse;
		}).toList();
		List<ClueListResponse> dataList = clueService.buildListData(allList, orgId);
		Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CLUE.getKey(), orgId);
		//构建导出数据
		List<List<Object>> data = new ArrayList<>();
		for (ClueListResponse response : dataList) {
			if (ExportThreadRegistry.isInterrupted(taskId)) {
				throw new InterruptedException("线程已被中断，主动退出");
			}
			List<Object> value = buildData(headList, response, fieldConfigMap);
			data.add(value);
		}

		return data;
	}
}
