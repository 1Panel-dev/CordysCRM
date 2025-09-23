package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 产品自定义属性大文本
 *
 * @author jianxing
 * @date 2025-02-27 14:43:46
 */
@Data
@Table(name = "product_field_blob")
public class ProductFieldBlob extends BaseResourceField {
}
