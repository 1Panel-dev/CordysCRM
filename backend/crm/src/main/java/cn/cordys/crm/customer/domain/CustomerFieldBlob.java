package cn.cordys.crm.customer.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 客户自定义属性大文本
 *
 * @author jianxing
 */
@Data
@Table(name = "customer_field_blob")
public class CustomerFieldBlob extends BaseResourceField {
}
