package cn.cordys.crm.customer.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 客户联系人自定义属性大文本
 *
 * @author jianxing
 * @date 2025-02-27 14:43:46
 */
@Data
@Table(name = "customer_contact_field_blob")
public class CustomerContactFieldBlob extends BaseResourceField {}
