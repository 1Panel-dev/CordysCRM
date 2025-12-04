package cn.cordys.common.resolver.field;

import cn.cordys.common.utils.RegionUtils;
import cn.cordys.crm.system.dto.field.LocationField;

/**
 * @author song-cc-rock
 */
public class LocationResolver extends AbstractModuleFieldResolver<LocationField> {

    public static final String PCD = "PCD";
    public static final String PC = "PC";
    public static final String P = "P";
    public static final String C = "C";

    @Override
    public void validate(LocationField customField, Object value) {

    }


    @Override
    public Object trans2Value(LocationField locationField, String value) {
        return RegionUtils.codeToName(value);
    }

    @Override
    public Object text2Value(LocationField field, String text) {
        return RegionUtils.mapping(text, true);
    }
}
