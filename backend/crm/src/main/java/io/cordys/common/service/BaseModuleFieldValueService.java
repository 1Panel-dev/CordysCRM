package io.cordys.common.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import io.cordys.mybatis.lambda.XFunction;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资源与模块字段值的公共处理类
 *
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseModuleFieldValueService {

    /**
     * 获取资源和模块字段的 map
     *
     * @param resourceIds
     * @param resourceIdGetFunc
     * @param <T>
     * @return
     */
    public <T extends BaseModuleFieldValue> Map<String, List<T>> getResourceFiledMap(List<String> resourceIds,
                                                                                     XFunction<T, String> resourceIdGetFunc,
                                                                                     BaseMapper<T> mapper) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            return Map.of();
        }
        List<T> customerFields = getModuleFieldValuesByResourceIds(resourceIds, resourceIdGetFunc, mapper);
        return customerFields.stream()
                .collect(Collectors.groupingBy(resourceIdGetFunc));
    }

    /**
     * 查询指定资源的模块字段值
     *
     * @param resourceIds
     * @param resourceIdGetFunc
     * @param <T>
     * @return
     */
    public <T extends BaseModuleFieldValue> List<T> getModuleFieldValuesByResourceIds(List<String> resourceIds,
                                                                                      XFunction<T, String> resourceIdGetFunc,
                                                                                      BaseMapper<T> mapper) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(resourceIdGetFunc, resourceIds);
        return mapper.selectListByLambda(wrapper);
    }

}