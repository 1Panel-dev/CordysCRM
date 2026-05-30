package cn.cordys.common.utils;

import java.lang.reflect.Field;

public class BeanCopyUtils {

    public static void fillEmptyFields(Object target, Object source) {
        if (target == null || source == null) {
            return;
        }

        Field[] fields = target.getClass().getDeclaredFields();

        for (Field field : fields) {
            try {
                field.setAccessible(true);

                Object targetValue = field.get(target);
                Object sourceValue = field.get(source);

                // target为空，并且source有值，才赋值
                if ((targetValue == null || "".equals(targetValue))
                        && sourceValue != null
                ) {
                    field.set(target, sourceValue);
                }

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}