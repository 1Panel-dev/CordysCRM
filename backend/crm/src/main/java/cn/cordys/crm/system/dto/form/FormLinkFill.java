package cn.cordys.crm.system.dto.form;

import cn.cordys.common.domain.BaseModuleFieldValue;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormLinkFill<T> {

  /** 主表实体 */
  private T entity;

  /** 自定义字段属性值 */
  private List<BaseModuleFieldValue> fields;
}
