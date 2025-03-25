package io.cordys.crm.system.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.constants.BusinessModuleField;
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
import io.cordys.crm.customer.dto.response.CustomerListResponse;
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
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.PRODUCT.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ProductListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

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
        return product;
    }

    public Product update(ProductEditRequest request, String userId, String orgId) {
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setUpdateTime(System.currentTimeMillis());
        product.setUpdateUser(userId);
        product.setOrganizationId(orgId);

        // 校验名称重复
        checkUpdateExist(product);
        productBaseMapper.update(product);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields(), orgId, userId);
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

    public void delete(String id) {
        // 删除产品
        productBaseMapper.deleteByPrimaryKey(id);
        // 删除产品模块字段
        productFieldService.deleteByResourceId(id);
    }

    public void batchUpdate(ProductBatchEditRequest request, String userId, String orgId) {
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setUpdateTime(System.currentTimeMillis());
        product.setUpdateUser(userId);
        product.setOrganizationId(orgId);
        extProductMapper.updateProduct(request.getIds(),product);
        batchUpdateModuleField(request.getIds(),request.getModuleFields());
    }

    public void batchDelete(List<String> ids, String userId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Product::getId, ids);
        List<Product> products = productBaseMapper.selectListByLambda(wrapper);
        productBaseMapper.deleteByIds(ids);
        productFieldService.deleteByResourceIds(ids);
        List<LogDTO> logs = new ArrayList<>();
        products.forEach(product -> {
            LogDTO logDTO = new LogDTO(product.getOrganizationId(), product.getId(), userId, LogType.DELETE, LogModule.OPPORTUNITY, product.getName());
            logDTO.setOriginalValue(product);
            logs.add(logDTO);
        });
        logService.batchAdd(logs);
    }
}