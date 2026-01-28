package cn.cordys.crm.product.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.product.domain.ProductPriceField;
import cn.cordys.crm.product.domain.ProductPriceFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProductPriceFieldService
    extends BaseResourceFieldService<ProductPriceField, ProductPriceFieldBlob> {

  @Resource private BaseMapper<ProductPriceField> productPriceFieldMapper;
  @Resource private BaseMapper<ProductPriceFieldBlob> productPriceFieldBlobMapper;

  @Override
  protected String getFormKey() {
    return FormKey.PRICE.getKey();
  }

  @Override
  protected BaseMapper<ProductPriceField> getResourceFieldMapper() {
    return productPriceFieldMapper;
  }

  @Override
  protected BaseMapper<ProductPriceFieldBlob> getResourceFieldBlobMapper() {
    return productPriceFieldBlobMapper;
  }
}
