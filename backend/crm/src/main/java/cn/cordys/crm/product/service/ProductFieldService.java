package cn.cordys.crm.product.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.product.domain.ProductField;
import cn.cordys.crm.product.domain.ProductFieldBlob;
import cn.cordys.mybatis.BaseMapper;
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
  @Resource private BaseMapper<ProductField> productFieldMapper;
  @Resource private BaseMapper<ProductFieldBlob> productFieldBlobMapper;

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
