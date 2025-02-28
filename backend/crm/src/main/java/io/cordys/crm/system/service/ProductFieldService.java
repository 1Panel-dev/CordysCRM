package io.cordys.crm.system.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.service.BaseResourceFieldService;
import io.cordys.crm.system.domain.ProductField;
import io.cordys.crm.system.domain.ProductFieldBlob;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProductFieldService extends BaseResourceFieldService<ProductField, ProductFieldBlob> {
    @Resource
    private BaseMapper<ProductField> productFieldMapper;
    @Resource
    private BaseMapper<ProductFieldBlob> productFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.PRODUCT.getKey();
    }

    @Override
    protected BaseMapper<ProductField> getResourceFieldMapper() {
        return productFieldMapper;
    }

    @Override
    protected BaseMapper<ProductFieldBlob> getResourceFieldBlobMapper() {
        return productFieldBlobMapper;
    }
}