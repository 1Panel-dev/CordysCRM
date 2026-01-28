package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.SubField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品子表字段
 *
 * @author song-cc-rock
 */
@Data
@JsonTypeName(value = "SUB_PRODUCT")
@EqualsAndHashCode(callSuper = true)
public class ProductSubField extends SubField {}
