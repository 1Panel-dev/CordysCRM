package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 产品子表字段
 * @author song-cc-rock
 */
@Data
@JsonTypeName(value = "PRODUCT_TABLE")
@EqualsAndHashCode(callSuper = true)
public class ProductSubField extends SubField {

}
