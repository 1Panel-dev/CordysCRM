package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogDTO;
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
import cn.cordys.crm.system.domain.Product;
import cn.cordys.crm.system.dto.request.ProductBatchEditRequest;
import cn.cordys.crm.system.dto.request.ProductEditRequest;
import cn.cordys.crm.system.dto.request.ProductPageRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.product.ProductGetResponse;
import cn.cordys.crm.system.dto.response.product.ProductListResponse;
import cn.cordys.crm.system.mapper.ExtProductMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public ProductGetResponse get(String id) {
        Product product = productBaseMapper.selectByPrimaryKey(id);
        ProductGetResponse productGetResponse = BeanUtils.copyBean(new ProductGetResponse(), product);

        // 获取模块字段
        List<BaseModuleFieldValue> productFields = productFieldService.getModuleFieldValuesByResourceId(id);

        productGetResponse.setModuleFields(productFields);
        ModuleFormConfigDTO productFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRODUCT.getKey(), product.getOrganizationId());
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(productFormConfig, productFields);
        productGetResponse.setOptionMap(optionMap);
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

    public void batchUpdate(ProductBatchEditRequest request, String userId, String orgId) {
        if (CollectionUtils.isEmpty(request.getIds())) {
            return;
        }
        // 批量更新产品
        List<Product> products = extProductMapper.listByIds(request.getIds());
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setUpdateTime(System.currentTimeMillis());
        product.setUpdateUser(userId);
        product.setOrganizationId(orgId);
        extProductMapper.updateProduct(request.getIds(), product);
        // batchUpdateModuleField(request.getIds(),request.getModuleFields());
        List<LogDTO> logDTOList = getLogDTOList(request, userId, products);
        logService.batchAdd(logDTOList);
    }

    @NotNull
    private static List<LogDTO> getLogDTOList(ProductBatchEditRequest request, String userId, List<Product> products) {
        List<LogDTO> logDTOList = new ArrayList<>();
        //目前只记录批量上下架
        for (Product oldProduct : products) {
            LogDTO logDTO = new LogDTO(oldProduct.getOrganizationId(), oldProduct.getId(), userId, LogType.UPDATE, LogModule.PRODUCT_MANAGEMENT, oldProduct.getName());
            Map<String, String> oldMap = new HashMap<>();
            oldMap.put("status", oldProduct.getStatus());
            Map<String, String> newMap = new HashMap<>();
            newMap.put("status", request.getStatus());
            logDTO.setOriginalValue(oldMap);
            logDTO.setModifiedValue(newMap);
            logDTOList.add(logDTO);
        }
        return logDTOList;
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
        if (products.size() > 20) {
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
}