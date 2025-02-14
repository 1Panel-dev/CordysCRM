package io.cordys.crm.system.service;

import io.cordys.common.request.ModuleFieldValueDTO;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.domain.ProductField;
import io.cordys.crm.system.dto.request.ProductEditRequest;
import io.cordys.crm.system.dto.response.ProductListResponse;
import io.cordys.crm.system.dto.request.ProductPageRequest;
import io.cordys.crm.system.dto.response.ProductGetResponse;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProductService {

    @Resource
    private BaseMapper<Product> productBaseMapper;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private BaseMapper<ProductField> productFieldBaseMapper;

    @Resource
    private BaseService baseService;

    public List<ProductListResponse> list(ProductPageRequest request, String orgId) {
        List<ProductListResponse> list = extProductMapper.list(request, orgId);
        return buildListData(list);
    }

    private List<ProductListResponse> buildListData(List<ProductListResponse> list) {
        List<String> productIds = list.stream().map(ProductListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<ProductField>> productFiledMap = getProductFiledMap(productIds);
        list.forEach(productListResponse -> {
            // 获取自定义字段
            List<ProductField> productFields = productFiledMap.get(productListResponse.getId());
            if (CollectionUtils.isNotEmpty(productFields)) {
                List<ModuleFieldValueDTO> moduleFieldValues = getModuleFieldValues(productFields);
                productListResponse.setModuleFields(moduleFieldValues);
            }
        });

        return baseService.setCreateAndUpdateUserName(list);
    }

    private List<ModuleFieldValueDTO> getModuleFieldValues(List<ProductField> productFields) {
        List<ModuleFieldValueDTO> moduleFieldValues = productFields.stream().map(productField -> {
            ModuleFieldValueDTO moduleFieldValue = new ModuleFieldValueDTO();
            moduleFieldValue.setId(productField.getFieldId());
            moduleFieldValue.setValue(productField.getFieldValue());
            return moduleFieldValue;
        }).collect(Collectors.toList());
        return moduleFieldValues;
    }

    public Map<String, List<ProductField>> getProductFiledMap(List<String> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Map.of();
        }
        List<ProductField> productFields = getProductFieldsByProductIds(productIds);
        return productFields.stream().collect(Collectors.groupingBy(ProductField::getProductId));
    }

    private List<ProductField> getProductFieldsByProductIds(List<String> productIds) {
        LambdaQueryWrapper<ProductField> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ProductField::getProductId, productIds);
        return productFieldBaseMapper.selectListByLambda(wrapper);
    }

    public ProductGetResponse get(String id) {
        Product product = productBaseMapper.selectByPrimaryKey(id);
        ProductGetResponse productGetResponse = BeanUtils.copyBean(new ProductGetResponse(), product);

        // 获取模块字段
        List<ProductField> productFields = getProductFieldsByProductIds(List.of(id));
        List<ModuleFieldValueDTO> moduleFieldValues = getModuleFieldValues(productFields);
        productGetResponse.setModuleFields(moduleFieldValues);
        return baseService.setCreateAndUpdateUserName(productGetResponse);
    }

    public Product add(ProductEditRequest request, String userId, String orgId) {
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setCreateTime(System.currentTimeMillis());
        product.setUpdateTime(System.currentTimeMillis());
        product.setUpdateUser(userId);
        product.setCreateUser(userId);
        product.setOrganizationId(orgId);
        product.setId(IDGenerator.nextStr());
        productBaseMapper.insert(product);
        // 校验名称重复 todo
//        checkAddExist(customer);

        //保存自定义字段
        saveModuleField(product.getId(), request.getModuleFields());
        return product;
    }

    /**
     *
     * @param moduleFieldValues
     */
    public void saveModuleField(String productId, List<ModuleFieldValueDTO> moduleFieldValues) {
        if (CollectionUtils.isEmpty(moduleFieldValues)) {
            return;
        }
        //  todo 字段的校验
        List<ProductField> customerFields = moduleFieldValues.stream().map(custom -> {
            ProductField productField = new ProductField();
            productField.setProductId(productId);
            productField.setFieldId(custom.getId());
            productField.setFieldValue(custom.getValue());
            productField.setId(IDGenerator.nextStr());
            return productField;
        }).toList();
        productFieldBaseMapper.batchInsert(customerFields);
    }

    public Product update(ProductEditRequest request, String userId) {
        Product product = BeanUtils.copyBean(new Product(), request);
        product.setUpdateTime(System.currentTimeMillis());
        product.setUpdateUser(userId);

        // 校验名称重复 todo
//        checkUpdateExist(customer);
        productBaseMapper.update(product);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields());
        return productBaseMapper.selectByPrimaryKey(product.getId());
    }

    private void updateModuleField(String productId, List<ModuleFieldValueDTO> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        deleteProductFieldByProductId(productId);
        // 再保存
        saveModuleField(productId, moduleFields);
    }

    private void deleteProductFieldByProductId(String productId) {
        ProductField example = new ProductField();
        example.setProductId(productId);
        productFieldBaseMapper.delete(example);
    }


    public void delete(String id) {
        // 删除产品
        productBaseMapper.deleteByPrimaryKey(id);
        // 删除产品模块字段
        deleteProductFieldByProductId(id);
    }
}