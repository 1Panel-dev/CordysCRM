package io.cordys.common.util;

import io.cordys.common.constants.MoveTypeEnum;
import io.cordys.common.constants.QuadFunction;
import io.cordys.common.dto.request.PosRequest;
import io.cordys.common.exception.GenericException;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServiceUtils {

    //用于排序的pos
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

    public static <T> void updatePosFieldByAsc(PosRequest request, Class<T> clazz, String userId, String resourceType,
                                               Function<String, T> selectByPrimaryKeyFunc,
                                               QuadFunction<String, Long, String, String, Long> getPrePosFunc,
                                               QuadFunction<String, Long, String, String, Long> getLastPosFunc,
                                               Consumer<T> updateByPrimaryKeySelectiveFuc) {
        Long pos;
        Long lastOrPrePos;
        try {
            Method getPos = clazz.getMethod("getPos");
            Method setId = clazz.getMethod("setId", String.class);
            Method setPos = clazz.getMethod("setPos", Long.class);

            // 获取移动的参考对象
            T target = selectByPrimaryKeyFunc.apply(request.getTargetId());

            if (target == null) {
                // 如果参考对象被删除，则不处理
                return;
            }

            Long targetPos = (Long) getPos.invoke(target);

            if (request.getMoveMode().equals(MoveTypeEnum.AFTER.name())) {
                // 追加到参考对象的之后
                pos = targetPos + POS_STEP;
                // ，因为是正序排，则查找比目标 order 大的一个order
                lastOrPrePos = getLastPosFunc.apply(request.getOrgId(), targetPos, userId, resourceType);
            } else {
                // 追加到前面
                pos = targetPos - POS_STEP;
                // 因为是正序排，则查找比目标 order 更小的一个order
                lastOrPrePos = getPrePosFunc.apply(request.getOrgId(), targetPos, userId, resourceType);
            }
            if (lastOrPrePos != null) {
                // 如果不是第一个或最后一个则取中间值
                pos = (targetPos + lastOrPrePos) / 2;
            }

            // 更新order值
            T updateObj = (T) clazz.getDeclaredConstructor().newInstance();
            setId.invoke(updateObj, request.getMoveId());
            setPos.invoke(updateObj, pos);
            updateByPrimaryKeySelectiveFuc.accept(updateObj);
        } catch (Exception e) {
            throw new GenericException("更新 pos 字段失败");
        }
    }

    public static <T> void updatePosFieldByDesc(PosRequest request, Class<T> clazz, String userId, String resourceType,
                                               Function<String, T> selectByPrimaryKeyFunc,
                                               QuadFunction<String, Long, String, String, Long> getPrePosFunc,
                                               QuadFunction<String, Long, String, String, Long> getLastPosFunc,
                                               Consumer<T> updateByPrimaryKeySelectiveFuc) {
        Long pos;
        Long lastOrPrePos;
        try {
            Method getPos = clazz.getMethod("getPos");
            Method setId = clazz.getMethod("setId", String.class);
            Method setPos = clazz.getMethod("setPos", Long.class);

            // 获取移动的参考对象
            T target = selectByPrimaryKeyFunc.apply(request.getTargetId());

            if (target == null) {
                // 如果参考对象被删除，则不处理
                return;
            }

            Long targetPos = (Long) getPos.invoke(target);

            if (request.getMoveMode().equals(MoveTypeEnum.AFTER.name())) {
                pos = targetPos - POS_STEP;
                // ，因为是降序排，则查找比目标 order 小的一个order
                lastOrPrePos = getPrePosFunc.apply(request.getOrgId(), targetPos, userId, resourceType);
            } else {
                pos = targetPos + POS_STEP;
                // 因为是降序排，则查找比目标 order 更大的一个order
                lastOrPrePos = getLastPosFunc.apply(request.getOrgId(), targetPos, userId, resourceType);
            }
            if (lastOrPrePos != null) {
                // 如果不是第一个或最后一个则取中间值
                pos = (targetPos + lastOrPrePos) / 2;
            }

            // 更新order值
            T updateObj = (T) clazz.getDeclaredConstructor().newInstance();
            setId.invoke(updateObj, request.getMoveId());
            setPos.invoke(updateObj, pos);
            updateByPrimaryKeySelectiveFuc.accept(updateObj);
        } catch (Exception e) {
            throw new GenericException("更新 pos 字段失败");
        }
    }

}
