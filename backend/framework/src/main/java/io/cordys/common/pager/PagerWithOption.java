package io.cordys.common.pager;

import io.cordys.common.dto.OptionDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PagerWithOption<T> extends Pager<T> {

	/**
	 * 选项集合
	 */
	private Map<String, List<OptionDTO>> optionMap;
}
