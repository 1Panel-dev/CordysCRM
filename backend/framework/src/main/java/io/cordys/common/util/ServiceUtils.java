package io.cordys.common.util;

import io.cordys.common.constants.MoveTypeEnum;
import io.cordys.common.constants.QuadFunction;
import io.cordys.common.dto.request.PosRequest;
import io.cordys.common.exception.GenericException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServiceUtils {

    // 用于排序的pos
    public static final int POS_STEP = 4096;

    /**
     * 保存资源名称，在处理 NOT_FOUND 异常时，拼接资源名称
     */
    private static final ThreadLocal<String> resourceName = new ThreadLocal<>();

    public static String getResourceName() {
        return resourceName.get();
    }

    public static void clearResourceName() {
        resourceName.remove();
    }

    // 反射元数据缓存，减少重复反射开销
    private static final Map<Class<?>, Accessors> ACCESSOR_CACHE = new ConcurrentHashMap<>();

    public static <T> void updatePosFieldByAsc(
            PosRequest request,
            Class<T> clazz,
            String userId,
            String resourceType,
            Function<String, T> selectByPrimaryKeyFunc,
            QuadFunction<String, Long, String, String, Long> getPrePosFunc,
            QuadFunction<String, Long, String, String, Long> getLastPosFunc,
            Consumer<T> updateByPrimaryKeySelectiveFuc) {
        updatePosField(request, clazz, userId, resourceType, selectByPrimaryKeyFunc, getPrePosFunc, getLastPosFunc, updateByPrimaryKeySelectiveFuc, true);
    }

    public static <T> void updatePosFieldByDesc(
            PosRequest request,
            Class<T> clazz,
            String userId,
            String resourceType,
            Function<String, T> selectByPrimaryKeyFunc,
            QuadFunction<String, Long, String, String, Long> getPrePosFunc,
            QuadFunction<String, Long, String, String, Long> getLastPosFunc,
            Consumer<T> updateByPrimaryKeySelectiveFuc) {
        updatePosField(request, clazz, userId, resourceType, selectByPrimaryKeyFunc, getPrePosFunc, getLastPosFunc, updateByPrimaryKeySelectiveFuc, false);
    }

    private static <T> void updatePosField(
            PosRequest request,
            Class<T> clazz,
            String userId,
            String resourceType,
            Function<String, T> selectByPrimaryKeyFunc,
            QuadFunction<String, Long, String, String, Long> getPrePosFunc,
            QuadFunction<String, Long, String, String, Long> getLastPosFunc,
            Consumer<T> updateByPrimaryKeySelectiveFuc,
            boolean asc) {
        try {
            Accessors acc = getAccessors(clazz);

            // 获取移动的参考对象
            T target = selectByPrimaryKeyFunc.apply(request.getTargetId());
            if (target == null) {
                // 如果参考对象被删除，则不处理
                return;
            }

            Long targetPos = (Long) acc.getPos.invoke(target);
            boolean moveAfter = MoveTypeEnum.AFTER.name().equals(request.getMoveMode());

            Long neighborPos;
            long pos;

            if (asc) {
                if (moveAfter) {
                    pos = targetPos + POS_STEP;
                    neighborPos = getLastPosFunc.apply(request.getOrgId(), targetPos, userId, resourceType);
                } else {
                    pos = targetPos - POS_STEP;
                    neighborPos = getPrePosFunc.apply(request.getOrgId(), targetPos, userId, resourceType);
                }
            } else {
                if (moveAfter) {
                    pos = targetPos - POS_STEP;
                    neighborPos = getPrePosFunc.apply(request.getOrgId(), targetPos, userId, resourceType);
                } else {
                    pos = targetPos + POS_STEP;
                    neighborPos = getLastPosFunc.apply(request.getOrgId(), targetPos, userId, resourceType);
                }
            }

            if (neighborPos != null) {
                // 如果不是第一个或最后一个则取中间值
                pos = (targetPos + neighborPos) / 2;
            }

            @SuppressWarnings("unchecked")
            T updateObj = (T) acc.newInstance();
            acc.setId.invoke(updateObj, request.getMoveId());
            acc.setPos.invoke(updateObj, pos);
            updateByPrimaryKeySelectiveFuc.accept(updateObj);
        } catch (Exception e) {
            throw new GenericException("更新 pos 字段失败: " + e.getMessage());
        }
    }

    private static Accessors getAccessors(Class<?> clazz) {
        return ACCESSOR_CACHE.computeIfAbsent(clazz, c -> {
            try {
                Method getPos = c.getMethod("getPos");
                Method setId = c.getMethod("setId", String.class);
                Method setPos = c.getMethod("setPos", Long.class);
                Constructor<?> ctor = c.getDeclaredConstructor();
                if (!ctor.canAccess(null)) {
                    ctor.setAccessible(true);
                }
                return new Accessors(getPos, setId, setPos, ctor);
            } catch (Exception e) {
                throw new GenericException("初始化反射元数据失败: " + e.getMessage());
            }
        });
    }

    private record Accessors(Method getPos, Method setId, Method setPos, Constructor<?> ctor) {

        Object newInstance() throws Exception {
            return ctor.newInstance();
        }
    }
}