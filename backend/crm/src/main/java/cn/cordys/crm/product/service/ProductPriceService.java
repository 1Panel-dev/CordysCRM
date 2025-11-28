package cn.cordys.crm.product.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
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
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.enums.CellExtraTypeEnum;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Service
public class ProductPriceService {

    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
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

    /**
     * 价格列表
     *
     * @param request    请求参数
     * @param currentOrg 当前组织
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
        return PageUtils.setPageInfoWithOption(page, results, optionMap);
    }

    /**
     * 新增价格表
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param currentOrg  当前组织
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
        baseService.handleAddLogWithSubTable(productPrice, request.getModuleFields(), "products", Translator.get("products_info"));
        return productPrice;
    }

    /**
     * 修改价格表
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param currentOrg  当前组织
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
        baseService.handleUpdateLogWithSubTable(oldPrice, productPrice, originFields, request.getModuleFields(), request.getId(), productPrice.getName(), "products", Translator.get("products_info"));
        return productPriceMapper.selectByPrimaryKey(request.getId());
    }

    /**
     * 价格表详情
     *
     * @param id 价格表ID
     * @return 价格表详情
     */
    public ProductPriceGetResponse get(String id) {
        ProductPrice price = productPriceMapper.selectByPrimaryKey(id);
        if (price == null) {
            throw new GenericException(Translator.get("product.price.not.exist"));
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
        ProductPrice price = productPriceMapper.selectByPrimaryKey(id);
        if (price == null) {
            throw new GenericException(Translator.get("product.price.not.exist"));
        }
        productPriceMapper.deleteByPrimaryKey(id);
        productPriceFieldService.deleteByResourceId(id);
        // 添加日志上下文
        OperationLogContext.setResourceName(price.getName());
    }

	/**
	 * 批量更新价格表
	 * @param request 请求参数
	 * @param currentUser 当前用户
	 * @param currentOrg 当前组织
	 */
	public void batchUpdate(ResourceBatchEditRequest request, String currentUser, String currentOrg) {
		BaseField field = productPriceFieldService.getAndCheckField(request.getFieldId(), currentOrg);
		List<ProductPrice> prices = productPriceMapper.selectByIds(request.getIds());
		productPriceFieldService.batchUpdate(request, field, prices, ProductPrice.class,
				LogModule.PRODUCT_PRICE_MANAGEMENT, extProductPriceMapper::batchUpdate, currentUser, currentOrg);
	}

	/**
	 * 下载导入的模板
	 * @param response 响应
	 */
	public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
		new EasyExcelExporter().exportMultiSheetTplWithSharedHandler(response,
				moduleFormService.getCustomImportHeads(FormKey.PRICE.getKey(), currentOrg),
				Translator.get("product.price.import_tpl.name"),
				Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
				new CustomTemplateWriteHandler(moduleFormService.getCustomImportFields(FormKey.PRICE.getKey(), currentOrg)),
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
			List<BaseField> fields = moduleFormService.getCustomImportFields(FormKey.PRICE.getKey(), currentOrg);
			CustomFieldMergeCellEventListener mergeCellEventListener = new CustomFieldMergeCellEventListener();
			FastExcelFactory.read(file.getInputStream(), mergeCellEventListener).extraRead(CellExtraTypeEnum.MERGE)
					.headRowNumber(moduleFormService.supportSubHead(fields) ? 2 : 1).ignoreEmptyRow(true).sheet().doRead();
			CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "product_price", "product_price_field", currentOrg,
					mergeCellEventListener.getMergeCellMap());
			FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(moduleFormService.supportSubHead(fields) ? 2 : 1).ignoreEmptyRow(true).sheet().doRead();
			return ImportResponse.builder().errorMessages(eventListener.getErrList())
					.successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
		} catch (Exception e) {
			LogUtils.error("price import pre-check error: {}", e.getMessage());
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
			List<BaseField> fields = moduleFormService.getCustomImportFields(FormKey.PRICE.getKey(), currentOrg);
			CustomFieldImportEventListener<ProductPrice> eventListener = getPriceEventListener(currentOrg, currentUser, fields);
			FastExcelFactory.read(file.getInputStream(), eventListener).extraRead(CellExtraTypeEnum.MERGE)
					.headRowNumber(moduleFormService.supportSubHead(fields) ? 2 : 1).ignoreEmptyRow(true).sheet().doRead();
			return ImportResponse.builder().errorMessages(eventListener.getErrList())
					.successCount(eventListener.getDataList().size()).failCount(eventListener.getErrList().size()).build();
		} catch (Exception e) {
			LogUtils.error("product price import error: ", e.getMessage());
			throw new GenericException(e.getMessage());
		}
	}

	/**
	 * 价格表导入监听器
	 * @param currentOrg 当前组织
	 * @param currentUser 当前用户
	 * @param fields 自定义字段集合
	 * @return 导入监听器
	 */
	private CustomFieldImportEventListener<ProductPrice> getPriceEventListener(String currentOrg, String currentUser, List<BaseField> fields) {
		CustomImportAfterDoConsumer<ProductPrice, BaseResourceSubField> afterDo = (prices, priceFields, priceFieldBlobs) -> {
			List<LogDTO> logs = new ArrayList<>();
			prices.forEach(price -> {
				price.setPos(getNextOrder(currentOrg));
				logs.add(new LogDTO(currentOrg, price.getId(), currentUser, LogType.ADD, LogModule.PRODUCT_PRICE_MANAGEMENT, price.getName()));
			});
			productPriceMapper.batchInsert(prices);
			productPriceFieldMapper.batchInsert(priceFields.stream().map(field -> BeanUtils.copyBean(new ProductPriceField(), field)).toList());
			productPriceFieldBlobMapper.batchInsert(priceFieldBlobs.stream().map(field -> BeanUtils.copyBean(new ProductPriceFieldBlob(), field)).toList());
			// record logs
			logService.batchAdd(logs);
		};
		return new CustomFieldImportEventListener<>(fields, ProductPrice.class, currentOrg, currentUser, "product_price_field", afterDo, 2000);
	}

	/**
     * 构建列表数据
     *
     * @param listData 列表数据
     * @return 列表数据
     */
    private List<ProductPriceResponse> buildList(List<ProductPriceResponse> listData) {
        // 查询列表数据的自定义字段
        Map<String, List<BaseModuleFieldValue>> dataFieldMap = productPriceFieldService.getResourceFieldMap(
                listData.stream().map(ProductPriceResponse::getId).toList(), true);
        // 列表项设置自定义字段&&用户名
		listData.forEach(item -> item.setModuleFields(dataFieldMap.get(item.getId())));
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
     * @return 下一个排序值
     */
    public Long getNextOrder(String orgId) {
        Long pos = extProductPriceMapper.getPos(orgId);
        return (pos == null ? 0 : pos) + ServiceUtils.POS_STEP;
    }
}
