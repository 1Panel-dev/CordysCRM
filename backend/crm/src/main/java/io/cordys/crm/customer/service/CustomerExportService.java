package io.cordys.crm.customer.service;

import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.ExportHeadDTO;
import io.cordys.common.service.BaseExportService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.customer.dto.request.CustomerExportRequest;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.customer.utils.CustomerFieldUtils;
import io.cordys.crm.system.constants.ExportConstants;
import io.cordys.crm.system.domain.ExportTask;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.service.ExportTaskService;
import io.cordys.registry.ExportThreadRegistry;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerExportService extends BaseExportService {

    @Resource
    private CustomerService customerService;
    @Resource
    private ExportTaskService exportTaskService;
    @Resource
    private ExtCustomerMapper extCustomerMapper;


    public String export(String userId, CustomerExportRequest request, String orgId, DeptDataPermissionDTO deptDataPermission) {
        String fileId = IDGenerator.nextStr();
        ExportTask exportTask = exportTaskService.saveTask(orgId, fileId, userId, ExportConstants.ExportType.CUSTOMER.toString(), request.getFileName());
        Thread.startVirtualThread(() -> {
            try {
                ExportThreadRegistry.register(exportTask.getId(), Thread.currentThread());
                //表头信息
                List<List<String>> headList = request.getHeadList().stream()
                        .map(head -> Collections.singletonList(head.getTitle()))
                        .toList();

                //获取导出数据
                List<List<Object>> data = getExportData(request.getHeadList(), request, userId, orgId, deptDataPermission, exportTask.getId());
                if (CollectionUtils.isNotEmpty(data)) {
                    //导出
                    exportData(headList, fileId, request.getFileName(), data, exportTask.getId());
                    //更新导出任务状态
                    exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.SUCCESS.toString(), userId);
                }
            } catch (InterruptedException e) {
                LogUtils.error("任务停止中断", e);
            } catch (Exception e) {
                //更新任务
                exportTaskService.update(exportTask.getId(), ExportConstants.ExportStatus.ERROR.toString(), userId);
            } finally {
                //从注册中心移除
                ExportThreadRegistry.remove(exportTask.getId());
                //日志
                exportLog(orgId, exportTask.getId(), userId, LogType.EXPORT, LogModule.CUSTOMER_INDEX, request.getFileName());
            }
        });
        return exportTask.getId();
    }


    /**
     * 构建导出的数据
     *
     * @param headList 表头列表
     * @param request 导出请求
     * @param userId 用户ID
     * @param orgId 组织id
     * @param deptDataPermission 部门数据权限
     * @return 导出数据列表
     */
    private List<List<Object>> getExportData(List<ExportHeadDTO> headList, CustomerExportRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission, String taskId) throws InterruptedException {
        //获取数据
        List<CustomerListResponse> allList = extCustomerMapper.list(request, orgId, userId, deptDataPermission);
        List<CustomerListResponse> dataList = customerService.buildListData(allList, orgId);
        Map<String, BaseField> fieldConfigMap = getFieldConfigMap(FormKey.CUSTOMER.getKey(), orgId);
        //构建导出数据
        List<List<Object>> data = new ArrayList<>();
        for (CustomerListResponse response : dataList) {
            if (ExportThreadRegistry.isStop(taskId)) {
                throw new InterruptedException("线程已被中断，主动退出");
            }
            List<Object> value = buildData(headList, response, fieldConfigMap);
            data.add(value);
        }

        return data;
    }

    private List<Object> buildData(List<ExportHeadDTO> headList, CustomerListResponse data, Map<String, BaseField> fieldConfigMap) {
        List<Object> dataList = new ArrayList<>();
        //固定字段map
        LinkedHashMap<String, Object> systemFiledMap = CustomerFieldUtils.getSystemFieldMap(data);
        //自定义字段map
        Map<String, Object> moduldFieldMap = data.getModuleFields().stream()
                .collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue));
        //处理数据转换
        transModuleFieldValue(headList, systemFiledMap, moduldFieldMap, dataList, fieldConfigMap);
        return dataList;
    }


}
