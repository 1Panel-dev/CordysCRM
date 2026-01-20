package cn.cordys.crm.product.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.*;
import cn.cordys.crm.opportunity.domain.OpportunityQuotationField;
import cn.cordys.crm.opportunity.domain.OpportunityQuotationFieldBlob;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.product.domain.ProductPriceField;
import cn.cordys.crm.product.domain.ProductPriceFieldBlob;
import cn.cordys.crm.product.dto.request.ProductPriceAddRequest;
import cn.cordys.crm.product.dto.request.ProductPriceEditRequest;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.dto.response.ProductPriceGetResponse;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;
import cn.cordys.crm.product.mapper.ExtProductPriceMapper;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldMergeCellEventListener;
import cn.cordys.crm.system.service.*;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.enums.CellExtraTypeEnum;
import cn.idev.excel.metadata.CellExtra;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ProductPriceService {

    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
	@Resource
	private ModuleFieldExtService moduleFieldExtService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private BaseMapper<ProductPrice> productPriceMapper;
    @Resource
    private BaseMapper<ProductPriceField> productPriceFieldMapper;
    @Resource
    private BaseMapper<ProductPriceFieldBlob> productPriceFieldBlobMapper;
    @Resource
    private ProductPriceFieldService productPriceFieldService;
    @Resource
    private ExtProductPriceMapper extProductPriceMapper;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<OpportunityQuotationField> opportunityFieldMapper;
    @Resource
    private BaseMapper<OpportunityQuotationFieldBlob> opportunityQuotationFieldBlobBaseMapper;
    @Resource
    private AttachmentService attachmentService;

    public static final int MAX_NAME_SPLIT_LENGTH = 243;

    /**
     * 价格列表
     *
     * @param request    请求参数
     * @param currentOrg 当前组织
     *
     * @return 价格列表
     */
    public PagerWithOption<List<ProductPriceResponse>> list(ProductPricePageRequest request, String currentOrg) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ProductPriceResponse> list = extProductPriceMapper.list(request, currentOrg);
        List<ProductPriceResponse> results = buildList(list);
        // 处理自定义字段选项
        ModuleFormConfigDTO priceFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), currentOrg);
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(results, ProductPriceResponse::getModuleFields);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(priceFormConfig, moduleFieldValues);
        return PageUtils.setPageInfoWithOption(page, processList(results, priceFormConfig), optionMap);
    }

    /**
     * 新增价格表
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param currentOrg  当前组织
     *
     * @return 价格表
     */
    @OperationLog(module = LogModule.PRODUCT_PRICE_MANAGEMENT, type = LogType.ADD, resourceName = "{#request.name}", operator = "{#currentUser}")
    public ProductPrice add(ProductPriceAddRequest request, String currentUser, String currentOrg) {
        ProductPrice productPrice = BeanUtils.copyBean(new ProductPrice(), request);
        productPrice.setId(IDGenerator.nextStr());
        productPrice.setOrganizationId(currentOrg);
        productPrice.setPos(getNextOrder(currentOrg));
        productPrice.setCreateTime(System.currentTimeMillis());
        productPrice.setUpdateTime(System.currentTimeMillis());
        productPrice.setCreateUser(currentUser);
        productPrice.setUpdateUser(currentUser);
        // 设置子表格字段值
        request.getModuleFields().add(new BaseModuleFieldValue("products", request.getProducts()));
        productPriceFieldService.saveModuleField(productPrice, currentOrg, currentUser, request.getModuleFields(), false);
        productPriceMapper.insert(productPrice);
        // 处理日志上下文
        ModuleFormConfigDTO priceFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), currentOrg);
        baseService.handleAddLogWithSubTable(productPrice, request.getModuleFields(), Translator.get("products_info"), priceFormConfig);
        return productPrice;
    }

    /**
     * 修改价格表
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param currentOrg  当前组织
     *
     * @return 价格表
     */
    @OperationLog(module = LogModule.PRODUCT_PRICE_MANAGEMENT, type = LogType.UPDATE, operator = "{#currentUser}")
    public ProductPrice update(ProductPriceEditRequest request, String currentUser, String currentOrg) {
        ProductPrice oldPrice = productPriceMapper.selectByPrimaryKey(request.getId());
        if (oldPrice == null) {
            throw new GenericException(Translator.get("product.price.not.exist"));
        }
        List<BaseModuleFieldValue> originFields = productPriceFieldService.getModuleFieldValuesByResourceId(request.getId());
        ProductPrice productPrice = BeanUtils.copyBean(new ProductPrice(), request);
        productPrice.setUpdateTime(System.currentTimeMillis());
        productPrice.setUpdateUser(currentUser);
        // 设置子表格字段值
        request.getModuleFields().add(new BaseModuleFieldValue("products", request.getProducts()));
        updateFields(request.getModuleFields(), productPrice, currentOrg, currentUser);
        productPriceMapper.update(productPrice);
        // 处理日志上下文
        ModuleFormConfigDTO priceFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), currentOrg);
        baseService.handleUpdateLogWithSubTable(oldPrice, productPrice, originFields, request.getModuleFields(), request.getId(), productPrice.getName(), Translator.get("products_info"), priceFormConfig);
        return productPriceMapper.selectByPrimaryKey(request.getId());
    }

    /**
     * ⚠️反射调用; 勿修改入参, 返回, 方法名!
     *
     * @param id 价格表ID
     *
     * @return 价格表详情
     */
    public ProductPriceGetResponse get(String id) {
        ProductPrice price = productPriceMapper.selectByPrimaryKey(id);
        if (price == null) {
            return null;
        }
        ProductPriceGetResponse priceDetail = BeanUtils.copyBean(new ProductPriceGetResponse(), price);
        // 处理自定义字段(包括详情附件)
        List<BaseModuleFieldValue> fieldValues = productPriceFieldService.getModuleFieldValuesByResourceId(id);
        ModuleFormConfigDTO priceFormConf = moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), price.getOrganizationId());
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(priceFormConf, fieldValues);
        priceDetail.setOptionMap(optionMap);
        moduleFormService.processBusinessFieldValues(priceDetail, fieldValues, priceFormConf);
        priceDetail.setAttachmentMap(moduleFormService.getAttachmentMap(priceFormConf, priceDetail.getModuleFields()));
        return baseService.setCreateAndUpdateUserName(priceDetail);
    }

    /**
     * 删除价格表
     *
     * @param id 价格表ID
     */
    @OperationLog(module = LogModule.PRODUCT_PRICE_MANAGEMENT, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        // 1. 查询价格表，不存在则抛错
        ProductPrice price = Optional.ofNullable(
                productPriceMapper.selectByPrimaryKey(id)
        ).orElseThrow(() -> new GenericException(
                Translator.get("product.price.not.exist")
        ));

        // 2. 检查是否被引用（报价单等）
        List<OpportunityQuotationField> opportunityFields = opportunityFieldMapper.selectListByLambda(
                new LambdaQueryWrapper<OpportunityQuotationField>()
                        .eq(OpportunityQuotationField::getFieldValue, id)
        );

        List<OpportunityQuotationFieldBlob> opportunityBlobFields = opportunityQuotationFieldBlobBaseMapper.selectListByLambda(
                new LambdaQueryWrapper<OpportunityQuotationFieldBlob>()
                        .eq(OpportunityQuotationFieldBlob::getFieldValue, id)
        );


        if (!opportunityFields.isEmpty() || !opportunityBlobFields.isEmpty()) {
            throw new GenericException(
                    Translator.get("product.price.in.use.cannot.delete")
            );
        }

        // 3. 删除主表和自定义字段表数据
        productPriceMapper.deleteByPrimaryKey(id);
        productPriceFieldService.deleteByResourceId(id);

        // 4. 记录日志上下文（用于审计）
        OperationLogContext.setResourceName(price.getName());
    }

    /**
     * 复制价格表
     *
     * @param id          价格表ID
     * @param currentUser 当前用户
     * @param currentOrg  当前组织
     *
     * @return 复制后价格表
     */
    @OperationLog(module = LogModule.PRODUCT_PRICE_MANAGEMENT, type = LogType.ADD, operator = "{#currentUser}")
    public ProductPrice copy(String id, String currentUser, String currentOrg) {
        // 1. 查询价格表，不存在则抛错
        ProductPrice price = Optional.ofNullable(productPriceMapper.selectByPrimaryKey(id))
                .orElseThrow(() -> new GenericException(Translator.get("product.price.not.exist")));
        // 2. 复制价格表基础信息
        price.setId(IDGenerator.nextStr());
        price.setPos(getNextOrder(currentOrg));
        if (price.getName() != null && price.getName().length() > MAX_NAME_SPLIT_LENGTH) {
            price.setName(price.getName().substring(0, 243));
        }
        price.setName(price.getName() + "_copy_" + RandomStringUtils.random(6, 0, 0, true, true, null, new Random()));
        price.setCreateUser(currentUser);
        price.setCreateTime(System.currentTimeMillis());
        price.setUpdateUser(currentUser);
        price.setUpdateTime(System.currentTimeMillis());
        price.setOrganizationId(currentOrg);
        productPriceMapper.insert(price);
        // 3. 复制价格表自定义字段信息
        copyPriceFields(id, price.getId(), currentOrg, currentUser);
        // 4. 处理日志上下文
        ModuleFormConfigDTO priceFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), currentOrg);
        List<BaseModuleFieldValue> fvs = productPriceFieldService.getModuleFieldValuesByResourceId(price.getId());
        baseService.handleAddLogWithSubTable(price, fvs, Translator.get("products_info"), priceFormConfig);
        OperationLogContext.getContext().setResourceId(price.getId());
        OperationLogContext.getContext().setResourceName(price.getName());
        return price;
    }

    /**
     * 批量更新价格表
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param currentOrg  当前组织
     */
    public void batchUpdate(ResourceBatchEditRequest request, String currentUser, String currentOrg) {
        BaseField field = productPriceFieldService.getAndCheckField(request.getFieldId(), currentOrg);
        List<ProductPrice> prices = productPriceMapper.selectByIds(request.getIds());
        productPriceFieldService.batchUpdate(request, field, prices, ProductPrice.class,
                LogModule.PRODUCT_PRICE_MANAGEMENT, extProductPriceMapper::batchUpdate, currentUser, currentOrg);
    }

    /**
     * 下载导入的模板
     *
     * @param response 响应
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
        new EasyExcelExporter().exportMultiSheetTplWithSharedHandler(response,
                moduleFormService.getCustomImportHeadsNoRef(FormKey.PRICE.getKey(), currentOrg),
                Translator.get("product.price.import_tpl.name"),
                Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
                new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(FormKey.PRICE.getKey(), currentOrg)),
                new CustomHeadColWidthStyleStrategy()
        );
    }

    /**
     * 导入检查
     *
     * @param file       导入文件
     * @param currentOrg 当前组织
     *
     * @return 导入检查信息
     */
    public ImportResponse importPreCheck(MultipartFile file, String currentOrg) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        return checkImportExcel(file, currentOrg);
    }

    /**
     * 检查导入的文件
     *
     * @param file       文件
     * @param currentOrg 当前组织
     *
     * @return 检查信息
     */
    private ImportResponse checkImportExcel(MultipartFile file, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(
                    FormKey.PRICE.getKey(),
                    currentOrg
            );

            boolean supportSubHead = moduleFormService.supportSubHead(fields);
            int headRowNumber = supportSubHead ? 2 : 1;

            // 1 先读取合并单元格信息
            CustomFieldMergeCellEventListener mergeCellEventListener =
                    new CustomFieldMergeCellEventListener();

            FastExcelFactory.read(file.getInputStream(), mergeCellEventListener)
                    .extraRead(CellExtraTypeEnum.MERGE)
                    .headRowNumber(headRowNumber)
                    .ignoreEmptyRow(true)
                    .sheet()
                    .doRead();

            // 2 校验数据
            CustomFieldCheckEventListener eventListener =
                    new CustomFieldCheckEventListener(
                            fields,
                            "product_price",
                            "product_price_field",
                            currentOrg,
                            mergeCellEventListener.getMergeCellMap(),
                            mergeCellEventListener.getMergeRowDataMap()
                    );

            FastExcelFactory.read(file.getInputStream(), eventListener)
                    .headRowNumber(headRowNumber)
                    .ignoreEmptyRow(true)
                    .sheet()
                    .doRead();

            return ImportResponse.builder()
                    .errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess())
                    .failCount(eventListener.getErrList().size())
                    .build();

        } catch (Exception e) {
            log.error("price import pre-check error: {}", e.getMessage(), e);
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 价格表导入
     *
     * @param file        导入文件
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     *
     * @return 导入返回信息
     */
    public ImportResponse realImport(MultipartFile file, String currentOrg, String currentUser) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(
                    FormKey.PRICE.getKey(),
                    currentOrg
            );

            boolean supportSubHead = moduleFormService.supportSubHead(fields);
            int headRowNumber = supportSubHead ? 2 : 1;

            // 1 读取合并单元格信息
            CustomFieldMergeCellEventListener mergeCellEventListener =
                    new CustomFieldMergeCellEventListener();

            FastExcelFactory.read(file.getInputStream(), mergeCellEventListener)
                    .extraRead(CellExtraTypeEnum.MERGE)
                    .headRowNumber(headRowNumber)
                    .ignoreEmptyRow(true)
                    .sheet()
                    .doRead();

            // 2 实际导入
            CustomFieldImportEventListener<ProductPrice> eventListener =
                    getPriceEventListener(
                            currentOrg,
                            currentUser,
                            fields,
                            mergeCellEventListener.getMergeCellMap(),
                            mergeCellEventListener.getMergeRowDataMap()
                    );

            FastExcelFactory.read(file.getInputStream(), eventListener)
                    .extraRead(CellExtraTypeEnum.MERGE)
                    .headRowNumber(headRowNumber)
                    .ignoreEmptyRow(true)
                    .sheet()
                    .doRead();

            return ImportResponse.builder()
                    .errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount())
                    .failCount(eventListener.getErrList().size())
                    .build();

        } catch (Exception e) {
            log.error("价格表导入失败, 原因: {}", e.getMessage(), e);
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 价格表导入监听器
     *
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     * @param fields      自定义字段集合
     *
     * @return 导入监听器
     */
    private CustomFieldImportEventListener<ProductPrice> getPriceEventListener(
            String currentOrg,
            String currentUser,
            List<BaseField> fields,
            Map<Integer, List<CellExtra>> mergeCellMap,
            Map<Integer, Map<Integer, String>> mergeRowDataMap) {

        AtomicLong initPos = new AtomicLong(getNextOrder(currentOrg));

        CustomImportAfterDoConsumer<ProductPrice, BaseResourceSubField> afterDo =
                (prices, priceFields, priceFieldBlobs) -> {

                    List<LogDTO> logs = new ArrayList<>();

                    prices.forEach(price -> {
                        price.setPos(initPos.getAndAdd(ServiceUtils.POS_STEP));
                        logs.add(new LogDTO(
                                currentOrg,
                                price.getId(),
                                currentUser,
                                LogType.ADD,
                                LogModule.PRODUCT_PRICE_MANAGEMENT,
                                price.getName()
                        ));
                    });

                    productPriceMapper.batchInsert(prices);

                    productPriceFieldMapper.batchInsert(
                            priceFields.stream()
                                    .map(field -> BeanUtils.copyBean(new ProductPriceField(), field))
                                    .toList()
                    );

                    productPriceFieldBlobMapper.batchInsert(
                            priceFieldBlobs.stream()
                                    .map(field -> BeanUtils.copyBean(new ProductPriceFieldBlob(), field))
                                    .toList()
                    );

                    // record logs
                    logService.batchAdd(logs);
                };

        return new CustomFieldImportEventListener<>(
                fields,
                ProductPrice.class,
                currentOrg,
                currentUser,
                "product_price_field",
                afterDo,
                2000,
                mergeCellMap,
                mergeRowDataMap
        );
    }

    /**
     * 构建列表数据
     *
     * @param listData 列表数据
     *
     * @return 列表数据
     */
    public List<ProductPriceResponse> buildList(List<ProductPriceResponse> listData) {
        // 查询列表数据的自定义字段
        Map<String, List<BaseModuleFieldValue>> dataFieldMap = productPriceFieldService.getResourceFieldMap(
                listData.stream().map(ProductPriceResponse::getId).toList(), true);
        // 列表项设置自定义字段&&用户名
        listData.forEach(item -> item.setModuleFields(dataFieldMap.get(item.getId())));
        return baseService.setCreateAndUpdateUserName(listData);
    }

    /**
     * 处理列表数据
     *
     * @param listData 列表数据
     *
     * @return 列表数据
     */
    public List<ProductPriceResponse> processList(List<ProductPriceResponse> listData, ModuleFormConfigDTO priceFormConf) {
        // 查询列表数据的自定义字段
        Map<String, List<BaseModuleFieldValue>> dataFieldMap = productPriceFieldService.getResourceFieldMap(
                listData.stream().map(ProductPriceResponse::getId).toList(), true);
        // 列表项设置自定义字段&&用户名
        listData.forEach(item -> {
            if (!dataFieldMap.containsKey(item.getId())) {
                return;
            }
            moduleFormService.processBusinessFieldValues(item, dataFieldMap.get(item.getId()), priceFormConf);
        });
        return baseService.setCreateAndUpdateUserName(listData);
    }

    /**
     * 更新自定义字段
     *
     * @param fields      自定义字段集合
     * @param price       价格表
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     */
    private void updateFields(List<BaseModuleFieldValue> fields, ProductPrice price, String currentOrg, String currentUser) {
        if (fields == null) {
            return;
        }
        productPriceFieldService.deleteByResourceId(price.getId());
        productPriceFieldService.saveModuleField(price, currentOrg, currentUser, fields, true);
    }

    /**
     * 拖拽排序
     *
     * @param request 请求参数
     */
    public void editPos(PosRequest request) {
        ServiceUtils.updatePosFieldByAsc(request,
                ProductPrice.class,
                null,
                null,
                productPriceMapper::selectByPrimaryKey,
                extProductPriceMapper::getPrePos,
                extProductPriceMapper::getLastPos,
                productPriceMapper::update);
    }

    /**
     * 获取下一个排序值
     *
     * @param orgId 组织ID
     *
     * @return 下一个排序值
     */
    public Long getNextOrder(String orgId) {
        Long pos = extProductPriceMapper.getPos(orgId);
        return (pos == null ? 0 : pos) + ServiceUtils.POS_STEP;
    }

    /**
     * 获取价格表名称
     *
     * @param id id
     *
     * @return 名称
     */
    public String getProductPriceName(String id) {
        ProductPrice productPrice = productPriceMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(productPrice).map(ProductPrice::getName).orElse(null);
    }

    /**
     * 通过ID集合获取价格表名称串
     *
     * @param ids ID集合
     *
     * @return 名称字符串
     */
    public String getProductPriceNameByIds(List<String> ids) {
        List<ProductPrice> productPrices = productPriceMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(productPrices)) {
            List<String> names = productPrices.stream().map(ProductPrice::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }

    /**
     * 通过名称获取价格表集合
     *
     * @param names 名称集合
     *
     * @return 价格表集合
     */
    public List<ProductPrice> getProductPriceListByNames(List<String> names) {
        LambdaQueryWrapper<ProductPrice> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(ProductPrice::getName, names);
        return productPriceMapper.selectListByLambda(lambdaQueryWrapper);
    }

    /**
     * 复制价格表自定义字段信息
     *
     * @param sourceId 源价格表ID
     * @param targetId 目标价格表ID
     */
    private void copyPriceFields(String sourceId, String targetId, String currentOrg, String currentUser) {

        // 1 普通字段
        LambdaQueryWrapper<ProductPriceField> fieldQuery = new LambdaQueryWrapper<>();
        fieldQuery.eq(ProductPriceField::getResourceId, sourceId);
        List<ProductPriceField> sourceFields = productPriceFieldMapper.selectListByLambda(fieldQuery);

        if (CollectionUtils.isNotEmpty(sourceFields)) {
            List<ProductPriceField> targetFields = sourceFields.stream()
                    .peek(field -> {
                        field.setId(IDGenerator.nextStr());
                        field.setResourceId(targetId);
                    })
                    .toList();

            productPriceFieldMapper.batchInsert(targetFields);
        }

        // 2 Blob 字段（含附件）
        LambdaQueryWrapper<ProductPriceFieldBlob> blobQuery = new LambdaQueryWrapper<>();
        blobQuery.eq(ProductPriceFieldBlob::getResourceId, sourceId);
        List<ProductPriceFieldBlob> sourceBlobs = productPriceFieldBlobMapper.selectListByLambda(blobQuery);

        Map<String, String> attachmentIdMap = new HashMap<>(8);
        List<String> attachmentFieldIds = moduleFieldExtService.getFieldIdsOfForm(FormKey.PRICE.getKey(), currentOrg);

        if (CollectionUtils.isNotEmpty(sourceBlobs)) {
            List<ProductPriceFieldBlob> targetBlobs = sourceBlobs.stream()
                    .peek(blob -> {
                        blob.setId(IDGenerator.nextStr());
                        blob.setResourceId(targetId);

                        if (attachmentFieldIds.contains(blob.getFieldId()) && blob.getFieldValue() != null) {

                            List<String> attachmentIds = JSON.parseArray(
                                    blob.getFieldValue().toString(),
                                    String.class
                            );

                            List<String> copyAttachmentIds = new ArrayList<>();
                            attachmentIds.forEach(id -> {
                                attachmentIdMap.putIfAbsent(id, IDGenerator.nextStr());
                                copyAttachmentIds.add(attachmentIdMap.get(id));
                            });

                            blob.setFieldValue(JSON.toJSONString(copyAttachmentIds));
                        }
                    })
                    .toList();

            productPriceFieldBlobMapper.batchInsert(targetBlobs);
        }

        // 3 复制附件实体
        attachmentService.batchCopyOfIdMap(attachmentIdMap, targetId, currentUser);
    }
}
