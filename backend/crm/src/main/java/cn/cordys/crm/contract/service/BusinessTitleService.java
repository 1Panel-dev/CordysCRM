package cn.cordys.crm.contract.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.constants.BusinessTitleType;
import cn.cordys.crm.contract.constants.ContractApprovalStatus;
import cn.cordys.crm.contract.domain.BusinessTitle;
import cn.cordys.crm.contract.domain.BusinessTitleConfig;
import cn.cordys.crm.contract.domain.ContractInvoice;
import cn.cordys.crm.contract.dto.request.BusinessTitleAddRequest;
import cn.cordys.crm.contract.dto.request.BusinessTitleApprovalRequest;
import cn.cordys.crm.contract.dto.request.BusinessTitlePageRequest;
import cn.cordys.crm.contract.dto.request.BusinessTitleUpdateRequest;
import cn.cordys.crm.contract.dto.response.BusinessTitleListResponse;
import cn.cordys.crm.contract.excel.domain.BusinessTitleExcelDataFactory;
import cn.cordys.crm.contract.excel.handler.BusinessTitleTemplateWriteHandler;
import cn.cordys.crm.contract.excel.listener.BusinessTitleCheckEventListener;
import cn.cordys.crm.contract.excel.listener.BusinessTitleImportEventListener;
import cn.cordys.crm.contract.mapper.ExtBusinessTitleMapper;
import cn.cordys.crm.opportunity.constants.ApprovalState;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.excel.domain.UserExcelDataFactory;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class BusinessTitleService {

    @Resource
    private BaseMapper<BusinessTitle> businessTitleMapper;
    @Resource
    private BaseMapper<ContractInvoice> contractInvoiceMapper;
    @Resource
    private ExtBusinessTitleMapper extBusinessTitleMapper;
    @Resource
    private LogService logService;
    @Resource
    private BaseService baseService;
    @Resource
    private BaseMapper<BusinessTitleConfig> businessTitleConfigMapper;


    /**
     * 添加工商抬头
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.BUSINESS_TITLE, type = LogType.ADD, resourceName = "{#request.businessName}")
    public BusinessTitle add(BusinessTitleAddRequest request, String userId, String orgId) {
        checkName(request.getBusinessName(), orgId, null);

        BusinessTitle businessTitle = BeanUtils.copyBean(new BusinessTitle(), request);
        if (Strings.CI.equals(BusinessTitleType.CUSTOM.name(), businessTitle.getType())) {
            businessTitle.setApprovalStatus(ContractApprovalStatus.APPROVING.name());
        } else {
            businessTitle.setApprovalStatus(ContractApprovalStatus.APPROVED.name());
        }
        businessTitle.setCreateTime(System.currentTimeMillis());
        businessTitle.setCreateUser(userId);
        businessTitle.setUpdateTime(System.currentTimeMillis());
        businessTitle.setUpdateUser(userId);
        businessTitle.setId(IDGenerator.nextStr());
        businessTitle.setOrganizationId(orgId);

        businessTitleMapper.insert(businessTitle);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(businessTitle.getId())
                        .resourceName(businessTitle.getBusinessName())
                        .modifiedValue(businessTitle)
                        .build()
        );
        return businessTitle;
    }


    /**
     * 校验名称
     *
     * @param businessName
     * @param orgId
     * @param id
     */
    private void checkName(String businessName, String orgId, String id) {
        if (extBusinessTitleMapper.countByName(businessName, orgId, id) > 0) {
            throw new GenericException(Translator.get("business_title.exist"));
        }
    }


    /**
     * 编辑工商抬头
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.BUSINESS_TITLE, type = LogType.UPDATE, resourceId = "{#request.id}")
    public BusinessTitle update(BusinessTitleUpdateRequest request, String userId, String orgId) {
        BusinessTitle oldTitle = checkTitle(request.getId());

        BusinessTitle newTitle = BeanUtils.copyBean(new BusinessTitle(), request);
        if (Strings.CI.equals(BusinessTitleType.CUSTOM.name(), newTitle.getType())) {
            newTitle.setApprovalStatus(ContractApprovalStatus.APPROVING.name());
        } else {
            newTitle.setApprovalStatus(ContractApprovalStatus.APPROVED.name());
        }
        newTitle.setUpdateTime(System.currentTimeMillis());
        newTitle.setUpdateUser(userId);
        businessTitleMapper.update(newTitle);

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(request.getBusinessName())
                        .originalValue(oldTitle)
                        .modifiedValue(newTitle)
                        .build()
        );
        return newTitle;
    }

    private BusinessTitle checkTitle(String id) {
        BusinessTitle title = businessTitleMapper.selectByPrimaryKey(id);
        if (title == null) {
            throw new GenericException(Translator.get("business_title.not.exist"));
        }
        return title;
    }


    /**
     * 删除
     *
     * @param id
     */
    @OperationLog(module = LogModule.CONTRACT_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        BusinessTitle businessTitle = checkTitle(id);
        if (!checkHasInvoice(id)) {
            businessTitleMapper.deleteByPrimaryKey(id);
            OperationLogContext.setResourceName(businessTitle.getBusinessName());
        }
    }


    /**
     * 校验是否开过票
     *
     * @param id
     * @return
     */
    public boolean checkHasInvoice(String id) {
        LambdaQueryWrapper<ContractInvoice> invoiceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        invoiceLambdaQueryWrapper.eq(ContractInvoice::getBusinessTitleId, id);
        List<ContractInvoice> contractInvoices = contractInvoiceMapper.selectListByLambda(invoiceLambdaQueryWrapper);
        return !CollectionUtils.isEmpty(contractInvoices);
    }


    /**
     * 列表
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    public Pager<List<BusinessTitleListResponse>> list(BusinessTitlePageRequest request, String userId, String orgId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<BusinessTitleListResponse> list = extBusinessTitleMapper.list(request, orgId, userId);
        baseService.setCreateAndUpdateUserName(list);
        return PageUtils.setPageInfo(page, list);
    }


    /**
     * 详情
     *
     * @param id
     * @return
     */
    public BusinessTitleListResponse get(String id) {
        BusinessTitle businessTitle = businessTitleMapper.selectByPrimaryKey(id);
        BusinessTitleListResponse businessTitleListResponse = BeanUtils.copyBean(new BusinessTitleListResponse(), businessTitle);
        baseService.setCreateAndUpdateUserName(List.of(businessTitleListResponse));
        return businessTitleListResponse;
    }


    /**
     * 审核通过/不通过
     *
     * @param request
     * @param userId
     * @param orgId
     */
    public void approvalContract(BusinessTitleApprovalRequest request, String userId, String orgId) {
        BusinessTitle businessTitle = checkTitle(request.getId());

        String state = businessTitle.getApprovalStatus();
        businessTitle.setApprovalStatus(request.getApprovalStatus());
        businessTitle.setUpdateTime(System.currentTimeMillis());
        businessTitle.setUpdateUser(userId);
        businessTitleMapper.update(businessTitle);
        // 添加日志上下文
        LogDTO logDTO = getApprovalLogDTO(orgId, request.getId(), userId, businessTitle.getBusinessName(), state, request.getApprovalStatus());
        logService.add(logDTO);
    }


    private LogDTO getApprovalLogDTO(String orgId, String id, String userId, String response, String state, String newState) {
        LogDTO logDTO = new LogDTO(orgId, id, userId, LogType.APPROVAL, LogModule.BUSINESS_TITLE, response);
        Map<String, String> oldMap = new HashMap<>();
        oldMap.put("approvalStatus", Translator.get("contract.approval_status." + state.toLowerCase()));
        logDTO.setOriginalValue(oldMap);
        Map<String, String> newMap = new HashMap<>();
        newMap.put("approvalStatus", Translator.get("contract.approval_status." + newState.toLowerCase()));
        logDTO.setModifiedValue(newMap);
        return logDTO;
    }


    /**
     * 撤销审核
     *
     * @param id
     * @param userId
     * @param orgId
     * @return
     */
    public String revoke(String id, String userId, String orgId) {
        BusinessTitle businessTitle = checkTitle(id);

        String originApprovalStatus = businessTitle.getApprovalStatus();

        businessTitle.setApprovalStatus(ApprovalState.REVOKED.toString());
        businessTitle.setUpdateUser(userId);
        businessTitle.setUpdateTime(System.currentTimeMillis());
        businessTitleMapper.update(businessTitle);


        // 添加日志上下文
        LogDTO logDTO = getApprovalLogDTO(orgId, id, userId, businessTitle.getBusinessName(), originApprovalStatus, ApprovalState.REVOKED.toString());
        logService.add(logDTO);

        return businessTitle.getApprovalStatus();
    }

    /**
     * 下载模板
     *
     * @param response
     * @param orgId
     */
    public void downloadImportTpl(HttpServletResponse response, String orgId) {
        //获取表头字段
        List<List<String>> heads = getTemplateHead();

        new EasyExcelExporter()
                .exportMultiSheetTplWithSharedHandler(response, heads,
                        Translator.get("business_title_import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
                        new BusinessTitleTemplateWriteHandler(heads, getBusinessTitleConfig(orgId)), new CustomHeadColWidthStyleStrategy());
    }


    private Map<String, Boolean> getBusinessTitleConfig(String orgId) {
        LambdaQueryWrapper<BusinessTitleConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(BusinessTitleConfig::getOrganizationId, orgId);
        List<BusinessTitleConfig> businessTitleConfigs = businessTitleConfigMapper.selectListByLambda(configWrapper);
        Map<String, Boolean> map = businessTitleConfigs.stream().collect(Collectors.toMap(BusinessTitleConfig::getField, BusinessTitleConfig::getRequired));
        return map;
    }

    private List<List<String>> getTemplateHead() {
        return new BusinessTitleExcelDataFactory().getExcelDataLocal().getHead();
    }


    /**
     * 导入检查
     *
     * @param file
     * @param orgId
     * @return
     */
    public ImportResponse importPreCheck(MultipartFile file, String orgId) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        return checkImportExcel(file, orgId);
    }


    /**
     * 检查导入文件
     *
     * @param file
     * @param orgId
     * @return
     */
    private ImportResponse checkImportExcel(MultipartFile file, String orgId) {
        try {
            Class<?> clazz = new UserExcelDataFactory().getExcelDataByLocal();
            BusinessTitleCheckEventListener eventListener = new BusinessTitleCheckEventListener(clazz, getBusinessTitleConfig(orgId), orgId);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }


    /**
     * 导入
     *
     * @param file
     * @param userId
     * @param orgId
     * @return
     */
    public ImportResponse realImport(MultipartFile file, String userId, String orgId) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        try {
            Class<?> clazz = new UserExcelDataFactory().getExcelDataByLocal();
            Consumer<List<BusinessTitle>> afterDto = (businessTitles) -> {
                List<LogDTO> logs = new ArrayList<>();
                businessTitles.forEach(title -> {
                    title.setType(BusinessTitleType.CUSTOM.name());
                    logs.add(new LogDTO(orgId, title.getId(), userId, LogType.ADD, LogModule.CUSTOMER_INDEX, title.getBusinessName()));
                });
                businessTitleMapper.batchInsert(businessTitles);
                logService.batchAdd(logs);
            };
            BusinessTitleImportEventListener eventListener = new BusinessTitleImportEventListener(clazz, BusinessTitle.class, getBusinessTitleConfig(orgId), orgId, userId, afterDto);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("importExcel error", e);
            throw new GenericException(e.getMessage());
        }
    }
}
