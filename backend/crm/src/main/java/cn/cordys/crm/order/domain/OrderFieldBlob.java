package cn.cordys.crm.order.domain;

import cn.cordys.common.domain.BaseResourceSubField;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Table(name = "sales_order_field_blob")
public class OrderFieldBlob extends BaseResourceSubField {

}
