package cn.cordys.crm.system.dto;

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
public class FormLinkFillDTO<T> {

	/**
	 * 主表实体
	 */
	private T entity;
	/**
	 * 自定义字段属性值
	 */
	private List<BaseModuleFieldValue> fields;
}
