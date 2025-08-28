package cn.cordys.crm.system.dto.form;

import cn.cordys.common.domain.BaseModuleFieldValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormLinkFill<T> {

	/**
	 * 主表实体
	 */
	private T entity;
	/**
	 * 自定义字段属性值
	 */
	private List<BaseModuleFieldValue> fields;
}
