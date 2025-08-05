package io.cordys.crm.system.dto.field.base;

import java.util.List;

public interface HasOption {

	/**
	 * 获取选项集合
	 * @return 选项集合
	 */
	List<OptionProp> getOptions();
}
