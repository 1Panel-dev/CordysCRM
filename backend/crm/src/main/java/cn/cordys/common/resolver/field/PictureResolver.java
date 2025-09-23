package cn.cordys.common.resolver.field;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.dto.field.PictureField;

/**
 * @author song-cc-rock
 */
public class PictureResolver extends AbstractModuleFieldResolver<PictureField> {

    @Override
    public void validate(PictureField customField, Object value) {

    }

    @Override
    public Object parse2Value(PictureField selectField, String value) {
        return JSON.parseArray(value, String.class);
    }
}
