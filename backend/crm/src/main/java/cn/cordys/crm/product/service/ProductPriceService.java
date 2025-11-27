package cn.cordys.crm.product.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.ServiceUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.product.dto.request.ProductPriceAddRequest;
import cn.cordys.crm.product.dto.request.ProductPriceEditRequest;
import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.dto.response.ProductPriceGetResponse;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;
import cn.cordys.crm.product.mapper.ExtProductPriceMapper;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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
    private ProductPriceFieldService productPriceFieldService;
    @Resource
    private ExtProductPriceMapper extProductPriceMapper;

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
		ModuleFormConfigDTO priceFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), currentOrg);
        List<ProductPriceResponse> results = buildList(list, priceFormConfig);
        // 处理自定义字段选项
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
        baseService.handleAddLog(productPrice, request.getModuleFields());
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
		ModuleFormConfigDTO priceFormConf = moduleFormCacheService.getBusinessFormConfig(FormKey.PRICE.getKey(), price.getOrganizationId());
        // 处理自定义字段(包括详情附件)
		List<BaseModuleFieldValue> fieldValues = productPriceFieldService.getModuleFieldValuesByResourceId(id);
		moduleFormService.processBusinessFieldValues(priceDetail, fieldValues, priceFormConf);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(priceFormConf, priceDetail.getModuleFields());
        priceDetail.setOptionMap(optionMap);
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
     * 构建列表数据
     *
     * @param listData 列表数据
     * @return 列表数据
     */
    private List<ProductPriceResponse> buildList(List<ProductPriceResponse> listData, ModuleFormConfigDTO priceFormConfig) {
        // 查询列表数据的自定义字段
        Map<String, List<BaseModuleFieldValue>> dataFieldMap = productPriceFieldService.getResourceFieldMap(
                listData.stream().map(ProductPriceResponse::getId).toList(), true);
        // 列表项设置自定义字段&&用户名
		listData.forEach(item -> {
			List<BaseModuleFieldValue> fieldValues = dataFieldMap.get(item.getId());
			moduleFormService.processBusinessFieldValues(item, fieldValues, priceFormConfig);
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
     * @return 下一个排序值
     */
    public Long getNextOrder(String orgId) {
        Long pos = extProductPriceMapper.getPos(orgId);
        return (pos == null ? 0 : pos) + ServiceUtils.POS_STEP;
    }
}
