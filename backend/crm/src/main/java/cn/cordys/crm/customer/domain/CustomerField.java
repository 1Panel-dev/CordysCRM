package cn.cordys.crm.customer.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 客户自定义属性
 *
 * @author jianxing
 * @date 2025-02-10 18:12:46
 */
@Data
@Table(name = "customer_field")
public class CustomerField extends BaseResourceField {
}
