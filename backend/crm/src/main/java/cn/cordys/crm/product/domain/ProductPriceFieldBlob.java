package cn.cordys.crm.product.domain;

import cn.cordys.common.domain.BaseResourceSubField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 产品价格表自定义属性 (部分大文本)
 *
 * @author song-cc-rock
 */
@Data
@Table(name = "product_price_field_blob")
public class ProductPriceFieldBlob extends BaseResourceSubField {}
