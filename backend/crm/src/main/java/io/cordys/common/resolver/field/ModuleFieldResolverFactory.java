package io.cordys.common.resolver.field;

import io.cordys.crm.system.constants.FieldType;

import java.util.HashMap;

/**
 * @author jainxing
 */
public class ModuleFieldResolverFactory {

    private static final HashMap<String, AbstractModuleFieldResolver> resolverMap = new HashMap<>();

    private static final DefaultModuleFieldResolver defaultModuleFieldResolver = new DefaultModuleFieldResolver();

    public static final String MULTI_SELECT = "MULTI_SELECT";
    public static final String MULTI_MEMBER = "MULTI_MEMBER";
    public static final String MULTI_DEPARTMENT = "MULTI_DEPARTMENT";

    static {
        resolverMap.put(FieldType.SELECT.name(), new SelectResolver());
        resolverMap.put(FieldType.RADIO.name(), new RadioResolver());

        resolverMap.put(FieldType.CHECKBOX.name(), new CheckBoxResolver());

        resolverMap.put(FieldType.INPUT.name(), new TextResolver());
        resolverMap.put(FieldType.TEXTAREA.name(), new TextResolver());

        resolverMap.put(FieldType.MULTIPLE_INPUT.name(), new MultipleTextResolver());

        resolverMap.put(FieldType.DATE_TIME.name(), new DateTimeResolver());

        resolverMap.put(FieldType.MEMBER.name(), new MemberResolver());

        resolverMap.put(FieldType.INPUT_NUMBER.name(), new NumberResolver());
        resolverMap.put(FieldType.PICTURE.name(), new PictureResolver());

        resolverMap.put(MULTI_SELECT, new MultipleSelectResolver());
        resolverMap.put(FieldType.DATA_SOURCE.name(), new DatasourceResolver());
        resolverMap.put(MULTI_MEMBER, new MultipleMemberResolver());
        resolverMap.put(FieldType.DEPARTMENT.name(), new DepartmentResolver());
        resolverMap.put(MULTI_DEPARTMENT, new MultipleDepartmentResolver());
    }

    public static AbstractModuleFieldResolver getResolver(String type) {
        AbstractModuleFieldResolver moduleFieldResolver = resolverMap.get(type);
        return moduleFieldResolver == null ? defaultModuleFieldResolver : moduleFieldResolver;
    }
}
