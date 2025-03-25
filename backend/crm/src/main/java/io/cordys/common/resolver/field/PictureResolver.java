package io.cordys.common.resolver.field;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.PictureField;
import io.cordys.crm.system.service.PicService;

import java.util.List;

/**
 * @author song-cc-rock
 */
public class PictureResolver extends AbstractModuleFieldResolver<PictureField>{

	@Override
	public void validate(PictureField customField, Object value) {

	}

	@Override
	public Object parse2Value(PictureField selectField, String value) {
		List<String> ids = JSON.parseArray(value, String.class);
		PicService picService = CommonBeanFactory.getBean(PicService.class);
		if (picService == null) {
			return ids;
		}
		return picService.getPicInfos(ids);
	}
}
