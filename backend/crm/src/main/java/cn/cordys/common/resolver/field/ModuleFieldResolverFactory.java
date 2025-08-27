package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.constants.FieldType;

import java.util.HashMap;

/**
 * @author jainxing
 */
public class ModuleFieldResolverFactory {

    private static final HashMap<String, AbstractModuleFieldResolver> resolverMap = new HashMap<>();

    private static final DefaultModuleFieldResolver defaultModuleFieldResolver = new DefaultModuleFieldResolver();

    static {
        resolverMap.put(FieldType.RADIO.name(), new RadioResolver());

        resolverMap.put(FieldType.CHECKBOX.name(), new CheckBoxResolver());

        resolverMap.put(FieldType.INPUT.name(), new TextResolver());
        resolverMap.put(FieldType.TEXTAREA.name(), new TextResolver());

        resolverMap.put(FieldType.INPUT_MULTIPLE.name(), new TextMultipleResolver());

        resolverMap.put(FieldType.DATE_TIME.name(), new DateTimeResolver());

        resolverMap.put(FieldType.INPUT_NUMBER.name(), new NumberResolver());
        resolverMap.put(FieldType.PICTURE.name(), new PictureResolver());

        resolverMap.put(FieldType.SELECT.name(), new SelectResolver());
        resolverMap.put(FieldType.SELECT_MULTIPLE.name(), new SelectMultipleResolver());

        resolverMap.put(FieldType.MEMBER.name(), new MemberResolver());
        resolverMap.put(FieldType.MEMBER_MULTIPLE.name(), new MemberMultipleResolver());

        resolverMap.put(FieldType.DATA_SOURCE.name(), new DatasourceResolver());
        resolverMap.put(FieldType.DATA_SOURCE_MULTIPLE.name(), new DatasourceMultipleResolver());

        resolverMap.put(FieldType.DEPARTMENT.name(), new DepartmentResolver());
        resolverMap.put(FieldType.DEPARTMENT_MULTIPLE.name(), new DepartmentMultipleResolver());

        resolverMap.put(FieldType.LOCATION.name(), new LocationResolver());
        resolverMap.put(FieldType.PHONE.name(), new PhoneResolver());
    }

    public static AbstractModuleFieldResolver getResolver(String type) {
        AbstractModuleFieldResolver moduleFieldResolver = resolverMap.get(type);
        return moduleFieldResolver == null ? defaultModuleFieldResolver : moduleFieldResolver;
    }
}
