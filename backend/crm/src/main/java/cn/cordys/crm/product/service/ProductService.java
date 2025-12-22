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
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.ServiceUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.product.domain.ProductField;
import cn.cordys.crm.product.domain.ProductFieldBlob;
import cn.cordys.crm.product.dto.request.ProductEditRequest;
import cn.cordys.crm.product.dto.request.ProductPageRequest;
import cn.cordys.crm.product.dto.response.ProductGetResponse;
import cn.cordys.crm.product.dto.response.ProductListResponse;
import cn.cordys.crm.product.mapper.ExtProductMapper;
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
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author guoyuqi
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProductService {

    @Resource
    private BaseMapper<Product> productBaseMapper;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ProductFieldService productFieldService;
    @Resource
    private LogService logService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<ProductField> productFieldMapper;
    @Resource
    private BaseMapper<ProductFieldBlob> productFieldBlobMapper;

    public PagerWithOption<List<ProductListResponse>> list(ProductPageRequest request, String orgId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ProductListResponse> list = extProductMapper.list(request, orgId);
        List<ProductListResponse> buildList = buildListData(list);
        // 处理自定义字段选项数据
        ModuleFormConfigDTO productFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRODUCT.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ProductListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(productFormConfig, moduleFieldValues);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    private List<ProductListResponse> buildListData(List<ProductListResponse> list) {
        List<String> productIds = list.stream().map(ProductListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> productFiledMap = productFieldService.getResourceFieldMap(productIds, true);

        list.forEach(productListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> productFields = productFiledMap.get(productListResponse.getId());
            productListResponse.setModuleFields(productFields);
        });

        return baseService.setCreateAndUpdateUserName(list);
    }

	/**
	 * ⚠️反射调用; 勿修改入参, 返回, 方法名!
	 * @param id 产品ID
	 * @return 产品详情
	 */
    public ProductGetResponse get(String id) {
        Product product = productBaseMapper.selectByPrimaryKey(id);
		if (product == null) {
			return null;
		}
        ProductGetResponse productGetResponse = BeanUtils.copyBean(new ProductGetResponse(), product);

        // 获取模块字段
        List<BaseModuleFieldValue> productFields = productFieldService.getModuleFieldValuesByResourceId(id);

        productGetResponse.setModuleFields(productFields);
        ModuleFormConfigDTO productFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRODUCT.getKey(), product.getOrganizationId());
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(productFormConfig, productFields);
        productGetResponse.setOptionMap(optionMap);
        // 附件信息
        productGetResponse.setAttachmentMap(moduleFormService.getAttachmentMap(productFormConfig, productFields));
        return baseService.setCreateAndUpdateUserName(productGetResponse);
    }

    @OperationLog(module = LogModule.PRODUCT_MANAGEMENT, type = LogType.ADD, resourceName = "{#request.name}", operator = "{#userId}")
    public Product add(ProductEditRequest request, String userId, String orgId) {
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStatus(request.getStatus());
        product.setCreateTime(System.currentTimeMillis());
        product.setUpdateTime(System.currentTimeMillis());
        product.setPos(getNextOrder(orgId));
        product.setUpdateUser(userId);
        product.setCreateUser(userId);
        product.setOrganizationId(orgId);
        product.setId(IDGenerator.nextStr());

        //保存自定义字段
        productFieldService.saveModuleField(product, orgId, userId, request.getModuleFields(), false);

        productBaseMapper.insert(product);

        // 添加日志上下文
        baseService.handleAddLog(product, request.getModuleFields());
        return product;
    }

    @OperationLog(module = LogModule.PRODUCT_MANAGEMENT, type = LogType.UPDATE, operator = "{#userId}")
    public Product update(ProductEditRequest request, String userId, String orgId) {
        if (StringUtils.isBlank(request.getId())) {
            throw new GenericException(Translator.get("product.id.empty"));
        }
        Product oldProduct = productBaseMapper.selectByPrimaryKey(request.getId());
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setUpdateTime(System.currentTimeMillis());
        product.setUpdateUser(userId);
        product.setOrganizationId(orgId);

        // 获取模块字段
        List<BaseModuleFieldValue> originCustomerFields = productFieldService.getModuleFieldValuesByResourceId(request.getId());

        // 更新模块字段
        updateModuleField(product, request.getModuleFields(), orgId, userId);
        productBaseMapper.update(product);

        //添加日志
        baseService.handleUpdateLog(oldProduct, product, originCustomerFields, request.getModuleFields(), request.getId(), product.getName());

        return productBaseMapper.selectByPrimaryKey(product.getId());
    }

    private void updateModuleField(Product product, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        productFieldService.deleteByResourceId(product.getId());
        // 再保存
        productFieldService.saveModuleField(product, orgId, userId, moduleFields, true);
    }

    private void batchUpdateModuleField(List<String> productIds, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        productFieldService.deleteByResourceIds(productIds);
        // 再保存
        productFieldService.saveModuleFieldByResourceIds(productIds, moduleFields);
    }

    @OperationLog(module = LogModule.PRODUCT_MANAGEMENT, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Product product = productBaseMapper.selectByPrimaryKey(id);
        // 删除产品
        productBaseMapper.deleteByPrimaryKey(id);
        // 删除产品模块字段
        productFieldService.deleteByResourceId(id);
        // 添加日志上下文
        OperationLogContext.setResourceName(product.getName());
    }

    public void batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = productFieldService.getAndCheckField(request.getFieldId(), organizationId);
        List<Product> products = productBaseMapper.selectByIds(request.getIds());
        productFieldService.batchUpdate(request, field, products, Product.class, LogModule.PRODUCT_MANAGEMENT, extProductMapper::batchUpdate, userId, organizationId);
    }

    /**
     * 批量删除产品
     *
     * @param ids    产品id集合
     * @param userId 操作人id
     */
    public void batchDelete(List<String> ids, String userId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Product::getId, ids);
        List<Product> products = productBaseMapper.selectListByLambda(wrapper);
        productBaseMapper.deleteByIds(ids);
        productFieldService.deleteByResourceIds(ids);
        List<LogDTO> logs = new ArrayList<>();
        products.forEach(product -> {
            LogDTO logDTO = new LogDTO(product.getOrganizationId(), product.getId(), userId, LogType.DELETE, LogModule.PRODUCT_MANAGEMENT, product.getName());
            logDTO.setOriginalValue(product.getName());
            logs.add(logDTO);
        });
        logService.batchAdd(logs);
    }

    public void checkProductList(List<String> products) {
        if (products != null && products.size() > 20) {
            throw new GenericException(Translator.get("product.length"));
        }
    }

    public Long getNextOrder(String orgId) {
        Long pos = extProductMapper.getPos(orgId);
        return (pos == null ? 0 : pos) + ServiceUtils.POS_STEP;
    }

    public void editPos(PosRequest request) {

        ServiceUtils.updatePosFieldByAsc(request,
                Product.class,
                null,
                null,
                productBaseMapper::selectByPrimaryKey,
                extProductMapper::getPrePos,
                extProductMapper::getLastPos,
                productBaseMapper::update);
    }

    public List<Product> getProductListByNames(List<String> names) {
        LambdaQueryWrapper<Product> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Product::getName, names);
        return productBaseMapper.selectListByLambda(lambdaQueryWrapper);
    }

    public String getProductName(String id) {
        Product product = productBaseMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(product).map(Product::getName).orElse(null);
    }

    public String getProductNameByIds(List<String> ids) {
        List<Product> productList = productBaseMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(productList)) {
            List<String> names = productList.stream().map(Product::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }

    /**
     * 下载导入的模板
     *
     * @param response 响应
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
        new EasyExcelExporter()
                .exportMultiSheetTplWithSharedHandler(response, moduleFormService.getCustomImportHeadsNoRef(FormKey.PRODUCT.getKey(), currentOrg),
                        Translator.get("product.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
						new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(FormKey.PRODUCT.getKey(), currentOrg)), new CustomHeadColWidthStyleStrategy());
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
     * 产品导入
     *
     * @param file        导入文件
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     *
     * @return 导入返回信息
     */
    public ImportResponse realImport(MultipartFile file, String currentOrg, String currentUser) {
        try {
			AtomicLong initPos = new AtomicLong(getNextOrder(currentOrg));
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.PRODUCT.getKey(), currentOrg);
            CustomImportAfterDoConsumer<Product, BaseResourceSubField> afterDo = (products, productFields, productFieldBlobs) -> {
                List<LogDTO> logs = new ArrayList<>();
                products.forEach(product -> {
                    product.setPos(initPos.getAndAdd(ServiceUtils.POS_STEP));
                    logs.add(new LogDTO(currentOrg, product.getId(), currentUser, LogType.ADD, LogModule.PRODUCT_MANAGEMENT, product.getName()));
                });
                productBaseMapper.batchInsert(products);
                productFieldMapper.batchInsert(productFields.stream().map(field -> BeanUtils.copyBean(new ProductField(), field)).toList());
                productFieldBlobMapper.batchInsert(productFieldBlobs.stream().map(field -> BeanUtils.copyBean(new ProductFieldBlob(), field)).toList());
                // record logs
                logService.batchAdd(logs);
            };
            CustomFieldImportEventListener<Product> eventListener = new CustomFieldImportEventListener<>(fields, Product.class, currentOrg, currentUser,
                    "product_field", afterDo, 2000, null, null);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("product import error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
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
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(FormKey.PRODUCT.getKey(), currentOrg);
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "product", "product_field", currentOrg);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("product import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 获取产品选项列表
     *
     * @param organizationId 组织ID
     *
     * @return 产品选项列表
     */
    public List<OptionDTO> listOption(String organizationId) {
        return extProductMapper.getOptions(organizationId);
    }
}