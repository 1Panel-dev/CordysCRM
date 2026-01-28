package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author song-cc-rock
 */
@Data
@JsonTypeName(value = "INDUSTRY")
@EqualsAndHashCode(callSuper = true)
public class IndustryField extends BaseField {}
