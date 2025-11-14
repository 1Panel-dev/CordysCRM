package cn.cordys.common.resolver.field;

import cn.cordys.common.utils.IndustryUtils;
import cn.cordys.crm.system.dto.field.IndustryField;

/**
 * @author song-cc-rock
 */
public class IndustryResolver extends AbstractModuleFieldResolver<IndustryField> {

    @Override
    public void validate(IndustryField customField, Object value) {

    }

    @Override
    public Object trans2Value(IndustryField field, String value) {
        return IndustryUtils.mapping(value, false);
    }

    @Override
    public Object text2Value(IndustryField field, String text) {
        return IndustryUtils.mapping(text, true);
    }
}
