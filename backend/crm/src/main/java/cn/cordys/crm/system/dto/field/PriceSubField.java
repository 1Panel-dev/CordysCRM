package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.SubField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName("SUB_PRICE")
@EqualsAndHashCode(callSuper = true)
public class PriceSubField extends SubField {}
