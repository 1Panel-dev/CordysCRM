package cn.cordys.crm.product.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.service.BaseExportService;
import cn.cordys.common.service.ExportExecutor;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.crm.product.dto.request.ProductPriceExportRequest;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.service.ExportTaskService;
import cn.cordys.registry.ExportThreadRegistry;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.annotation.Resource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 价格表导出
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProductPriceExportService extends BaseExportService {

	@Resource
	private ExportTaskService exportTaskService;

	/**
	 * 导出全部
	 * @param request 请求参数
	 * @param currentUser 当前用户
	 * @param currentOrg 当前组织
	 * @param locale 语言环境
	 * @return 任务ID
	 */
	public String exportAll(ProductPriceExportRequest request, String currentUser, String currentOrg, Locale locale) {
		return asyncExport(request.getFileName(), currentOrg, currentUser, locale, (task) -> {
			//表头信息
			List<List<String>> headList = request.getHeadList().stream().map(head -> Collections.singletonList(head.getTitle())).toList();

			//分批查询数据并写入文件
			batchHandleData(task.getFileId(), headList, task, request.getFileName(), request,
					t -> getExportData(request, currentUser, currentOrg, task.getId()));
		});
	}

	/**
	 * 导出勾选
	 * @param request 请求参数
	 * @param currentUser 当前用户
	 * @param currentOrg 当前组织
	 * @param locale 语言环境
	 * @return 任务ID
	 */
	public String exportSelect(ExportSelectRequest request, String currentUser, String currentOrg, Locale locale) {
		return asyncExport(request.getFileName(), currentOrg, currentUser, locale, (task) -> {
			//表头信息
			List<List<String>> headList = request.getHeadList().stream().map(head -> Collections.singletonList(head.getTitle())).toList();

			// 准备导出文件
			File file = prepareExportFile(task.getFileId(), request.getFileName(), task.getOrganizationId());
			try (ExcelWriter writer = EasyExcel.write(file).head(headList).excelType(ExcelTypeEnum.XLSX).build()) {
				WriteSheet sheet = EasyExcel.writerSheet("导出数据").build();
				SubListUtils.dealForSubList(request.getIds(), SubListUtils.DEFAULT_EXPORT_BATCH_SIZE, (subIds) -> {
					List<List<Object>> data = new ArrayList<>();
					try {
						data = getExportDataBySelect(request.getHeadList(), subIds, currentOrg, task.getId());
					} catch (InterruptedException e) {
						LogUtils.error("任务停止中断", e);
						exportTaskService.update(task.getId(), ExportConstants.ExportStatus.STOP.toString(), currentUser);
					}
					writer.write(data, sheet);
				});
			}
		});
	}

	/**
	 * 异步导出通用方法
	 * @param exportFileName 导出文件名
	 * @param currentOrg 当前组织
	 * @param currentUser 当前用户
	 * @param locale 语言环境
	 * @param executor 导出执行器
	 */
	public String asyncExport(String exportFileName, String currentOrg, String currentUser, Locale locale,
							ExportExecutor executor) {
		checkFileName(exportFileName);
		exportTaskService.checkUserTaskLimit(currentUser, ExportConstants.ExportStatus.PREPARED.toString());
		String fileId = IDGenerator.nextStr();
		ExportTask exportTask = exportTaskService.saveTask(currentOrg, fileId, currentUser, ExportConstants.ExportType.PRODUCT_PRICE.toString(), exportFileName);
		Thread.startVirtualThread(() -> {
			try {
				LocaleContextHolder.setLocale(locale);
				ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
				// 业务方法执行
				executor.execute(exportTask);
				exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), currentUser);
			} catch (Exception e) {
				LogUtils.error("Price export error: {}", e);
				exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), currentUser);
			} finally {
				ExportThreadRegistry.remove(exportTask.getId());
				exportLog(currentOrg, exportTask.getId(), currentUser, LogType.EXPORT, LogModule.PRODUCT_PRICE_MANAGEMENT, exportFileName);
			}
		});
		return exportTask.getId();
	}

	/**
	 * 获取导出数据 (分页查询)
	 * @param request 请求参数
	 * @param currentUser 当前用户
	 * @param currentOrg 当前组织
	 * @param taskId 任务ID
	 * @return 导出数据
	 */
	private List<List<Object>> getExportData(ProductPriceExportRequest request, String currentUser, String currentOrg, String taskId) {
		// TODO 查询导出数据
		return Collections.emptyList();
	}

	public List<List<Object>> getExportDataBySelect(List<ExportHeadDTO> headList, List<String> ids, String orgId, String taskId) throws InterruptedException {
		// TODO 查询导出数据
		return Collections.emptyList();
	}
}
