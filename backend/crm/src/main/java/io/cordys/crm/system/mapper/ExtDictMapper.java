package io.cordys.crm.system.mapper;

import io.cordys.crm.system.domain.DictConfig;
import org.apache.ibatis.annotations.Param;

public interface ExtDictMapper {

	/**
	 * 更新模块配置
	 * @param config 配置
	 */
	void updateModuleConfig(@Param("config")DictConfig config);
}
