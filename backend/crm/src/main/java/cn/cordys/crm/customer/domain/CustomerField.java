package cn.cordys.crm.customer.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 客户自定义属性
 *
 * @author jianxing
 */
@Data
@Table(name = "customer_field")
public class CustomerField extends BaseResourceField {
}
