package io.cordys.crm.system.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.dto.request.ProductBatchEditRequest;
import io.cordys.crm.system.dto.request.ProductEditRequest;
import io.cordys.crm.system.dto.request.ProductPageRequest;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.dto.response.product.ProductGetResponse;
import io.cordys.crm.system.dto.response.product.ProductListResponse;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
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

        Map<String, List<BaseModuleFieldValue>> productFiledMap = productFieldService.getResourceFieldMap(productIds);

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
        return baseService.setCreateAndUpdateUserName(productGetResponse);
    }

    @OperationLog(module = LogModule.PRODUCT, type = LogType.ADD, operator = "{#userId}")
    public Product add(ProductEditRequest request, String userId, String orgId) {
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStatus(request.getStatus());
        product.setCreateTime(System.currentTimeMillis());
        product.setUpdateTime(System.currentTimeMillis());
        product.setUpdateUser(userId);
        product.setCreateUser(userId);
        product.setOrganizationId(orgId);
        product.setId(IDGenerator.nextStr());

        // 校验名称重复
        checkAddExist(product);

        productBaseMapper.insert(product);


        //保存自定义字段
        productFieldService.saveModuleField(product.getId(), orgId, userId, request.getModuleFields());

        // 添加日志上下文
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(product.getId())
                        .resourceName(product.getName())
                        .modifiedValue(product.getName())
                        .build()
        );
        return product;
    }

    @OperationLog(module = LogModule.PRODUCT, type = LogType.UPDATE, operator = "{#userId}")
    public Product update(ProductEditRequest request, String userId, String orgId) {
        if (StringUtils.isBlank(request.getId())) {
            throw new GenericException(Translator.get("product.id.empty"));
        }
        Product oldProduct = productBaseMapper.selectByPrimaryKey(request.getId());
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setUpdateTime(System.currentTimeMillis());
        product.setUpdateUser(userId);
        product.setOrganizationId(orgId);

        // 校验名称重复
        checkUpdateExist(product);
        productBaseMapper.update(product);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields(), orgId, userId);

        // 添加日志上下文
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(product.getId())
                        .resourceName(product.getName())
                        .originalValue(oldProduct)
                        .modifiedValue(product)
                        .build()
        );

        return productBaseMapper.selectByPrimaryKey(product.getId());
    }

    private void checkAddExist(Product product) {
        if (extProductMapper.checkAddExist(product)) {
            throw new GenericException(Translator.get("product.exist"));
        }
    }

    private void checkUpdateExist(Product product) {
        if (extProductMapper.checkUpdateExist(product)) {
            throw new GenericException(Translator.get("product.exist"));
        }
    }

    private void updateModuleField(String productId, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        productFieldService.deleteByResourceId(productId);
        // 再保存
        productFieldService.saveModuleField(productId, orgId, userId, moduleFields);
    }

    private void batchUpdateModuleField(List<String>  productIds, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        productFieldService.deleteByResourceIds(productIds);
        // 再保存
        productFieldService.saveModuleFieldByResourceIds(productIds, moduleFields);
    }

    @OperationLog(module = LogModule.PRODUCT, type = LogType.DELETE, resourceId = "{#id}")
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
        extProductMapper.updateProduct(request.getIds(),product);
        batchUpdateModuleField(request.getIds(),request.getModuleFields());
        for (Product oldProduct : products) {
            LogDTO logDTO = new LogDTO(oldProduct.getOrganizationId(), oldProduct.getId(), userId, LogType.UPDATE, LogModule.PRODUCT, oldProduct.getName());
            String oldStatus = getProductStatusName(oldProduct.getStatus());
            String newStatus = getProductStatusName(request.getStatus());
            logDTO.setOriginalValue(oldStatus);
            logDTO.setModifiedValue(newStatus);
            logService.add(logDTO);
        }
    }

    private String getProductStatusName(String status) {
        if (StringUtils.equalsIgnoreCase(status, "1")){
            return Translator.get("product.shelves");
        }
        else{
           return Translator.get("product.unShelves");
        }

    }

    /**
     * 批量删除产品
     *
     * @param ids 产品id集合
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
            LogDTO logDTO = new LogDTO(product.getOrganizationId(), product.getId(), userId, LogType.DELETE, LogModule.PRODUCT, product.getName());
            logDTO.setOriginalValue(product.getName());
            logs.add(logDTO);
        });
        logService.batchAdd(logs);
    }
}